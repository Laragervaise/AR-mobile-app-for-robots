/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdf.parser;

import java.util.LinkedList;
import java.util.List;


public class UrdfLink {
	private Component visual;
	private Component collision;
	private String name;
	private LinkedList<Component> componentList = new LinkedList<Component>();


	public UrdfLink(Component visual, Component collision, String name) {
		this.visual = visual;
		this.collision = collision;
		this.name = name;
		
		if(visual != null)
			componentList.add(visual);
		if(collision != null)
			componentList.add(collision);
	}

	public Component getVisual() {
		return visual;
	}

	public Component getCollision() {
		return collision;
	}

	public String getName() {
		return name;
	}
	
	public List<Component> getComponents() {
		return componentList;
	}


	@Override
	public String toString() {
		return "UrdfLink [visual=" + visual + ", collision=" + collision + ", name=" + name + "]";
	}
	
}
