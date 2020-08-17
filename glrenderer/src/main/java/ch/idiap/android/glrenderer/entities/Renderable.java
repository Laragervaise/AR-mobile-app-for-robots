/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.entities;


import android.opengl.GLES20;

import androidx.annotation.NonNull;

import org.joml.AABBf;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.materials.Material;
import ch.idiap.android.glrenderer.materials.MaterialManager;
import ch.idiap.android.glrenderer.meshes.Mesh;
import ch.idiap.android.glrenderer.meshes.SubMesh;
import ch.idiap.android.glrenderer.shaders.GLSLProgram;
import ch.idiap.android.glrenderer.shaders.ShaderManager;

import static ch.idiap.android.glrenderer.meshes.SubMesh.NORMAL_DATA_SIZE;
import static ch.idiap.android.glrenderer.meshes.SubMesh.NORMAL_OFFSET;
import static ch.idiap.android.glrenderer.meshes.SubMesh.POSITION_DATA_SIZE;
import static ch.idiap.android.glrenderer.meshes.SubMesh.POSITION_OFFSET;
import static ch.idiap.android.glrenderer.meshes.SubMesh.TEXCOORDS_DATA_SIZE;
import static ch.idiap.android.glrenderer.meshes.SubMesh.TEXCOORDS_OFFSET;


public class Renderable extends Transformable {

    protected Mesh mesh;
    protected Material material;
    protected GLSLProgram shader;
    protected boolean visible;


    public Renderable(Mesh mesh) {
        this(mesh, null, null);
    }


    public Renderable(Mesh mesh, Material material) {
        this(mesh, material, null);
    }


    public Renderable(Mesh mesh, Material material, GLSLProgram shader) {
        this.mesh = mesh;
        this.material = material;
        this.shader = shader;
        this.visible = true;

        if (this.material == null) {
            if (this.mesh.getDefaultMaterialName() != null)
                this.material = MaterialManager.get(this.mesh.getDefaultMaterialName());

            if (this.material == null)
                this.material = new Material();
        }

        if (this.shader == null) {
            if (this.mesh.getSubMeshes().get(0).textured)
                this.shader = ShaderManager.get(ShaderManager.TexturedShaded);
            else
                this.shader = ShaderManager.get(ShaderManager.MaterialShaded);
        }
    }


    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    public AABBf getBoundingBox() {
        return mesh.getBoundingBox();
    }


    public void draw(BaseCamera camera, Color ambientLight, Light light) {
        if (!visible)
            return;

        if (!shader.isCompiled())
            shader.compile();

        shader.use();


        Matrix4f MVP = new Matrix4f(camera.getViewport().getProjectionMatrix())
                .mul(camera.getViewMatrix())
                .mul(transforms.toMatrix());


        if (shader.getName().equals(ShaderManager.MaterialShaded)) {

            float[] glMVP = new float[16];
            MVP.get(glMVP);

            GLES20.glUniformMatrix4fv(shader.getUniformLocation(GLSLProgram.MVP_MATRIX),
                    1, false, glMVP, 0);

            float[] modelTransforms = new float[16];
            transforms.toOpenGL(modelTransforms);

            GLES20.glUniformMatrix4fv(shader.getUniformLocation(GLSLProgram.M_MATRIX),
                    1, false, modelTransforms, 0);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_EMISSIVE),
                    material.emissive.red, material.emissive.green,
                    material.emissive.blue, material.emissive.alpha);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_AMBIENT),
                    material.ambient.red, material.ambient.green,
                    material.ambient.blue, material.ambient.alpha);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_DIFFUSE),
                    material.diffuse.red, material.diffuse.green,
                    material.diffuse.blue, material.diffuse.alpha);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_SPECULAR),
                    material.specular.red, material.specular.green,
                    material.specular.blue, material.specular.alpha);

            GLES20.glUniform1f(shader.getUniformLocation(GLSLProgram.MATERIAL_SHININESS),
                    material.shininess);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.AMBIENT_LIGHT),
                    ambientLight.red, ambientLight.green, ambientLight.blue, ambientLight.alpha);


            Vector3fc eyePos = camera.transforms.getWorldPosition();

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.EYE_POS),
                    eyePos.x(), eyePos.y(), eyePos.z(), 1.0f);


            Vector3fc lightPosition = light.transforms.getWorldPosition();

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.LIGHT_POS),
                    lightPosition.x(), lightPosition.y(), lightPosition.z(), 1.0f);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.LIGHT_COLOR),
                    light.color.red, light.color.green, light.color.blue, light.color.alpha);

        } else if (shader.getName().equals(ShaderManager.TexturedShaded)) {

            float[] glMVP = new float[16];
            MVP.get(glMVP);

            GLES20.glUniformMatrix4fv(shader.getUniformLocation(GLSLProgram.MVP_MATRIX),
                    1, false, glMVP, 0);

            float[] modelTransforms = new float[16];
            transforms.toOpenGL(modelTransforms);

            GLES20.glUniformMatrix4fv(shader.getUniformLocation(GLSLProgram.M_MATRIX),
                    1, false, modelTransforms, 0);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_EMISSIVE),
                    material.emissive.red, material.emissive.green,
                    material.emissive.blue, material.emissive.alpha);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.MATERIAL_SPECULAR),
                    material.specular.red, material.specular.green,
                    material.specular.blue, material.specular.alpha);

            GLES20.glUniform1f(shader.getUniformLocation(GLSLProgram.MATERIAL_SHININESS),
                    material.shininess);


            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, material.diffuseTexture.getId());
            GLES20.glUniform1i(shader.getUniformLocation(GLSLProgram.DIFFUSE_TEXTURE), 0);


            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.AMBIENT_LIGHT),
                    ambientLight.red, ambientLight.green, ambientLight.blue, ambientLight.alpha);

            Vector3fc eyePos = camera.transforms.getWorldPosition();

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.EYE_POS),
                    eyePos.x(), eyePos.y(), eyePos.z(), 1.0f);


            Vector3fc lightPosition = light.transforms.getWorldPosition();

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.LIGHT_POS),
                    lightPosition.x(), lightPosition.y(), lightPosition.z(), 1.0f);

            GLES20.glUniform4f(shader.getUniformLocation(GLSLProgram.LIGHT_COLOR),
                    light.color.red, light.color.green, light.color.blue, light.color.alpha);
        }


        for (SubMesh submesh: mesh.getSubMeshes()) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, submesh.bufferIdx);

            GLES20.glEnableVertexAttribArray(shader.getAttributeLocation(GLSLProgram.POSITION));

            GLES20.glVertexAttribPointer(
                    shader.getAttributeLocation(GLSLProgram.POSITION), POSITION_DATA_SIZE,
                    GLES20.GL_FLOAT, false, submesh.stride, POSITION_OFFSET
            );

            GLES20.glEnableVertexAttribArray(shader.getAttributeLocation(GLSLProgram.NORMAL));

            GLES20.glVertexAttribPointer(
                    shader.getAttributeLocation(GLSLProgram.NORMAL), NORMAL_DATA_SIZE,
                    GLES20.GL_FLOAT, false, submesh.stride, NORMAL_OFFSET
            );


            if (submesh.textured) {
                GLES20.glEnableVertexAttribArray(shader.getAttributeLocation(GLSLProgram.TEXCOORD));

                GLES20.glVertexAttribPointer(
                        shader.getAttributeLocation(GLSLProgram.TEXCOORD), TEXCOORDS_DATA_SIZE,
                        GLES20.GL_FLOAT, false, submesh.stride, TEXCOORDS_OFFSET
                );
            }


            if (submesh.indicesBuffer != null) {
                GLES20.glDrawElements(submesh.mode, submesh.nbVertices, GLES20.GL_UNSIGNED_INT,
                        submesh.indicesBuffer);
            } else {
                GLES20.glDrawArrays(submesh.mode, 0, submesh.nbVertices);
            }


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    @NonNull
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + " mesh='" + mesh.getName() + "' (" +
                mesh.getSubMeshes().size() + " submeshes), material='" +
                material.getName() + "', shader='" + shader.getName() + "', visible=" + visible + "]";
    }
}
