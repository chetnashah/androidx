/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated API uses the experimental {@link ImageAnalysis.Analyzer}
 * methods which allow additional features like overriding {@link ImageAnalysis} configuration and
 * coordinates transformation.
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalAnalyzer {
}
