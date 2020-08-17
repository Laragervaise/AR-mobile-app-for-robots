/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.opengl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import ch.idiap.android.ros.ROSManager;


public class MainActivity extends Activity {

    private static Context context;

    private GLSurfaceView surfaceView;


    public MainActivity() {
        context = this;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        surfaceView = new GLSurfaceView(this);
        setContentView(surfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();

        // Initialise the ROSManager API
        ROSManager.init(this);
        ROSManager.startRos(new ROSManager.ROSConnectionListener() {
            @Override
            public void onConnected() {
                ROSManager.startListeners();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    public static Context getContext() {
        return context;
    }

}
