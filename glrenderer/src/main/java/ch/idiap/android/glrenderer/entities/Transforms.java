/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.entities;

import androidx.annotation.NonNull;

import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;


/** Holds all the transformations needed for a 3D entity
 *
 * Can be organised in a hierarchy, where the parent transforms affect the children ones
 * */
public class Transforms {

    static public final int TRANSFORM_SPACE_LOCAL = 0;
    static public final int TRANSFORM_SPACE_PARENT = 1;
    static public final int TRANSFORM_SPACE_WORLD = 2;


    static public final Vector3fc ZERO = new Vector3f(0.0f, 0.0f, 0.0f);
    static public final Vector3fc UNIT_X = new Vector3f(1.0f, 0.0f, 0.0f);
    static public final Vector3fc UNIT_Y = new Vector3f(0.0f, 1.0f, 0.0f);
    static public final Vector3fc UNIT_Z = new Vector3f(0.0f, 0.0f, 1.0f);
    static public final Vector3fc UNIT_SCALE = new Vector3f(1.0f, 1.0f, 1.0f);

    static public final Quaternionfc IDENTITY = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);


    private Transformable transformable = null;
    public String tag = null;

    // Parent-children relation
    private Transforms parent = null;
    private final ArrayList<Transforms> children = new ArrayList<>();

    // Relative transforms
    private Vector3f position;
    private Quaternionf orientation;
    private Vector3f scale;

    // Full (world) transforms
    private Vector3f fullPosition;
    private Quaternionf fullOrientation;
    private Vector3f fullScale;

    private boolean dirty;
    private boolean inheritOrientation;
    private boolean inheritScale;


    public Transforms() {
        this(null);
    }


    public Transforms(Transformable transformable) {
        this.transformable = transformable;

        position = new Vector3f();
        orientation = new Quaternionf();
        scale = new Vector3f(1.0f);

        fullPosition = new Vector3f();
        fullOrientation = new Quaternionf();
        fullScale = new Vector3f(1.0f);

        dirty = true;
        inheritOrientation = true;
        inheritScale = true;
    }


    public Transformable getTransformable() {
        return transformable;
    }


    public void setParent(Transforms transforms) {
        if (parent != null)
        {
            parent.children.remove(this);
            parent = null;
        }

        if (transforms != null) {
            parent = transforms;
            parent.children.add(this);
        }

        needUpdate();
    }


    public Transforms getParent() {
        return parent;
    }


    public ArrayList<Transforms> getChildren() {
        return children;
    }


    public void toOpenGL(float[] dest) {
        if (dirty)
            update();

        Matrix3f rot3x3 = new Matrix3f().set(fullOrientation);

        // Set up final matrix with full scale, rotation and translation
        dest[0] = fullScale.x * rot3x3.m00;
        dest[1] = fullScale.x * rot3x3.m01;
        dest[2] = fullScale.x * rot3x3.m02;
        dest[3] = 0.0f;

        dest[4] = fullScale.y * rot3x3.m10;
        dest[5] = fullScale.y * rot3x3.m11;
        dest[6] = fullScale.y * rot3x3.m12;
        dest[7] = 0.0f;

        dest[8] = fullScale.z * rot3x3.m20;
        dest[9] = fullScale.z * rot3x3.m21;
        dest[10] = fullScale.z * rot3x3.m22;
        dest[11] = 0.0f;

        dest[12] = fullPosition.x;
        dest[13] = fullPosition.y;
        dest[14] = fullPosition.z;
        dest[15] = 1.0f;
    }


    public Matrix4f toMatrix() {
        if (dirty)
            update();

        Matrix3f rot3x3 = new Matrix3f().set(fullOrientation);

        // Set up final matrix with full scale, rotation and translation
        return new Matrix4f(
                fullScale.x * rot3x3.m00, fullScale.x * rot3x3.m01, fullScale.x * rot3x3.m02, 0.0f,
                fullScale.y * rot3x3.m10, fullScale.y * rot3x3.m11, fullScale.y * rot3x3.m12, 0.0f,
                fullScale.z * rot3x3.m20, fullScale.z * rot3x3.m21, fullScale.z * rot3x3.m22, 0.0f,
                fullPosition.x, fullPosition.y, fullPosition.z, 1.0f
        );
    }


    public void set(Matrix4fc m) {
        m.getTranslation(position);
        m.getNormalizedRotation(orientation);
        m.getScale(scale);
        needUpdate();
    }


    @NonNull
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + " position=" + position + ", orientation=" +
                orientation + ", scale=" + scale + ", tag='" + tag + "']";
    }


    public void setWorldTransforms(
            Vector3fc worldPosition, Quaternionfc worldOrientation, Vector3fc worldScale) {

        if (parent != null) {
            setPosition(Transforms.ZERO);
            setOrientation(Transforms.IDENTITY);

            Vector3f relativeScale = new Vector3f(worldScale).div(parent.getWorldScale());

            Vector3f relativePosition = new Vector3f(worldPosition).sub(parent.getWorldPosition());

            Quaternionf relativeRotation =
                    new Quaternionf(worldOrientation).mul(
                            new Quaternionf(parent.getWorldOrientation()).invert());

            setScale(relativeScale);
            translate(relativePosition, Transforms.TRANSFORM_SPACE_WORLD);
            rotate(relativeRotation, Transforms.TRANSFORM_SPACE_WORLD);

        } else {
            setPosition(worldPosition);
            setOrientation(worldOrientation);
            setScale(worldScale);
        }
    }


    public void setWorldTransforms(
            Vector3fc worldPosition, Quaternionfc worldOrientation) {

        setWorldTransforms(worldPosition, worldOrientation, UNIT_SCALE);
    }


    /************************************ POSITION *************************************/

    public void setPosition(Vector3fc pos) {
        position.set(pos);
        needUpdate();
    }


    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        needUpdate();
    }


    public void setPosition(float[] pos) {
        position.set(pos[0], pos[1], pos[2]);
        needUpdate();
    }


    public void translate(Vector3fc d, int transform_space) {
        switch (transform_space) {
            case TRANSFORM_SPACE_LOCAL: {
                position.add(orientation.transform(new Vector3f(d)));
                break;
            }

            case TRANSFORM_SPACE_PARENT: {
                position.add(d);
                break;
            }

            case TRANSFORM_SPACE_WORLD: {
                if (parent != null) {
                    Quaternionf invertedParentOrientation = new Quaternionf(parent.getWorldOrientation()).invert();
                    position.add(invertedParentOrientation.transform(new Vector3f(d)).div(parent.getWorldScale()));
                } else {
                    position.add(d);
                }
                break;
            }
        }

        needUpdate();
    }


    public void translate(float x, float y, float z, int transform_space) {
        translate(new Vector3f(x, y, z), transform_space);
    }


    public void translate(float x, float y, float z) {
        translate(new Vector3f(x, y, z), TRANSFORM_SPACE_LOCAL);
    }


    public void translate(Vector3fc d) {
        translate(d, TRANSFORM_SPACE_LOCAL);
    }


    public Vector3fc getWorldPosition() {
        if (dirty)
            update();

        return fullPosition;
    }


    public Vector3fc getPosition() {
        return position;
    }


    /*********************************** ORIENTATION ************************************/

    public void setOrientation(Quaternionfc q) {
        orientation.set(q);
        needUpdate();
    }


    public void setOrientation(float x, float y, float z, float w) {
        orientation.set(x, y, z, w);
        needUpdate();
    }


    public void setOrientation(float[] q) {
        orientation.set(q[0], q[1], q[2], q[3]);
        needUpdate();
    }


    public void roll(float angle, int transform_space) {
        rotate(UNIT_Z, angle, transform_space);
    }


    public void pitch(float angle, int transform_space) {
        rotate(UNIT_X, angle, transform_space);
    }


    public void yaw(float angle, int transform_space) {
        rotate(UNIT_Y, angle, transform_space);
    }


    public void roll(float angle) {
        rotate(UNIT_Z, angle, TRANSFORM_SPACE_LOCAL);
    }


    public void pitch(float angle) {
        rotate(UNIT_X, angle, TRANSFORM_SPACE_LOCAL);
    }


    public void yaw(float angle) {
        rotate(UNIT_Y, angle, TRANSFORM_SPACE_LOCAL);
    }


    public void rotate(Vector3fc axis, float angle, int transform_space) {
        rotate(new Quaternionf(new AxisAngle4f(angle, axis)), transform_space);
    }


    public void rotate(Vector3fc axis, float angle) {
        rotate(new Quaternionf(new AxisAngle4f(angle, axis)), TRANSFORM_SPACE_LOCAL);
    }


    public void rotate(Quaternionfc q, int transform_space) {
        switch (transform_space) {

            case TRANSFORM_SPACE_PARENT: {
                orientation.premul(q);
                break;
            }

            case TRANSFORM_SPACE_WORLD: {
                Quaternionf invertedWorldOrientation = new Quaternionf(getWorldOrientation()).invert();
                orientation.mul(invertedWorldOrientation).mul(q).mul(getWorldOrientation());
                break;
            }

            case TRANSFORM_SPACE_LOCAL: {
                orientation.mul(q);
                break;
            }
        }

        needUpdate();
    }


    public void rotate(Quaternionfc q) {
        rotate(q, TRANSFORM_SPACE_LOCAL);
    }


    public void rotate(float x, float y, float z, float w, int transform_space) {
        rotate(new Quaternionf(x, y, z, w), transform_space);
    }


    public void rotate(float x, float y, float z, float w) {
        rotate(new Quaternionf(x, y, z, w), TRANSFORM_SPACE_LOCAL);
    }


    public void resetOrientation() {
        orientation.identity();
        needUpdate();
    }


    public void setInheritOrientation(boolean inherit) {
        inheritOrientation = inherit;
        needUpdate();
    }


    public boolean getInheritOrientation() {
        return inheritOrientation;
    }


    public Quaternionfc getWorldOrientation() {
        if (dirty)
            update();

        return fullOrientation;
    }


    public Quaternionfc getOrientation() {
        return orientation;
    }


    /************************************** SCALE **************************************/

    public void setScale(Vector3fc scale) {
        this.scale.set(scale);
        needUpdate();
    }


    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
        needUpdate();
    }


    public void setScale(float[] scale) {
        this.scale.set(scale[0], scale[1], scale[2]);
        needUpdate();
    }


    public void scale(Vector3fc scale) {
        this.scale.mul(scale);
        needUpdate();
    }


    public void scale(float x, float y, float z) {
        scale(new Vector3f(x, y, z));
    }


    public void setInheritScale(boolean inherit) {
        inheritScale = inherit;
        needUpdate();
    }


    public boolean getInheritScale() {
        return inheritScale;
    }


    public Vector3fc getWorldScale() {
        if (dirty)
            update();

        return fullScale;
    }


    public Vector3fc getScale() {
        return scale;
    }


    /********************************* INTERNAL METHODS ********************************/

    private void needUpdate() {
        dirty = true;

        for (Transforms child: children) {
            child.needUpdate();
        }
    }


    private void update() {
        if (!dirty)
            return;

        if (parent != null) {
            // Update orientation
            Quaternionfc parentOrientation = parent.getWorldOrientation();
            if (inheritOrientation) {
                // Combine orientation with the one of the parent
                fullOrientation.set(parentOrientation).mul(orientation).normalize();
            } else {
                fullOrientation.set(orientation);
            }

            // Update scale
            Vector3fc parentScale = parent.getWorldScale();
            if (inheritScale) {
                // Scale own scale by parent scale (nb: just combine as equivalent axes, no shearing)
                fullScale.set(parentScale).mul(scale);
            } else {
                fullScale.set(scale);
            }

            // Change position vector based on parent's orientation and scale
            parentScale.mul(position, fullPosition);
            fullPosition = parentOrientation.transform(fullPosition);

            // Add altered position vector to parent's
            fullPosition.add(parent.getWorldPosition());
        } else {
            // No parent
            fullPosition.set(position);
            fullOrientation.set(orientation);
            fullScale.set(scale);
        }

        dirty = false;
    }
}
