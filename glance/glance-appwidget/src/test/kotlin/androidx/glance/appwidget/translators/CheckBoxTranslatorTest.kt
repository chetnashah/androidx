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

package androidx.glance.appwidget.translators

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.ImageViewSubject.Companion.assertThat
import androidx.glance.appwidget.applyRemoteViews
import androidx.glance.appwidget.findViewByType
import androidx.glance.appwidget.runAndTranslate
import androidx.glance.appwidget.test.R
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CheckBoxTranslatorTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resolved_unchecked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = false,
                text = "Check",
                colors = CheckBoxColors(checked = Color.Red, unchecked = Color.Blue)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Blue)
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resolved_checked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                text = "Check",
                colors = CheckBoxColors(checked = Color.Red, unchecked = Color.Blue)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter(Color.Red)
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resource_unchecked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = false,
                text = "Check",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter("#0000FF")
    }

    @Config(sdk = [21, 23])
    @Test
    fun canTranslateCheckBox_resource_checked() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            CheckBox(
                checked = true,
                text = "Check",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
        }

        val checkBoxRoot = assertIs<ViewGroup>(context.applyRemoteViews(rv))
        val icon = checkBoxRoot.findViewByType<ImageView>()
        assertThat(icon).hasColorFilter("#FF0000")
    }
}