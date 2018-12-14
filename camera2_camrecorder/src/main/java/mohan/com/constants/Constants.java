package mohan.com.constants;

import mohan.com.permissionhelper.PermissionUtils;

/*
 * Copyright (c) 2018. Created by Mohanraj.S,Innobot Systems on 17/11/18 for CameraWorkSapce
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class Constants {
    public static final String[] PERMISSION_LIST = new String[]{PermissionUtils.Manifest_WRITE_EXTERNAL_STORAGE, PermissionUtils.Manifest_READ_PHONE_STATE, PermissionUtils.Manifest_READ_EXTERNAL_STORAGE, PermissionUtils.Manifest_ACCESS_FINE_LOCATION, PermissionUtils.Manifest_CAMERA, PermissionUtils.Manifest_ACCESS_COARSE_LOCATION, PermissionUtils.Manifest_RECORD_AUDIO,PermissionUtils.Manifest_READ_CONTACTS};
}
