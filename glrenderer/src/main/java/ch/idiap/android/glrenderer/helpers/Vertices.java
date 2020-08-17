/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *   damonkohler@google.com (Damon Kohler)
 *
 * Original implementation:
 *   Copyright (C) 2011 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.glrenderer.helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;


public class Vertices {

	private Vertices() {
		// Utility class.
	}

	public static FloatBuffer toFloatBuffer(float[] vertices) {
		FloatBuffer floatBuffer;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8);
		byteBuffer.order(ByteOrder.nativeOrder());
		floatBuffer = byteBuffer.asFloatBuffer();
		floatBuffer.put(vertices);
		floatBuffer.position(0);
		return floatBuffer;
	}

	public static ShortBuffer toShortBuffer(short[] indices) {
		ShortBuffer shortBuffer;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(indices.length * Short.SIZE / 8);
		byteBuffer.order(ByteOrder.nativeOrder());
		shortBuffer = byteBuffer.asShortBuffer();
		shortBuffer.put(indices);
		shortBuffer.position(0);
		return shortBuffer;
	}

	public static IntBuffer toIntBuffer(int[] indices) {
		IntBuffer intBuffer;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(indices.length * Integer.SIZE / 8);
		byteBuffer.order(ByteOrder.nativeOrder());
		intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(indices);
		intBuffer.position(0);
		return intBuffer;
	}

	public static ByteBuffer toByteBuffer(byte[] data) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
		byteBuffer.put(data);
		byteBuffer.position(0);
		return byteBuffer;
	}
}
