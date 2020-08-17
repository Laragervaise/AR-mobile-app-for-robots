/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.arcore.entities;


import com.google.ar.core.Anchor;

import org.joml.Vector3f;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.entities.Entity;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.glrenderer.rays.Ray;
import ch.idiap.android.glrenderer.entities.Transforms;


public class AnchorEntity extends Entity {

    public Anchor anchor = null;


    public AnchorEntity(Anchor anchor) {
        super();

        this.anchor = anchor;

        updateTransforms();
    }


    @Override
    public void destroy(World world) {
        anchor.detach();

        super.destroy(world);
    }


    public void replaceAnchor(Anchor anchor) {
        this.anchor.detach();

        this.anchor = anchor;

        updateTransforms();
    }


    @Override
    public void draw(BaseCamera camera, Color ambientLight, Light light) {
        updateTransforms();

        super.draw(camera, ambientLight, light);
    }


    public boolean intersects(Ray ray) {
        Vector3f mean = new Vector3f(Transforms.ZERO);

        for (Transforms child: transforms.getChildren())
            mean.add(child.getWorldPosition());

        mean.div(transforms.getChildren().size());

        Vector3f rayResult = new Vector3f();
        return ray.intersectsSphere(mean, 0.2f, rayResult);
    }


    private void updateTransforms() {
        float[] position = anchor.getPose().getTranslation();
        float[] orientation = anchor.getPose().getRotationQuaternion();

        transforms.setPosition(position[0], position[1], position[2]);
        transforms.setOrientation(orientation[0], orientation[1], orientation[2], orientation[3]);
    }
}
