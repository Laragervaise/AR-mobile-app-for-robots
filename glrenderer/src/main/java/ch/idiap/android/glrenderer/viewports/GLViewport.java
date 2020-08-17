/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *   damonkohler@google.com (Damon Kohler)
 *
 * Original implementation:
 *   Copyright (C) 2011 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.glrenderer.viewports;

import android.opengl.GLES20;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;


public class GLViewport implements Viewport {

    private final int width;
    private final int height;

    private Matrix4f projectionMatrix = new Matrix4f();


    public GLViewport(int width, int height) {
        this.width = width;
        this.height = height;

        setup(0.1f, 1000, 45.0f);
    }


    public GLViewport(int width, int height, float zNear, float zFar, float fov) {
        this.width = width;
        this.height = height;

        setup(zNear, zFar, fov);
    }


    public void setup(float zNear, float zFar, float fov) {
        float aspectRatio = (float) width/ (float) height;

        float fW, fH;
        fH = (float) (Math.tan(fov / 360 * Math.PI) * zNear);
        fW = fH * aspectRatio;

        projectionMatrix.frustum(-fW, fW, -fH, fH, zNear, zFar);
    }


    public void apply() {
        GLES20.glViewport(0, 0, width, height);
    }


    public void setProjectionMatrix(Matrix4fc projectionMatrix) {
        this.projectionMatrix.set(projectionMatrix);
    }


    public Matrix4fc getProjectionMatrix() {
        return new Matrix4f(projectionMatrix);
    }


    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }
}
