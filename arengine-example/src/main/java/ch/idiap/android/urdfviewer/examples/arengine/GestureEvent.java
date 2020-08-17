/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2018-2019. All rights reserved.
 */

package ch.idiap.android.urdfviewer.examples.arengine;

import android.view.MotionEvent;

public class GestureEvent {

    // define gesture event type, default value is 0;
    public static final int GESTURE_EVENT_TYPE_UNKNOW = 0;
    public static final int GESTURE_EVENT_TYPE_DOWN = 1;
    public static final int GESTURE_EVENT_TYPE_SINGLETAPUP = 2;
    public static final int GESTURE_EVENT_TYPE_SCROLL = 3;
    public static final int GESTURE_EVENT_TYPE_FLING = 4;

    private int type;
    private MotionEvent e1;
    private MotionEvent e2;
    private float distanceX;
    private float distanceY;
    private float velocityX;
    private float velocityY;

    private GestureEvent() {
    }

    public int getType() {
        return type;
    }

    public MotionEvent getE1() {
        return e1;
    }

    public MotionEvent getE2() {
        return e2;
    }

    public float getDistanceX() {
        return distanceX;
    }

    public float getDistanceY() {
        return distanceY;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public static GestureEvent createDownEvent(MotionEvent e) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_DOWN;
        ret.e1 = e;
        return ret;
    }

    public static GestureEvent createSingleTapUpEvent(MotionEvent e) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SINGLETAPUP;
        ret.e1 = e;
        return ret;
    }

    public static GestureEvent createScrollEvent(MotionEvent e1, MotionEvent e2, float x, float y) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SCROLL;
        ret.e1 = e1;
        ret.e2 = e2;
        ret.distanceX = x;
        ret.distanceY = y;
        return ret;
    }

    public static GestureEvent createFlingEvent(MotionEvent e1, MotionEvent e2, float x, float y) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_FLING;
        ret.e1 = e1;
        ret.e2 = e2;
        ret.velocityX = x;
        ret.velocityY = y;
        return ret;
    }
}
