/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.arcore.ar;

import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;

import org.joml.Vector3f;

import java.util.List;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.rays.Ray;
import ch.idiap.android.urdfviewer.examples.arcore.entities.AnchorEntity;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.renderers.PlaneRenderer;


public class ARHits {

    static public HitResult filterHits(Frame frame, List<HitResult> hits) {

        // Determine the horizontal plane hit by the touch
        for (HitResult hit : hits) {

            // Check if any plane was hit
            Trackable trackable = hit.getTrackable();

            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;

                // Check if the plane was hit inside the plane polygon
                if (!plane.isPoseInPolygon(hit.getHitPose()) ||
                        (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), frame.getCamera().getPose()) <= 0)) {
                    break;
                }

                // Check if the plane is horizontal
                float[] planeYAxis = plane.getCenterPose().getYAxis();

                Vector3f planeUp = new Vector3f(planeYAxis[0], planeYAxis[1], planeYAxis[2]).normalize();

                if ((planeUp.dot(Transforms.UNIT_X) < 0.05f) && (planeUp.dot(Transforms.UNIT_Z) < 0.05f)) {
                    return hit;
                }
            }
        }

        return null;
    }


    static public AnchorEntity createAnchorOnPlane(
            Frame frame, BaseCamera camera, int touch_x, int touch_y, Plane plane,
            List<AnchorEntity> anchors, Vector3f offset) {

        // Determine the horizontal plane hit by the touch
        List<HitResult> hits = frame.hitTest(touch_x, touch_y);
        HitResult hit = ARHits.filterHits(frame, hits);

        if ((hit != null) && hit.getTrackable().equals(plane)) {
            offset.x = 0.0f;
            offset.y = 0.0f;
            offset.z = 0.0f;

            AnchorEntity anchor = new AnchorEntity(hit.createAnchor());
            anchors.add(anchor);

            return anchor;
        }

        // Extend the reference plane to infinity
        Ray ray = new Ray(camera, touch_x, touch_y);

        if (!ray.intersectsPlane(Transforms.UNIT_Y, -plane.getCenterPose().ty(), offset))
            return null;

        float[] plane_position = plane.getCenterPose().getTranslation();
        Vector3f direction = new Vector3f(
                plane_position[0], plane_position[1], plane_position[2]).sub(offset);

        float length = 10.0f * direction.length();

        direction.normalize();

        float[] newOrigin = new float[3];

        float[] newDirection = new float[3];
        newDirection[0] = 0.0f;
        newDirection[1] = -1.0f;
        newDirection[2] = 0.0f;

        for (int i = 1; i <= length; ++i)
        {
            Vector3f position = new Vector3f(
                    offset).add(direction.x * i / 10.0f, direction.y * i / 10.0f, direction.y * i / 10.0f);

            newOrigin[0] = position.x;
            newOrigin[1] = position.y + 0.01f;
            newOrigin[2] = position.z;

            hit = ARHits.filterHits(frame, frame.hitTest(newOrigin, 0, newDirection, 0));
            if (hit != null) {
                AnchorEntity anchor = new AnchorEntity(hit.createAnchor());
                anchors.add(anchor);

                offset.sub(anchor.transforms.getWorldPosition());

                return anchor;
            }
        }

        return null;
    }


    static public AnchorEntity createOrFindNearestAnchor(
            Frame frame, List<HitResult> hits, List<AnchorEntity> anchors, Vector3f position) {

        AnchorEntity anchor = null;

        HitResult hit = ARHits.filterHits(frame, hits);
        if (hit != null) {
            anchor = new AnchorEntity(hit.createAnchor());
            anchors.add(anchor);
        } else {
            anchor = findNearestAnchor(anchors, position);
        }

        return anchor;
    }


    static public AnchorEntity findNearestAnchor(List<AnchorEntity> anchors, Vector3f position) {
        float dist = Float.POSITIVE_INFINITY;
        AnchorEntity anchor = null;

        for (AnchorEntity anchor2 : anchors) {
            float dist2 = anchor2.transforms.getWorldPosition().distanceSquared(position);
            if (dist2 < dist) {
                anchor = anchor2;
                dist = dist2;
            }
        }

        return anchor;
    }

}
