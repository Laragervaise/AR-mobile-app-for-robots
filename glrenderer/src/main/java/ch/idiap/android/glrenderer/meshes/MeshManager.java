/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.meshes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MeshManager {

    static private Map<String, Mesh> meshes = new HashMap<>();


    // Must be called when the OpenGL surface is created
    static public void init() {
        for (Mesh mesh: meshes.values()) {
            for (SubMesh subMesh: mesh.getSubMeshes())
                subMesh.createGLBuffer();
        }
    }


    static public void register(Mesh mesh) {
        meshes.put(mesh.getName(), mesh);
    }


    static public Mesh get(String name) {
        return meshes.get(name);
    }


    static public List<Mesh> find(String pattern) {
        ArrayList<Mesh> result = new ArrayList<>();

        for (String name: meshes.keySet()) {
            if (name.contains(pattern))
                result.add(meshes.get(name));
        }

        return result;
    }
}
