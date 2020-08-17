/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.playback;

import org.ros.message.Time;

import ch.idiap.android.urdf.robot.RobotState.Transforms;


public class PlaybackTransform {

    public String linkName;
    public Time timestamp = new Time();
    public Transforms transforms = new Transforms();

}
