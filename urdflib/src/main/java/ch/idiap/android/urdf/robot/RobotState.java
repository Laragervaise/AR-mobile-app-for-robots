/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdf.robot;


import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;


public class RobotState {

    public static class Transforms {
        public Vector3f position = new Vector3f();
        public Quaternionf orientation = new Quaternionf();
    }


    public static class JointState {
        public float position = 0.0f;
        public float velocity = 0.0f;
    }


    private Map<String, Transforms> transforms = new HashMap<>();
    private Map<String, JointState> jointStates = new HashMap<>();


    public void putTransforms(String linkName, Vector3fc position, Quaternionfc orientation) {
        Transforms t = transforms.get(linkName);
        if (t == null) {
            t = new Transforms();
            transforms.put(linkName, t);
        }

        t.position.set(position);
        t.orientation.set(orientation);
    }


    public Transforms getTransforms(String linkName) {
        return transforms.get(linkName);
    }


    public void putJointState(String jointName, float position, float velocity) {
        JointState s = jointStates.get(jointName);
        if (s == null) {
            s = new JointState();
            jointStates.put(jointName, s);
        }

        s.position = position;
        s.velocity = velocity;
    }


    public JointState getJointState(String jointName) {
        return jointStates.get(jointName);
    }

}
