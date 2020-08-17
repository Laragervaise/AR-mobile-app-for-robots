/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.playback;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import ch.idiap.android.urdf.robot.RobotState;


public class Parser {

    static public List< List<PlaybackTransform> > parseTransforms(Context context, String filename) throws IOException {
        InputStream inputStream = context.getAssets().open(filename);

        String line;
        boolean isParsingTranslation = true;

        PlaybackTransform transform = null;
        List<PlaybackTransform> entry = null;

        List< List<PlaybackTransform> > result = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                if (line.equals("transforms:")) {
                    entry = new ArrayList<>();

                } else if (line.equals("---")) {
                    if (entry != null) {
                        if (transform != null) {
                            entry.add(transform);
                            transform = null;
                        }

                        result.add(entry);
                    }

                    entry = null;

                } else if (line.equals("-")) {
                    if (transform != null)
                        entry.add(transform);

                    transform = new PlaybackTransform();

                } else if (line.startsWith("secs:")) {
                    int v = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                    transform.timestamp.secs = v;

                } else if (line.startsWith("nsecs:")) {
                    int v = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                    transform.timestamp.nsecs = v;

                } else if (line.startsWith("child_frame_id:")) {
                    transform.linkName = line.substring(line.indexOf('"') + 1, line.length() - 1).trim();

                } else if (line.startsWith("translation:")) {
                    isParsingTranslation = true;

                } else if (line.startsWith("rotation:")) {
                    isParsingTranslation = false;

                } else if (line.startsWith("x:")) {
                    double v = Double.parseDouble(line.substring(line.indexOf(" ") + 1).trim());

                    if (isParsingTranslation)
                        transform.transforms.position.x = (float) v;
                    else
                        transform.transforms.orientation.x = (float) v;

                } else if (line.startsWith("y:")) {
                    double v = Double.parseDouble(line.substring(line.indexOf(" ") + 1).trim());

                    if (isParsingTranslation)
                        transform.transforms.position.y = (float) v;
                    else
                        transform.transforms.orientation.y = (float) v;

                } else if (line.startsWith("z:")) {
                    double v = Double.parseDouble(line.substring(line.indexOf(" ") + 1).trim());

                    if (isParsingTranslation)
                        transform.transforms.position.z = (float) v;
                    else
                        transform.transforms.orientation.z = (float) v;

                } else if (line.startsWith("w:")) {
                    double v = Double.parseDouble(line.substring(line.indexOf(" ") + 1).trim());

                    if (!isParsingTranslation)
                        transform.transforms.orientation.w = (float) v;
                }
            }
        }

        inputStream.close();

        return result;
    }


    static public List<PlaybackJointStates> parseJointStates(Context context, String filename) throws IOException {
        InputStream inputStream = context.getAssets().open(filename);

        String line;
        String names = null;

        PlaybackJointStates jointStates = null;
        List<PlaybackJointStates> result = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                if (names != null) {
                    names += line;

                    if (names.contains("]")) {
                        jointStates.names = names.replace("[", "").replace("]", "").replace(" ", "").split(",");
                        names = null;
                    }

                } else if (line.equals("header:")) {
                    jointStates = new PlaybackJointStates();

                } else if (line.equals("---")) {
                    if (jointStates != null)
                        result.add(jointStates);

                    jointStates = null;

                } else if (line.startsWith("secs:")) {
                    int v = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                    jointStates.timestamp.secs = v;

                } else if (line.startsWith("nsecs:")) {
                    int v = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                    jointStates.timestamp.nsecs = v;

                } else if (line.startsWith("name:")) {
                    names = line.substring(line.indexOf(" ") + 1).trim();

                    if (names.contains("]")) {
                        jointStates.names = names.replace("[", "").replace("]", "").replace(" ", "").split(",");
                        names = null;
                    }

                } else if (line.startsWith("position:")) {
                    line = line.substring(line.indexOf(" ") + 1).trim();
                    String[] values = line.replace("[", "").replace("]", "").replace(" ", "").split(",");

                    jointStates.positions = new float[values.length];

                    for (int i = 0; i < values.length; ++i)
                        jointStates.positions[i] = Float.parseFloat(values[i]);

                } else if (line.startsWith("velocity:")) {
                    line = line.substring(line.indexOf(" ") + 1).trim();
                    String[] values = line.replace("[", "").replace("]", "").replace(" ", "").split(",");

                    jointStates.velocities = new float[values.length];

                    for (int i = 0; i < values.length; ++i)
                        jointStates.velocities[i] = Float.parseFloat(values[i]);

                } else if (line.startsWith("effort:")) {
                    line = line.substring(line.indexOf(" ") + 1).trim();
                    String[] values = line.replace("[", "").replace("]", "").replace(" ", "").split(",");

                    jointStates.efforts = new float[values.length];

                    for (int i = 0; i < values.length; ++i)
                        jointStates.efforts[i] = Float.parseFloat(values[i]);
                }
            }
        }

        inputStream.close();

        return result;
    }
}
