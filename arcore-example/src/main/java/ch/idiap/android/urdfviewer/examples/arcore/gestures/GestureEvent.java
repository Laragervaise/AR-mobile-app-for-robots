/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.arcore.gestures;

import android.view.MotionEvent;


public class GestureEvent {

    // Define gesture event type
    public enum GestureType {
        DOWN,
        UP,
        SINGLETAPUP,
        SCROLL,
        FLING,
        PINCH
    }

    // Attributes
    private GestureType type;
    private MotionEvent event1;
    private MotionEvent event2;
    private float distanceX;
    private float distanceY;
    private float velocityX;
    private float velocityY;


    private GestureEvent() {
    }

    public GestureType getType() {
        return type;
    }

    public MotionEvent getEvent1() {
        return event1;
    }

    public MotionEvent getEvent2() {
        return event2;
    }

    public float getX() {
        return event1.getX();
    }

    public float getY() {
        return event1.getY();
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
        ret.type = GestureType.DOWN;
        ret.event1 = e;
        return ret;
    }

    public static GestureEvent createUpEvent(MotionEvent e) {
        GestureEvent ret = new GestureEvent();
        ret.type = GestureType.UP;
        ret.event1 = e;
        return ret;
    }

    public static GestureEvent createSingleTapUpEvent(MotionEvent e) {
        GestureEvent ret = new GestureEvent();
        ret.type = GestureType.SINGLETAPUP;
        ret.event1 = e;
        return ret;
    }

    public static GestureEvent createScrollEvent(MotionEvent e1, MotionEvent e2, float x, float y) {
        GestureEvent ret = new GestureEvent();
        ret.type = GestureType.SCROLL;
        ret.event1 = e1;
        ret.event2 = e2;
        ret.distanceX = x;
        ret.distanceY = y;
        return ret;
    }

    public static GestureEvent createFlingEvent(MotionEvent e1, MotionEvent e2, float x, float y) {
        GestureEvent ret = new GestureEvent();
        ret.type = GestureType.FLING;
        ret.event1 = e1;
        ret.event2 = e2;
        ret.velocityX = x;
        ret.velocityY = y;
        return ret;
    }

    public static GestureEvent createPinchEvent(MotionEvent e1, MotionEvent e2, float x, float y) {
        GestureEvent ret = new GestureEvent();
        ret.type = GestureType.PINCH;
        ret.event1 = e1;
        ret.event2 = e2;
        ret.velocityX = x;
        ret.velocityY = y;
        return ret;
    }
}
