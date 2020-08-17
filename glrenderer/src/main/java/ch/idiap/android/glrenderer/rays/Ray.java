/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.rays;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import ch.idiap.android.glrenderer.cameras.BaseCamera;


public class Ray {

    private Vector3f origin = new Vector3f();
    private Vector3f direction = new Vector3f();


    public Ray(Vector3f origin, Vector3f direction) {
        this.origin.set(origin);
        this.direction.set(direction);
    }


    public Ray(BaseCamera camera) {
        setup(camera, camera.getViewport().getWidth() / 2,
              camera.getViewport().getHeight() / 2);
    }


    public Ray(BaseCamera camera, float touch_x, float touch_y) {
        setup(camera, touch_x, touch_y);
    }


    private void setup(BaseCamera camera, float touch_x, float touch_y) {

        touch_y = camera.getViewport().getHeight() - touch_y;

        float x = (touch_x * 2.0f) / camera.getViewport().getWidth() - 1.0f;
        float y = (touch_y * 2.0f) / camera.getViewport().getHeight() - 1.0f;

        Vector4f farScreenPoint = new Vector4f(x, y, 1.0f, 1.0f);
        Vector4f nearScreenPoint = new Vector4f(x, y, -1.0f, 1.0f);
        Vector4f farPlanePoint = new Vector4f();
        Vector4f nearPlanePoint = new Vector4f();

        Matrix4f invertedProjectionMatrix = new Matrix4f();
        camera.getViewport().getProjectionMatrix().mul(camera.getViewMatrix(), invertedProjectionMatrix);
        invertedProjectionMatrix.invert();


        invertedProjectionMatrix.transform(nearScreenPoint, nearPlanePoint);
        invertedProjectionMatrix.transform(farScreenPoint, farPlanePoint);

        direction = new Vector3f(
                farPlanePoint.x / farPlanePoint.w,
                farPlanePoint.y / farPlanePoint.w,
                farPlanePoint.z / farPlanePoint.w
        );

        origin = new Vector3f(
                nearPlanePoint.x / nearPlanePoint.w,
                nearPlanePoint.y / nearPlanePoint.w,
                nearPlanePoint.z / nearPlanePoint.w
        );

        direction.sub(origin).normalize();
    }


    public String toString() {
        return "Ray [origin=" + origin.toString() + ", direction=" + direction + "]";
    }


    public Vector3fc getOrigin() {
        return origin;
    }


    public Vector3fc getDirection() {
        return direction;
    }


    public boolean intersectsSphere(Vector3fc center, float radius, Vector3f result) {

        Vector3f O_C = new Vector3f();
        origin.sub(center, O_C);

        float b = direction.dot(O_C);
        float c = O_C.dot(O_C) - radius * radius;

        float det = b * b - c;

        if (det < 0.0f)
            return false;

        float t;

        if (det > 0.0f) {
            float t1 = -b + (float) Math.sqrt((double) det);
            float t2 = -b - (float) Math.sqrt((double) det);

            t = (t1 < t2 ? t1 : t2);
        } else {
            t = -b + (float) Math.sqrt((double) det);
        }

        direction.mul(t, result);
        result.add(origin);

        return true;
    }


    public boolean intersectsPlane(Vector3fc normal, float distance, Vector3f result) {

        float t = -(origin.dot(normal) + distance) / direction.dot(normal);

        if (t < 0.0f)
            return false;

        direction.mul(t, result);
        result.add(origin);

        return true;
    }
}
