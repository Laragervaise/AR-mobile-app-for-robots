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

import java.util.ArrayList;
import java.util.List;

import ch.idiap.android.glrenderer.helpers.InvalidXMLException;
import ch.idiap.android.glrenderer.meshes.loaders.VTDXmlReader;

import android.util.Log;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Read a URDF file and produce a UrdfLink object for each link in the file
 * 
 * @author azimmerman
 * 
 */
public class UrdfReader extends VTDXmlReader {

	public interface UrdfReadingProgressListener {
		public void readLink(int linkNumber, int linkCount);
	}

	private UrdfReadingProgressListener listener;

	public void setListener(UrdfReadingProgressListener l) {
		listener = l;
	}

	private void publishProgress(int link, int count) {
		if(listener != null)
			listener.readLink(link, count);
	}

	private List<UrdfLink> links = new ArrayList<>();
	private List<UrdfJoint> joints = new ArrayList<>();


	public UrdfReader() {
		super();
	}


	public void readUrdf(String urdf) throws InvalidXMLException {
		links.clear();
		joints.clear();

		if (super.parse(urdf)) {
			try {
				parseLinks();
				parseJoints();
			} catch(IllegalArgumentException e) {
				Log.e("URDF", "Can't parse URDF: " + e.getMessage());
				e.printStackTrace();
				throw new InvalidXMLException(e.getMessage());
			}
		} else {
			throw new InvalidXMLException("Improper XML formatting");
		}
	}


	public List<UrdfLink> getLinks() {
		return links;
	}

	public List<UrdfJoint> getJoints() {
		return joints;
	}


	private void parseLinks() {
		List<String> links = getAttributeList("/robot/link/@name");

		int nodeLength = links.size();

		for(int i = 0; i < nodeLength; i++) {
			Log.i("URDF", "Parsing link " + (i + 1) + " of " + nodeLength);

			// Link name
			String name = links.get(i);
			String prefix = "/robot/link[@name='" + name + "']";

			Component visual = null;
			Component collision = null;

			// Check for visual component
			if(nodeExists(prefix, "visual")) {
				String vprefix = prefix + "/visual";

				// Get geometry type
				String gtype = getSingleContents(vprefix, "geometry/*");

				Component.Builder visBuilder = new Component.Builder(gtype);

				switch(visBuilder.getType()) {
				case BOX: {
					String size = getSingleAttribute(vprefix, "/box/@size");
					visBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@radius"));
					float length = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@length"));
					visBuilder.setRadius(radius);
					visBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/sphere/@radius"));
					visBuilder.setRadius(radius);
				}
					break;
				case MESH:
					visBuilder.setMesh(getSingleAttribute(vprefix, "/mesh/@filename"));
					if(attributeExists(vprefix, "/mesh/@scale"))
						visBuilder.setMeshScale(super.toFloatArray(existResult));
					break;
				}

				// OPTIONAL - get origin
				if(attributeExists(vprefix, "/origin/@xyz")) {
					visBuilder.setOffset(toFloatArray(existResult));
				}
				if(attributeExists(vprefix, "/origin/@rpy")) {
					visBuilder.setRotation(toFloatArray(existResult));
				}

				// OPTIONAL - get material
				if(attributeExists(vprefix, "/material/@name")) {
					visBuilder.setMaterialName(existResult);
				}
				if(attributeExists(vprefix, "/material/color/@rgba")) {
					visBuilder.setMaterialColor(toFloatArray(existResult));
				}
				visual = visBuilder.build();
			}

			// Check for collision component
			if(nodeExists(prefix, "collision")) {
				String vprefix = prefix + "/collision";

				// Get geometry type
				String gtype = getSingleContents(vprefix, "geometry/*");

				Component.Builder colBuilder = new Component.Builder(gtype);

				switch(colBuilder.getType()) {
				case BOX: {
					String size = getSingleAttribute(vprefix, "/box/@size");
					colBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@radius"));
					float length = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@length"));
					colBuilder.setRadius(radius);
					colBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/sphere/@radius"));
					colBuilder.setRadius(radius);
				}
					break;
				case MESH:
					colBuilder.setMesh(getSingleAttribute(vprefix, "/mesh/@filename"));
					break;
				}

				// OPTIONAL - get origin
				if(attributeExists(vprefix, "/origin/@xyz"))
					colBuilder.setOffset(toFloatArray(existResult));
				if(attributeExists(vprefix, "/origin/@rpy"))
					colBuilder.setRotation(toFloatArray(existResult));

				collision = colBuilder.build();
			}

			UrdfLink newLink = new UrdfLink(visual, collision, name);
			this.links.add(newLink);
			publishProgress(i + 1, nodeLength);
		}
	}


	private void parseJoints() {
		List<String> joints = getAttributeList("/robot/joint/@name");

		int nodeLength = joints.size();

		for(int i = 0; i < nodeLength; i++) {
			Log.i("URDF", "Parsing joint " + (i + 1) + " of " + nodeLength);

			// Joint name
			String name = joints.get(i);
			String prefix = "/robot/joint[@name='" + name + "']";

			// Parent link name
			String parentLink = getSingleAttribute(prefix, "/parent/@link");

			// Child link name
			String childLink = getSingleAttribute(prefix, "/child/@link");

			// Joint type
			String type = getSingleAttribute(prefix, "/@type");

			// OPTIONAL - get origin
			Vector3f position = new Vector3f();
			if (attributeExists(prefix, "/origin/@xyz")) {
				float[] xyz = toFloatArray(existResult);
				position.set(xyz[0], xyz[1], xyz[2]);
			}

			Quaternionf orientation = new Quaternionf();
			if (attributeExists(prefix, "/origin/@rpy")) {
				float[] rpy = toFloatArray(existResult);
				orientation.rotateXYZ(rpy[0], rpy[1], rpy[2]);
			}

			// OPTIONAL - get axis
			Vector3f axis = new Vector3f();
			if (attributeExists(prefix, "/axis/@xyz")) {
				float[] xyz = toFloatArray(existResult);
				axis.set(xyz[0], xyz[1], xyz[2]);
			}


			UrdfJoint newJoint = new UrdfJoint(name, parentLink, childLink, type, position, orientation, axis);
			this.joints.add(newJoint);

			publishProgress(i + 1, nodeLength);
		}
	}
}
