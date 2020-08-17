/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.meshes;


import android.opengl.GLES20;


public class MeshBuilder {

    private static final float TWO_PI = (float) (2 * Math.PI);


    /********************************** CUBE **********************************/

    static public Mesh buildCube() {
        return buildCube(null, 1.0f, 1.0f, 1.0f);
    }


    static public Mesh buildCube(String name) {
        return buildCube(name, 1.0f, 1.0f, 1.0f);
    }


    static public Mesh buildCube(String name, float dim_x, float dim_y, float dim_z) {
        dim_x *= 0.5f;
        dim_y *= 0.5f;
        dim_z *= 0.5f;

        final float cubeVertices[] = {
                -dim_x, -dim_y,  dim_z,	//[0]
                dim_x, -dim_y,  dim_z,	//[1]
                -dim_x,  dim_y,  dim_z,	//[2]

                dim_x, -dim_y,  dim_z,		//[1]
                dim_x,  dim_y,  dim_z,		//[3]
                -dim_x,  dim_y,  dim_z,	//[2]

                -dim_x, dim_y,  dim_z,	//[2]
                dim_x,  dim_y,  dim_z,		//[3]
                dim_x,  dim_y, -dim_z,		//[7]

                dim_x,  dim_y,  dim_z,		//[3]
                dim_x, -dim_y,  dim_z,		//[1]
                dim_x,  dim_y, -dim_z,		//[7]

                dim_x,  dim_y, -dim_z,		//[7]
                dim_x, -dim_y,  dim_z,		//[1]
                dim_x, -dim_y, -dim_z,		//[5]

                dim_x, -dim_y,  dim_z,		//[1]
                -dim_x, -dim_y, -dim_z,	//[4]
                dim_x, -dim_y, -dim_z,		//[5]

                dim_x, -dim_y, -dim_z,		//[5]
                -dim_x, -dim_y, -dim_z,	//[4]
                dim_x,  dim_y, -dim_z,		//[7]

                -dim_x, -dim_y, -dim_z,	//[4]
                -dim_x,  dim_y, -dim_z,	//[6]
                dim_x,  dim_y, -dim_z,		//[7]

                dim_x,  dim_y, -dim_z,		//[7]
                -dim_x,  dim_y, -dim_z,	//[6]
                -dim_x,  dim_y,  dim_z,	//[2]

                -dim_x,  dim_y, -dim_z,	//[6]
                -dim_x, -dim_y, -dim_z,	//[4]
                -dim_x,  dim_y,  dim_z,	//[2]

                -dim_x,  dim_y,  dim_z,	//[2]
                -dim_x, -dim_y, -dim_z,	//[4]
                -dim_x, -dim_y,  dim_z,	//[0]

                -dim_x, -dim_y, -dim_z,	//[4]
                dim_x, -dim_y,  dim_z,		//[1]
                -dim_x, -dim_y,  dim_z,	//[0]

        };

        final float cubeNormals[] = {
                0f,0f,1f,0f,0f,1f,0f,0f,1f,
                0f,0f,1f,0f,0f,1f,0f,0f,1f,
                0f,1f,0f,0f,1f,0f,0f,1f,0f,
                1f,0f,0f,1f,0f,0f,1f,0f,0f,
                1f,0f,0f,1f,0f,0f,1f,0f,0f,
                0f,-1f,0f,0f,-1f,0f,0f,-1f,0f,
                0f,0f,-1f,0f,0f,-1f,0f,0f,-1f,
                0f,0f,-1f,0f,0f,-1f,0f,0f,-1f,
                0f,1f,0f,0f,1f,0f,0f,1f,0f,
                -1f,0f,0f,-1f,0f,0f,-1f,0f,0f,
                -1f,0f,0f,-1f,0f,0f,-1f,0f,0f,
                0f,-1f,0f,0f,-1f,0f,0f,-1f,0f
        };


        Mesh mesh = new Mesh(name, cubeVertices, cubeNormals);

        if (name != null)
            MeshManager.register(mesh);

        return mesh;
    }


    /******************************** CYLINDER ********************************/

    static public Mesh buildCylinder() {
        return buildCylinder(null, 1.0f, 1.0f);
    }


    static public Mesh buildCylinder(String name) {
        return buildCylinder(name, 1.0f, 1.0f);
    }


    static public Mesh buildCylinder(String name, float radius, float length) {
        int sides = 17;
        double dTheta = TWO_PI / sides;

        float[] sideVertices = new float[(sides + 1) * 6];
        float[] sideNormals = new float[(sides + 1) * 6];

        int sideVidx = 0;
        int sideNidx = 0;

        float[] topVertices = new float[(sides + 2) * 3];
        float[] topNormals = new float[(sides + 2) * 3];
        float[] bottomVertices = new float[(sides + 2) * 3];
        float[] bottomNormals = new float[(sides + 2) * 3];

        int capVidx = 3;
        int capNidx = 3;

        topVertices[0] = 0f;
        topVertices[1] = 0f;
        topVertices[2] = .5f * length;
        topNormals[0] = 0f;
        topNormals[1] = 0f;
        topNormals[2] = 1f;
        bottomVertices[0] = 0f;
        bottomVertices[1] = 0f;
        bottomVertices[2] = -.5f * length;
        bottomNormals[0] = 0f;
        bottomNormals[1] = 0f;
        bottomNormals[2] = -1f;

        for (float theta = 0; theta <= (TWO_PI + dTheta); theta += dTheta) {
            sideVertices[sideVidx++] = (float) Math.cos(theta) * radius; // X
            sideVertices[sideVidx++] = (float) Math.sin(theta) * radius; // Y
            sideVertices[sideVidx++] = 0.5f * length; // Z

            sideVertices[sideVidx++] = (float) Math.cos(theta) * radius; // X
            sideVertices[sideVidx++] = (float) Math.sin(theta) * radius; // Y
            sideVertices[sideVidx++] = -0.5f * length; // Z

            sideNormals[sideNidx++] = (float) Math.cos(theta) * radius; // X
            sideNormals[sideNidx++] = (float) Math.sin(theta) * radius; // Y
            sideNormals[sideNidx++] = 0f; // Z

            sideNormals[sideNidx++] = (float) Math.cos(theta) * radius; // X
            sideNormals[sideNidx++] = (float) Math.sin(theta) * radius; // Y
            sideNormals[sideNidx++] = 0f; // Z

            // X
            topVertices[capVidx] = (float) Math.cos(theta) * radius;
            bottomVertices[capVidx++] = (float) Math.cos(TWO_PI - theta) * radius;
            // Y
            topVertices[capVidx] = (float) Math.sin(theta) * radius;
            bottomVertices[capVidx++] = (float) Math.sin(TWO_PI - theta) * radius;
            // Z
            topVertices[capVidx] = 0.5f * length;
            bottomVertices[capVidx++] = -0.5f * length;

            // Normals
            topNormals[capNidx] = 0f;
            bottomNormals[capNidx++] = 0f;
            topNormals[capNidx] = 0f;
            bottomNormals[capNidx++] = 0f;
            topNormals[capNidx] = 1f;
            bottomNormals[capNidx++] = -1f;
        }


        Mesh mesh = new Mesh(name);
        mesh.addSubMesh(new SubMesh(topVertices, topNormals, GLES20.GL_TRIANGLE_FAN));
        mesh.addSubMesh(new SubMesh(bottomVertices, bottomNormals, GLES20.GL_TRIANGLE_FAN));
        mesh.addSubMesh(new SubMesh(sideVertices, sideNormals, GLES20.GL_TRIANGLE_STRIP));

        if (name != null)
            MeshManager.register(mesh);

        return mesh;
    }


    /********************************* SPHERE *********************************/

    static public Mesh buildSphere() {
        return buildSphere(null, 1.0f);
    }


    static public Mesh buildSphere(String name) {
        return buildSphere(name, 1.0f);
    }


    static public Mesh buildSphere(String name, float radius) {
        return buildEllipsoid(name, radius, radius, radius);
    }

    /********************************* ELLIPSOID *********************************/

    static public Mesh buildEllipsoid() {
        return buildEllipsoid(null, 1.0f, 0.5f, 0.25f);
    }


    static public Mesh buildEllipsoid(String name) {
        return buildEllipsoid(name, 1.0f, 0.5f, 0.25f);
    }


    static public Mesh buildEllipsoid(String name, float radius1, float radius2, float radius3) {
        int vIndex = 0, nIndex = 0, m_Stacks = 17, m_Slices = 14;
        float m_Squash = 1.0f;

        float[] vertexData = new float[3 * ((m_Slices * 2 + 2) * m_Stacks)];
        float[] normalData = new float[(3 * (m_Slices * 2 + 2) * m_Stacks)];

        int phiIdx, thetaIdx;

        for(phiIdx = 0; phiIdx < m_Stacks; phiIdx++) {
            float phi0 = (float) Math.PI * ((float) (phiIdx + 0) * (1.0f / (float) (m_Stacks)) - 0.5f);
            float phi1 = (float) Math.PI * ((float) (phiIdx + 1) * (1.0f / (float) (m_Stacks)) - 0.5f);

            float cosPhi0 = (float) Math.cos(phi0);
            float sinPhi0 = (float) Math.sin(phi0);
            float cosPhi1 = (float) Math.cos(phi1);
            float sinPhi1 = (float) Math.sin(phi1);

            float cosTheta, sinTheta;

            for(thetaIdx = 0; thetaIdx < m_Slices; thetaIdx++) {
                float theta = (float) (2.0f * (float) Math.PI * ((float) thetaIdx) * (1.0 / (float) (m_Slices - 1)));
                cosTheta = (float) Math.cos(theta);
                sinTheta = (float) Math.sin(theta);

                vertexData[vIndex + 0] = radius1 * cosPhi0 * cosTheta;
                vertexData[vIndex + 1] = radius2 * (sinPhi0 * m_Squash);
                vertexData[vIndex + 2] = radius3 * (cosPhi0 * sinTheta);

                vertexData[vIndex + 3] = radius1 * cosPhi1 * cosTheta;
                vertexData[vIndex + 4] = radius2 * (sinPhi1 * m_Squash);
                vertexData[vIndex + 5] = radius3 * (cosPhi1 * sinTheta);

                normalData[nIndex + 0] = cosPhi0 * cosTheta;
                normalData[nIndex + 1] = sinPhi0;
                normalData[nIndex + 2] = cosPhi0 * sinTheta;

                normalData[nIndex + 3] = cosPhi1 * cosTheta;
                normalData[nIndex + 4] = sinPhi1;
                normalData[nIndex + 5] = cosPhi1 * sinTheta;

                vIndex += 2 * 3;
                nIndex += 2 * 3;
            }

            vertexData[vIndex + 0] = vertexData[vIndex + 3] = vertexData[vIndex - 3];
            vertexData[vIndex + 1] = vertexData[vIndex + 4] = vertexData[vIndex - 2];
            vertexData[vIndex + 2] = vertexData[vIndex + 5] = vertexData[vIndex - 1];
        }


        Mesh mesh = new Mesh(name, vertexData, normalData, GLES20.GL_TRIANGLE_STRIP);

        if (name != null)
            MeshManager.register(mesh);

        return mesh;
    }

    /********************************* referential *********************************/

    static public Mesh buildReferential(String name, float radius1, float radius2, float radius3) {
        int vIndex = 0, nIndex = 0, m_Stacks = 17, m_Slices = 14;
        float m_Squash = 1.0f;

        float[] vertexData = new float[3 * ((m_Slices * 2 + 2) * m_Stacks)];
        float[] normalData = new float[(3 * (m_Slices * 2 + 2) * m_Stacks)];

        int phiIdx, thetaIdx;

        for(phiIdx = 0; phiIdx < m_Stacks; phiIdx++) {
            float phi0 = (float) Math.PI * ((float) (phiIdx + 0) * (1.0f / (float) (m_Stacks)) - 0.5f);
            float phi1 = (float) Math.PI * ((float) (phiIdx + 1) * (1.0f / (float) (m_Stacks)) - 0.5f);

            float cosPhi0 = (float) Math.cos(phi0);
            float sinPhi0 = (float) Math.sin(phi0);
            float cosPhi1 = (float) Math.cos(phi1);
            float sinPhi1 = (float) Math.sin(phi1);

            float cosTheta, sinTheta;

            for(thetaIdx = 0; thetaIdx < m_Slices; thetaIdx++) {
                float theta = (float) (2.0f * (float) Math.PI * ((float) thetaIdx) * (1.0 / (float) (m_Slices - 1)));
                cosTheta = (float) Math.cos(theta);
                sinTheta = (float) Math.sin(theta);

                vertexData[vIndex + 0] = radius1 * cosPhi0 * cosTheta;
                vertexData[vIndex + 1] = radius2 * (sinPhi0 * m_Squash);
                vertexData[vIndex + 2] = radius3 * (cosPhi0 * sinTheta);

                vertexData[vIndex + 3] = radius1 * cosPhi1 * cosTheta;
                vertexData[vIndex + 4] = radius2 * (sinPhi1 * m_Squash);
                vertexData[vIndex + 5] = radius3 * (cosPhi1 * sinTheta);

                normalData[nIndex + 0] = cosPhi0 * cosTheta;
                normalData[nIndex + 1] = sinPhi0;
                normalData[nIndex + 2] = cosPhi0 * sinTheta;

                normalData[nIndex + 3] = cosPhi1 * cosTheta;
                normalData[nIndex + 4] = sinPhi1;
                normalData[nIndex + 5] = cosPhi1 * sinTheta;

                vIndex += 2 * 3;
                nIndex += 2 * 3;
            }

            vertexData[vIndex + 0] = vertexData[vIndex + 3] = vertexData[vIndex - 3];
            vertexData[vIndex + 1] = vertexData[vIndex + 4] = vertexData[vIndex - 2];
            vertexData[vIndex + 2] = vertexData[vIndex + 5] = vertexData[vIndex - 1];
        }


        Mesh mesh = new Mesh(name, vertexData, normalData, GLES20.GL_TRIANGLE_STRIP);

        if (name != null)
            MeshManager.register(mesh);

        return mesh;
    }


}
