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

package androidx.camera.integration.extensions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureExtenderValidationTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector

    @Before
    fun setUp(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        cameraCharacteristics = Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )

        // NoSuchMethodError will be thrown if getSupportedResolutions is not implemented in
        // vendor library, and then the test will fail.
        impl.supportedResolutions
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun returnsNullFromOnPresetSession_whenAPILevelOlderThan28() {
        // Creates the ImageCaptureExtenderImpl to check that onPresetSession() returns null when
        // API level is older than 28.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )
        assertThat(impl.onPresetSession()).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeSameAsImplClass_aboveVersion1_2(): Unit = runBlocking {
        assumeTrue(
            ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) >= 0
        )

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo = extensionsManager.getEstimatedCaptureLatencyRange(
            baseCameraSelector,
            extensionMode
        )

        // Calls bind to lifecycle to get the selected camera
        var camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
        val characteristics = Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)

        // Creates ImageCaptureExtenderImpl directly to retrieve the capture latency range info
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            characteristics
        )
        val expectedLatencyInfo = impl.getEstimatedCaptureLatencyRange(null)

        // Compares the values obtained from ExtensionsManager and ImageCaptureExtenderImpl are
        // the same.
        assertThat(latencyInfo).isEqualTo(expectedLatencyInfo)
    }

    @LargeTest
    @Test
    fun returnCaptureStages_whenCaptureProcessorIsNotNull(): Unit = runBlocking {
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )

        // Only runs the test when CaptureProcessor is not null
        assumeTrue(impl.captureProcessor != null)

        val lifecycleOwner: FakeLifecycleOwner
        withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
        }

        val imageCapture = ImageCapture.Builder().build()

        val countDownLatch = CountDownLatch(1)

        withContext(Dispatchers.Main) {
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, extensionCameraSelector, imageCapture
            )

            camera.cameraInfo.cameraState.observeForever { cameraState ->
                if (cameraState.type == CameraState.Type.OPEN) {
                    countDownLatch.countDown()
                }
            }
        }

        // Wait for the CameraState type becomes OPEN to make sure the capture session has been
        // created.
        countDownLatch.await(10000, TimeUnit.MILLISECONDS)

        // Retrieve the CaptureBundle from ImageCapture's config
        val captureBundle =
            imageCapture.currentConfig.retrieveOption(ImageCaptureConfig.OPTION_CAPTURE_BUNDLE)

        // Calls CaptureBundle#getCaptureStages() will call
        // ImageCaptureExtenderImpl#getCaptureStages(). Checks the returned value is not empty.
        assertThat(captureBundle!!.captureStages).isNotEmpty()
    }
}