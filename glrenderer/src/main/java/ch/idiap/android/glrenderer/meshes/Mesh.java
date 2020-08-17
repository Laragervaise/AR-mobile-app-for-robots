/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.meshes;


import android.opengl.GLES20;

import org.joml.AABBf;

import java.util.ArrayList;
import java.util.List;


public class Mesh {

    private String name = null;
    private final List<SubMesh> submeshes = new ArrayList<>();
    private String defaultMaterialName = null;
    private final AABBf boundingBox = new AABBf();


    public Mesh() {
    }


    public Mesh(String name) {
        this.name = name;
    }


    public Mesh(String name, float[] vertices, float[] normals, float[] uvs, int[] indices, int mode) {
        this(name);

        SubMesh submesh = new SubMesh(vertices, normals, uvs, indices, mode);
        submeshes.add(submesh);

        updateBoundingBox();
    }


    public Mesh(float[] vertices, float[] normals, float[] uvs, int[] indices, int mode) {
        this(null, vertices, normals, uvs, indices, mode);
    }


    public Mesh(float[] vertices, float[] normals, int[] indices, int mode) {
        this(null, vertices, normals, null, indices, mode);
    }


    public Mesh(String name, float[] vertices, float[] normals, float[] uvs, int[] indices) {
        this(name, vertices, normals, uvs, indices, GLES20.GL_TRIANGLES);
    }


    public Mesh(String name, float[] vertices, float[] normals, int[] indices) {
        this(name, vertices, normals, null, indices, GLES20.GL_TRIANGLES);
    }


    public Mesh(float[] vertices, float[] normals, float[] uvs, int[] indices) {
        this(null, vertices, normals, uvs, indices, GLES20.GL_TRIANGLES);
    }


    public Mesh(float[] vertices, float[] normals, int[] indices) {
        this(null, vertices, normals, null, indices, GLES20.GL_TRIANGLES);
    }


    public Mesh(String name, float[] vertices, float[] normals, float[] uvs, int mode) {
        this(name, vertices, normals, uvs, null, mode);
    }


    public Mesh(String name, float[] vertices, float[] normals, int mode) {
        this(name, vertices, normals, null, null, mode);
    }


    public Mesh(float[] vertices, float[] normals, float[] uvs, int mode) {
        this(null, vertices, normals, uvs, null, mode);
    }


    public Mesh(float[] vertices, float[] normals, int mode) {
        this(null, vertices, normals, null, null, mode);
    }


    public Mesh(String name, float[] vertices, float[] normals, float[] uvs) {
        this(name, vertices, normals, uvs, null, GLES20.GL_TRIANGLES);
    }


    public Mesh(String name, float[] vertices, float[] normals) {
        this(name, vertices, normals, null, null, GLES20.GL_TRIANGLES);
    }


    public Mesh(float[] vertices, float[] normals, float[] uvs) {
        this(null, vertices, normals, uvs, null, GLES20.GL_TRIANGLES);
    }


    public Mesh(float[] vertices, float[] normals) {
        this(null, vertices, normals, null, null, GLES20.GL_TRIANGLES);
    }


    public String getName() {
        return name;
    }


    public void addSubMesh(SubMesh submesh) {
        submeshes.add(submesh);
        updateBoundingBox();
    }


    public List<SubMesh> getSubMeshes() {
        return submeshes;
    }


    public void setDefaultMaterialName(String name) {
        defaultMaterialName = name;
    }


    public String getDefaultMaterialName() {
        return defaultMaterialName;
    }


    public AABBf getBoundingBox() {
        return boundingBox;
    }


    private void updateBoundingBox() {
        boundingBox.setMin(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        boundingBox.setMax(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        for (SubMesh submesh : submeshes)
            boundingBox.union(submesh.boundingBox);
    }
}
