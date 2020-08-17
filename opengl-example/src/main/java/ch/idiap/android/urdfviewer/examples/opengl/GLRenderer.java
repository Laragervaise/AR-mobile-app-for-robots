/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.opengl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import org.joml.Vector3f;

import ch.idiap.android.glrenderer.cameras.Camera;
import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.glrenderer.shaders.ShaderManager;
import ch.idiap.android.glrenderer.viewports.GLViewport;
import ch.idiap.android.glrenderer.viewports.Viewport;
import ch.idiap.android.ros.ROSManager;
import ch.idiap.android.urdf.robot.Robot;


/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class GLRenderer implements GLSurfaceView.Renderer {

    private float angle = 0.0f;

    private Robot robot = null;
    private Camera camera = null;
    private Light light = null;


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Initialise OpenGL
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Set the background frame color
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        // Initialise the ROSManager API
        ROSManager.init(MainActivity.getContext());

        // Reinitialise the subsystems dependant on the OpenGL surface
        ShaderManager.init(MainActivity.getContext());
        MeshManager.init();

        // Create the robot model
        robot = ROSManager.createPandaArm(null);

        // Create the camera
        camera = new Camera();
        camera.lookAt(new Vector3f(0.0f, 0.5f, 2.0f),
                new Vector3f(0.0f, 0.5f, 0.0f), Transforms.UNIT_Y);

        // Create the light
        light = new Light();
        light.color.set(0.7f, 0.7f, 0.7f, 1.0f);
        light.transforms.setPosition(3.0f, 4.0f, 5.0f);
    }


    @Override
    public void onDrawFrame(GL10 unused) {
        camera.getViewport().apply();

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Draw the robot
        robot.transforms.setOrientation(Transforms.IDENTITY);
        robot.transforms.rotate(Transforms.UNIT_Y, angle);
        robot.draw(camera, new Color(0.3f, 0.3f, 0.3f, 1.0f), light);
    }


    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Viewport viewport = new GLViewport(width, height, 0.1f, 10.0f, 60.0f);
        camera.setViewport(viewport);
        viewport.apply();
    }


    public float getAngle() {
        return angle;
    }


    public void setAngle(float angle) {
        this.angle = angle;
    }


    public Camera getCamera() {
        return camera;
    }

}