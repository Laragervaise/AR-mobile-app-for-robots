/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;

import java.net.URI;

import ch.idiap.android.ros.R;
import ch.idiap.android.ros.ROSManager;


public class NodeMainExecutorServiceConnection implements ServiceConnection {

    private Context context;
    private URI masterURI;
    private ROSManager.ROSConnectionListener listener;
    private NodeMainExecutorService nodeMainExecutorService = null;


    public NodeMainExecutorServiceConnection(
            Context context, URI masterURI, final ROSManager.ROSConnectionListener listener) {
        super();
        this.context = context;
        this.masterURI = masterURI;
        this.listener = listener;
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) service).getService();

        nodeMainExecutorService.setMasterUri(masterURI);
        nodeMainExecutorService.setRosHostname(getDefaultHostAddress());

        final SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(context.getString(R.string.master_uri_key), masterURI.toString());
        editor.apply();

        if (listener != null)
            listener.onConnected();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
    }


    public NodeMainExecutorService getMainExecutor() {
        return nodeMainExecutorService;
    }


    public URI getMasterUri() {
        return masterURI;
    }


    public static String getDefaultHostAddress() {
        return InetAddressFactory.newNonLoopback().getHostAddress();
    }

}
