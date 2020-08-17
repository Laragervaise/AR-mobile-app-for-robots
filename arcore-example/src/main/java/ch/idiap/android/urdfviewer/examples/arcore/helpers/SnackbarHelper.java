/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 *
 * Original implementation:
 *   Copyright (C) 2017 Google Inc., licensed under the Apache License, Version 2.0
 */

package ch.idiap.android.urdfviewer.examples.arcore.helpers;

import android.app.Activity;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import android.widget.TextView;


/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
public final class SnackbarHelper {
    private enum DismissBehavior { HIDE, SHOW, FINISH };

    private static final int BACKGROUND_COLOR = 0xbf323232;
    private static Snackbar messageSnackbar;
    private static int maxLines = 2;
    private static String lastMessage = "";

    public static boolean isShowing() {
    return messageSnackbar != null;
    }

    /** Shows a snackbar with a given message. */
    public static void showMessage(Activity activity, String message) {
        if (!message.isEmpty() && (!isShowing() || !lastMessage.equals(message))) {
            lastMessage = message;
            show(activity, message, DismissBehavior.HIDE);
        }
    }

    /** Shows a snackbar with a given message, and a dismiss button. */
    public static void showMessageWithDismiss(Activity activity, String message) {
        show(activity, message, DismissBehavior.SHOW);
    }

    /**
    * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
    * for notifying errors, where no further interaction with the activity is possible.
    */
    public static void showError(Activity activity, String errorMessage) {
        show(activity, errorMessage, DismissBehavior.FINISH);
    }

    /**
    * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
    * call even if snackbar is not shown.
    */
    public static void hide(Activity activity) {
        if (!isShowing()) {
            return;
        }

        lastMessage = "";
        Snackbar messageSnackbarToHide = messageSnackbar;
        messageSnackbar = null;

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        messageSnackbarToHide.dismiss();
                    }
                });
    }

    public static void setMaxLines(int lines) {
    maxLines = lines;
  }

    private static void show(
            final Activity activity, final String message, final DismissBehavior dismissBehavior) {

        activity.runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    messageSnackbar =
                        Snackbar.make(
                            activity.findViewById(android.R.id.content),
                            message,
                            Snackbar.LENGTH_INDEFINITE);

                    messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);

                    if (dismissBehavior != DismissBehavior.HIDE) {
                        messageSnackbar.setAction(
                                "Dismiss",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                      messageSnackbar.dismiss();
                                    }
                            });

                        if (dismissBehavior == DismissBehavior.FINISH) {
                            messageSnackbar.addCallback(
                                new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                  @Override
                                  public void onDismissed(Snackbar transientBottomBar, int event) {
                                    super.onDismissed(transientBottomBar, event);
                                    activity.finish();
                                  }
                                });
                        }
                    }

                    ((TextView) messageSnackbar.getView()
                                .findViewById(com.google.android.material.R.id.snackbar_text))
                            .setMaxLines(maxLines);

                    messageSnackbar.show();
                }
        });
    }
}
