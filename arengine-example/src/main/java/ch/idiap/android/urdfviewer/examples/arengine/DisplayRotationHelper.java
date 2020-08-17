/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2017 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arengine;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.view.Display;
import android.view.WindowManager;

import com.huawei.hiar.ARSession;

public class DisplayRotationHelper implements DisplayListener{

    private boolean mViewportChanged;
    private int mViewportWidth;
    private int mViewportHeight;
    private final Context mContext;
    private final Display mDisplay;


    public DisplayRotationHelper(Context context) {
        this.mContext = context;
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();
    }

    public void onResume() {
        mContext.getSystemService(DisplayManager.class).registerDisplayListener(this, null);
    }

    public void onPause() {
        mContext.getSystemService(DisplayManager.class).unregisterDisplayListener(this);
    }

    public void onSurfaceChanged(int width, int height) {
        mViewportWidth = width;
        mViewportHeight = height;
        mViewportChanged = true;
    }

    public void updateSessionIfNeeded(ARSession session) {
        if (mViewportChanged) {
            int displayRotation = mDisplay.getRotation();
            session.setDisplayGeometry(displayRotation, mViewportWidth, mViewportHeight);
            mViewportChanged = false;
        }
    }

    public int getRotation() {
        return mDisplay.getRotation();
    }

    @Override
    public void onDisplayAdded(int displayId) {

    }

    @Override
    public void onDisplayRemoved(int displayId) {

    }

    @Override
    public void onDisplayChanged(int displayId) {
        mViewportChanged = true;
    }
}
