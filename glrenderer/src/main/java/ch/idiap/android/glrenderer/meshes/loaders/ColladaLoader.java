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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.idiap.android.glrenderer.helpers.AssetsUtils;
import ch.idiap.android.glrenderer.materials.Material;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.materials.MaterialManager;
import ch.idiap.android.glrenderer.meshes.Mesh;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.glrenderer.textures.Texture;
import ch.idiap.android.glrenderer.textures.TextureManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;

import org.joml.Matrix4f;


public class ColladaLoader extends VTDXmlReader implements Loader {

	private static Color defaultColor = new Color(1f, 1f, 1f, 1);


	private enum semanticType {
		POSITION(3), NORMAL(3), TEXCOORD(2);

		private int mul;

		semanticType(int mul) {
			this.mul = mul;
		}

		public int numElements(int vertexCount) {
			return mul * vertexCount;
		}
	}


	private List<String> meshes = new ArrayList<String>();
	private String imgPrefix;
	private String prefix;
	private CoordinatesSystemTransformation coordSystemTransform;


	public List<String> load(Context context, String filename) {
		return load(context, filename, CoordinatesSystemTransformation.X__Y__Z);
	}


	public List<String> load(Context context, String filename, CoordinatesSystemTransformation transform) {
		String content = AssetsUtils.loadAssetAsString(context, filename);
		if (content == null)
			return null;

		int lastIndex = filename.lastIndexOf(".dae");
		prefix = filename.substring(0, lastIndex);

		lastIndex = filename.lastIndexOf("/");
		imgPrefix = filename.substring(0, lastIndex);

		coordSystemTransform = transform;

		if (!parse(content))
			return null;

		parseMaterials(context);
		parseGeometries(context);

		return meshes;
	}


	private void parseMaterials(Context context) {
		// Get the ID of each material section
		List<String> nodes = super.getAttributeList("/COLLADA/library_materials/material/@id");

		Map<String, Texture> textures = getTextures(context);

		for(int i = 0; i < nodes.size(); i++) {
			String ID = nodes.get(i);
			Log.d("DAE", "Parsing material " + ID);

			Material material = new Material(prefix + "/" + ID);
			material.diffuse = Color.copyOf(defaultColor);

			if (attributeExists("/COLLADA/library_materials/material[@id='" + ID + "']/instance_effect", "@url")) {
				String effectId = super.existResult.substring(1);

				if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//emission/color/text()")) {
					float[] colorData = toFloatArray(super.existResult);
					material.emissive.red = colorData[0];
					material.emissive.green = colorData[1];
					material.emissive.blue = colorData[2];
					material.emissive.alpha = colorData[3];
				}

				if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//ambient/color/text()")) {
					float[] colorData = toFloatArray(super.existResult);
					material.ambient.red = colorData[0];
					material.ambient.green = colorData[1];
					material.ambient.blue = colorData[2];
					material.ambient.alpha = colorData[3];
				}

				if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//diffuse/color/text()")) {
					float[] colorData = toFloatArray(super.existResult);
					material.diffuse.red = colorData[0];
					material.diffuse.green = colorData[1];
					material.diffuse.blue = colorData[2];
					material.diffuse.alpha = colorData[3];
				}

				if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//specular/color/text()")) {
					float[] colorData = toFloatArray(super.existResult);
					material.specular.red = colorData[0];
					material.specular.green = colorData[1];
					material.specular.blue = colorData[2];
					material.specular.alpha = colorData[3];
				}

				if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//shininess/float/text()")) {
					float[] shininessData = toFloatArray(super.existResult);
					material.shininess = shininessData[0];
				}

				if (attributeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//diffuse/texture", "@texture")) {
					String samplerId = super.existResult;

					if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//newparam[@sid='" + samplerId + "']//source/text()")) {
						String surfaceId = super.existResult;

						if (nodeExists("/COLLADA/library_effects/effect[@id='" + effectId + "']//newparam[@sid='" + surfaceId + "']//init_from/text()")) {
							String imageId = super.existResult;

							if (nodeExists("/COLLADA/library_images/image[@id='" + imageId + "']//init_from/text()")) {
								String filename = super.existResult;

								material.diffuseTexture = TextureManager.get(filename);
							}
						}
					}
				}
			}

			MaterialManager.register(material);
		}
	}


	private void parseGeometries(Context context) {
		// Get the ID of each geometry section
		List<String> nodes = super.getAttributeList("/COLLADA/library_geometries/geometry/@id");

		for (int i = 0; i < nodes.size(); i++) {
			String ID = nodes.get(i);
			Log.d("DAE", "Parsing geometry " + ID);
			parseGeometry(context, ID);
		}
	}


	private enum TYPES {
		triangles(3, 0), tristrips(1, -2), trifans(1, -2);
		private int mul;
		private int sub;

		TYPES(int b, int i) {
			this.mul = b;
			this.sub = i;
		}

		public int getVertexCount(int elementCount) {
			return (mul * elementCount) + sub;
		}
	};


	// Each geometry can have multiple associated triangles in different
	// configurations using different materials
	// They all use the same vertices and normals though. If they don't, they
	// aren't supported by this loader currently.
	private void parseGeometry(Context context, String id) {
		String prefix = "/COLLADA/library_geometries/geometry[@id='" + id + "']/mesh";

		// If the selected geometry doesn't contain one of the types, return
		// null. We're not interested in lines or any other shenanigans
		boolean acceptableGeometry = false;
		for (TYPES t : TYPES.values()) {
			if (nodeExists(prefix, "", t.toString())) {
				acceptableGeometry = true;
				break;
			}
		}

		if (!acceptableGeometry) {
			return;
		}

		// For each submesh inside the mesh tag, parse its vertices, normals, and texture data
		for (TYPES type : TYPES.values()) {
			List<String> nodes = super.getNodeList(prefix, type.toString());
			for(int i = 1; i <= nodes.size(); i++) {
				Mesh mesh = parseSubMesh(context, this.prefix + "/"  + id + "_" + String.valueOf(i), prefix, type, i);
				if (mesh != null)
					meshes.add(mesh.getName());
			}
		}
	}


	private Mesh parseSubMesh(Context context, String name, String prefix, TYPES type, int submeshIndex) {
		// Load all necessary data (vertices, normals, texture coordinates, etc
		Map<String, InputData> data = null;
		try {
			data = getDataFromAllInputs(prefix, type.toString());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}

		// Load indices
		int[] indices = toIntArray(getSingleAttribute(prefix, type + "[" + submeshIndex + "]/p"));

		// Find the triangle count
		int triCount = Integer.parseInt(getSingleAttribute(prefix, type.toString(), "@count"));

		Log.d("DAE", triCount + " triangles.");


		// Find the scale of the mesh (if present)
		if(nodeExists("/COLLADA/library_visual_scenes//scale/text()")) {
			float[] scales = toFloatArray(super.existResult);
			float[] vertices = data.get("POSITION").getData().getArray();
			for(int i = 0; i < vertices.length; i++) {
				vertices[i] = vertices[i] * scales[i % 3];
			}
		}

		// Find the transforms of the mesh (if present)
		int start = prefix.indexOf("[@id='");
		int end = prefix.indexOf("']", start);
		String geometryId = prefix.substring(start + 6, end);

		if(attributeExists("/COLLADA/library_visual_scenes//node/instance_geometry[@url='#" + geometryId + "']", "@name")) {
			String nodeId = super.existResult;

			float[] transform = new float[16];

			if (nodeExists("/COLLADA/library_visual_scenes//node[@id='"+ nodeId + "']/matrix[@sid='transform']/text()")) {
				Matrix4f m = new Matrix4f().set(toFloatArray(super.existResult)).transpose();
				m.get(transform);
			}
			else {
				Matrix.setIdentityM(transform, 0);
			}

			float[] src = new float[4];
			float[] dst = new float[4];

			src[3] = 1.0f;

			float[] vertices = data.get("POSITION").getData().getArray();
			for (int i = 0; i < vertices.length; i += 3) {
				src[0] = vertices[i];
				src[1] = vertices[i + 1];
				src[2] = vertices[i + 2];

				Matrix.multiplyMV(dst, 0, transform, 0, src, 0);

				if (coordSystemTransform == CoordinatesSystemTransformation.X__Y__Z) {
					vertices[i] = dst[0];
					vertices[i + 1] = dst[1];
					vertices[i + 2] = dst[2];
				} else if (coordSystemTransform == CoordinatesSystemTransformation.X__Z__MINUS_Y) {
					vertices[i] = dst[0];
					vertices[i + 1] = dst[2];
					vertices[i + 2] = -dst[1];
				}
			}

			if (data.containsKey("NORMAL")) {
				float[] normals = data.get("NORMAL").getData().getArray();
				for (int i = 0; i < normals.length; i += 3) {
					src[0] = normals[i];
					src[1] = normals[i + 1];
					src[2] = normals[i + 2];

					Matrix.multiplyMV(dst, 0, transform, 0, src, 0);

					if (coordSystemTransform == CoordinatesSystemTransformation.X__Y__Z) {
						normals[i] = dst[0];
						normals[i + 1] = dst[1];
						normals[i + 2] = dst[2];
					} else if (coordSystemTransform == CoordinatesSystemTransformation.X__Z__MINUS_Y) {
						normals[i] = dst[0];
						normals[i + 1] = dst[2];
						normals[i + 2] = -dst[1];
					}
				}
			}
		}


		// Find the name of the material of the mesh (if present)
		String defaultMaterialName = null;
		if (attributeExists("/COLLADA/library_visual_scenes//node/instance_geometry[@url='#" + geometryId + "']//instance_material", "@target")) {
			defaultMaterialName = this.prefix + "/" + super.existResult.substring(1);
		}


		boolean textured = false;

		// Load the images if the mesh is textured. Otherwise, if the normals and positions are the only
		// values included AND they have the same offset, there's no need to deindex, can return a mesh immediately
		if (data.containsKey("TEXCOORD")) {
			textured = true;
		} else if (data.size() == 2 && data.containsKey("NORMAL") && data.containsKey("POSITION") && (data.get("NORMAL").getOffset() == data.get("POSITION").getOffset())) {
			Log.d("DAE", "Deindexing is not necessary for this mesh!");

			Mesh mesh = new Mesh(name, data.get("POSITION").getData().getArray(), data.get("NORMAL").getData().getArray(), indices);
			MeshManager.register(mesh);

			return mesh;
		}

		// Deindex
		Map<String, FloatVector> results = deindex(data, indices, type.getVertexCount(triCount));

		Log.i("DAE", "The following information is available for each vertex: " + results.keySet());

		if (!results.containsKey("NORMAL"))
			return null;

		Mesh mesh = null;

		if (!textured) {
			switch(type) {
				case triangles:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							GLES20.GL_TRIANGLES);
					break;

				case tristrips:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							GLES20.GL_TRIANGLE_STRIP);
					break;

				case trifans:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							GLES20.GL_TRIANGLE_FAN);
					break;

				default:
					break;
			}
		} else {
			switch(type) {
				case triangles:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							results.get("TEXCOORD").getArray(),
							GLES20.GL_TRIANGLES);
					break;

				case tristrips:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							results.get("TEXCOORD").getArray(),
							GLES20.GL_TRIANGLE_STRIP);
					break;

				case trifans:
					mesh = new Mesh(name, results.get("POSITION").getArray(),
							results.get("NORMAL").getArray(),
							results.get("TEXCOORD").getArray(),
							GLES20.GL_TRIANGLE_FAN);
					break;

				default:
					break;
			}
		}

		if (mesh != null) {
			mesh.setDefaultMaterialName(defaultMaterialName);
			MeshManager.register(mesh);
			return mesh;
		}

		return null;
	}


	private enum textureType {
		diffuse
	};


	private Map<String, Texture> getTextures(Context context) {
		Map<String, Texture> retval = new HashMap<>();

		// Find which types of acceptable textures are present (diffuse, bump, etc)
		for (textureType t : textureType.values()) {
			if (attributeExists("/COLLADA/library_effects/", t.toString(), "texture/@texture")) {
				String texPointer = super.existResult;
				
				String filename = null;
				// If the image library has an image with texPointer's ID, use that
				// otherwise, follow the pointer trail
				if (attributeExists("/COLLADA/library_images/image[@id='" + texPointer + "']/init_from")) {
					filename = super.existResult;
					Log.d("DAE", "Shortcut to texture name: " + filename);
				} else {
					// Locate the image ID from the texture pointer
					String imgID = getSingleAttribute("/COLLADA/library_effects//newparam[@sid='" + texPointer + "']/sampler2D/source");
					
					// Locate the image name
					String imgName = getSingleAttribute("/COLLADA/library_effects//newparam[@sid='" + imgID + "']/surface/init_from");
					
					// Locate the filename
					filename = getSingleAttribute("/COLLADA/library_images/image[@id='" + imgName + "']/init_from");
				}
				
				Log.d("DAE", "Filename = " + filename);

				if (filename.length() == 0)
					Log.e("DAE", "Filename = 0 length!");


				Texture texture = TextureManager.get(filename);
				if (texture == null) {
					Bitmap bitmap = null;

					InputStream stream = AssetsUtils.loadAsset(context, "models/" + filename);
					if (stream == null) {
						Log.e("DAE", "Unable to get file '" + filename + "'");
						bitmap = Bitmap.createBitmap(new int[]{0,0}, 1, 1, Bitmap.Config.RGB_565);
					} else {
						bitmap = BitmapFactory.decodeStream(stream);

						try {
							stream.close();
						} catch (IOException ex) {
						}
					}

					// Flip the image
					android.graphics.Matrix flip = new android.graphics.Matrix();
					flip.postScale(1f, -1f);

					Bitmap bitmap2 = Bitmap.createBitmap(
							bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flip, true
					);
					bitmap.recycle();

					// Create the texture
					texture = new Texture(filename, bitmap2);

					// Register it
					TextureManager.register(texture);
				}

				// Add the compressed texture to the return map
				retval.put(t.toString(), texture);
			}
		}

		return retval;
	}


	private Map<String, FloatVector> deindex(Map<String, InputData> data, int[] indices, int vertexCount) {
		Map<String, FloatVector> retval = new HashMap<String, FloatVector>();

		List<InputData> sources = new ArrayList<InputData>(data.values());

		int inputCount = -99;
		for(InputData id : sources) {
			inputCount = Math.max(inputCount, id.getOffset());
			retval.put(id.getSemantic(), new FloatVector(id.getFloatElements(vertexCount)));
		}

		int curOffset = 0;
		for(Integer s : indices) {
			for(InputData id : sources) {
				if(curOffset == id.getOffset()) {
					FloatVector reciever = retval.get(id.getSemantic());
					id.appendData(reciever, s);
				}
			}

			if(curOffset == inputCount)
				curOffset = 0;
			else
				curOffset++;
		}

		return retval;
	}

	private Map<String, InputData> getDataFromAllInputs(String prefix, String subMeshType) throws NumberFormatException, NavException, XPathEvalException {
		Map<String, InputData> retval = new HashMap<String, InputData>();

		getExpression(prefix, subMeshType, "input");
		int i;
		List<Integer> inputNodeLocations = new LinkedList<Integer>();
		while((i = ap.evalXPath()) != -1) {
			inputNodeLocations.add(i);
		}

		for(Integer b : inputNodeLocations) {
			vn.recoverNode(b);
			String semantic = vn.toString(vn.getAttrVal("semantic"));
			String sourceID = vn.toString(vn.getAttrVal("source")).substring(1);
			int offset = Integer.parseInt(vn.toString(vn.getAttrVal("offset")));
			List<InputData> returned = getDataFromInput(prefix, semantic, sourceID);
			for(InputData id : returned) {
				id.setOffset(offset);
				retval.put(id.getSemantic(), id);
			}
		}

		return retval;
	}

	private List<InputData> getDataFromInput(String prefix, String semantic, String sourceID) {
		List<InputData> retval = new ArrayList<InputData>();

		// Find whatever node has the requested ID
		String nodetype = getSingleContents(prefix, "/*[@id='" + sourceID + "']");

		// If it's a vertices node, get the data from the inputs it references
		if(nodetype.equals("vertices")) {
			List<String> inputs = super.getAttributeList(prefix, "/vertices[@id='" + sourceID + "']/input/@semantic");
			for(String subSemantic : inputs) {
				retval.addAll(getDataFromInput(prefix, subSemantic, getSingleAttribute(prefix, "/vertices[@id='" + sourceID + "']/input[@semantic='" + subSemantic + "']/@source").substring(1)));
			}

		} else
		// If it's a source, grab its float_array data
		if(nodetype.equals("source")) {
			retval.add(new InputData(semantic, new FloatVector(toFloatArray(getSingleContents(prefix, "/source[@id='" + sourceID + "']/float_array/text()")))));
			return retval;
		} else {
			Log.e("DAE", "ERR! UNKNOWN NODE TYPE: " + nodetype);
		}

		return retval;
	}

	private class InputData {
		private semanticType sType;
		private int offset = -1;
		private FloatVector data;

		public InputData(String semantic, FloatVector data) {
			super();
			this.sType = semanticType.valueOf(semantic);
			this.data = data;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public String getSemantic() {
			return sType.toString();
		}

		public int getOffset() {
			return offset;
		}

		public FloatVector getData() {
			return data;
		}

		public int getFloatElements(int vertexCount) {
			return sType.numElements(vertexCount);
		}

		@Override
		public String toString() {
			return "InputData [semantic=" + sType.toString() + ", offset=" + offset + ", data size=" + data.getIdx() + "]";
		}

		public void appendData(FloatVector destination, int idx) {
			switch(sType) {
			case TEXCOORD:
				for(int b = (idx * 2); b < (idx * 2) + 2; b++)
					destination.add(data.get(b));
				break;
			case POSITION:
				for(int b = (idx * 3); b < (idx * 3) + 3; b++)
					destination.add(data.get(b));
				break;
			case NORMAL:
				// Normalize the loaded normal
				int offset = idx * 3;
				float x = data.get(offset++);
				float y = data.get(offset++);
				float z = data.get(offset++);
				float len = (float) Math.sqrt(x * x + y * y + z * z);

				destination.add(x / len);
				destination.add(y / len);
				destination.add(z / len);
				break;
			}
		}
	}
}
