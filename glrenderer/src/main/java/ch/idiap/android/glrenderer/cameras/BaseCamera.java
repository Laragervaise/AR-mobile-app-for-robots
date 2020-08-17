/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.cameras;


import org.joml.Matrix4fc;

import ch.idiap.android.glrenderer.entities.Transformable;
import ch.idiap.android.glrenderer.viewports.Viewport;


public abstract class BaseCamera extends Transformable {

    //_____ Attributes __________

    protected Viewport viewport = null;


    //_____ Methods __________

    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }


    //_____ Methods to implement __________

    abstract public Matrix4fc getViewMatrix();
}
