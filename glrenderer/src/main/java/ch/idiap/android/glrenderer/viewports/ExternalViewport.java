/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.viewports;


import android.opengl.GLES20;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;


public class ExternalViewport implements Viewport {

    private final int width;
    private final int height;

    private Matrix4f projectionMatrix = new Matrix4f();


    public ExternalViewport(int width, int height) {
        this.width = width;
        this.height = height;
    }


    public void apply() {
        GLES20.glViewport(0, 0, width, height);
    }


    public void setProjectionMatrix(Matrix4fc projectionMatrix) {
        this.projectionMatrix.set(projectionMatrix);
    }


    public Matrix4fc getProjectionMatrix() {
        return projectionMatrix;
    }


    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }
}
