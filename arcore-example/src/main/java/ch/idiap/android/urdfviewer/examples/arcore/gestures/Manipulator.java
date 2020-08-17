/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.arcore.gestures;


import android.content.Context;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;

import org.joml.AABBf;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.lang.Math;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.entities.Renderable;
import ch.idiap.android.glrenderer.helpers.AssetsUtils;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.urdf.robot.Robot;
import ch.idiap.android.urdfviewer.examples.arcore.ar.ARHits;
import ch.idiap.android.urdfviewer.examples.arcore.entities.AnchorEntity;
import ch.idiap.android.glrenderer.rays.Ray;
import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.entities.Entity;


public class Manipulator {

    private final float DOWN_RAY_OFFSET = 0.01f;
    private final float MAX_TRANSLATION_DISTANCE = 3.0f;


    private enum Action {
        ACTION_NONE,
        ACTION_TRANSLATE_XZ,
        ACTION_ROTATE,
        ACTION_SCALE,
        ACTION_TRANSLATE_Y,
        ACTION_ROTATE_Y,
        ACTION_ROTATE_X,
    }


    class Placement {
        public HitResult hit = null;
        public Vector3f desiredPosition = new Vector3f();
        boolean planeChanged = false;
    }


    private Action action = Action.ACTION_NONE;
    private Entity entity = null;
    private AnchorEntity target = null;
    private HitResult lastHit = null;

    private static final String TAG = Manipulator.class.getSimpleName();


    public Manipulator(Context context) {
        entity = new Entity();

        List<String> meshNames = AssetsUtils.load3DAsset(context, "models/manipulator.dae");

        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_NONE.toString();
            entity.addRenderable(renderable);
        }

        meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_move.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_TRANSLATE_XZ.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }

        /*meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_rotate.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_ROTATE.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }*/

        meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_rotate.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_ROTATE_Y.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }

        /*meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_rotate_90.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_ROTATE_X.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }*/

        /*meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_move_Y.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_TRANSLATE_Y.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }

        meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_rotate_Z.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_ROTATE_Z.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }

        meshNames = AssetsUtils.load3DAsset(context, "models/manipulator_rotate_X.dae");
        for (String name: meshNames) {
            Renderable renderable = new Renderable(MeshManager.get(name));
            renderable.tag = Action.ACTION_ROTATE_X.toString();
            renderable.setVisible(false);
            entity.addRenderable(renderable);
        }*/
    }


    public void select(AnchorEntity anchor) {
        if (target != null) {
            Vector3fc position = entity.transforms.getPosition();

            while (!entity.getChildren().isEmpty()) {
                Entity child = entity.getChildren().get(0);

                child.transforms.translate(position.x(), 0.0f, position.z(), Transforms.TRANSFORM_SPACE_PARENT);
                child.transforms.rotate(entity.transforms.getOrientation());
                child.transforms.scale(entity.transforms.getScale());
                child.setParent(target);

                if ((child.getPhysicBody() != null) && (child.getPhysicBody().isKinematic()))
                    child.getPhysicBody().switchToDynamic();
            }

            entity.setParent(null);
        }

        target = anchor;
        action = Action.ACTION_NONE;

        entity.transforms.setPosition(Transforms.ZERO);
        entity.transforms.setOrientation(Transforms.IDENTITY);
        entity.transforms.setScale(Transforms.UNIT_SCALE);

        if (target != null) {
            Vector3f mean = new Vector3f(Transforms.ZERO);

            if (target.getChildren().size() > 0) {
                for (Entity child : target.getChildren())
                    mean.add(child.transforms.getPosition());

                mean.div(target.transforms.getChildren().size());

                entity.transforms.translate(mean.x, mean.y, mean.z);
            }

            while (!target.getChildren().isEmpty()) {
                Entity child = target.getChildren().get(0);
                child.setParent(entity);
                child.transforms.translate(-mean.x, -mean.y, -mean.z, Transforms.TRANSFORM_SPACE_PARENT);

                if ((child.getPhysicBody() != null) && (child.getPhysicBody().isDynamic())) {
                    child.transforms.translate(0.0f, 0.01f, 0.0f, Transforms.TRANSFORM_SPACE_WORLD);
                    child.getPhysicBody().switchToKinematic();
                }
            }

            entity.setParent(target);

            float scale = getTargetDiameter() / 0.4f;

            for (Renderable renderable: entity.getRenderables()) {
                renderable.transforms.setScale(scale, scale, scale);
                renderable.setVisible(renderable.tag.equals(Action.ACTION_NONE.toString()));
            }
        }
    }


    public boolean hasSelection() {
        return (target != null);
    }


    public boolean intersects(Ray ray) {
        if (!hasSelection())
            return false;

        Vector3f rayResult = new Vector3f();
        return ray.intersectsSphere(
                entity.transforms.getWorldPosition(), getTargetDiameter() * 0.5f, rayResult
        );
    }


    public boolean isSelected(Entity entity) {
        return this.entity.getChildren().contains(entity);
    }


    public void processEvent(GestureEvent event, BaseCamera camera, Frame frame) {

        // Don't do anything if nothing is selected
        if (target == null)
            return;

        // What to do depends on the current action
        switch (action) {
            case ACTION_NONE: {
                if (event.getType() == GestureEvent.GestureType.SCROLL) {

                    Ray ray = new Ray(camera, event.getX(), event.getY());

                    if (intersects(ray)) {
                        action = Action.ACTION_TRANSLATE_XZ;

                        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
                            renderable.setVisible(false);

                        for (Renderable renderable: entity.getRenderables(Action.ACTION_TRANSLATE_XZ.toString()))
                            renderable.setVisible(true);

                        translate_XZ(event, frame, camera);
                    } else {
                        /*action = Action.ACTION_ROTATE;

                        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
                            renderable.setVisible(false);

                        for (Renderable renderable: entity.getRenderables(Action.ACTION_ROTATE.toString()))
                            renderable.setVisible(true);

                        rotate_Y(event, frame, camera);
                        rotate_X(event, frame, camera); */

                        if(Math.abs(event.getDistanceX()) > Math.abs(event.getDistanceY())) {

                            action = Action.ACTION_ROTATE_Y;

                            for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
                                renderable.setVisible(false);

                            for (Renderable renderable: entity.getRenderables(Action.ACTION_ROTATE_Y.toString()))
                                renderable.setVisible(true);

                            rotate_Y(event, frame, camera);

                        } else {

                            action = Action.ACTION_ROTATE_X;

                            for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
                                renderable.setVisible(false);

                            /*for (Renderable renderable: entity.getRenderables(Action.ACTION_ROTATE_X.toString()))
                                renderable.setVisible(true);*/

                            rotate_X(event, frame, camera);

                        }
                    }
                } else if (event.getType() == GestureEvent.GestureType.FLING) {  // swipe
                    if (intersects(new Ray(camera, event.getX(), event.getY()))) {

                        action = Action.ACTION_TRANSLATE_Y;

                        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
                            renderable.setVisible(false);
                        /*for (Renderable renderable: entity.getRenderables(Action.ACTION_TRANSLATE_Y.toString()))
                                renderable.setVisible(true);*/ // manipulator rendering not implemented

                        translate_Y(event, frame, camera);
                    }
                }
                break;
            }

            case ACTION_TRANSLATE_XZ: {
                if (event.getType() == GestureEvent.GestureType.SCROLL)
                    translate_XZ(event, frame, camera);
                else if (event.getType() == GestureEvent.GestureType.UP)
                    endTranslation(event, frame, camera);
                break;
            }

            case ACTION_ROTATE: {
                if (event.getType() == GestureEvent.GestureType.SCROLL) {
                    rotate_Y(event, frame, camera);
                    rotate_X(event, frame, camera);
                } else if (event.getType() == GestureEvent.GestureType.UP)
                    endRotation();
                break;
            }

            case ACTION_ROTATE_Y: {
                if (event.getType() == GestureEvent.GestureType.SCROLL) {
                    rotate_Y(event, frame, camera);
                } else if (event.getType() == GestureEvent.GestureType.UP) {
                    endRotation();
                }
                break;
            }

            case ACTION_ROTATE_X: {
                if (event.getType() == GestureEvent.GestureType.SCROLL) {
                    rotate_X(event, frame, camera);
                } else if (event.getType() == GestureEvent.GestureType.UP) {
                    /*action = Action.ACTION_NONE;
                    for (Renderable renderable : entity.getRenderables(Action.ACTION_NONE.toString()))
                        renderable.setVisible(true);
                    /*for (Renderable renderable: entity.getRenderables(Action.ACTION_ROTATE_X.toString()))
                                renderable.setVisible(false);*/
                    //select(target);  // to not combine the rotations
                    endRotation();
                }
                break;
            }

            case ACTION_TRANSLATE_Y: {
                if (event.getType() == GestureEvent.GestureType.FLING) {
                    translate_Y(event, frame, camera);
                } else if (event.getType() == GestureEvent.GestureType.UP)
                    endTranslation_Y(event, frame, camera);
                break;
            }
        }
    }


    private void translate_XZ(GestureEvent event, Frame frame, BaseCamera camera) {
        Placement placement = getHit(frame, camera, (int) event.getEvent2().getX(), (int) event.getEvent2().getY());
        if (placement == null)
            return;

        if (placement.planeChanged) {
            Quaternionfc desiredOrientation = entity.transforms.getWorldOrientation();

            target.replaceAnchor(placement.hit.createAnchor());

            Quaternionfc currentOrientation = target.transforms.getWorldOrientation();

            Quaternionf relativeOrientation = new Quaternionf(desiredOrientation);
            relativeOrientation.mul(new Quaternionf(currentOrientation).invert());

            entity.transforms.setOrientation(Transforms.IDENTITY);
            entity.transforms.rotate(relativeOrientation, Transforms.TRANSFORM_SPACE_WORLD);
        }

        Vector3fc currentPosition = target.transforms.getWorldPosition();

        Vector3f relativeTranslation = new Vector3f(placement.desiredPosition);
        relativeTranslation.sub(currentPosition);

        if (relativeTranslation.length() > MAX_TRANSLATION_DISTANCE)
            relativeTranslation.normalize().mul(MAX_TRANSLATION_DISTANCE);

        entity.transforms.setPosition(Transforms.ZERO);
        entity.transforms.translate(relativeTranslation, Transforms.TRANSFORM_SPACE_WORLD);

        if (placement.hit != null)
            lastHit = placement.hit;
    }

    private void translate_Y(GestureEvent event, Frame frame, BaseCamera camera) {
        //entity.transforms.translate(position) += (int) event.getEvent2().getY();
        translate_XZ(event, frame, camera);
    }

    private void endTranslation_Y(GestureEvent event, Frame frame, BaseCamera camera) {
        if (lastHit != null) {
            Vector3fc desiredPosition = entity.transforms.getWorldPosition();
            Quaternionfc desiredOrientation = entity.transforms.getWorldOrientation();

            target.replaceAnchor(lastHit.createAnchor());

            Vector3fc currentPosition = target.transforms.getWorldPosition();

            Vector3f relativeTranslation = new Vector3f(desiredPosition);
            relativeTranslation.sub(currentPosition);

            entity.transforms.setPosition(Transforms.ZERO);
            entity.transforms.translate(relativeTranslation, Transforms.TRANSFORM_SPACE_WORLD);

            Quaternionfc currentOrientation = target.transforms.getWorldOrientation();

            Quaternionf relativeOrientation = new Quaternionf(desiredOrientation);
            relativeOrientation.mul(new Quaternionf(currentOrientation).invert());

            entity.transforms.setOrientation(Transforms.IDENTITY);
            entity.transforms.rotate(relativeOrientation, Transforms.TRANSFORM_SPACE_WORLD);

            lastHit = null;
        }

        action = Action.ACTION_NONE;

        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
            renderable.setVisible(true);
    }


    private void endTranslation(GestureEvent event, Frame frame, BaseCamera camera) {
        if (lastHit != null) {
            Vector3fc desiredPosition = entity.transforms.getWorldPosition();
            Quaternionfc desiredOrientation = entity.transforms.getWorldOrientation();

            target.replaceAnchor(lastHit.createAnchor());

            Vector3fc currentPosition = target.transforms.getWorldPosition();

            Vector3f relativeTranslation = new Vector3f(desiredPosition);
            relativeTranslation.sub(currentPosition);

            entity.transforms.setPosition(Transforms.ZERO);
            entity.transforms.translate(relativeTranslation, Transforms.TRANSFORM_SPACE_WORLD);

            Quaternionfc currentOrientation = target.transforms.getWorldOrientation();

            Quaternionf relativeOrientation = new Quaternionf(desiredOrientation);
            relativeOrientation.mul(new Quaternionf(currentOrientation).invert());

            entity.transforms.setOrientation(Transforms.IDENTITY);
            entity.transforms.rotate(relativeOrientation, Transforms.TRANSFORM_SPACE_WORLD);

            lastHit = null;
        }

        action = Action.ACTION_NONE;

        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
            renderable.setVisible(true);

        for (Renderable renderable: entity.getRenderables(Action.ACTION_TRANSLATE_XZ.toString()))
            renderable.setVisible(false);
    }


    private void rotate_Y(GestureEvent event, Frame frame, BaseCamera camera) {
        float rotationAmount = event.getDistanceX() / camera.getViewport().getWidth() * 2.0f;
        entity.transforms.yaw(rotationAmount);
    }

    private void rotate_X(GestureEvent event, Frame frame, BaseCamera camera) {
        float rotationAmount = - event.getDistanceY() / camera.getViewport().getWidth() * 2.0f;
        entity.transforms.pitch(rotationAmount);
    }


    private void endRotation() {
        action = Action.ACTION_NONE;

        for (Renderable renderable: entity.getRenderables(Action.ACTION_NONE.toString()))
            renderable.setVisible(true);

        for (Renderable renderable: entity.getRenderables(Action.ACTION_ROTATE_Y.toString()))
            renderable.setVisible(false);

        select(null); // to not combine the rotations
        select(target);
    }


    private Placement getHit(Frame frame, BaseCamera camera, int touch_x, int touch_y) {

        // Determine the horizontal plane hit by the touch
        HitResult hit = ARHits.filterHits(frame, frame.hitTest(touch_x, touch_y));

        if (hit != null) {
            float[] desiredPosition = hit.getHitPose().getTranslation();

            Placement placement = new Placement();
            placement.hit = hit;
            placement.desiredPosition = new Vector3f(desiredPosition[0], desiredPosition[1], desiredPosition[2]);

            return placement;
        }

        // No plane found: extend the current grounding plane to infinity
        Ray ray = new Ray(camera, touch_x, touch_y);
        Vector3f groundingPoint = new Vector3f();

        if (!ray.intersectsPlane(Transforms.UNIT_Y, -entity.transforms.getWorldPosition().y(), groundingPoint)) {
            return null;
        }

        // Cast straight down onto AR planes that are lower than the current grounding plane
        float[] newOrigin = new float[3];
        newOrigin[0] = groundingPoint.x;
        newOrigin[1] = groundingPoint.y - DOWN_RAY_OFFSET;
        newOrigin[2] = groundingPoint.z;

        float[] newDirection = new float[3];
        newDirection[0] = 0.0f;
        newDirection[1] = 0.0f; // -1
        newDirection[2] = 0.0f;

        hit = ARHits.filterHits(frame, frame.hitTest(newOrigin, 0, newDirection, 0));
        if (hit != null) {
            float[] desiredPosition = hit.getHitPose().getTranslation();

            Placement placement = new Placement();
            placement.hit = hit;
            placement.desiredPosition = new Vector3f(desiredPosition[0], desiredPosition[1], desiredPosition[2]);
            placement.planeChanged = true;

            return placement;
        }

        Placement placement = new Placement();
        groundingPoint.y = entity.transforms.getWorldPosition().y(); //
        placement.desiredPosition = groundingPoint;
        return placement;
    }


    private float getTargetDiameter() {
        if (target == null)
            return 0.0f;

        float diameter = 0.4f;

        if ((entity.getChildren().size() != 1) || !(entity.getChildren().get(0) instanceof Robot)) {
            AABBf boundingBox = new AABBf();

            for (Entity child : entity.getChildren())
                boundingBox.union(child.getBoundingBox());

            diameter = new Vector3f(boundingBox.maxX, 0.0f, boundingBox.maxZ)
                                .distance(boundingBox.minX, 0.0f, boundingBox.minZ);
        }

        return diameter;
    }
}
