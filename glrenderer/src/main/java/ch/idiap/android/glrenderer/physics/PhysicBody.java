/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import javax.vecmath.Vector3f;

import androidx.annotation.NonNull;
import ch.idiap.android.glrenderer.entities.Entity;
import ch.idiap.android.glrenderer.entities.Transformable;


public class PhysicBody extends Transformable
{
    protected RigidBody rigidBody = null;
    protected MotionState motionState = null;
    protected Entity entity = null;


    public PhysicBody() {
        motionState = new MotionState(this);
    }


    public void setDynamic(World world, float mass, CollisionShape shape) {
        if (rigidBody != null)
        {
            world.removeRigidBody(rigidBody);
            rigidBody = null;
        }

        Vector3f inertia = new Vector3f(0.0f, 0.0f, 0.0f);
        shape.calculateLocalInertia(mass, inertia);

        RigidBodyConstructionInfo constructionInfo = new RigidBodyConstructionInfo(
                mass, motionState, shape, inertia
        );

        constructionInfo.linearSleepingThreshold *= World.SCALE;

        rigidBody = new RigidBody(constructionInfo);
        rigidBody.setUserPointer(this);

        int collisionFlags = rigidBody.getCollisionFlags();
        collisionFlags &= ~(CollisionFlags.STATIC_OBJECT | CollisionFlags.KINEMATIC_OBJECT);

        rigidBody.setCollisionFlags(collisionFlags);

        rigidBody.setFriction(1.0f);

        world.addRigidBody(rigidBody);
    }


    public void setStatic(World world, CollisionShape shape) {
        if (rigidBody != null)
        {
            world.removeRigidBody(rigidBody);
            rigidBody = null;
        }

        rigidBody = new RigidBody(0.0f, motionState, shape);
        rigidBody.setUserPointer(this);
        rigidBody.setFriction(1.0f);

        int collisionFlags = rigidBody.getCollisionFlags();
        collisionFlags &= ~CollisionFlags.KINEMATIC_OBJECT;
        collisionFlags |= CollisionFlags.STATIC_OBJECT;

        rigidBody.setCollisionFlags(collisionFlags);

        world.addRigidBody(rigidBody);
    }


    public void setKinematic(World world, CollisionShape shape) {
        if (rigidBody != null)
        {
            world.removeRigidBody(rigidBody);
            rigidBody = null;
        }

        rigidBody = new RigidBody(0.0f, motionState, shape);
        rigidBody.setUserPointer(this);
        rigidBody.setFriction(1.0f);

        int collisionFlags = rigidBody.getCollisionFlags();
        collisionFlags &= ~CollisionFlags.STATIC_OBJECT;
        collisionFlags |= CollisionFlags.KINEMATIC_OBJECT;

        rigidBody.setCollisionFlags(collisionFlags);

        rigidBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

        world.addRigidBody(rigidBody);
    }


    public RigidBody getRigidBody() {
        return rigidBody;
    }


    public MotionState getMotionState() {
        return motionState;
    }


    public boolean isDynamic() {
        return (rigidBody != null) && !isStatic() && !isKinematic();
    }


    public boolean isStatic() {
        return (rigidBody != null) && ((rigidBody.getCollisionFlags() & CollisionFlags.STATIC_OBJECT) != 0);
    }


    public boolean isKinematic() {
        return (rigidBody != null) && ((rigidBody.getCollisionFlags() & CollisionFlags.KINEMATIC_OBJECT) != 0);
    }


    public void switchToDynamic() {
        if (!isKinematic())
            return;

        if (rigidBody != null)
        {
            rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() & ~CollisionFlags.KINEMATIC_OBJECT);
            rigidBody.setActivationState(CollisionObject.ACTIVE_TAG);
        }
    }


    public void switchToKinematic() {
        if (!isDynamic())
            return;

        if (rigidBody != null)
        {
            rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | CollisionFlags.KINEMATIC_OBJECT);
            rigidBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
        }
    }


    public void setFriction(float friction) {
        if (rigidBody != null)
            rigidBody.setFriction(friction);
    }


    public Entity getEntity() {
        return entity;
    }


    public void _setEntity(Entity entity) {
        this.entity = entity;
    }


    @NonNull
    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + " ";

        if (isDynamic())
            s += "dynamic";
        else if (isStatic())
            s += "static";
        else
            s += "kinematic";

        s += "]";

        return s;
    }
}
