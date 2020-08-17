/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.lights;

import ch.idiap.android.glrenderer.entities.Transformable;
import ch.idiap.android.glrenderer.materials.Color;


public class Light extends Transformable {

    public Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

}
