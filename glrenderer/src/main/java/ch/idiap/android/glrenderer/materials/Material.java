/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.materials;


import ch.idiap.android.glrenderer.textures.Texture;


public class Material {

    public Color emissive = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public Color ambient = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public Color diffuse = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public Color specular = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public float shininess = 0.0f;

    public Texture diffuseTexture = null;

    private String name = null;


    public Material(String name) {
        this.name = name;
    }


    public Material() {
    }


    public String getName() {
        return name;
    }


    public Material clone() {
        Material copy = new Material();

        copy.emissive = Color.copyOf(emissive);
        copy.ambient = Color.copyOf(ambient);
        copy.diffuse = Color.copyOf(diffuse);
        copy.specular = Color.copyOf(specular);
        copy.shininess = shininess;

        copy.diffuseTexture = diffuseTexture;

        return copy;
    }
}
