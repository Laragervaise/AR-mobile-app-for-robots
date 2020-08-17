/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.listeners;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.util.List;

import ch.idiap.android.urdf.robot.RobotState;


public class JointsListener extends AbstractNodeMain {

    private RobotState robotState = null;


    public JointsListener(RobotState robotState) {
        this.robotState = robotState;
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("AndroidUrdflib/joints_listener");
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {

        MessageListener<sensor_msgs.JointState> listener = new MessageListener<sensor_msgs.JointState>() {
            @Override
            public void onNewMessage(sensor_msgs.JointState message) {
                if (robotState == null)
                    return;

                List<String> names = message.getName();
                double[] positions = message.getPosition();
                double[] velocities = message.getVelocity();

                for (int i = 0; i < names.size(); ++i) {
                    float position = 0.0f;
                    float velocity = 0.0f;

                    if (positions.length > i)
                        position = (float) positions[i];

                    if (velocities.length > i)
                        velocity = (float) velocities[i];

                    robotState.putJointState(names.get(i), position, velocity);
                }
            }
        };


        Subscriber<sensor_msgs.JointState> subscriber = connectedNode.newSubscriber("/joint_states","sensor_msgs/JointState");
        subscriber.addMessageListener(listener);
    }

}
