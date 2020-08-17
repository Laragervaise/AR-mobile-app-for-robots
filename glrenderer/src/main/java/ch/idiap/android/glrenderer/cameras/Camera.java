/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.cameras;


import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;


public class Camera extends BaseCamera {

    public void lookAt(Vector3fc eye, Vector3fc target, Vector3fc up) {
        Matrix4f m = new Matrix4f();
        m.setLookAt(eye, target, up);

        transforms.set(m.invert());
    }


    public Matrix4fc getViewMatrix() {
        return transforms.toMatrix().invert();
    }
}
