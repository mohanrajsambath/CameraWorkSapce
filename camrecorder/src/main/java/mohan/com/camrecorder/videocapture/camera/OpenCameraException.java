package mohan.com.camrecorder.videocapture.camera;

/*
 * Copyright (c) 2018. Created for CYRANOAPP, PoweredBy INNOBOT SYSYTEMS Pvt.Ltd.,Coimbatore,TamilNadu,India.
 * All Rights Reserved,Company Confidential.
 *
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
 *
 * Project Name : cyranoapp-android.
 * Created by : Mohanraj.S, Android Application Developer.
 * Created on : 8/29/2018
 * Updated on : 29/8/18 3:06 PM.
 * File Name : OpenCameraException.java.
 * ClassName : OpenCameraException.
 * QualifiedClassName : com.videocapture.camera.OpenCameraException.
 * Module Name : app.
 * Workstation Username : innobot-linux-4.
 */

import android.support.annotation.NonNull;

import com.ibot.cyranoapp.utils.AppLog;

public class OpenCameraException extends Exception {

	private static final String LOG_PREFIX			= "Unable to open camera - ";
	private static final long	serialVersionUID	= -7340415176385044242L;

	public enum OpenType {
		INUSE("Camera disabled or in use by other process"), NOCAMERA("Device does not have camera");

		private String mMessage;

		OpenType(String msg) {
			mMessage = msg;
		}

		public String getMessage() {
			return mMessage;
		}

	}

	@NonNull
    private final OpenType	mType;

	public OpenCameraException(OpenType type) {
		super(type.getMessage());
		mType = type;
	}

	@Override
	public void printStackTrace() {
		AppLog.e(AppLog.EXCEPTION, LOG_PREFIX + mType.getMessage());
		super.printStackTrace();
	}
}
