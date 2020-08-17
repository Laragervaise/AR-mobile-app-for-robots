/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *   damonkohler@google.com (Damon Kohler)
 *
 * Original implementation:
 *   Copyright (C) 2011 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.glrenderer.materials;


/**
 * Defines a color based on RGBA values in the range [0, 1].
 */
public class Color {

    public float red;
    public float green;
    public float blue;
    public float alpha;


    public static Color copyOf(Color color) {
        return new Color(color.red, color.green, color.blue, color.alpha);
    }


    public static Color fromHexAndAlpha(String hex, float alpha) {
        float red = Integer.parseInt(hex.substring(0, 2), 16) / 255.0f;
        float green = Integer.parseInt(hex.substring(2, 4), 16) / 255.0f;
        float blue = Integer.parseInt(hex.substring(4), 16) / 255.0f;

        return new Color(red, green, blue, alpha);
    }


    public Color(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }


    public void set(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }


    public void set(Color color) {
        this.red = color.red;
        this.green = color.green;
        this.blue = color.blue;
        this.alpha = color.alpha;
    }


    @Override
    public String toString() {
        return "RGBA: (" + red + ", " + green + ", " + blue + ", " + alpha + ")";
    }

}
