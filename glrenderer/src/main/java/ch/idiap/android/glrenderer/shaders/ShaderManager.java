/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.shaders;


import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import ch.idiap.android.glrenderer.helpers.AssetsUtils;


public class ShaderManager {

    public static final String FlatColor = "glrenderer/FlatColor";
    public static final String FlatShaded = "glrenderer/FlatShaded";
    public static final String ColoredVertex = "glrenderer/ColoredVertex";
    public static final String TexturedShaded = "glrenderer/TexturedShaded";
    public static final String MaterialShaded = "glrenderer/MaterialShaded";


    static private Map<String, String> shaders = null;
    static private Map<String, GLSLProgram> programs = null;


    // Must be called when the OpenGL surface is created
    static public void init(Context context) {

        if (shaders == null) {
            shaders = new HashMap<>();

            loadShaderFile(context, "vp_flat_color");
            loadShaderFile(context, "vp_flat_shaded");
            loadShaderFile(context, "vp_colored_vertex");
            loadShaderFile(context, "vp_textured_shaded");
            loadShaderFile(context, "vp_material_shaded");

            loadShaderFile(context, "fp_flat_color");
            loadShaderFile(context, "fp_textured_shaded");
            loadShaderFile(context, "fp_material_shaded");
        }

        if (programs == null) {
            programs = new HashMap<>();
            createFlatColorProgram();
            createFlatShadedProgram();
            createColoredVertexProgram();
            createTexturedShadedProgram();
            createMaterialShadedProgram();
        } else {
            for (GLSLProgram program: programs.values())
                program.reset();
        }
    }


    static public void cleanup() {
        for (GLSLProgram shader: programs.values())
            shader.cleanup();
    }


    static public void register(GLSLProgram program) {
        programs.put(program.getName(), program);
    }


    static public GLSLProgram get(String programName) {
        return programs.get(programName);
    }


    static public String loadShaderFile(Context context, String name, String filename) {
        if (shaders.containsKey(name))
            return shaders.get(name);

        String data = AssetsUtils.loadAssetAsString(context, filename);
        shaders.put(name, data);
        return data;
    }


    static private String loadShaderFile(Context context, String name) {
        return loadShaderFile(context, name, "shaders/" + name + ".shader");
    }


    static public String getShader(String name) {
        return shaders.get(name);
    }


    static private GLSLProgram createFlatColorProgram() {
        String vertexShader = shaders.get("vp_flat_color");
        String fragmentShader = shaders.get("fp_flat_color");

        if ((vertexShader == null) || (fragmentShader == null))
            return null;


        GLSLProgram program = new GLSLProgram(FlatColor, vertexShader, fragmentShader);

        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareUniform(GLSLProgram.UNIFORM_COLOR, "u_Color");
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_ModelViewProjectionMatrix");

        return program;
    }


    static private GLSLProgram createFlatShadedProgram() {
        String vertexShader = shaders.get("vp_flat_shaded");
        String fragmentShader = shaders.get("fp_flat_color");

        if ((vertexShader == null) || (fragmentShader == null))
            return null;


        GLSLProgram program = new GLSLProgram(FlatShaded, vertexShader, fragmentShader);

        // Attributes
        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareAttribute(GLSLProgram.NORMAL, "a_Normal");

        // Uniforms
        program.declareUniform(GLSLProgram.UNIFORM_COLOR, "u_Color");
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_MVPMatrix");
        program.declareUniform(GLSLProgram.LIGHT_DIR, "u_lightVector");
        program.declareUniform(GLSLProgram.NORM_MATRIX, "u_NormMatrix");

        return program;
    }


    static private GLSLProgram createColoredVertexProgram() {
        String vertexShader = shaders.get("vp_colored_vertex");
        String fragmentShader = shaders.get("fp_flat_color");

        if ((vertexShader == null) || (fragmentShader == null))
            return null;


        GLSLProgram program = new GLSLProgram(ColoredVertex, vertexShader, fragmentShader);

        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareAttribute(GLSLProgram.ATTRIB_COLOR, "a_Color");
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_MVPMatrix");

        return program;
    }


    static private GLSLProgram createTexturedShadedProgram() {
        String vertexShader = shaders.get("vp_textured_shaded");
        String fragmentShader = shaders.get("fp_textured_shaded");

        if ((vertexShader == null) || (fragmentShader == null))
            return null;


        GLSLProgram program = new GLSLProgram(TexturedShaded, vertexShader, fragmentShader);

        // Attributes
        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareAttribute(GLSLProgram.NORMAL, "a_Normal");
        program.declareAttribute(GLSLProgram.TEXCOORD, "a_TexCoord");

        // Uniform
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_ModelViewProjectionMatrix");
        program.declareUniform(GLSLProgram.M_MATRIX, "u_ModelMatrix");
        program.declareUniform(GLSLProgram.MATERIAL_EMISSIVE, "u_MaterialEmissive");
        program.declareUniform(GLSLProgram.MATERIAL_SPECULAR, "u_MaterialSpecular");
        program.declareUniform(GLSLProgram.MATERIAL_SHININESS, "u_MaterialShininess");
        program.declareUniform(GLSLProgram.DIFFUSE_TEXTURE, "u_DiffuseTexture");
        program.declareUniform(GLSLProgram.AMBIENT_LIGHT, "u_AmbientLight");
        program.declareUniform(GLSLProgram.EYE_POS, "u_EyePos");
        program.declareUniform(GLSLProgram.LIGHT_POS, "u_LightPos");
        program.declareUniform(GLSLProgram.LIGHT_COLOR, "u_LightColor");

        return program;
    }


    static private GLSLProgram createMaterialShadedProgram() {
        String vertexShader = shaders.get("vp_material_shaded");
        String fragmentShader = shaders.get("fp_material_shaded");

        if ((vertexShader == null) || (fragmentShader == null))
            return null;


        GLSLProgram program = new GLSLProgram(MaterialShaded, vertexShader, fragmentShader);

        // Attributes
        program.declareAttribute(GLSLProgram.POSITION, "a_Position");
        program.declareAttribute(GLSLProgram.NORMAL, "a_Normal");

        // Uniform
        program.declareUniform(GLSLProgram.MVP_MATRIX, "u_ModelViewProjectionMatrix");
        program.declareUniform(GLSLProgram.M_MATRIX, "u_ModelMatrix");
        program.declareUniform(GLSLProgram.MATERIAL_EMISSIVE, "u_MaterialEmissive");
        program.declareUniform(GLSLProgram.MATERIAL_AMBIENT, "u_MaterialAmbient");
        program.declareUniform(GLSLProgram.MATERIAL_DIFFUSE, "u_MaterialDiffuse");
        program.declareUniform(GLSLProgram.MATERIAL_SPECULAR, "u_MaterialSpecular");
        program.declareUniform(GLSLProgram.MATERIAL_SHININESS, "u_MaterialShininess");
        program.declareUniform(GLSLProgram.AMBIENT_LIGHT, "u_AmbientLight");
        program.declareUniform(GLSLProgram.EYE_POS, "u_EyePos");
        program.declareUniform(GLSLProgram.LIGHT_POS, "u_LightPos");
        program.declareUniform(GLSLProgram.LIGHT_COLOR, "u_LightColor");

        return program;
    }
}
