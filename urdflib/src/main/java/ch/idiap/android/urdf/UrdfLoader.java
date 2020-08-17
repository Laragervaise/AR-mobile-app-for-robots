/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdf;

import android.content.Context;
import android.util.Log;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.extras.gimpact.GImpactMeshShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3f;

import ch.idiap.android.glrenderer.entities.Renderable;
import ch.idiap.android.glrenderer.helpers.AssetsUtils;
import ch.idiap.android.glrenderer.helpers.InvalidXMLException;
import ch.idiap.android.glrenderer.materials.Material;
import ch.idiap.android.glrenderer.materials.MaterialManager;
import ch.idiap.android.glrenderer.meshes.Mesh;
import ch.idiap.android.glrenderer.meshes.MeshBuilder;
import ch.idiap.android.glrenderer.meshes.MeshManager;
import ch.idiap.android.glrenderer.meshes.loaders.Loader;
import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.glrenderer.physics.helpers.SubMeshWrapper;
import ch.idiap.android.urdf.parser.Component;
import ch.idiap.android.urdf.parser.UrdfJoint;
import ch.idiap.android.urdf.parser.UrdfLink;
import ch.idiap.android.urdf.parser.UrdfReader;
import ch.idiap.android.urdf.robot.Robot;


public class UrdfLoader {
    private static final String TAG = UrdfLoader.class.getSimpleName();
    private static final Map<String, List<String>> loadedMeshFiles = new HashMap<>();


    public static Robot load(Context context, String assetName, World world) {
        Robot robot = new Robot(world);

        // Load the URDF file content
        String urdf_xml = AssetsUtils.loadAssetAsString(context, assetName);

        // Parse the URDF file content
        List<UrdfLink> links = new ArrayList<>();
        List<UrdfJoint> joints = new ArrayList<>();

        try {
            UrdfReader reader = new UrdfReader();
            reader.readUrdf(urdf_xml);
            links = Collections.synchronizedList(reader.getLinks());
            joints = Collections.synchronizedList(reader.getJoints());
        } catch (InvalidXMLException e) {
            Log.e(TAG, "Invalid XML", e);
            return null;
        }

        // Process each links
        int lastIndex = assetName.lastIndexOf(".");
        String prefix = assetName.substring(0, lastIndex);

        // Load the mesh files if necessary
        loadMeshes(context, links, prefix);

        for (UrdfLink ul : links) {
            List<Renderable> renderables = new ArrayList<>();
            CollisionShape shape = null;

            // Visual component
            Component c = ul.getVisual();
            if (c != null) {
                if (c.getType() == Component.GEOMETRY.MESH) {
                    String meshFileName = c.getMesh();

                    // Create one renderable by mesh
                    for (String meshName : loadedMeshFiles.get(meshFileName)) {
                        Mesh mesh = MeshManager.get(meshName);
                        Material material = MaterialManager.get(mesh.getDefaultMaterialName());

                        Renderable renderable = new Renderable(mesh, material);
                        renderables.add(renderable);
                    }

                } else {
                    String meshName = prefix + "/" + ul.getName() + "/visual";

                    Mesh mesh = MeshManager.get(loadedMeshFiles.get(meshName).get(0));

                    Material material = new Material();
                    material.diffuse.set(c.getMaterialColor());

                    Renderable renderable = new Renderable(mesh, material);
                    renderables.add(renderable);
                }
            }

            // Collision component
            c = ul.getCollision();
            if ((world != null) && (c != null)) {
                if (c.getType() == Component.GEOMETRY.MESH) {
                    String meshFileName = c.getMesh();

                    String meshName = loadedMeshFiles.get(meshFileName).get(0);

                    Mesh mesh = MeshManager.get(meshName);

                    SubMeshWrapper wrapper = new SubMeshWrapper(mesh.getSubMeshes().get(0));
                    shape = new GImpactMeshShape(wrapper);
                    ((GImpactMeshShape) shape).updateBound();
                } else {
                    if (c.getType() == Component.GEOMETRY.CYLINDER) {
                        shape = new CylinderShape(
                                new Vector3f(c.getRadius(), c.getLength() / 2.0f, c.getRadius())
                        );
                    } else if (c.getType() == Component.GEOMETRY.BOX) {
                        float[] size = c.getSize();
                        shape = new BoxShape(
                                new Vector3f(size[0] / 2.0f, size[1] / 2.0f, size[2] / 2.0f)
                        );
                    } else if (c.getType() == Component.GEOMETRY.SPHERE) {
                        shape = new SphereShape(c.getRadius());
                    }
                }

                if (shape != null)
                    shape.setMargin(0.001f);
            }

            if (!renderables.isEmpty() || (shape != null))
                robot.addLink(ul, renderables, shape);
        }

        // Process each joints
        for (UrdfJoint joint : joints) {
            robot.addJoint(joint);
        }

        return robot;
    }


    public static boolean loadMeshes(Context context, String assetName) {
        // Load the URDF file content
        String urdf_xml = AssetsUtils.loadAssetAsString(context, assetName);

        // Parse the URDF file content
        List<UrdfLink> links = new ArrayList<>();

        try {
            UrdfReader reader = new UrdfReader();
            reader.readUrdf(urdf_xml);
            links = Collections.synchronizedList(reader.getLinks());
        } catch (InvalidXMLException e) {
            Log.e(TAG, "Invalid XML", e);
            return false;
        }

        // Process each links
        int lastIndex = assetName.lastIndexOf(".");
        String prefix = assetName.substring(0, lastIndex);

        loadMeshes(context, links, prefix);

        return true;
    }


    private static void loadMeshes(Context context, List<UrdfLink> links, String prefix) {
        for (UrdfLink ul : links) {

            // Visual component
            Component c = ul.getVisual();
            if (c != null) {
                if (c.getType() == Component.GEOMETRY.MESH) {
                    String meshFileName = c.getMesh();

                    // Load the mesh file if necessary
                    if (!loadedMeshFiles.containsKey(meshFileName)) {
                        Log.v(TAG, "Loading mesh: " + meshFileName);
                        loadedMeshFiles.put(meshFileName,
                                AssetsUtils.load3DAsset(context, meshFileName,
                                        Loader.CoordinatesSystemTransformation.X__Z__MINUS_Y)
                        );
                    }

                } else {
                    String meshName = prefix + "/" + ul.getName() + "/visual";
                    Log.v(TAG, "Creating mesh: " + meshName);

                    if (c.getType() == Component.GEOMETRY.CYLINDER) {
                        MeshBuilder.buildCylinder(meshName, c.getRadius(), c.getLength());
                    } else if (c.getType() == Component.GEOMETRY.BOX) {
                        float[] size = c.getSize();
                        MeshBuilder.buildCube(meshName, size[0], size[1], size[2]);
                    } else if (c.getType() == Component.GEOMETRY.SPHERE) {
                        MeshBuilder.buildSphere(meshName, c.getRadius());
                    }

                    ArrayList<String> names = new ArrayList<>();
                    names.add(meshName);

                    loadedMeshFiles.put(meshName, names);
                }
            }

            // Collision component
            c = ul.getCollision();
            if ((c != null) && (c.getType() == Component.GEOMETRY.MESH)) {
                String meshFileName = c.getMesh();

                // Load the mesh file if necessary
                if (!loadedMeshFiles.containsKey(meshFileName)) {
                    Log.v(TAG, "Loading mesh: " + meshFileName);
                    loadedMeshFiles.put(meshFileName,
                            AssetsUtils.load3DAsset(context, meshFileName,
                                    Loader.CoordinatesSystemTransformation.X__Z__MINUS_Y)
                    );
                }
            }
        }
    }
}
