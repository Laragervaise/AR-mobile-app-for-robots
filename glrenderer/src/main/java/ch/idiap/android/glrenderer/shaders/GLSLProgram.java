/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (c) 2012, Willow Garage, Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.glrenderer.shaders;

import java.util.HashMap;
import java.util.Map;

import android.opengl.GLES20;
import android.util.Log;


public class GLSLProgram {

	// Standard attributes
	public static final String POSITION = "POSITION";
	public static final String NORMAL = "NORMAL";
	public static final String TEXCOORD = "TEXCOORD";
	public static final String ATTRIB_COLOR = "ATTRIB_COLOR";

	// Standard uniforms
	public static final String MVP_MATRIX = "MVP_MATRIX";
	public static final String MV_MATRIX = "MV_MATRIX";
	public static final String M_MATRIX = "M_MATRIX";
	public static final String NORM_MATRIX = "NORM_MATRIX";
	public static final String DIFFUSE_TEXTURE = "DIFFUSE_TEXTURE";
	public static final String MATERIAL_EMISSIVE = "MATERIAL_EMISSIVE";
	public static final String MATERIAL_AMBIENT = "MATERIAL_AMBIENT";
	public static final String MATERIAL_DIFFUSE = "MATERIAL_DIFFUSE";
	public static final String MATERIAL_SPECULAR = "MATERIAL_SPECULAR";
	public static final String MATERIAL_SHININESS = "MATERIAL_SHININESS";
	public static final String AMBIENT_LIGHT = "AMBIENT_LIGHT";
	public static final String LIGHT_COLOR = "LIGHT_COLOR";
	public static final String LIGHT_POS = "LIGHT_POS";
	public static final String LIGHT_DIR = "LIGHT_DIR";
	public static final String EYE_POS = "EYE_POS";
	public static final String UNIFORM_COLOR = "UNIFORM_COLOR";


	private class ShaderParameter {
		String name;
		int location;
	}


	private String name;
	private String vertexShader;
	private String fragmentShader;
	private int programID = 0;
	private int fShaderHandle = 0;
	private int vShaderHandle = 0;
	private boolean compiled = false;

	private Map<String, ShaderParameter> attributeLocations = new HashMap<>();
	private Map<String, ShaderParameter> uniformsLocations = new HashMap<>();


	public GLSLProgram(String name, String vertex, String fragment) {
		if(vertex == null || fragment == null)
			throw new IllegalArgumentException("Vertex/fragment shader program cannot be null!");

		this.name = name;
		this.vertexShader = vertex;
		this.fragmentShader = fragment;

		ShaderManager.register(this);
	}


	public String getName() {
		return name;
	}


	public boolean compile() {
		programID = GLES20.glCreateProgram();

		// Check that attributes are in place
		if (attributeLocations.isEmpty() && uniformsLocations.isEmpty())
			throw new IllegalArgumentException("No attribute or uniform configured");

		// Load and compile
		vShaderHandle = loadShader(vertexShader, GLES20.GL_VERTEX_SHADER);
		fShaderHandle = loadShader(fragmentShader, GLES20.GL_FRAGMENT_SHADER);

		if(vShaderHandle == 0 || fShaderHandle == 0) {
			Log.e("GLSL", "Unable to compile shaders!");
			return false;
		}

		GLES20.glAttachShader(programID, vShaderHandle);
		GLES20.glAttachShader(programID, fShaderHandle);

		// Bind all attributes
		for (Map.Entry<String, ShaderParameter> attribute : attributeLocations.entrySet()) {
			GLES20.glBindAttribLocation(programID, attribute.getValue().location, attribute.getValue().name);
			Log.i("GLSL", "Bound attribute '" + attribute.getValue().name + "' to index #" + attribute.getValue().location);
		}

		// Link program
		int[] linkStatus = new int[1];
		GLES20.glLinkProgram(programID);
		GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);

		if (linkStatus[0] != GLES20.GL_TRUE) {
			Log.e("GLSL", "Unable to link program:");
			Log.e("GLSL", GLES20.glGetProgramInfoLog(programID));
			cleanup();
			return false;
		} else {
			Log.d("GLSL", "Linking ok!");
		}

		// Fetch all uniform locations
		for (Map.Entry<String, ShaderParameter> uniform : uniformsLocations.entrySet()) {
			uniform.getValue().location = GLES20.glGetUniformLocation(programID, uniform.getValue().name);
			Log.i("GLSL", "Fetched uniform '" + uniform.getValue().name + "' = " + uniform.getValue().location);
		}

		Log.d("GLSL", "Shader ID " + programID + " compiled successfully!");

		compiled = true;
		return true;
	}

	public boolean isCompiled() {
		return compiled;
	}

	public void use() {
		GLES20.glUseProgram(programID);
	}

	public void declareAttribute(String attribute, String name) {
		ShaderParameter param = new ShaderParameter();
		param.name = name;
		param.location = attributeLocations.size();

		attributeLocations.put(attribute, param);
	}

	public int getAttributeLocation(String attribute) {
		return attributeLocations.get(attribute).location;
	}

	public void declareUniform(String uniform, String name) {
		ShaderParameter param = new ShaderParameter();
		param.name = name;
		param.location = -1;

		uniformsLocations.put(uniform, param);
	}

	public int getUniformLocation(String uniform) {
		return uniformsLocations.get(uniform).location;
	}

	/* load a Vertex or Fragment shader */
	private int loadShader(String source, int shaderType) {
		int shader = GLES20.glCreateShader(shaderType);
		if(shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0) {
				Log.e("GLSL", "Could not compile shader " + shaderType + ":");
				Log.e("GLSL", GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
				throw new RuntimeException("Unable to compile shader!");
			}
		}
		Log.i("GLSL", "shader compiled: " + shader);
		return shader;
	}

	public void cleanup() {
		if(programID > 0)
			GLES20.glDeleteProgram(programID);
		if(vShaderHandle > 0)
			GLES20.glDeleteShader(vShaderHandle);
		if(fShaderHandle > 0)
			GLES20.glDeleteShader(fShaderHandle);

		fShaderHandle = 0;
		vShaderHandle = 0;
		programID = 0;
		compiled = false;
	}

	// Must only be called when a new OpenGL surface is created
	public void reset() {
		fShaderHandle = 0;
		vShaderHandle = 0;
		programID = 0;
		compiled = false;
	}
}
