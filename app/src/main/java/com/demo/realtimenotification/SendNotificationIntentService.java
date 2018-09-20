package com.demo.realtimenotification;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by Sagar Bhagwat 8/24/18.
 */

public class SendNotificationIntentService extends IntentService {

    private static final String TAG = "NotificationService";

    public SendNotificationIntentService() {
        super("SendNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {

            Log.e(TAG, "Goefencing Error " + geofencingEvent.getErrorCode());

            return;

        }

        // Get the transition type.
        int mGeofenceTransition = geofencingEvent.getGeofenceTransition();

        List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();

        Geofence geofence = geofences.get(0);

        if (mGeofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER && geofence.getRequestId().equals(Constants.GEOFENCE_ID)) {

            showNotification();

            // app is in foreground, broadcast the push message
            Intent pushNotification = new Intent(Constants.PUSH_NOTIFICATION);

            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

        } else

            Log.e(TAG, "Error ");

    }

    @SuppressLint("InlinedApi")
    public void showNotification() {

        Intent intent = new Intent(this, RealTimeNotificationActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "ID");

        notificationBuilder.setAutoCancel(true)

                .setDefaults(Notification.DEFAULT_ALL)

                .setSmallIcon(R.mipmap.ic_launcher)

                .setContentIntent(pendingNotificationIntent)

                .setPriority(Notification.PRIORITY_MAX)

                .setContentTitle(getApplicationContext().getString(R.string.app_name))

                .setContentText(getApplicationContext().getString(R.string.notification_text))

                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {

            notificationManager.notify(1, notificationBuilder.build());

        }

    }
}
