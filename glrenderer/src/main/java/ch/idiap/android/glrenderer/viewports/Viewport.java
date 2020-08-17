/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.viewports;


import org.joml.Matrix4fc;


public interface Viewport {

    void apply();

    void setProjectionMatrix(Matrix4fc projectionMatrix);

    Matrix4fc getProjectionMatrix();

    int getWidth();

    int getHeight();
}
