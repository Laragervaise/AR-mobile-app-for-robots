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


import ch.idiap.android.glrenderer.cameras.ExternalCamera;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.glrenderer.shaders.ShaderManager;
import ch.idiap.android.ros.ROSManager;
import ch.idiap.android.urdf.robot.Robot;

import org.joml.Matrix4f;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARAnchor;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.AREnginesSelector;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableDeviceNotCompatibleException;
import com.huawei.hiar.exceptions.ARUnavailableEmuiNotCompatibleException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.huawei.hiar.exceptions.ARUnavailableUserDeclinedInstallationException;

import ch.idiap.android.urdfviewer.examples.arengine.rendering.BackgroundRenderer;
import ch.idiap.android.urdfviewer.examples.arengine.rendering.PlaneRenderer;
import ch.idiap.android.urdfviewer.examples.arengine.rendering.PointCloudRenderer;
import ch.idiap.android.glrenderer.viewports.ExternalViewport;
import ch.idiap.android.glrenderer.viewports.Viewport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MainActivity extends Activity implements GLSurfaceView.Renderer {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ARSession session;
    private GLSurfaceView surfaceView;
    private GestureDetector gestureDetector;
    private DisplayRotationHelper displayRotationHelper;

    private BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private PlaneRenderer planeRenderer = new PlaneRenderer();
    private PointCloudRenderer pointCloud = new PointCloudRenderer();

    private final float[] anchorMatrix = new float[UtilsCommon.MATRIX_NUM];

    private ArrayBlockingQueue<GestureEvent> gestureEvents = new ArrayBlockingQueue<>(2);

    private boolean installRequested;

    private final ArrayList<ARAnchor> anchors = new ArrayList<>();

    private Robot robot = null;
    private ExternalCamera camera = null;
    private Light light = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(this);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onGestureEvent(GestureEvent.createSingleTapUpEvent(e));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                onGestureEvent(GestureEvent.createDownEvent(e));
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY));
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {
                onGestureEvent(GestureEvent.createFlingEvent(e1, e2, velocityX, velocityY));
                return true;
            }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        installRequested = false;
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        Exception exception = null;
        String message = null;
        if (null == session) {
            try {
                //If you do not want to switch engines, AREnginesSelector is useless.
                // You just need to use AREnginesApk.requestInstall() and the default engine
                // is Huawei AR Engine.
                AREnginesSelector.AREnginesAvaliblity enginesAvaliblity = AREnginesSelector.checkAllAvailableEngines(this);
                if ((enginesAvaliblity.ordinal()
                        & AREnginesSelector.AREnginesAvaliblity.HWAR_ENGINE_SUPPORTED.ordinal()) != 0) {

                    AREnginesSelector.setAREngine(AREnginesSelector.AREnginesType.HWAR_ENGINE);

                    Log.d(TAG, "installRequested:" + installRequested);
                    switch (AREnginesApk.requestInstall(this, !installRequested)) {
                        case INSTALL_REQUESTED:
                            Log.d(TAG, "INSTALL_REQUESTED");
                            installRequested = true;
                            return;
                        case INSTALLED:
                            break;
                    }

                    if (!CameraPermissionHelper.hasPermission(this)) {
                        CameraPermissionHelper.requestPermission(this);
                        return;
                    }

                    session = new ARSession(/*context=*/this);

                    ARWorldTrackingConfig config = new ARWorldTrackingConfig(session);
                    session.configure(config);
                } else {
                    message = "This device does not support Huawei AR Engine ";
                }
            } catch (ARUnavailableServiceNotInstalledException e) {
                message = "Please install HuaweiARService.apk";
                exception = e;
            } catch (ARUnavailableServiceApkTooOldException e) {
                message = "Please update HuaweiARService.apk";
                exception = e;
            } catch (ARUnavailableClientSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (ARUnavailableDeviceNotCompatibleException e) {
                message = "This device does not support Huawei AR Engine ";
                exception = e;
            } catch (ARUnavailableEmuiNotCompatibleException e) {
                message = "Please update EMUI version";
                exception = e;
            } catch (ARUnavailableUserDeclinedInstallationException e) {
                message = "Please agree to install!";
                exception = e;
            } catch (ARUnSupportedConfigurationException e) {
                message = "The configuration is not supported by the device!";
                exception = e;
            } catch (Exception e) {
                message = "exception throwed";
                exception = e;
            }
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Creating sesson", exception);
                if (session != null) {
                    session.stop();
                    session = null;
                }
                return;
            }
        }

        session.resume();
        surfaceView.onResume();
        displayRotationHelper.onResume();

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
    protected void onPause() {

        super.onPause();
        if (session != null) {
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasPermission(this)) {
            Toast.makeText(this,
                    "This application needs camera permission.", Toast.LENGTH_LONG).show();

            finish();
        }
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
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onGestureEvent(GestureEvent e) {
        gestureEvents.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Reinitialise the subsystems dependant on the OpenGL surface
        ShaderManager.init(this);
        MeshManager.init();

        backgroundRenderer.createOnGlThread(/*context=*/this);

        try {
            planeRenderer.createOnGlThread(/*context=*/this, "trigrid.png");

        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }

        pointCloud.createOnGlThread(/*context=*/this);

        // Create the robot model
        robot = ROSManager.createPandaArm(null);

        // Create the camera
        camera = new ExternalCamera();

        // Create the light
        light = new Light();
        light.color.set(0.7f, 0.7f, 0.7f, 1.0f);
        light.transforms.setPosition(3.0f, 4.0f, 5.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);

        GLES20.glViewport(0, 0, width, height);

        Viewport viewport = new ExternalViewport(width, height);
        camera.setViewport(viewport);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (null == session) {
            return;
        }
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            ARFrame frame = session.update();
            ARCamera arCamera = frame.getCamera();

            handleGestureEvent(frame, arCamera);

            backgroundRenderer.draw(frame);

            if (arCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
                return;
            }

            float[] projectionMatrix = new float[UtilsCommon.MATRIX_NUM];
            arCamera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

            float[] viewMatrix = new float[UtilsCommon.MATRIX_NUM];
            arCamera.getViewMatrix(viewMatrix, 0);

            ARPointCloud arPointCloud = frame.acquirePointCloud();
            pointCloud.update(arPointCloud);
            pointCloud.draw(viewMatrix, projectionMatrix);
            arPointCloud.release();

            planeRenderer.drawPlanes(session.getAllTrackables(ARPlane.class), arCamera.getDisplayOrientedPose(), projectionMatrix);

            // Use the view and projection matrices used by your OpenGL code for the visualization
            // of the robot models
            camera.setViewMatrix(new Matrix4f(
                    viewMatrix[0], viewMatrix[1], viewMatrix[2], viewMatrix[3],
                    viewMatrix[4], viewMatrix[5], viewMatrix[6], viewMatrix[7],
                    viewMatrix[8], viewMatrix[9], viewMatrix[10], viewMatrix[11],
                    viewMatrix[12], viewMatrix[13], viewMatrix[14], viewMatrix[15]
            ));

            camera.getViewport().setProjectionMatrix(new Matrix4f(
                    projectionMatrix[0], projectionMatrix[1], projectionMatrix[2], projectionMatrix[3],
                    projectionMatrix[4], projectionMatrix[5], projectionMatrix[6], projectionMatrix[7],
                    projectionMatrix[8], projectionMatrix[9], projectionMatrix[10], projectionMatrix[11],
                    projectionMatrix[12], projectionMatrix[13], projectionMatrix[14], projectionMatrix[15]
            ));

            // Draw the robots
            Iterator<ARAnchor> iter = anchors.iterator();
            while (iter.hasNext()) {
                ARAnchor anchor = iter.next();
                if (anchor.getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                    iter.remove();
                } else if (anchor.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    anchor.getPose().toMatrix(anchorMatrix, 0);
                    robot.transforms.set(new Matrix4f(
                            anchorMatrix[0], anchorMatrix[1], anchorMatrix[2], anchorMatrix[3],
                            anchorMatrix[4], anchorMatrix[5], anchorMatrix[6], anchorMatrix[7],
                            anchorMatrix[8], anchorMatrix[9], anchorMatrix[10], anchorMatrix[11],
                            anchorMatrix[12], anchorMatrix[13], anchorMatrix[14], anchorMatrix[15]
                    ));
                    robot.draw(camera, new Color(0.3f, 0.3f, 0.3f, 1.0f), light);
                }
            }

            // Publish the current camera image if necessary
            if (ROSManager.imagePublisher != null) {
                Image image = frame.acquireCameraImage();
                int screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                ROSManager.imagePublisher.publish(image, screenOrientation);
            }

            // Publish the current camera image if necessary
            if (ROSManager.depthPublisher != null) {
                Image image = frame.acquireDepthImage();
                int screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                ROSManager.depthPublisher.publish(image, screenOrientation);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }


    @Override
    protected void onDestroy() {
        if (session != null) {
            session.stop();
            session = null;
        }
        super.onDestroy();
    }


    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleGestureEvent(ARFrame frame, ARCamera camera) {
        GestureEvent event = gestureEvents.poll();
        if (event == null) {
            return;
        }

        // do nothing when no tracking
        if (camera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            return;
        }

        float[] projmtx = new float[UtilsCommon.MATRIX_NUM];
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

        float[] viewmtx = new float[UtilsCommon.MATRIX_NUM];
        camera.getViewMatrix(viewmtx, 0);

        int eventType = event.getType();
        switch (eventType) {
            case GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPUP: {

                MotionEvent tap = event.getE1();

                ARHitResult hitResult = hitTest4Result(frame, camera, tap);

                //if hit both Plane and Point,take Plane at the first priority.
                if (hitResult == null) {
                    break;
                }

                // Adding an Anchor tells AREngine that it should track this position in
                // space. This anchor is created on the Plane to place the 3D model
                // in the correct position relative both to the world and to the plane.
                anchors.add(hitResult.createAnchor());
                break;
            }
            default: {
                Log.e(TAG, "unknown motion event type, and do nothing.");
            }
        }
    }

    private ARHitResult hitTest4Result(ARFrame frame, ARCamera camera, MotionEvent event) {
        ARHitResult ret = null;
        List<ARHitResult> hitTestResult = frame.hitTest(event);
        for (int i = 0; i < hitTestResult.size(); i++) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            ARHitResult hitResultTemp = hitTestResult.get(i);
            ARTrackable trackable = hitResultTemp.getTrackable();
            if ((trackable instanceof ARPlane
                    && ((ARPlane) trackable).isPoseInPolygon(hitResultTemp.getHitPose())
                    && (PlaneRenderer.calculateDistanceToPlane(hitResultTemp.getHitPose(), camera.getPose()) > 0))
                    || (trackable instanceof ARPoint
                    && ((ARPoint) trackable).getOrientationMode() == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                ret = hitResultTemp;
                if (trackable instanceof ARPlane) {
                    break;
                }
            }
        }
        return ret;
    }
}
