/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.meshes.loaders;


import android.content.Context;

import java.util.List;


public interface Loader {

    // Represents the transformations that must be applied to the coordinates in a 3D asset
    // file to convert them to the one used by OpenGL
    enum CoordinatesSystemTransformation {

        // From right-hand OpenGL XYZ coordinates system: no change
        //    y
        //    |
        //    |
        //    ------ x
        //   /
        //  /
        // z
        X__Y__Z,


        // From right-hand Blender XYZ coordinates system
        // z  y
        // | /
        // |/
        // ------ x
        X__Z__MINUS_Y,
    };


    List<String> load(Context context, String filename);

    List<String> load(Context context, String filename, CoordinatesSystemTransformation transform);
}
