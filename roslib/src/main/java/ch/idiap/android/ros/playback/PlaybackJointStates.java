/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.playback;

import org.ros.message.Time;


public class PlaybackJointStates {

    public Time timestamp = new Time();
    public String[] names;
    public float[] positions;
    public float[] velocities;
    public float[] efforts;

}
