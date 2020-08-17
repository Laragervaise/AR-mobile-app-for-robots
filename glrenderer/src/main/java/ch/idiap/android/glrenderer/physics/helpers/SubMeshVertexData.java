/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics.helpers;

import com.bulletphysics.collision.shapes.VertexData;

import javax.vecmath.Tuple3f;

import ch.idiap.android.glrenderer.meshes.SubMesh;
import ch.idiap.android.glrenderer.physics.World;


public class SubMeshVertexData extends VertexData {

    private SubMesh submesh = null;


    public SubMeshVertexData(SubMesh submesh) {
        this.submesh = submesh;
    }


    public int getVertexCount() {
        return submesh.nbVertices;
    }


    public int getIndexCount() {
        if (submesh.indicesBuffer != null)
            return submesh.indicesBuffer.capacity();

        return getVertexCount();
    }


    public <T extends Tuple3f> T getVertex(int idx, T out) {
        int offset = idx * submesh.vertexSize + SubMesh.POSITION_OFFSET;

        submesh.verticesBuffer.position(0);

        out.x = submesh.verticesBuffer.get(offset) * World.SCALE;
        out.y = submesh.verticesBuffer.get(offset + 1) * World.SCALE;
        out.z = submesh.verticesBuffer.get(offset + 2) * World.SCALE;

        return out;
    }


    public void setVertex(int idx, float x, float y, float z) {
    }


    public int getIndex(int idx) {
        if (submesh.indicesBuffer != null)
            return submesh.indicesBuffer.get(idx);

        return idx;
    }
}
