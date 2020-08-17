/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.textures;


import android.graphics.Bitmap;
import android.opengl.GLES20;


public class Texture
{
    private String name = null;
    private int textureId = -1;
    private Bitmap bitmap = null;


    public Texture(String name, Bitmap bitmap) {
        this.name = name;
        this.bitmap = bitmap;

        createGLBuffer();
    }


    public String getName() {
        return name;
    }


    public int getId() {
        return textureId;
    }


    public void createGLBuffer() {
        int[] tmp = new int[1];
        GLES20.glGenTextures(1, tmp, 0);
        textureId = tmp[0];

        // Bind and load the texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // UV mapping parameters
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

}
