package mohan.com.permissionhelper;


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
 * File Name : ActivityManagePermission.java.
 * ClassName : ActivityManagePermission.
 * QualifiedClassName : com.permissionhelper.ActivityManagePermission.
 * Module Name : app.
 * Workstation Username : innobot-linux-4.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


@SuppressWarnings({"MissingPermission"})
public class ActivityManagePermission extends AppCompatActivity {


	private final int KEY_PERMISSION = 200;
	private PermissionResult permissionResult;
	private String permissionsAsk[];


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	/**
	 * @param context    current Context
	 * @param permission String permission to ask
	 * @return boolean true/false
	 */
	public boolean isPermissionGranted(@NonNull Context context, @NonNull String permission) {
		boolean granted = ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED));
		return granted;
	}

	/**
	 * @param context     current Context
	 * @param permissions String[] permission to ask
	 * @return boolean true/false
	 */
	public boolean isPermissionsGranted(@NonNull Context context, @NonNull String permissions[]) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			return true;

		boolean granted = true;

		for (String permission : permissions) {
			if (!(ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED))
				granted = false;
		}

		return granted;
	}


	private void internalRequestPermission(String[] permissionAsk) {
		String arrayPermissionNotGranted[];
		ArrayList<String> permissionsNotGranted = new ArrayList<>();

		for (int i = 0; i < permissionAsk.length; i++) {
			if (!isPermissionGranted(ActivityManagePermission.this, permissionAsk[i])) {
				permissionsNotGranted.add(permissionAsk[i]);
			}
		}


		if (permissionsNotGranted.isEmpty()) {

			if (permissionResult != null)
				permissionResult.permissionGranted();

		} else {

			arrayPermissionNotGranted = new String[permissionsNotGranted.size()];
			arrayPermissionNotGranted = permissionsNotGranted.toArray(arrayPermissionNotGranted);
			ActivityCompat.requestPermissions(ActivityManagePermission.this, arrayPermissionNotGranted, KEY_PERMISSION);

		}


	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		if (requestCode != KEY_PERMISSION) {
			return;
		}

		List<String> permissionDenied = new LinkedList<>();
		boolean granted = true;

		for (int i = 0; i < grantResults.length; i++) {

			if (!(grantResults[i] == PackageManager.PERMISSION_GRANTED)) {
				granted = false;
				permissionDenied.add(permissions[i]);
			}

		}

		if (permissionResult != null) {
			if (granted) {
				permissionResult.permissionGranted();
			} else {
				for (String s : permissionDenied) {
					if (!ActivityCompat.shouldShowRequestPermissionRationale(this, s)) {
						permissionResult.permissionForeverDenied();
						return;
					}
				}

				permissionResult.permissionDenied();


			}
		}

	}

	/**
	 * @param permission       String permission ask
	 * @param permissionResult callback PermissionResult
	 */
	public void askCompactPermission(String permission, PermissionResult permissionResult) {
		permissionsAsk = new String[]{permission};
		this.permissionResult = permissionResult;
		internalRequestPermission(permissionsAsk);

	}

	/**
	 * @param permissions      String[] permissions ask
	 * @param permissionResult callback PermissionResult
	 */
	public void askCompactPermissions(String permissions[], PermissionResult permissionResult) {
		permissionsAsk = permissions;
		this.permissionResult = permissionResult;
		internalRequestPermission(permissionsAsk);
	}

	public void openSettingsApp(@NonNull Context context) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.setData(Uri.parse("package:" + context.getPackageName()));
			startActivity(intent);
		}


	}

}
