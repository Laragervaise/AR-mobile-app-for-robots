/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdf.parser;


import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;


public class UrdfJoint {
	private String name;
	private String parent;
	private String child;
	private String type;
	private Vector3f originPosition;
	private Quaternionf originOrientation;
	private Vector3f axis;


	public UrdfJoint(String name, String parent, String child, String type,
					 Vector3fc originPosition, Quaternionfc originOrientation,
					 Vector3fc axis) {
		this.name = name;
		this.parent = parent;
		this.child = child;
		this.type = type;
		this.originPosition = new Vector3f(originPosition);
		this.originOrientation = new Quaternionf(originOrientation);
		this.axis = new Vector3f(axis);
	}


	public String getName() {
		return name;
	}

	public String getParentLink() {
		return parent;
	}

	public String getChildLink() {
		return child;
	}

	public String getType() {
		return type;
	}

	public Vector3fc getOriginPosition() {
		return originPosition;
	}

	public Quaternionfc getOriginOrientation() {
		return originOrientation;
	}

	public Vector3fc getAxis() {
		return axis;
	}


	@Override
	public String toString() {
		return "UrdfJoint [name=" + name + ", parent_link=" + parent + ", child_link=" + child +
				", origin_position=" + originPosition + ", origin_orientation=" +
				originOrientation + "]";
	}
}
