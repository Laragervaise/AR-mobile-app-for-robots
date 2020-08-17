/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (c) 2012, Willow Garage, Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdf.parser;


import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.materials.Color;


public class Component {
	public static enum GEOMETRY {
		CYLINDER, SPHERE, BOX, MESH
	};

	private GEOMETRY type;

	// Sphere & cylinder
	private float radius;
	private float length;

	// Box
	private float[] size;

	// Mesh
	private String mesh;

	// Origin
	private Transforms origin;

	// Material
	private String materialName;
	private Color materialColor;


	public GEOMETRY getType() {
		return type;
	}

	public float getRadius() {
		return radius;
	}

	public float getLength() {
		return length;
	}

	public float[] getSize() {
		return size;
	}

	public String getMesh() {
		return mesh;
	}

	public Transforms getOrigin() {
		return origin;
	}

	public String getMaterialName() {
		return materialName;
	}

	public Color getMaterialColor() {
		return materialColor;
	}

	public void setMaterial(Color materialColor) {
		this.materialColor = materialColor;
	}

	private Component() {
	}

	@Override
	public String toString() {
		return "Component [type=" + type + ", radius=" + radius + ", length=" + length + ", size=" +
				Arrays.toString(size) + ", mesh=" + mesh + ", scale=" + size[0] +
				", origin=" + origin + ", materialName=" + materialName + "]";
	}



	public static class Builder {
		private GEOMETRY type;
		private float radius = -1;
		private float length = -1;
		private float[] size = new float[] { 1f, 1f, 1f };
		private String mesh;
		private Quaternionf originOrientation = new Quaternionf();
		private Vector3f originTranslation = new Vector3f();
		private String material_name;
		private Color material_color;


		public Builder(GEOMETRY type) {
			this.type = type;
		}

		public Builder(String type) {
			this.type = GEOMETRY.valueOf(type.toUpperCase());
		}

		public void setRadius(float radius) {
			if(this.type == GEOMETRY.CYLINDER || this.type == GEOMETRY.SPHERE) {
				this.radius = radius;
			} else {
				throw new IllegalArgumentException("Can't set radius!");
			}
		}

		public void setLength(float length) {
			if(this.type == GEOMETRY.CYLINDER) {
				this.length = length;
			} else {
				throw new IllegalArgumentException("Can't set length!");
			}
		}

		public void setSize(float[] size) {
			if(this.type == GEOMETRY.BOX && size.length == 3) {
				this.size = size;
			} else {
				throw new IllegalArgumentException("Can't set size!");
			}
		}

		public void setMesh(String mesh) {
			if(this.type == GEOMETRY.MESH) {
				this.mesh = mesh;
			} else {
				throw new IllegalArgumentException("Can't set mesh!");
			}
		}

		public void setMeshScale(float[] scale) {
			if(this.type == GEOMETRY.MESH && scale.length == 3) {
				this.size = scale;
			} else {
				throw new IllegalArgumentException("Can't set mesh scale!");
			}
		}

		public void setOffset(float[] offset) {
			if(offset.length == 3) {
				this.originTranslation.set(offset[0], offset[1], offset[2]);
			} else {
				throw new IllegalArgumentException("Can't set offset!");
			}
		}

		public void setRotation(float[] rotation) {
			if(rotation.length == 3) {
				for(int i = 0; i < 3; i++) {
					this.originOrientation.rotateXYZ(rotation[0], rotation[1], rotation[2]);
				}
			} else {
				throw new IllegalArgumentException("Can't set rotation!");
			}
		}

		public void setMaterialName(String material_name) {
			this.material_name = material_name;
		}

		public void setMaterialColor(float[] color) {
			if(color.length == 4)
				this.material_color = new Color(color[0], color[1], color[2], color[3]);
			else
				throw new IllegalArgumentException("Can't set material color!");
		}

		public Component build() {
			Component retval = new Component();

			switch(type) {
			case MESH:
				if(mesh == null)
					throw new IllegalArgumentException("Never set a mesh name!");
				break;
			case BOX:
				if(size == null)
					throw new IllegalArgumentException("Never set a box size!");
				break;
			case CYLINDER:
				if(length < 0)
					throw new IllegalArgumentException("Never set a proper length!");
			case SPHERE:
				if(radius < 0)
					throw new IllegalArgumentException("Never set a proper radius!");
				break;
			}

			if(material_color != null && material_name == null)
				throw new IllegalArgumentException("Forgot to name the color " + material_color);

			if(material_color == null)
				material_color = new Color(1f, .5f, 0.15f, 1f);

			retval.type = type;
			retval.radius = radius;
			retval.length = length;
			retval.size = size;
			retval.mesh = mesh;
			retval.materialName = material_name;
			retval.materialColor = material_color;

			retval.origin = new Transforms(null);
			retval.origin.setPosition(originTranslation);
			retval.origin.setOrientation(originOrientation);

			return retval;
		}

		public GEOMETRY getType() {
			return type;
		}
	}
}
