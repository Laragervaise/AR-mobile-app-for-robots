/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.meshes;


import android.opengl.GLES20;

import org.joml.AABBf;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import ch.idiap.android.glrenderer.helpers.Vertices;


public class SubMesh {

    public static final int FLOAT_SIZE = Float.SIZE / 8;

    public static final int POSITION_DATA_SIZE = 3;
    public static final int NORMAL_DATA_SIZE = 3;
    public static final int TEXCOORDS_DATA_SIZE = 2;

    public static final int POSITION_OFFSET = 0;
    public static final int NORMAL_OFFSET = POSITION_DATA_SIZE * FLOAT_SIZE;
    public static final int TEXCOORDS_OFFSET = NORMAL_OFFSET + NORMAL_DATA_SIZE * FLOAT_SIZE;


    public FloatBuffer verticesBuffer;
    public IntBuffer indicesBuffer;
    public int nbVertices;
    public int mode;
    public int bufferIdx = -1;
    public AABBf boundingBox = null;
    public boolean textured = false;
    public int vertexSize;
    public int stride;



    public SubMesh(float[] vertices, float[] normals, float[] uvs, int[] indices, int mode) {
        if (vertices.length != normals.length)
            throw new IllegalArgumentException("Vertex array and normal array must be the same length!");

        boundingBox = new AABBf();

        textured = (uvs != null);

        // Vertices buffer
        nbVertices = vertices.length / 3;
        vertexSize = (textured ? 8 : 6);
        stride = vertexSize * FLOAT_SIZE;

        int bufferLength = nbVertices * vertexSize;
        int vIdx = 0, nIdx = 0, uvIdx = 0;

        float[] packedBuffer = new float[bufferLength];

        for (int i = 0; i < nbVertices; i++) {
            int idx = i * vertexSize;

            packedBuffer[idx + 0] = vertices[vIdx++];
            packedBuffer[idx + 1] = vertices[vIdx++];
            packedBuffer[idx + 2] = vertices[vIdx++];

            packedBuffer[idx + 3] = normals[nIdx++];
            packedBuffer[idx + 4] = normals[nIdx++];
            packedBuffer[idx + 5] = normals[nIdx++];

            if (textured) {
                packedBuffer[idx + 6] = uvs[uvIdx++];
                packedBuffer[idx + 7] = uvs[uvIdx++];
            }

            boundingBox.union(packedBuffer[idx + 0], packedBuffer[idx + 1], packedBuffer[idx + 2]);
        }

        verticesBuffer = Vertices.toFloatBuffer(packedBuffer);

        // Indices buffer
        this.mode = mode;

        if (indices != null)
            indicesBuffer = Vertices.toIntBuffer(indices);

        createGLBuffer();
    }


    public SubMesh(float[] vertices, float[] normals, float[] uvs, int[] indices) {
        this(vertices, normals, uvs, indices, GLES20.GL_TRIANGLES);
    }


    public SubMesh(float[] vertices, float[] normals, int[] indices) {
        this(vertices, normals, null, indices, GLES20.GL_TRIANGLES);
    }


    public SubMesh(float[] vertices, float[] normals, float[] uvs, int mode) {
        this(vertices, normals, uvs, null, mode);
    }


    public SubMesh(float[] vertices, float[] normals, int mode) {
        this(vertices, normals, null, null, mode);
    }


    public void createGLBuffer() {
        // Create the OpenGL buffer
        final int[] buffers = new int[1];

        GLES20.glGenBuffers(1, buffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        verticesBuffer.position(0);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * FLOAT_SIZE,
                            verticesBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        bufferIdx = buffers[0];
    }

}
