/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.helpers;


import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import ch.idiap.android.glrenderer.meshes.loaders.ColladaLoader;
import ch.idiap.android.glrenderer.meshes.loaders.Loader;
import ch.idiap.android.glrenderer.meshes.loaders.StlLoader;


public class AssetsUtils {

    static public InputStream loadAsset(Context context, String filename) {
        try {
            return context.getAssets().open(filename);
        } catch(IOException e) {
            return null;
        }
    }


    static public String loadAssetAsString(Context context, String filename) {
        try {
            InputStream stream = context.getAssets().open(filename);

            StringBuilder builder = new StringBuilder();
            String line;

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()))) {

                while ((line = bufferedReader.readLine()) != null)
                    builder.append(line + "\n");
            }

            return builder.toString();

        } catch(IOException e) {
            return null;
        }
    }


    static public List<String> load3DAsset(Context context, String filename) {
        return load3DAsset(context, filename, Loader.CoordinatesSystemTransformation.X__Y__Z);
    }


    static public List<String> load3DAsset(Context context, String filename,
                                           Loader.CoordinatesSystemTransformation transform) {
        Loader loader = null;

        if (filename.toLowerCase().endsWith(".dae")) {
            loader = new ColladaLoader();

        } else if (filename.toLowerCase().endsWith(".stl")) {
            loader = new StlLoader();

        } else {
            return null;
        }

        return loader.load(context, filename, transform);
    }
}
