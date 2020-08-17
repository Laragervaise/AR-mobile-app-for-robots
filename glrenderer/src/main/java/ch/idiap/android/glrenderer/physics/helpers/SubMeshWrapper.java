/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics.helpers;

import com.bulletphysics.collision.shapes.StridingMeshInterface;
import com.bulletphysics.collision.shapes.VertexData;

import ch.idiap.android.glrenderer.meshes.SubMesh;


public class SubMeshWrapper extends StridingMeshInterface
{
    private SubMesh submesh = null;


    public SubMeshWrapper(SubMesh submesh) {
        this.submesh = submesh;
    }


    public VertexData getLockedVertexIndexBase(int subpart/*=0*/) {
        return new SubMeshVertexData(submesh);
    }


    public VertexData getLockedReadOnlyVertexIndexBase(int subpart/*=0*/) {
        return new SubMeshVertexData(submesh);
    }


    public void unLockVertexBase(int subpart) {
    }


    public void unLockReadOnlyVertexBase(int subpart) {
    }


    public int getNumSubParts() {
        return 1;
    }


    public void preallocateVertices(int numverts) {
    }


    public void preallocateIndices(int numindices) {
    }
}
