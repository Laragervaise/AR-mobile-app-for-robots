/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.entities;


public abstract class Transformable
{

    //_____ Attributes __________

    public Transforms transforms = null;
    public String tag = null;


    //_____ Methods __________

    public Transformable() {
        transforms = new Transforms(this);
    }


    public void setParent(Transformable transformable) {
        if (transformable != null)
            transforms.setParent(transformable.transforms);
        else
            transforms.setParent(null);
    }

}
