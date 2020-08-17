/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.ros.android.NodeMainExecutorService;
import org.ros.internal.node.client.MasterClient;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.net.URI;

import ch.idiap.android.glrenderer.physics.World;
import ch.idiap.android.ros.internal.NodeMainExecutorServiceConnection;
import ch.idiap.android.ros.listeners.JointsListener;
import ch.idiap.android.ros.listeners.TfListener;
import ch.idiap.android.ros.publishers.DepthPublisher;
import ch.idiap.android.ros.publishers.ImagePublisher;
import ch.idiap.android.urdf.robot.Robot;
import ch.idiap.android.urdf.robot.RobotState;
import ch.idiap.android.ros.playback.Player;
import ch.idiap.android.urdf.UrdfLoader;


public class ROSManager {

    public interface ROSConnectionListener {

        void onConnected();

    }

    private static final String TAG = ROSManager.class.getSimpleName();

    private static NodeMainExecutorServiceConnection nodeMainExecutorServiceConnection = null;

    private static NodeConfiguration nodeConfiguration = null;

    public static final RobotState robotState = new RobotState();

    public static TfListener tfListener = null;

    public static JointsListener jointsListener = null;

    public static ImagePublisher imagePublisher = null;

    public static DepthPublisher depthPublisher = null;

    public static Player player = null;

    public static Context context = null;


    /** Initialise the ROSManager API
     *
     * Call this method each time the OpenGL surface is recreated (might happens
     * several times during the life of an Android application)
     */
    public static void init(Context context) {
        ROSManager.context = context;
    }


    /** Indicates if a ROS node was started
     */
    public static boolean isROSStarted() {
        return (nodeMainExecutorServiceConnection != null);
    }



    public static void startRos(final ROSConnectionListener listener) {

        if (nodeMainExecutorServiceConnection != null)
            return;

        final SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        );

        String masterURI = preferences.getString(
                context.getString(R.string.master_uri_key),
                "http://" + NodeMainExecutorServiceConnection.getDefaultHostAddress() + ":11311"
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("URL of the ROS master");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(masterURI);
        builder.setView(input);

        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startRos(input.getText().toString(), listener);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    /** Start the ROS node
     *
     * Call this method from your overriding of org.ros.android.RosActivity#init(NodeMainExecutor)
     */
    public static void startRos(String masterUri, final ROSConnectionListener listener) {

        if (nodeMainExecutorServiceConnection != null)
            return;

        // Make sure the URI can be parsed
        URI uri;
        try {
            uri = URI.create(masterUri);
        } catch (Exception e) {
            toast("Invalid URI");
            return;
        }

        // Make sure that the master is reachable
        try {
            toast("Trying to reach master...");
            MasterClient masterClient = new MasterClient(uri);
            masterClient.getUri(GraphName.of("AndroidUrdflib"));
            toast("Connected!");
        } catch (Exception e) {
            toast("Failed to establish connection");
            return;
        }

        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(context, uri, listener);

        Intent intent = new Intent(context, NodeMainExecutorService.class);
        intent.setAction(NodeMainExecutorService.ACTION_START);
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, "AndroidUrdflib");
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, "AndroidUrdflib");

        context.startService(intent);

        context.bindService(intent, nodeMainExecutorServiceConnection, Context.BIND_AUTO_CREATE);
    }


    public static void stopRos() {

        if (nodeMainExecutorServiceConnection == null)
            return;

        ROSManager.stopListeners();

        nodeMainExecutorServiceConnection.getMainExecutor().forceShutdown();

        Intent intent = new Intent(context, NodeMainExecutorService.class);
        intent.setAction(NodeMainExecutorService.ACTION_SHUTDOWN);
        context.stopService(intent);

        nodeMainExecutorServiceConnection = null;
        nodeConfiguration = null;
    }


    public static void startListeners() {

        if ((nodeMainExecutorServiceConnection == null) || (tfListener != null))
            return;

        if (nodeConfiguration == null) {
            nodeConfiguration = NodeConfiguration.newPublic(
                    nodeMainExecutorServiceConnection.getDefaultHostAddress());

            nodeConfiguration.setMasterUri(nodeMainExecutorServiceConnection.getMasterUri());
        }

        tfListener = new TfListener(robotState);
        nodeMainExecutorServiceConnection.getMainExecutor().execute(tfListener, nodeConfiguration);

        jointsListener = new JointsListener(robotState);
        nodeMainExecutorServiceConnection.getMainExecutor().execute(jointsListener, nodeConfiguration);
    }


    public static void stopListeners() {

        if ((nodeMainExecutorServiceConnection == null) || (tfListener == null))
            return;

        nodeMainExecutorServiceConnection.getMainExecutor().shutdownNodeMain(tfListener);
        nodeMainExecutorServiceConnection.getMainExecutor().shutdownNodeMain(jointsListener);

        tfListener = null;
        jointsListener = null;
    }


    /** Initialise the playback of the recording of a movement of the Panda Arm robot
     *
     * This simulates a connection to a ROS server.
     *
     * The application must call the ROSManager#startPlayback()
     * method to start the playback, and regularly call the
     * ROSManager#updatePlayback() method for the robot state to be
     * updated.
     */
    public static void createPandaArmPlayback() {
        // Must have been initialised
        if (context == null) {
            Log.e(TAG, "Must call init() with a valid context first");
            return;
        }

        if (isROSStarted()) {
            Log.e(TAG, "Can't use a ROS node and a playback player at the same time");
            return;
        }

        // Create the player
        try {
            player = Player.load(context, "panda_arm_hand");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load the recording files", e);
        }
    }


    /** Start the playback of the recording (if any)
     */
    public static void startPlayback() {
        if (player == null)
            return;

        player.start(robotState);
    }


    /** Update the playback of the recording (if any)
     */
    public static void updatePlayback() {
        if (player == null)
            return;

        player.update(robotState);
    }


    /** Stop the playback of the recording (if any)
     */
    public static void stopPlayback() {
        if (player == null)
            return;

        player.stop();
    }


    /** Enable the publication of the RGB image captured by the camera
     *
     * Call this method after ROSManager#startRos(NodeMainExecutor, String, URI)
     */
    public static void enableRGBImagePublishing(NodeMainExecutor nodeMainExecutor) {

        if (imagePublisher != null)
            return;

        imagePublisher = new ImagePublisher();
        nodeMainExecutor.execute(imagePublisher, nodeConfiguration);
    }


    /** Enable the publication of the depth image captured by the camera
     *
     * Call this method after ROSManager#startRos(NodeMainExecutor, String, URI)
     */
    public static void enableDepthImagePublishing(NodeMainExecutor nodeMainExecutor) {

        if (depthPublisher != null)
            return;

        depthPublisher = new DepthPublisher();
        nodeMainExecutor.execute(depthPublisher, nodeConfiguration);
    }


    /** Helper method to load the assets needed for the Panda Arm robot
     */
    public static boolean loadPandaArmAssets() {
        // Must have been initialised
        if (context == null) {
            Log.e(TAG, "Must call init() with a valid context first");
            return false;
        }

        // Create the robot model
        if (!UrdfLoader.loadMeshes(context, "panda/panda_arm_hand.urdf")) {
            Log.e(TAG, "Failed to load the URDF file");
            return false;
        }

        return true;
    }


    /** Helper method to create a Robot for the Panda Arm robot
     */
    public static Robot createPandaArm(World world) {
        // Must have been initialised
        if (context == null) {
            Log.e(TAG, "Must call init() with a valid context first");
            return null;
       }

        // Create the robot model
        Robot robot = UrdfLoader.load(context, "panda/panda_arm_hand.urdf", world);
        if (robot == null) {
            Log.e(TAG, "Failed to load the URDF file");
            return null;
        }

        robot.setKinematicChain("panda_link0", "panda_hand");

        robot.setRobotState(robotState);

        return robot;
    }


    private static void toast(final String text) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
