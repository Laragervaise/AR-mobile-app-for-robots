/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (c) 2012, Willow Garage, Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.glrenderer.meshes.loaders;

import android.content.Context;

import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import ch.idiap.android.glrenderer.helpers.AssetsUtils;
import ch.idiap.android.glrenderer.meshes.Mesh;
import ch.idiap.android.glrenderer.meshes.MeshManager;


public class StlLoader implements Loader {

	private final ByteBuffer bb = ByteBuffer.allocateDirect(4);
	private ByteArrayInputStream in;


	public List<String> load(Context context, String filename) {
		return load(context, filename, CoordinatesSystemTransformation.X__Y__Z);
	}


	public List<String> load(Context context, String filename, CoordinatesSystemTransformation transform) {
		InputStream stream = AssetsUtils.loadAsset(context, filename);
		if (stream == null)
			return null;

		try {
			byte[] data = new byte[stream.available()];
			stream.read(data, 0, stream.available());
			in = new ByteArrayInputStream(data);
		} catch (IOException ex) {
			return null;
		}

		bb.order(ByteOrder.nativeOrder());


		// Skip 80 byte header
		in.skip(80);

		// Get number of triangles to load
		int nTriangles = getInt();

		float[] vertex = new float[nTriangles * 9];
		float[] normal = new float[nTriangles * 9];

		Vector3f normalVec = new Vector3f();
		Vector3f[] vertexVec = { new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };

		Vector3f tmp1 = new Vector3f();
		Vector3f tmp2 = new Vector3f();

		int vidx = 0;
		int nidx = 0;

		for(int i = 0; i < nTriangles; i++) {
			// Load the normal, check that it's properly formed
			normalVec.set(getFloat(), getFloat(), getFloat()).normalize();

			// Store the normalized normal
			if (transform == CoordinatesSystemTransformation.X__Y__Z) {
				for (int j = 0; j < 3; j++) {
					normal[nidx++] = normalVec.x;
					normal[nidx++] = normalVec.y;
					normal[nidx++] = normalVec.z;
				}
			} else if (transform == CoordinatesSystemTransformation.X__Z__MINUS_Y) {
				for (int j = 0; j < 3; j++) {
					normal[nidx++] = normalVec.x;
					normal[nidx++] = normalVec.z;
					normal[nidx++] = -normalVec.y;
				}
			}

			// Load and store the triangle vertices
			// Swap the order if necessary
			for(int b = 0; b < 3; b++) {
				vertexVec[b].set(getFloat(), getFloat(), getFloat());
			}

			vertexVec[1].sub(vertexVec[0], tmp1);
			vertexVec[2].sub(vertexVec[0], tmp2);

			if (tmp1.cross(tmp2).dot(normalVec) < 0) {
				vertexVec[3] = vertexVec[2];
				vertexVec[2] = vertexVec[1];
				vertexVec[1] = vertexVec[3];
			}

			if (transform == CoordinatesSystemTransformation.X__Y__Z) {
				for (int b = 0; b < 3; b++) {
					vertex[vidx++] = vertexVec[b].x;
					vertex[vidx++] = vertexVec[b].y;
					vertex[vidx++] = vertexVec[b].z;
				}
			} else if (transform == CoordinatesSystemTransformation.X__Z__MINUS_Y) {
				for (int b = 0; b < 3; b++) {
					vertex[vidx++] = vertexVec[b].x;
					vertex[vidx++] = vertexVec[b].z;
					vertex[vidx++] = -vertexVec[b].y;
				}
			}

			// Skip the footer
			in.skip(2);
		}


		int lastIndex = filename.lastIndexOf(".stl");
		String name = filename.substring(0, lastIndex);

		Mesh mesh = new Mesh(name, vertex, normal);
		MeshManager.register(mesh);

		List<String> meshes = new ArrayList<String>();
		meshes.add(name);

		return meshes;
	}


	private byte[] readBytes(int nbytes) {
		byte[] retval = new byte[nbytes];
		in.read(retval, 0, nbytes);
		return retval;
	}


	private int getInt() {
		bb.clear();
		bb.put(readBytes(4));
		bb.position(0);
		return bb.getInt();
	}


	private float getFloat() {
		bb.clear();
		bb.put(readBytes(4));
		bb.position(0);
		return bb.getFloat();
	}
}
