/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.cameras;


import org.joml.Matrix4f;
import org.joml.Matrix4fc;


public class ExternalCamera extends BaseCamera {

    //_____ Attributes __________

    protected Matrix4f viewMatrix = new Matrix4f();


    //_____ Methods __________

    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix.set(viewMatrix);
        transforms.set(new Matrix4f(viewMatrix).invert());
    }

    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

}
