# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# A rule that will keep classes that implement SidecarInterface$SidecarCallback if Sidecar seems
# be used. See b/157286362 and b/165268619 for details.
# TODO(b/208543178) investigate how to pass header jar to R8 so we don't need this rule
-if class androidx.window.layout.SidecarCompat {
  public void setExtensionCallback(androidx.window.layout.ExtensionInterfaceCompat$ExtensionCallbackInterface);
}
-keep class androidx.window.layout.SidecarCompat$TranslatingCallback,
 androidx.window.layout.SidecarCompat$DistinctSidecarElementCallback {
  public void onDeviceStateChanged(androidx.window.sidecar.SidecarDeviceState);
  public void onWindowLayoutChanged(android.os.IBinder, androidx.window.sidecar.SidecarWindowLayoutInfo);
}