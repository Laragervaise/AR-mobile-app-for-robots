/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.playback;


import android.content.Context;

import org.ros.message.Time;

import java.io.IOException;
import java.util.List;

import ch.idiap.android.urdf.robot.RobotState;


public class Player {

    private List< List<PlaybackTransform> > transforms_static = null;
    private List< List<PlaybackTransform> > transforms = null;
    private List<PlaybackJointStates> jointStates = null;
    private int currentIndex = -1;
    private long lastTime;
    private long timeCounter;
    private long delayToWait;


    public static Player load(Context context, String modelName) throws IOException {
        Player player = new Player();

        player.transforms_static = Parser.parseTransforms(context, "recordings/" + modelName + "/tf_static.txt");
        player.transforms = Parser.parseTransforms(context, "recordings/" + modelName + "/tf.txt");
        player.jointStates = Parser.parseJointStates(context, "recordings/" + modelName + "/joint_states.txt");

        return player;
    }


    public void start(RobotState robotState) {
        for (PlaybackTransform t: transforms_static.get(0))
            robotState.putTransforms(t.linkName, t.transforms.position, t.transforms.orientation);

        currentIndex = 0;
        timeCounter = 0;
        lastTime = System.nanoTime();
        delayToWait = -1;

        for (PlaybackTransform t: transforms.get(currentIndex))
            robotState.putTransforms(t.linkName, t.transforms.position, t.transforms.orientation);

        PlaybackJointStates jointStates = this.jointStates.get(currentIndex);
        for (int i = 0; i < jointStates.names.length; ++i) {
            float position = 0.0f;
            float velocity = 0.0f;

            if (jointStates.positions.length > i)
                position = (float) jointStates.positions[i];

            if (jointStates.velocities.length > i)
                velocity = (float) jointStates.velocities[i];

            robotState.putJointState(jointStates.names[i], position, velocity);
        }

    }


    public void stop() {
        currentIndex = -1;
    }


    public void update(RobotState robotState) {
        if (currentIndex == -1)
            return;

        int nextIndex = currentIndex + 1;
        if (nextIndex >= transforms.size()) {
            start(robotState);
            return;
        }

        long currentTime = System.nanoTime();
        timeCounter += currentTime - lastTime;
        lastTime = currentTime;

        if (delayToWait == -1) {
            Time currentTimestamp = transforms.get(currentIndex).get(0).timestamp;
            Time nextTimestamp = transforms.get(nextIndex).get(0).timestamp;

            delayToWait = nextTimestamp.totalNsecs() - currentTimestamp.totalNsecs();
        }

        if (timeCounter >= delayToWait) {
            currentIndex = nextIndex;
            timeCounter -= delayToWait;
            delayToWait = -1;

            for (PlaybackTransform t: transforms.get(currentIndex))
                robotState.putTransforms(t.linkName, t.transforms.position, t.transforms.orientation);

            PlaybackJointStates jointStates = this.jointStates.get(currentIndex);
            for (int i = 0; i < jointStates.names.length; ++i) {
                float position = 0.0f;
                float velocity = 0.0f;

                if (jointStates.positions.length > i)
                    position = (float) jointStates.positions[i];

                if (jointStates.velocities.length > i)
                    velocity = (float) jointStates.velocities[i];

                robotState.putJointState(jointStates.names[i], position, velocity);
            }
        }
    }
}
