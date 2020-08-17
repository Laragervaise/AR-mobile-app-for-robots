/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2018 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arengine;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;


class CameraPermissionHelper {

    private static final String[] permissionsArray = new String[]{
            Manifest.permission.CAMERA};

    // permission list to request
    private static List<String> permissionsList = new ArrayList<>();

    // return code
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // check permission
    public static boolean hasPermission(final Activity activity){
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // require permission
    public static void requestPermission(final Activity activity){
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
    }
}
