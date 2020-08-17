/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.glrenderer.entities;


import org.joml.AABBf;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;
import ch.idiap.android.glrenderer.physics.PhysicBody;
import ch.idiap.android.glrenderer.physics.World;


public class Entity extends Transformable {

    //_____ Attributes __________

    // Parent-children relationship
    private Entity parent = null;
    private final ArrayList<Entity> children = new ArrayList<>();

    // Renderables
    private final ArrayList<Renderable> renderables = new ArrayList<>();

    // Physics
    private PhysicBody physicBody = null;


    //_____ Parent-children relationship-related methods __________

    public void setParent(Entity entity) {
        if (parent != null)
        {
            parent.children.remove(this);
            parent = null;
        }

        if (entity != null) {
            parent = entity;
            parent.children.add(this);
        }

        super.setParent(parent);
    }


    public Entity getParent() {
        return parent;
    }


    public ArrayList<Entity> getChildren() {
        return children;
    }


    public void destroy(World world) {
        setParent(null);

        while (!children.isEmpty())
            children.get(0).destroy(world);

        if ((world != null) && (physicBody != null))
            world.removeRigidBody(physicBody.getRigidBody());
    }


    //_____ Renderables-related methods __________

    public void addRenderable(Renderable renderable) {
        renderable.transforms.setParent(transforms);
        renderables.add(renderable);
    }


    protected void addRenderable(Renderable renderable, Transforms parentTransforms) {
        renderable.transforms.setParent(parentTransforms);
        renderables.add(renderable);
    }


    public List<Renderable> getRenderables() {
        return renderables;
    }


    public List<Renderable> getRenderables(String tag) {
        List<Renderable> result = new ArrayList<>();

        for (Renderable renderable: renderables) {
            if (renderable.tag.equals(tag))
                result.add(renderable);
        }

        return result;
    }


    //_____ PhysicBody-related methods __________

    public void addPhysicBody(PhysicBody body) {
        if (physicBody != null) {
            physicBody._setEntity(null);
            physicBody.transforms.setParent(null);
        }

        physicBody = body;

        if (physicBody != null) {
            body.transforms.setParent(transforms);
            physicBody._setEntity(this);
        }
    }


    protected void addPhysicBody(PhysicBody body, Transforms parentTransforms) {
        if (physicBody != null) {
            physicBody._setEntity(null);
            physicBody.transforms.setParent(null);
        }

        physicBody = body;

        if (physicBody != null) {
            body.transforms.setParent(parentTransforms);
            physicBody._setEntity(this);
        }
    }


    public PhysicBody getPhysicBody() {
        return physicBody;
    }


    //_____ Methods __________

    public void draw(BaseCamera camera, Color ambientLight, Light light) {
        for (Renderable renderable : renderables) {
            renderable.draw(camera, ambientLight, light);
        }

        for (Entity child: children) {
            child.draw(camera, ambientLight, light);
        }
    }


    public AABBf getBoundingBox() {
        AABBf boundingBox = new AABBf();

        Vector3f min = new Vector3f();
        Vector3f max = new Vector3f();

        for (Renderable renderable : renderables) {
            AABBf bb = renderable.getBoundingBox();

            Matrix4fc transforms = renderable.transforms.toMatrix();
            transforms.transformAab(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, min, max);

            boundingBox.union(min);
            boundingBox.union(max);
        }

        for (Entity child : children)
            boundingBox.union(child.getBoundingBox());

        return boundingBox;
    }


    //_____ For debugging __________

    public void printCompleteDescription(String prefix) {
        System.out.println(prefix + "[" + this.getClass().getSimpleName() + "]");
        System.out.println(prefix + "    Nb renderables: " + renderables.size());
        System.out.println(prefix + "    Nb children: " + children.size());
        System.out.println(prefix + "    Transforms: " + transforms.toString());

        printRenderablesCompleteDescription(prefix + "        ", transforms);
    }


    private void printRenderablesCompleteDescription(String prefix, Transforms parentTransforms) {
        for (Transforms child : parentTransforms.getChildren()) {
            Transformable transformable = child.getTransformable();

            if (transformable != null) {
                try {
                    Renderable renderable = (Renderable) transformable;
                    System.out.println(prefix + child.toString());
                    System.out.println(prefix + "    " + renderable.toString());
                } catch (ClassCastException e) {
                }

                try {
                    PhysicBody body = (PhysicBody) transformable;
                    System.out.println(prefix + child.toString());
                    System.out.println(prefix + "    " + body.toString());
                } catch (ClassCastException e) {
                }

                try {
                    Entity entity = (Entity) transformable;
                    entity.printCompleteDescription(prefix);
                    continue;
                } catch (ClassCastException e) {
                }
            } else {
                System.out.println(prefix + child.toString());
            }

            printRenderablesCompleteDescription(prefix + "    ", child);
        }
    }
}
