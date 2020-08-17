/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2017 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arcore.rendering;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ch.idiap.android.glrenderer.cameras.ExternalCamera;
import ch.idiap.android.glrenderer.entities.Entity;
import ch.idiap.android.glrenderer.entities.Renderable;
import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.helpers.AssetsUtils;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.materials.Material;
import ch.idiap.android.glrenderer.meshes.Mesh;
import ch.idiap.android.glrenderer.meshes.MeshBuilder;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.glrenderer.physics.PhysicBody;
import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.glrenderer.rays.Ray;
import ch.idiap.android.glrenderer.shaders.ShaderManager;
import ch.idiap.android.glrenderer.textures.TextureManager;
import ch.idiap.android.glrenderer.viewports.ExternalViewport;
import ch.idiap.android.glrenderer.viewports.Viewport;
import ch.idiap.android.ros.ROSManager;
import ch.idiap.android.urdf.robot.Robot;
import ch.idiap.android.urdfviewer.examples.arcore.ar.TrackingStateHelper;
import ch.idiap.android.urdfviewer.examples.arcore.entities.AnchorEntity;
import ch.idiap.android.urdfviewer.examples.arcore.ar.ARHits;
import ch.idiap.android.urdfviewer.examples.arcore.helpers.DisplayRotationHelper;
import ch.idiap.android.urdfviewer.examples.arcore.gestures.GestureEvent;
import ch.idiap.android.urdfviewer.examples.arcore.gestures.GestureHelper;
import ch.idiap.android.urdfviewer.examples.arcore.gestures.Manipulator;
import ch.idiap.android.urdfviewer.examples.arcore.helpers.SnackbarHelper;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.renderers.BackgroundRenderer;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.renderers.PlaneRenderer;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.renderers.PointCloudRenderer;


public class GLRenderer implements GLSurfaceView.Renderer {

    public enum Mode {
        LISTENER,
        PLAYBACK,
    }


    public enum Action {
        NONE,

        DROP_BALL,
        DROP_CUBE,
        DROP_CYLINDER,

        PLACE_BALL,
        PLACE_CUBE,
        PLACE_CYLINDER,
        PLACE_CUBE_STACK,
        PLACE_CYLINDER_STACK,

        THROW_BALL,
        THROW_CUBE,
        THROW_CYLINDER,

        ADD_GOAL,
        REMOVE,
    }

    public interface Listener {
        void onRobotCreated();
    }

    private static final String TAG = GLRenderer.class.getSimpleName();

    private static final float GROUND_FRICTION = 5.0f;
    private static final float CUBE_FRICTION = 1.0f;
    private static final float CYLINDER_FRICTION = 5.0f;
    private static final float BALL_FRICTION = 1.0f;

    private static final float CUBE_MASS = 0.5f;
    private static final float CYLINDER_MASS = 0.5f;
    private static final float BALL_MASS = 0.5f;

    private final Context context;
    private final GestureHelper gestureHelper;
    private final Listener listener;
    private final DisplayRotationHelper displayRotationHelper;
    private Session session = null;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    private ExternalCamera camera = null;
    private final ArrayList<AnchorEntity> anchors = new ArrayList<>();
    private Robot robot = null;
    private Manipulator manipulator = null;
    private Light light = null;

    private World physicsWorld = null;
    private long previousTime = 0;
    private Plane groundPlane = null;
    private Entity collisionPlane = null;

    private Mode mode = Mode.LISTENER;
    private Action currentAction = Action.NONE;

    private String cubeMesh = null;
    private String cylinderMesh = null;
    private String axisMesh = null;


    public GLRenderer(Context context, GestureHelper gestureHelper, Listener listener) {
        this.context = context;
        this.gestureHelper = gestureHelper;
        this.listener = listener;
        this.displayRotationHelper = new DisplayRotationHelper(context);
    }

    public DisplayRotationHelper getDisplayRotationHelper() {
        return displayRotationHelper;
    }


    public void setSession(Session session) {
        this.session = session;
    }


    public void setMode(Mode mode) {
        this.mode = mode;
    }


    public boolean isInMode(Mode mode) {
        return (this.mode == mode);
    }


    public void setAction(Action action) {
        currentAction = action;
    }


    public boolean isRobotPlaced() {
        return (robot != null);
    }

    private List<AnchorEntity> ellipsoides;


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Initialise the ROSManager API
        ROSManager.init(context);

        // Reinitialise the subsystems dependent on the OpenGL surface
        ShaderManager.init(context);
        MeshManager.init();
        TextureManager.init();
        backgroundRenderer.init(context);
        planeRenderer.init(context, "models/trigrid.png");
        pointCloudRenderer.init(context);

        // Pre-load the assets
        ROSManager.loadPandaArmAssets();
        cubeMesh = AssetsUtils.load3DAsset(context, "models/cube.dae").get(0);
        cylinderMesh = AssetsUtils.load3DAsset(context, "models/tin_can.dae").get(0);
        //axisMesh = AssetsUtils.load3DAsset(context, "models/axis.dae").get(0);

        // Load the default animation of the robot
        ROSManager.createPandaArmPlayback();

        // Put the robot in the starting position of the animation
        ROSManager.startPlayback();
        ROSManager.stopPlayback();

        // Create the camera
        camera = new ExternalCamera();

        // Create the manipulator
        manipulator = new Manipulator(context);

        // Create the light
        light = new Light();
        light.color.set(0.7f, 0.7f, 0.7f, 1.0f);
        light.transforms.setPosition(3.0f, 4.0f, 5.0f);

        // Create the physics world
        physicsWorld = new World();
        //physicsWorld.enableDebugDrawing(camera);
        previousTime = System.nanoTime();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);

        Viewport viewport = new ExternalViewport(width, height);
        camera.setViewport(viewport);
        viewport.apply();
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        long currentTime = System.nanoTime();
        float elapsed = 1e-9f * (currentTime - previousTime);
        previousTime = currentTime;

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera arCamera = frame.getCamera();

            // Handle one gesture per frame.
            handleGesture(frame, arCamera);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (arCamera.getTrackingState() == TrackingState.PAUSED) {
                SnackbarHelper.showMessage(
                        (Activity) context, TrackingStateHelper.getTrackingFailureReasonString(arCamera));
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            arCamera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            arCamera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            Color ambientLight = new Color(colorCorrectionRgba[0], colorCorrectionRgba[1],
                    colorCorrectionRgba[2], colorCorrectionRgba[3]);

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                SnackbarHelper.hide((Activity) context);
            } else {
                SnackbarHelper.showMessage((Activity) context, SEARCHING_PLANE_MESSAGE);
            }

            // Visualize planes
            Collection<Plane> allPlanes = session.getAllTrackables(Plane.class);
            planeRenderer.drawPlanes(allPlanes, arCamera.getDisplayOrientedPose(), projmtx);

            // If needed, create the physical representation of the ground plane
            if ((groundPlane != null) && (collisionPlane == null)) {
                CollisionShape shape = new BoxShape(
                    new javax.vecmath.Vector3f(
                        groundPlane.getExtentX() / 2.0f * World.SCALE,
                        0.04f * World.SCALE,
                        groundPlane.getExtentZ() / 2.0f * World.SCALE
                    )
                );

                float[] translation = groundPlane.getCenterPose().getTranslation();
                float[] rotation = groundPlane.getCenterPose().getRotationQuaternion();

                Quaternionf orientation = new Quaternionf(0.0f, rotation[1], 0.0f, rotation[3]);
                orientation.normalize();

                collisionPlane = new Entity();

                collisionPlane.transforms.setPosition(translation[0], translation[1], translation[2]);
                collisionPlane.transforms.setOrientation(orientation);

                PhysicBody body = new PhysicBody();
                collisionPlane.addPhysicBody(body);

                body.transforms.translate(0.0f, -0.04f, 0.0f);

                body.setStatic(physicsWorld, shape);
                body.setFriction(GROUND_FRICTION);
            }

            // Update the robot state
            if (robot != null) {
                if (mode == Mode.PLAYBACK) {
                    // Update the playback
                    ROSManager.updatePlayback();
                }
            }

            // Step the physics simulation
            physicsWorld.step(elapsed);

            // Use the view and projection matrices used by the AR for the visualization of the robot models
            camera.setViewMatrix(new Matrix4f(
                    viewmtx[0], viewmtx[1], viewmtx[2], viewmtx[3],
                    viewmtx[4], viewmtx[5], viewmtx[6], viewmtx[7],
                    viewmtx[8], viewmtx[9], viewmtx[10], viewmtx[11],
                    viewmtx[12], viewmtx[13], viewmtx[14], viewmtx[15]
            ));

            camera.getViewport().setProjectionMatrix(new Matrix4f(
                    projmtx[0], projmtx[1], projmtx[2], projmtx[3],
                    projmtx[4], projmtx[5], projmtx[6], projmtx[7],
                    projmtx[8], projmtx[9], projmtx[10], projmtx[11],
                    projmtx[12], projmtx[13], projmtx[14], projmtx[15]
            ));

            // Visualize the entities
            for (Entity entity : anchors) {
                entity.draw(camera, ambientLight, light);
            }

            if (collisionPlane != null) {
                collisionPlane.draw(camera, ambientLight, light);
            }

            physicsWorld.debugDraw();

            // Publish the current camera image if necessary
            if (ROSManager.imagePublisher != null) {
                Image image = frame.acquireCameraImage();
                ROSManager.imagePublisher.publish(image, displayRotationHelper.getScreenOrientation());
            }

            // Remove the dynamic entities that are too far below the ground
            for (Entity entity : anchors) {
                ArrayList<Entity> toRemove = new ArrayList<>();

                for (Entity child : entity.getChildren()) {
                    if ((child.getPhysicBody() != null) && (child.transforms.getWorldPosition().y() < -10.0f))
                        toRemove.add(child);
                }

                for (Entity child : toRemove)
                    child.destroy(physicsWorld);
            }

            // Remove the anchors that don't have any child anymore (to increase ARCore performances)
            ArrayList<Entity> toRemove = new ArrayList<>();
            for (Entity entity : anchors) {
                if (entity.getChildren().isEmpty())
                    toRemove.add(entity);
            }

            for (Entity child : toRemove) {
                child.destroy(physicsWorld);
                anchors.remove(child);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }


    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleGesture(Frame frame, Camera arCamera) {
        GestureEvent gestureEvent = gestureHelper.poll();

        if ((gestureEvent == null) || (arCamera.getTrackingState() != TrackingState.TRACKING))
            return;

        while (gestureEvent != null) {
            switch (gestureEvent.getType()) {
                case SINGLETAPUP: {

                    if (currentAction == Action.ADD_GOAL) {

                        Vector3f offset = new Vector3f(0.0f, 0.0f, 0.0f);
                        AnchorEntity anchor = ARHits.createAnchorOnPlane(
                                frame, camera, (int) gestureEvent.getX(), (int) gestureEvent.getY(),
                                groundPlane, anchors, offset);


                        if (anchor == null)
                            break;

                        Mesh mesh = MeshBuilder.buildEllipsoid("ellipsoid", 0.1f, 0.05f, 0.15f);

                        Material material = new Material();
                        material.ambient.set(0.0f, 0.2f, 0.0f, 1.0f);
                        material.diffuse.set(0.0f, 0.6f, 0.0f, 1.0f);
                        material.specular.set(0.8f, 0.8f, 0.8f, 1.0f);
                        material.shininess = 20.0f;

                        Mesh mesh2 = MeshManager.get(axisMesh);

                        CollisionShape shape = new SphereShape(0.04f * World.SCALE);

                        createDynamicObject(anchor, mesh, material, shape, 0.0f, 0.0f,
                                offset.add(0.0f, 0.0f, 0.0f), Transforms.IDENTITY);

                        //createViaPoint(anchor, mesh, mesh2, material, null, shape, 0.0f, 0.0f,
                        //        offset.add(0.0f, 0.04f, 0.0f), Transforms.IDENTITY);

                        /*createDynamicObject(anchor, mesh2, null, shape, 0.0f, 0.0f,
                                offset.add(0.0f, 0.04f, 0.0f), new Quaternionf(new AxisAngle4f(
                                (float)(new Random().nextFloat() * Math.PI), Transforms.UNIT_Y)));*/

                        //ellipsoides.add(anchor);

                    } else if (currentAction == Action.REMOVE){

                        // Did we touch an existing object?
                        Ray ray = new Ray(camera, gestureEvent.getX(), gestureEvent.getY());

                        PhysicBody body = physicsWorld.rayTest(ray);
                        if (body != null) {
                            Entity entity = body.getEntity();
                            if (entity != null) {
                                entity.destroy(physicsWorld);
                                anchors.remove(entity);
                            }
                        }

                    }else if ((currentAction == Action.PLACE_CUBE) ||
                        (currentAction == Action.PLACE_BALL) ||
                        (currentAction == Action.PLACE_CYLINDER) ||
                        (currentAction == Action.PLACE_CUBE_STACK) ||
                        (currentAction == Action.PLACE_CYLINDER_STACK)) {

                        Vector3f offset = new Vector3f();
                        AnchorEntity anchor = ARHits.createAnchorOnPlane(
                                frame, camera, (int) gestureEvent.getX(), (int) gestureEvent.getY(),
                                groundPlane, anchors, offset);

                        if (anchor == null)
                            break;

                        if (currentAction == Action.PLACE_CUBE) {
                            Mesh mesh = MeshManager.get(cubeMesh);

                            CollisionShape shape = new BoxShape(
                                    new javax.vecmath.Vector3f(
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE
                                    )
                            );

                            createDynamicObject(
                                    anchor, mesh, null, shape,
                                    CUBE_MASS, CUBE_FRICTION,
                                    offset.add(0.0f, 0.04f, 0.0f),
                                    new Quaternionf(
                                            new AxisAngle4f(
                                                    (float) (new Random().nextFloat() * Math.PI),
                                                    Transforms.UNIT_Y)
                                    )
                            );

                        } else if (currentAction == Action.PLACE_BALL) {
                            Mesh mesh = MeshBuilder.buildSphere(null, 0.04f);

                            Material material = new Material();
                            material.ambient.set(0.2f, 0.0f, 0.0f, 1.0f);
                            material.diffuse.set(0.6f, 0.0f, 0.0f, 1.0f);
                            material.specular.set(0.8f, 0.8f, 0.8f, 1.0f);
                            material.shininess = 20.0f;

                            CollisionShape shape = new SphereShape(0.04f * World.SCALE);

                            createDynamicObject(
                                    anchor, mesh, material, shape,
                                    BALL_MASS, BALL_FRICTION,
                                    offset.add(0.0f, 0.04f, 0.0f),
                                    Transforms.IDENTITY
                            );

                        } else if (currentAction == Action.PLACE_CYLINDER) {
                            Mesh mesh = MeshManager.get(cylinderMesh);

                            CollisionShape shape = new CylinderShape(
                                    new javax.vecmath.Vector3f(
                                            0.03f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.03f * World.SCALE
                                    )
                            );

                            createDynamicObject(
                                    anchor, mesh, null, shape,
                                    CYLINDER_MASS, CYLINDER_FRICTION,
                                    offset.add(0.0f, 0.04f, 0.0f),
                                    new Quaternionf(
                                            new AxisAngle4f(
                                                    (float) (new Random().nextFloat() * 2 * Math.PI),
                                                    Transforms.UNIT_Y)
                                    )
                            );

                        } else if (currentAction == Action.PLACE_CUBE_STACK) {
                            Mesh mesh = MeshManager.get(cubeMesh);

                            CollisionShape shape = new BoxShape(
                                    new javax.vecmath.Vector3f(
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE
                                    )
                            );

                            Random random = new Random();

                            for (int i = 0; i < 7; ++i) {
                                createDynamicObject(
                                        anchor, mesh, null, shape,
                                        CUBE_MASS, CUBE_FRICTION,
                                        new Vector3f(offset.x, offset.y + 0.04f + i * 0.08f, offset.z),
                                        new Quaternionf(
                                                new AxisAngle4f(
                                                        (float) (random.nextFloat() * Math.PI),
                                                        Transforms.UNIT_Y)
                                        )
                                );
                            }

                        } else if (currentAction == Action.PLACE_CYLINDER_STACK) {
                            Mesh mesh = MeshManager.get(cylinderMesh);

                            CollisionShape shape = new CylinderShape(
                                    new javax.vecmath.Vector3f(
                                            0.03f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.03f * World.SCALE
                                    )
                            );

                            Random random = new Random();

                            for (int i = 0; i < 7; ++i) {
                                createDynamicObject(
                                        anchor, mesh, null, shape,
                                        CYLINDER_MASS, CYLINDER_FRICTION,
                                        new Vector3f(offset.x, offset.y + 0.04f + i * 0.08f, offset.z),
                                        new Quaternionf(
                                                new AxisAngle4f(
                                                        (float) (random.nextFloat() * 2 * Math.PI),
                                                        Transforms.UNIT_Y)
                                        )
                                );
                            }
                        }

                    } else if (currentAction == Action.NONE) {
                        // Did we touch an existing object?
                        Ray ray = new Ray(camera, gestureEvent.getX(), gestureEvent.getY());

                        if (manipulator.intersects(ray))
                            break;

                        PhysicBody body = physicsWorld.rayTest(ray);
                        if (body != null) {
                            Entity entity = body.getEntity();
                            if (entity != null) {
                                if (!manipulator.isSelected(entity)) {
                                    AnchorEntity anchor = AnchorEntity.class.cast(entity.getParent());
                                    manipulator.select(anchor);
                                }
                                break;
                            }
                        }

                        // Only allow one robot
                        if (robot != null) {
                            manipulator.select(null);
                            break;
                        }

                        // Did we touch an AR plane?
                        MotionEvent event = gestureEvent.getEvent1();

                        // Determine the horizontal plane hit by the touch
                        HitResult hit = ARHits.filterHits(frame, frame.hitTest(event));

                        if (hit != null) {
                            // Adding an Anchor tells ARCore that it should track this position in
                            // space. This anchor is created on the Plane to place the 3D model
                            // in the correct position relative both to the world and to the plane.
                            AnchorEntity anchor = new AnchorEntity(hit.createAnchor());

                            robot = ROSManager.createPandaArm(physicsWorld);
                            robot.setParent(anchor);

                            anchors.add(anchor);

                            manipulator.select(anchor);

                            groundPlane = (Plane) hit.getTrackable();

                            listener.onRobotCreated();
                        }

                    } else {
                        float x = (gestureEvent.getX() * 2.0f) / camera.getViewport().getWidth() - 1.0f;
                        float y = ((camera.getViewport().getHeight() - gestureEvent.getY()) * 2.0f) / camera.getViewport().getHeight() - 1.0f;

                        Vector4f screenPosition = new Vector4f(x, y, -0.9f, 1.0f);
                        Vector4f position = new Vector4f();

                        Matrix4f invertedProjectionMatrix = new Matrix4f();
                        camera.getViewport().getProjectionMatrix().mul(camera.getViewMatrix(), invertedProjectionMatrix);
                        invertedProjectionMatrix.invert();

                        invertedProjectionMatrix.transform(screenPosition, position);
                        position = position.div(position.w);

                        // Create a new anchor or find the nearest one
                        AnchorEntity anchor = ARHits.createOrFindNearestAnchor(
                                frame, frame.hitTest(gestureEvent.getEvent1()), anchors,
                                new Vector3f(position.x, position.y, position.z)
                        );

                        if (anchor == null)
                            break;

                        Mesh mesh;
                        Material material = null;
                        CollisionShape shape;
                        float mass;
                        float friction;

                        if ((currentAction == Action.DROP_BALL) || (currentAction == Action.THROW_BALL)) {
                            mesh = MeshBuilder.buildSphere(null, 0.04f);

                            material = new Material();
                            material.ambient.set(0.2f, 0.0f, 0.0f, 1.0f);
                            material.diffuse.set(0.6f, 0.0f, 0.0f, 1.0f);
                            material.specular.set(0.8f, 0.8f, 0.8f, 1.0f);
                            material.shininess = 20.0f;

                            shape = new SphereShape(0.04f * World.SCALE);

                            mass = BALL_MASS;
                            friction = BALL_FRICTION;

                        } else if ((currentAction == Action.DROP_CUBE) || (currentAction == Action.THROW_CUBE)) {
                            mesh = MeshManager.get(cubeMesh);

                            shape = new BoxShape(
                                    new javax.vecmath.Vector3f(
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.04f * World.SCALE
                                    )
                            );

                            mass = CUBE_MASS;
                            friction = CUBE_FRICTION;

                        } else if ((currentAction == Action.DROP_CYLINDER) || (currentAction == Action.THROW_CYLINDER)) {
                            mesh = MeshManager.get(cylinderMesh);

                            shape = new CylinderShape(
                                    new javax.vecmath.Vector3f(
                                            0.03f * World.SCALE,
                                            0.04f * World.SCALE,
                                            0.03f * World.SCALE
                                    )
                            );

                            mass = CYLINDER_MASS;
                            friction = CYLINDER_FRICTION;

                        } else {
                            break;
                        }

                        Entity entity = new Entity();
                        entity.addRenderable(new Renderable(mesh, material));
                        entity.setParent(anchor);

                        Vector3f relativePosition =
                                new Vector3f(position.x, position.y, position.z).sub(
                                        anchor.transforms.getWorldPosition());

                        entity.transforms.translate(
                                relativePosition, Transforms.TRANSFORM_SPACE_WORLD
                        );

                        Quaternionf relativeRotation =
                                new Quaternionf(anchor.transforms.getWorldOrientation()).invert().mul(
                                        0.0f, 0.0f, 0.0f, 1.0f);

                        entity.transforms.rotate(
                                relativeRotation, Transforms.TRANSFORM_SPACE_WORLD
                        );

                        PhysicBody body = new PhysicBody();
                        entity.addPhysicBody(body);

                        body.setDynamic(physicsWorld, mass, shape);
                        body.setFriction(friction);

                        if ((currentAction == Action.THROW_BALL) ||
                            (currentAction == Action.THROW_CUBE) ||
                            (currentAction == Action.THROW_CYLINDER)) {

                            Vector4f farScreenPosition = new Vector4f(x, y, 1.0f, 1.0f);
                            Vector4f farPosition = new Vector4f();

                            invertedProjectionMatrix.transform(farScreenPosition, farPosition);
                            farPosition = farPosition.div(farPosition.w);

                            Vector4f force = farPosition.sub(position).normalize();
                            force.mul(100.0f * World.SCALE);

                            body.getRigidBody().applyForce(
                                    new javax.vecmath.Vector3f(force.x, force.y, force.z),
                                    new javax.vecmath.Vector3f(0.0f, 0.0f, 0.0f)
                            );
                        }
                    }

                    break;
                }

                default: {
                    manipulator.processEvent(gestureEvent, camera, frame);
                    break;
                }
            }

            gestureEvent = gestureHelper.poll();
        }
    }


    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }


    private void createDynamicObject(
            AnchorEntity anchor, Mesh mesh, Material material, CollisionShape shape,
            float mass, float friction, Vector3fc position, Quaternionfc orientation) {

        Entity entity = new Entity();
        entity.addRenderable(new Renderable(mesh, material));
        entity.setParent(anchor);

        entity.transforms.translate(position);
        entity.transforms.rotate(orientation);

        PhysicBody body = new PhysicBody();
        entity.addPhysicBody(body);

        body.setDynamic(physicsWorld, mass, shape);
        body.setFriction(friction);

        if (new Vector3f(position.x(), 0.0f, position.z()).length() < 0.1f)
            body.getRigidBody().setActivationState(CollisionObject.WANTS_DEACTIVATION);
    }

    // combine the 3-axis mesh with the ellipsoid for the via-points
    private void createViaPoint(AnchorEntity anchor, Mesh mesh1, Mesh mesh2, Material material1,
                                Material material2, CollisionShape shape, float mass,
                                float friction, Vector3fc position, Quaternionfc orientation) {

        Entity entity = new Entity();
        entity.addRenderable(new Renderable(mesh1, material1));
        entity.addRenderable(new Renderable(mesh2, material2));
        entity.setParent(anchor);

        entity.transforms.translate(position);
        entity.transforms.rotate(orientation);

        PhysicBody body = new PhysicBody();
        entity.addPhysicBody(body);

        body.setDynamic(physicsWorld, mass, shape);
        body.setFriction(friction);

        if (new Vector3f(position.x(), 0.0f, position.z()).length() < 0.1f)
            body.getRigidBody().setActivationState(CollisionObject.WANTS_DEACTIVATION);
    }
}
