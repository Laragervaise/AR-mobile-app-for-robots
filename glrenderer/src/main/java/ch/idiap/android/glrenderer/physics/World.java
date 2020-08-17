/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.extras.gimpact.GImpactCollisionAlgorithm;

import javax.vecmath.Vector3f;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.physics.helpers.DebugDrawer;
import ch.idiap.android.glrenderer.rays.Ray;


public class World
{
    static public final float SCALE = 100.0f;

    private CollisionConfiguration collisionConfiguration = null;
    private CollisionDispatcher collisionDispatcher = null;
    private BroadphaseInterface overlappingPairCache = null;
    private SequentialImpulseConstraintSolver solver = null;
    private DiscreteDynamicsWorld world = null;


    public World() {
        collisionConfiguration = new DefaultCollisionConfiguration();
        collisionDispatcher = new CollisionDispatcher(collisionConfiguration);
        overlappingPairCache = new DbvtBroadphase();
        solver = new SequentialImpulseConstraintSolver();

        GImpactCollisionAlgorithm.registerAlgorithm(collisionDispatcher);

        world = new DiscreteDynamicsWorld(
                collisionDispatcher, overlappingPairCache, solver, collisionConfiguration
        );

        world.setGravity(new Vector3f(0.0f, -9.81f * SCALE, 0.0f));
    }


    public void step(float elapsed) {
        world.stepSimulation(elapsed, 100, 0.006f);
    }


    public void addRigidBody(RigidBody body) {
        world.addRigidBody(body);
    }


    public void removeRigidBody(RigidBody body) {
        world.removeRigidBody(body);
    }


    public void enableDebugDrawing(BaseCamera camera) {
        world.setDebugDrawer(new DebugDrawer(camera));
    }


    public void debugDraw() {
        world.debugDrawWorld();
    }


    public PhysicBody rayTest(Ray ray) {
        org.joml.Vector3f origin = new org.joml.Vector3f(ray.getOrigin()).mul(SCALE);
        org.joml.Vector3f end = new org.joml.Vector3f(ray.getDirection()).mul(10.0f * SCALE).add(origin);

        Vector3f from = new Vector3f(origin.x(), origin.y(), origin.z());
        Vector3f to = new Vector3f(end.x(), end.y(), end.z());

        CollisionWorld.ClosestRayResultCallback callback =
                new CollisionWorld.ClosestRayResultCallback(from, to);

        world.rayTest(from, to, callback);

        if (callback.collisionObject != null)
            return PhysicBody.class.cast(callback.collisionObject.getUserPointer());

        return null;
    }

}
