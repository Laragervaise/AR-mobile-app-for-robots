/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2017 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arcore.rendering.renderers;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.google.ar.core.PointCloud;

import ch.idiap.android.glrenderer.shaders.GLSLProgram;
import ch.idiap.android.glrenderer.shaders.ShaderManager;
import ch.idiap.android.urdfviewer.examples.arcore.rendering.GLUtils;


/** Renders a point cloud. */
public class PointCloudRenderer {
    private static final String TAG = PointCloud.class.getSimpleName();

    // Shader names.
    private static final String VERTEX_SHADER_NAME = "arcore-example/point_cloud.vert";
    private static final String FRAGMENT_SHADER_NAME = "arcore-example/point_cloud.frag";

    private static final String VERTEX_SHADER_FILENAME = "shaders/point_cloud.vert";
    private static final String FRAGMENT_SHADER_FILENAME = "shaders/point_cloud.frag";

    static private final String POINT_SIZE = "POINT_SIZE";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int FLOATS_PER_POINT = 4; // X,Y,Z,confidence.
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 1000;

    private int vbo;
    private int vboSize;

    private GLSLProgram program;

    private int numPoints = 0;

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private long lastTimestamp = 0;


    public PointCloudRenderer() {}

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    public void init(Context context) {
        GLUtils.checkGLError(TAG, "before create");

        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        vbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLUtils.checkGLError(TAG, "buffer alloc");

        String vertexShader = ShaderManager.loadShaderFile(context, VERTEX_SHADER_NAME, VERTEX_SHADER_FILENAME);
        String fragmentShader = ShaderManager.loadShaderFile(context, FRAGMENT_SHADER_NAME, FRAGMENT_SHADER_FILENAME);

        program = new GLSLProgram("arcore-example/BackgroundRenderer", vertexShader, fragmentShader);
        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareUniform(GLSLProgram.UNIFORM_COLOR, "u_Color");
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_ModelViewProjection");
        program.declareUniform(POINT_SIZE, "u_PointSize");
    }

    /**
     * Updates the OpenGL buffer contents to the provided point. Repeated calls with the same point
     * cloud will be ignored.
     */
    public void update(PointCloud cloud) {
        if (cloud.getTimestamp() == lastTimestamp) {
            // Redundant call.
            return;
        }
        GLUtils.checkGLError(TAG, "before update");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        lastTimestamp = cloud.getTimestamp();

        // If the VBO is not large enough to fit the new point cloud, resize it.
        numPoints = cloud.getPoints().remaining() / FLOATS_PER_POINT;
        if (numPoints * BYTES_PER_POINT > vboSize) {
            while (numPoints * BYTES_PER_POINT > vboSize) {
                vboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, cloud.getPoints());
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLUtils.checkGLError(TAG, "after update");
    }

    /**
     * Renders the point cloud. ARCore point cloud is given in world space.
     *
     * @param cameraView the camera view matrix for this frame, typically from {@link
     *     com.google.ar.core.Camera#getViewMatrix(float[], int)}.
     * @param cameraPerspective the camera projection matrix for this frame, typically from {@link
     *     com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)}.
     */
    public void draw(float[] cameraView, float[] cameraPerspective) {
        if (!program.isCompiled())
            program.compile();

        program.use();

        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

        GLUtils.checkGLError(TAG, "Before draw");

        GLES20.glEnableVertexAttribArray(program.getAttributeLocation(GLSLProgram.POSITION));
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(program.getAttributeLocation(GLSLProgram.POSITION), 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(program.getUniformLocation(GLSLProgram.UNIFORM_COLOR), 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(program.getUniformLocation(GLSLProgram.MVP_MATRIX), 1, false, modelViewProjection, 0);
        GLES20.glUniform1f(program.getUniformLocation(POINT_SIZE), 5.0f);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
        GLES20.glDisableVertexAttribArray(program.getAttributeLocation(GLSLProgram.POSITION));
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLUtils.checkGLError(TAG, "Draw");
    }
}
