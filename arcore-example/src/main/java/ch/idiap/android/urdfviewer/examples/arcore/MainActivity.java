/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2017 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arcore;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;

import ch.idiap.android.ros.ROSManager;

import ch.idiap.android.urdfviewer.examples.arcore.helpers.CameraPermissionHelper;
import ch.idiap.android.urdfviewer.examples.arcore.helpers.FullScreenHelper;
import ch.idiap.android.urdfviewer.examples.arcore.gestures.GestureHelper;
import ch.idiap.android.urdfviewer.examples.arcore.helpers.SnackbarHelper;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.GLRenderer;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;


/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the robot.
 */
public class MainActivity extends Activity implements GLRenderer.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();


    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private GLRenderer renderer;

    private boolean installRequested;

    private Session session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);


        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
        FloatingActionButton btnObjects = findViewById(R.id.btnObjects);
        FloatingActionButton btnStop = findViewById(R.id.btnStop);
        FloatingActionButton btnGoal = findViewById(R.id.btnGoal);

        // Setup the 'Settings' menu
        btnSettings.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.settings, popup.getMenu());

                // Hide irrelevant menu entries
                if (ROSManager.isROSStarted())
                    popup.getMenu().removeItem(R.id.ros_connect);
                else
                    popup.getMenu().removeItem(R.id.ros_disconnect);

                if (renderer.isRobotPlaced()) {
                    if (renderer.isInMode(GLRenderer.Mode.LISTENER))
                        popup.getMenu().removeItem(R.id.animation_stop);
                    else
                        popup.getMenu().removeItem(R.id.animation_play);
                } else {
                    popup.getMenu().removeItem(R.id.animation_stop);
                    popup.getMenu().removeItem(R.id.animation_play);
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.ros_connect:
                                ROSManager.startRos(new ROSManager.ROSConnectionListener() {
                                    @Override
                                    public void onConnected() {
                                        if (renderer.isInMode(GLRenderer.Mode.LISTENER))
                                            ROSManager.startListeners();
                                    }
                                });
                                return true;

                            case R.id.ros_disconnect:
                                ROSManager.stopRos();
                                return true;

                            case R.id.animation_play:
                                if (ROSManager.isROSStarted())
                                    ROSManager.stopListeners();

                                ROSManager.startPlayback();

                                renderer.setMode(GLRenderer.Mode.PLAYBACK);
                                return true;

                            case R.id.animation_stop:
                                ROSManager.stopPlayback();

                                renderer.setMode(GLRenderer.Mode.LISTENER);

                                if (ROSManager.isROSStarted())
                                    ROSManager.startListeners();
                                return true;

                            default:
                                return false;
                        }
                    }
                });

                popup.show();
            }
        });


        // Setup the 'Objects' menu
        btnObjects.hide();

        btnObjects.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.actions, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        btnStop.show();
                        btnSettings.hide();
                        btnObjects.hide();
                        btnGoal.hide();

                        switch (item.getItemId()) {
                            case R.id.drop_ball:
                                renderer.setAction(GLRenderer.Action.DROP_BALL);
                                return true;

                            case R.id.drop_cube:
                                renderer.setAction(GLRenderer.Action.DROP_CUBE);
                                return true;

                            case R.id.drop_cylinder:
                                renderer.setAction(GLRenderer.Action.DROP_CYLINDER);
                                return true;

                            case R.id.place_ball:
                                renderer.setAction(GLRenderer.Action.PLACE_BALL);
                                return true;

                            case R.id.place_cube:
                                renderer.setAction(GLRenderer.Action.PLACE_CUBE);
                                return true;

                            case R.id.place_cylinder:
                                renderer.setAction(GLRenderer.Action.PLACE_CYLINDER);
                                return true;

                            case R.id.place_cube_stack:
                                renderer.setAction(GLRenderer.Action.PLACE_CUBE_STACK);
                                return true;

                            case R.id.place_cylinder_stack:
                                renderer.setAction(GLRenderer.Action.PLACE_CYLINDER_STACK);
                                return true;

                            case R.id.throw_ball:
                                renderer.setAction(GLRenderer.Action.THROW_BALL);
                                return true;

                            case R.id.throw_cube:
                                renderer.setAction(GLRenderer.Action.THROW_CUBE);
                                return true;

                            case R.id.throw_cylinder:
                                renderer.setAction(GLRenderer.Action.THROW_CYLINDER);
                                return true;

                            default:
                                btnStop.hide();
                                btnSettings.show();
                                btnObjects.show();
                                btnGoal.show();
                                return false;
                        }
                    }
                });

                popup.show();
            }
        });


        // Setup the 'Stop' button
        btnStop.hide();

        btnStop.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderer.setAction(GLRenderer.Action.NONE);
                btnStop.hide();
                btnSettings.show();
                btnObjects.show();
                btnGoal.show();
            }
        });

        //Setup the goal button
        btnGoal.hide();

        btnGoal.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.goal, popup.getMenu());

                // Hide irrelevant menu entries
                if(!renderer.isRobotPlaced()){
                    popup.getMenu().removeItem(R.id.remove_goal);
                    popup.getMenu().removeItem(R.id.add_goal);
                /*}else{
                    if(renderer.getPath().isEmpty()){
                        popup.getMenu().removeItem(R.id.remove_goal);
                    }*/
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        btnStop.show();
                        btnSettings.hide();
                        btnObjects.hide();
                        btnGoal.hide();
                        switch (item.getItemId()) {

                            case R.id.add_goal:
                                renderer.setAction(GLRenderer.Action.ADD_GOAL);
                                return true;

                            case R.id.remove_goal:
                                renderer.setAction(GLRenderer.Action.REMOVE);
                                return true;

                            default:
                                btnStop.hide();
                                btnSettings.show();
                                btnObjects.show();
                                btnGoal.show();
                                return false;
                        }
                    }
                });
                popup.show();
            }
        });


        // Set up gestures listener.
        GestureHelper gestureHelper = new GestureHelper(/*context=*/ this);
        surfaceView.setOnTouchListener(gestureHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setWillNotDraw(false);

        renderer = new GLRenderer(this, gestureHelper, this);
        surfaceView.setRenderer(renderer);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        installRequested = false;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);
                renderer.setSession(session);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                SnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            SnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        renderer.getDisplayRotationHelper().onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            renderer.getDisplayRotationHelper().onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }


    @Override
    public void onRobotCreated() {
        FloatingActionButton btnObjects = findViewById(R.id.btnObjects);
        btnObjects.post(new Runnable() {
            public void run() {
                btnObjects.show();
            }
        });


        FloatingActionButton btnGoal = findViewById((R.id.btnGoal));
        btnGoal.post(new Runnable() {
            @Override
            public void run() {
                btnGoal.show();
            }
        });
    }
}
