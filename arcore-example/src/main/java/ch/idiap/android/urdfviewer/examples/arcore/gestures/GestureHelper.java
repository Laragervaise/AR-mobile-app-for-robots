/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.urdfviewer.examples.arcore.gestures;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Helper to detect taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
public final class GestureHelper implements OnTouchListener {

    private final GestureDetector gestureDetector;
    private final BlockingQueue<GestureEvent> queuedEvents = new ArrayBlockingQueue<>(16);


    /**
    * Creates the gesture helper.
    *
    * @param context the application's context.
    */
    public GestureHelper(Context context) {
        gestureDetector =
            new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        queuedEvents.offer(GestureEvent.createSingleTapUpEvent(e));
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                            float distanceX, float distanceY) {

                        if (e2.getPointerCount() == 2) {
                            queuedEvents.offer(GestureEvent.createPinchEvent(e1, e2, distanceX, distanceY));
                        } else {
                            queuedEvents.offer(GestureEvent.createScrollEvent(e1, e2, -distanceX, distanceY));
                        }

                        return true;
                    }
                }
            );
    }


    /**
    * Polls for a gesture.
    */
    public GestureEvent poll() {
        return queuedEvents.poll();
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (gestureDetector.onTouchEvent(motionEvent))
            return true;

        if (motionEvent.getAction() == MotionEvent.ACTION_UP)
            queuedEvents.offer(GestureEvent.createUpEvent(motionEvent));

        return false;
    }
}
