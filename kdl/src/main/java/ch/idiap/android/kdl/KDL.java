/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.kdl;


public class KDL {

    static {
        System.loadLibrary("KDLJni");
    }


    static public native long createRobot(String rootName);

    static public native void releaseRobot(long robot);

    static public native void addLink(
            long robot, String name, String jointName, String parentLinkName, String jointType,
            float origin_position_x, float origin_position_y, float origin_position_z,
            float origin_rotation_x, float origin_rotation_y, float origin_rotation_z, float origin_rotation_w,
            float axis_x, float axis_y, float axis_z);

    static public native boolean setKinematicChain(long robot, String root, String tip);

    static public native String[] getJointNames(long robot);

    static public native void computeJacobian(long robot, float[] positions);

    static public native float[] processJointPosition(long robot, String name, float position);

    static public native float[] forwardKinematics(long robot, float[] positions);

    static public native float[] inverseKinematics(long robot, float[] positions, float[] goalPos, float[] goalOrient);
}
