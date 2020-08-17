/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics;

import com.bulletphysics.linearmath.Transform;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.vecmath.Quat4f;

import ch.idiap.android.glrenderer.entities.Transformable;
import ch.idiap.android.glrenderer.entities.Transforms;


public class MotionState extends com.bulletphysics.linearmath.MotionState
{
    protected Transformable transformable = null;


    public MotionState(Transformable transformable) {
        this.transformable = transformable;
    }


    public Transform getWorldTransform(Transform out) {
        out.setIdentity();

        Vector3fc position = transformable.transforms.getWorldPosition();
        out.origin.set(
                position.x() * World.SCALE,
                position.y() * World.SCALE,
                position.z() * World.SCALE
        );

        Quaternionfc rotation = transformable.transforms.getWorldOrientation();
        out.setRotation(new Quat4f(rotation.x(), rotation.y(), rotation.z(), rotation.w()));

        return out;
    }


    public void setWorldTransform(Transform worldTrans) {
        Transforms targetTransforms = transformable.transforms.getParent();

        Quat4f rotation = new Quat4f();
        worldTrans.getRotation(rotation);

        Quaternionf targetOrientation =
                new Quaternionf(rotation.x, rotation.y, rotation.z, rotation.w).mul(
                        new Quaternionf(transformable.transforms.getOrientation()).invert()
                );

        Vector3f targetPosition =
                new Vector3f(
                        worldTrans.origin.x / World.SCALE,
                        worldTrans.origin.y / World.SCALE,
                        worldTrans.origin.z / World.SCALE
                ).add(
                        targetOrientation.transform(
                                new Vector3f(transformable.transforms.getPosition()).mul(
                                        targetTransforms.getWorldScale()))
                );

        targetTransforms.setWorldTransforms(targetPosition, targetOrientation);
    }
}
