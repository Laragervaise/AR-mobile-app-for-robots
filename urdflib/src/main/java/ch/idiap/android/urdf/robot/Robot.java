/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdf.robot;

import android.util.Log;

import com.bulletphysics.collision.shapes.CollisionShape;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.idiap.android.glrenderer.cameras.BaseCamera;
import ch.idiap.android.glrenderer.entities.Entity;
import ch.idiap.android.glrenderer.entities.Renderable;
import ch.idiap.android.glrenderer.entities.Transforms;
import ch.idiap.android.glrenderer.lights.Light;
import ch.idiap.android.glrenderer.materials.Color;

import ch.idiap.android.glrenderer.physics.PhysicBody;
import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.kdl.KDL;

import ch.idiap.android.urdf.parser.UrdfJoint;
import ch.idiap.android.urdf.parser.UrdfLink;


public class Robot extends Entity {

    private class Link {
        public String parentLink;
        public String parentJoint;
        public Transforms transforms;
        public Transforms visualTransforms;
        public Transforms collisionTransforms;
        public List<Renderable> renderables;
        public PhysicBody kinematicBody;
    }


    private RobotState robotState = null;
    private Map<String, Link> links = new HashMap<>();
    private long kdlRobot = 0;
    private List<String> kdlJointNames = null;
    private World world = null;

    private float robotLength = 20; // random value, to fix or compute

    private static final String TAG = Robot.class.getSimpleName();


    public Robot(World world) {
        this.world = world;
    }


    public Robot(RobotState robotState, World world) {
        this.robotState = robotState;
        this.world = world;
    }


    public World getWorld(){
        return world;
    } //


    public boolean setKinematicChain(String root, String tip) {
        if (KDL.setKinematicChain(kdlRobot, root, tip)) {
            kdlJointNames = Arrays.asList(KDL.getJointNames(kdlRobot));
            return true;
        }

        return false;
    }


    public void setRobotState(RobotState robotState) {
        this.robotState = robotState;
    }


    public RobotState getState() {
        return robotState;
    }


    public void addLink(UrdfLink link, List<Renderable> renderables, CollisionShape shape) {
        Link entry = new Link();
        entry.transforms = new Transforms();
        entry.visualTransforms = new Transforms();
        entry.collisionTransforms = new Transforms();
        entry.renderables = renderables;

        entry.visualTransforms.setParent(entry.transforms);
        entry.collisionTransforms.setParent(entry.transforms);
        entry.transforms.setParent(transforms);

        entry.transforms.tag = link.getName().toString() + "_joint";
        entry.visualTransforms.tag = link.getName().toString() + "_visual";
        entry.collisionTransforms.tag = link.getName().toString() + "_collision";

        Vector3fc pos = link.getVisual().getOrigin().getPosition();
        Quaternionfc rot = link.getVisual().getOrigin().getOrientation();

        entry.visualTransforms.translate(pos.x(), pos.z(), -pos.y(), Transforms.TRANSFORM_SPACE_PARENT);
        entry.visualTransforms.rotate(rot.x(), rot.z(), -rot.y(), rot.w(), Transforms.TRANSFORM_SPACE_PARENT);

        pos = link.getCollision().getOrigin().getPosition();
        rot = link.getCollision().getOrigin().getOrientation();

        entry.collisionTransforms.translate(pos.x(), pos.z(), -pos.y(), Transforms.TRANSFORM_SPACE_PARENT);
        entry.collisionTransforms.rotate(rot.x(), rot.z(), -rot.y(), rot.w(), Transforms.TRANSFORM_SPACE_PARENT);

        for (Renderable renderable : renderables)
            addRenderable(renderable, entry.visualTransforms);

        entry.kinematicBody = new PhysicBody();
        entry.kinematicBody.transforms.setParent(entry.collisionTransforms);

        entry.kinematicBody.setKinematic(world, shape);
        entry.kinematicBody._setEntity(this);

        links.put(link.getName(), entry);
    }


    public void addJoint(UrdfJoint joint) {
        String linkName = joint.getChildLink();
        String parentLinkName = joint.getParentLink();

        Link entry = links.get(linkName);

        if (entry == null) {
            entry = new Link();
            entry.transforms = new Transforms();
            entry.transforms.setParent(transforms);
            entry.transforms.tag = linkName + "_joint";
            links.put(linkName, entry);
        }

        entry.parentLink = parentLinkName;
        entry.parentJoint = joint.getName();

        Link parentLink = links.get(entry.parentLink);
        if (parentLink != null)
            entry.transforms.setParent(parentLink.transforms);

        Vector3fc pos = joint.getOriginPosition();
        Quaternionfc rot = joint.getOriginOrientation();

        entry.transforms.translate(pos.x(), pos.z(), -pos.y(), Transforms.TRANSFORM_SPACE_PARENT);
        entry.transforms.rotate(rot.x(), rot.z(), -rot.y(), rot.w(), Transforms.TRANSFORM_SPACE_PARENT);


        if (kdlRobot == 0)
            kdlRobot = KDL.createRobot(parentLinkName);

        Vector3fc axis = joint.getAxis();

        KDL.addLink(
                kdlRobot, linkName, joint.getName(), parentLinkName, joint.getType(),
                pos.x(), pos.y(), pos.z(),
                rot.x(), rot.y(), rot.z(), rot.w(),
                axis.x(), axis.y(), axis.z()
        );
    }


    @Override
    public void draw(BaseCamera camera, Color ambientLight, Light light) {
        if (robotState != null) {
            for (String linkName : links.keySet()) {
                Link link = links.get(linkName);

                RobotState.Transforms t = robotState.getTransforms(linkName);
                if (t != null) {
                    link.transforms.setPosition(0.0f, 0.0f, 0.0f);
                    link.transforms.setOrientation(0.0f, 0.0f, 0.0f, 1.0f);

                    link.transforms.translate(
                            t.position.x, t.position.z, -t.position.y,
                            Transforms.TRANSFORM_SPACE_PARENT
                    );

                    link.transforms.rotate(
                            t.orientation.x, t.orientation.z, -t.orientation.y, t.orientation.w(),
                            Transforms.TRANSFORM_SPACE_PARENT
                    );


                    Log.d(TAG, linkName + " from transforms getter: "+
                            links.get(linkName).transforms.getPosition());
                }
            }

            Log.d(TAG, " end-effector position: "+ (Vector3fc)forwardKinematics());

            computeJacobian();

        }

        super.draw(camera, ambientLight, light);
    }

    /**
     * Compute the forward kinematics
     * @return the end-effector 3D position
     */
    public Vector3f forwardKinematics() {
        float[] ROSpos = KDL.forwardKinematics(kdlRobot, getPositions());
        //convert from ROS to OpenGL coordinates
        return new Vector3f(ROSpos[0], ROSpos[2], -ROSpos[1]);
    }

    /**
     * Compute the inverse kinematics
     * @param the goal position for the end-effector
     * @param the goal orientation for the end-effector
     * @return an array of all the joint angles in the goal position
     */
    public float[] inverseKinematics(Vector3f goalPos, Quaternionf goalOrient){
        //convert from OpenGL to ROS coordinates
        return KDL.inverseKinematics(kdlRobot, getPositions(), new float[]{goalPos.x, -goalPos.z, goalPos.y},
                new float[]{goalOrient.x, goalOrient.z, goalOrient.y, goalOrient.w});
    }

    /**
     * Compute the inverse kinematics if the goal is reachable for the robot
     * @param the goal position for the end-effector in local coordinates
     * @param the goal orientation for the end-effector
     * @return an array of all the joint angles in the goal position
     */
    public float[] inverseKinematicsWithCheck(Vector3f goalPos, Quaternionf goalOrient, float[] actualPose) {
        if(distance(goalPos, links.get("panda_link0").transforms.getPosition())<=robotLength) {
            return inverseKinematics(goalPos, goalOrient);
        } else {
            Log.e(TAG, "The goal is out of reach!");
            return actualPose;
        }
    }

    public double distance(Vector3fc obj1, Vector3fc obj2) {
        return Math.abs(norm(obj1)-norm(obj2));
    }

    public double norm(Vector3fc obj) {
        return Math.sqrt(Math.pow(obj.x(), 2) + Math.pow(obj.y(), 2) + Math.pow(obj.z(), 2));
    }

    /*public void compareJoints() {
        for (int i = 0; i < kdlJointNames.size(); ++i) {
            Log.d(TAG, " real joint positions: "+ getPositions()[i]);
        }
        for (int i = 0; i < kdlJointNames.size(); ++i) {
            Log.d(TAG, " computed joint positions: "+ inverseKinematics(forwardKinematics(), new Quaternionf())[i]);
        }
    }*/


    public void computeJacobian() {
        float[] positions = getPositions();
        KDL.computeJacobian(kdlRobot, positions);
    }

    public float[] getPositions() {
        float[] positions = new float[kdlJointNames.size()];

        for (int i = 0; i < kdlJointNames.size(); ++i) {
            RobotState.JointState jointState = robotState.getJointState(kdlJointNames.get(i));

            if (jointState != null)
                positions[i] = jointState.position;
            else
                positions[i] = 0.0f;
        }

        return positions;
    }


    public void updateFromJoinStates() {
        Quaternionf q = new Quaternionf();

        for (int i = 0; i < kdlJointNames.size(); ++i) {
            String joint = kdlJointNames.get(i);

            RobotState.JointState jointState = robotState.getJointState(joint);

            float[] orientation = KDL.processJointPosition(
                    kdlRobot, joint, jointState.position
            );

            q.set(orientation[0], orientation[1], orientation[2], orientation[3]);

            for (String linkName : links.keySet()) {
                Link link = links.get(linkName);

                if (joint.equals(link.parentJoint)) {
                    RobotState.Transforms t = robotState.getTransforms(linkName);
                    robotState.putTransforms(linkName, t.position, q);
                    break;
                }
            }
        }
    }
}
