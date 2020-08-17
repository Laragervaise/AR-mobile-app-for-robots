/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.materials;


import java.util.HashMap;
import java.util.Map;


public class MaterialManager {

    static private Map<String, Material> materials = new HashMap<>();


    static public void register(Material material) {
        materials.put(material.getName(), material);
    }


    static public Material get(String name) {
        return materials.get(name);
    }
}
