/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.writer

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.ext.RoomTypeNames
import androidx.room.processor.DaoProcessor
import androidx.room.testing.context
import com.google.common.truth.StringSubject
import createVerifierFromEntitiesAndViews
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Tests that we generate the right calls for default method implementations.
 *
 * The assertions in these tests are more for visual inspection. The real test is that this code
 * compiles properly because when we generate the wrong code, it won't compile :).
 *
 * For Java default method tests, we have DefaultDaoMethodsTest in TestApp.
 */
@RunWith(Parameterized::class)
class DefaultsInDaoTest(
    private val jvmDefaultMode: JvmDefaultMode
) {
    @Test
    fun abstractDao() {
        val defaultWithCompatibilityAnnotation =
            if (jvmDefaultMode == JvmDefaultMode.ALL_COMPATIBILITY) {
                "@JvmDefaultWithoutCompatibility"
            } else {
                ""
            }

        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            class User
            interface BaseDao<T> {
                @Transaction
                fun upsert(obj: T) {
                    TODO("")
                }
            }

            $defaultWithCompatibilityAnnotation
            @Dao
            abstract class SubjectDao : BaseDao<User>
            """.trimIndent()
        )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public void upsert(final User obj)")
            generated.contains("SubjectDao_Impl.super.upsert(")
            generated.doesNotContain("SubjectDao.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            class User
            interface BaseDao<T> {
                @Transaction
                fun upsert(obj: T) {
                    TODO("")
                }
            }

            @Dao
            interface SubjectDao : BaseDao<User>
            """.trimIndent()
        )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public void upsert(final User obj)")
            if (jvmDefaultMode == JvmDefaultMode.DISABLE) {
                generated.contains("SubjectDao.DefaultImpls.upsert(SubjectDao_Impl.this")
            } else {
                generated.contains("SubjectDao.super.upsert(")
            }

            generated.doesNotContain("SubjectDao_Impl.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao_suspend() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            class User
            interface BaseDao<T> {
                @Transaction
                suspend fun upsert(obj: T) {
                    TODO("")
                }
            }

            @Dao
            interface SubjectDao : BaseDao<User>
            """.trimIndent()
        )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public Object upsert(final User obj, " +
                "final Continuation<? super Unit> continuation)")
            if (jvmDefaultMode == JvmDefaultMode.DISABLE) {
                generated.contains("SubjectDao.DefaultImpls.upsert(SubjectDao_Impl.this")
            } else {
                generated.contains("SubjectDao.super.upsert(")
            }

            generated.doesNotContain("SubjectDao_Impl.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    private fun compileInEachDefaultsMode(
        source: Source,
        handler: (StringSubject) -> Unit
    ) {
        // TODO should run these with KSP as well. https://github.com/google/ksp/issues/627
        runKaptTest(
            sources = listOf(source, COMMON.COROUTINES_ROOM, COMMON.ROOM_DATABASE_KTX),
            kotlincArguments = listOf("-Xjvm-default=${jvmDefaultMode.description}")
        ) { invocation ->
            invocation.roundEnv
                .getElementsAnnotatedWith(
                    androidx.room.Dao::class.qualifiedName!!
                ).filterIsInstance<XTypeElement>()
                .forEach { dao ->
                    val db = invocation.context.processingEnv
                        .requireTypeElement(RoomTypeNames.ROOM_DB)
                    val dbType = db.type
                    val parser = DaoProcessor(
                        baseContext = invocation.context,
                        element = dao,
                        dbType = dbType,
                        dbVerifier = createVerifierFromEntitiesAndViews(invocation)
                    )
                    val parsedDao = parser.process()
                    DaoWriter(parsedDao, db, invocation.processingEnv)
                        .write(invocation.processingEnv)
                    invocation.assertCompilationResult {
                        val relativePath = parsedDao.implTypeName.simpleName() + ".java"
                        handler(generatedSourceFileWithPath(relativePath))
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "jvmDefaultMode={0}")
        fun modes() = listOf(
            JvmDefaultMode.ALL_COMPATIBILITY,
            JvmDefaultMode.ALL_INCOMPATIBLE,
            JvmDefaultMode.DISABLE,
        )
    }
}