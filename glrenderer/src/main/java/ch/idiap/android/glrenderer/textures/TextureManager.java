/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.textures;


import java.util.HashMap;
import java.util.Map;


public class TextureManager
{
    static private Map<String, Texture> textures = new HashMap<>();


    // Must be called when the OpenGL surface is created
    static public void init() {
        for (Texture texture : textures.values())
            texture.createGLBuffer();
    }


    static public void register(Texture texture) {
        textures.put(texture.getName(), texture);
    }


    static public Texture get(String name) {
        return textures.get(name);
    }
}
