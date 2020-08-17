/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.physics.helpers;

import android.opengl.GLES20;
import android.util.Log;

import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import javax.vecmath.Vector3f;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.glrenderer.shaders.GLSLProgram;
import ch.idiap.android.glrenderer.shaders.ShaderManager;


public class DebugDrawer extends IDebugDraw
{
    protected static final int POSITION_DATA_SIZE = 3;
    protected static final int FLOAT_SIZE = Float.SIZE / 8;
    protected static final int STRIDE = POSITION_DATA_SIZE * FLOAT_SIZE;
    protected static final int POSITION_OFFSET = 0;


    protected int debugMode = DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB;
    protected BaseCamera camera = null;
    protected GLSLProgram shader = null;
    protected int bufferIdx = -1;
    protected FloatBuffer verticesBuffer = null;


    public DebugDrawer(BaseCamera camera) {
        this.camera = camera;
        shader = ShaderManager.get(ShaderManager.FlatColor);

        final int[] buffers = new int[1];

        GLES20.glGenBuffers(1, buffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        verticesBuffer = FloatBuffer.allocate(6);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        bufferIdx = buffers[0];
    }


    @Override
    public void drawLine(Vector3f from, Vector3f to, Vector3f color) {
        if (!shader.isCompiled())
            shader.compile();

        shader.use();


        Matrix4f MVP = new Matrix4f(camera.getViewport().getProjectionMatrix())
                                .mul(camera.getViewMatrix());

        float[] glMVP = new float[16];
        MVP.get(glMVP);

        GLES20.glUniformMatrix4fv(shader.getUniformLocation(GLSLProgram.MVP_MATRIX),
                1, false, glMVP, 0);

        GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.UNIFORM_COLOR),
                color.x, color.y, color.z, 1.0f);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx);

        verticesBuffer.position(0);
        verticesBuffer.put(from.x / World.SCALE);
        verticesBuffer.put(from.y / World.SCALE);
        verticesBuffer.put(from.z / World.SCALE);
        verticesBuffer.put(to.x / World.SCALE);
        verticesBuffer.put(to.y / World.SCALE);
        verticesBuffer.put(to.z / World.SCALE);
        verticesBuffer.position(0);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * FLOAT_SIZE,
                verticesBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glEnableVertexAttribArray(shader.getAttributeLocation(GLSLProgram.POSITION));

        GLES20.glVertexAttribPointer(
                shader.getAttributeLocation(GLSLProgram.POSITION), POSITION_DATA_SIZE,
                GLES20.GL_FLOAT, false, STRIDE, POSITION_OFFSET
        );

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }


    @Override
    public void drawContactPoint(
            Vector3f PointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {

    }


    @Override
    public void reportErrorWarning(String warningString) {
        Log.e("DebugDrawer", warningString);
    }


    @Override
    public void draw3dText(Vector3f location, String textString) {
    }


    @Override
    public void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }


    @Override
    public int getDebugMode() {
        return debugMode;
    }
}
