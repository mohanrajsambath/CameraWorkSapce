package mohan.com.camera2_camrecorder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import mohan.com.constants.Constants;
import mohan.com.permissionhelper.ActivityManagePermission;
import mohan.com.permissionhelper.PermissionResult;


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
public class SplashActivity extends ActivityManagePermission {
    Activity getActivityContext;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aactivity_splash);
        getActivityContext = SplashActivity.this;
        askCompactPermissions(Constants.PERMISSION_LIST, new PermissionResult() {
            @Override
            public void permissionGranted() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // This method will be executed once the timer is over
                        Intent i = new Intent(SplashActivity.this, PreviewActivity.class);
                        startActivity(i);
                        finish();
                        i=null;
                    }
                }, 1000);
            }

            @Override
            public void permissionDenied() {
                moveToSettings();
            }

            @Override
            public void permissionForeverDenied() {
                moveToSettings();
            }
        });

    }


    private void moveToSettings(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivityContext, R.style.DialogTheme);
        alertDialogBuilder.setMessage(R.string.msg_permission_denied);
        alertDialogBuilder.setNegativeButton(R.string.permission_exit_ext, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);

            }
        });
        AlertDialog dialog = alertDialogBuilder.setPositiveButton(R.string.permission_settings_txt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivityContext.getPackageName(), null);
                intent.setData(uri);
                ((Activity) getActivityContext).startActivityForResult(intent, 10001);
                uri=null;

            }
        }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(@NonNull DialogInterface dialogInterface) {
                Button positive = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setTextColor(getColor(R.color.dialog_text_color));
                Button negative = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setTextColor(getColor(R.color.dialog_text_color));
            }
        });
        dialog.show();
    }
}
