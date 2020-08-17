/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.listeners;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.util.List;

import ch.idiap.android.urdf.robot.RobotState;
import geometry_msgs.Quaternion;
import geometry_msgs.TransformStamped;
import geometry_msgs.Vector3;


public class TfListener extends AbstractNodeMain {

    private RobotState robotState = null;


    public TfListener(RobotState robotState) {
        this.robotState = robotState;
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("AndroidUrdflib/transforms_listener");
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {

        MessageListener<tf2_msgs.TFMessage> listener = new MessageListener<tf2_msgs.TFMessage>() {
            @Override
            public void onNewMessage(tf2_msgs.TFMessage message) {
                if (robotState == null)
                    return;

                final List<TransformStamped> transforms = message.getTransforms();
                Vector3f position = new Vector3f();
                Quaternionf orientation = new Quaternionf();

                for (TransformStamped transform: transforms) {
                    Vector3 pos = transform.getTransform().getTranslation();
                    Quaternion rot = transform.getTransform().getRotation();

                    position.set(pos.getX(), pos.getY(), pos.getZ());
                    orientation.set((float) rot.getX(), (float) rot.getY(), (float) rot.getZ(), (float) rot.getW());

                    robotState.putTransforms(transform.getChildFrameId(), position, orientation);
                }
            }
        };


        Subscriber<tf2_msgs.TFMessage> subscriber = connectedNode.newSubscriber("/tf","tf2_msgs/TFMessage");
        subscriber.addMessageListener(listener);

        Subscriber<tf2_msgs.TFMessage> subscriber2 = connectedNode.newSubscriber("/tf_static","tf2_msgs/TFMessage");
        subscriber2.addMessageListener(listener);
    }

}
