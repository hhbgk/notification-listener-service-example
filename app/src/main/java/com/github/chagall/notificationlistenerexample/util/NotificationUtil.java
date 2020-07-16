package com.github.chagall.notificationlistenerexample.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.github.chagall.notificationlistenerexample.MainActivity;
import com.github.chagall.notificationlistenerexample.R;

import java.util.Random;

/**
 * Des:
 * Author: Bob
 * Date:20-7-16
 * UpdateRemark:
 */
public final class NotificationUtil {

    private static int notificationId = 100;
    public static void createNotification(Context context) {
        context = context.getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = null;
        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            final String CHANNEL_ID = "org.notification.test";
            channel = new NotificationChannel(CHANNEL_ID, "Test", NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        String money = new Random().nextInt(100) + "$ ";
        Notification notification = builder
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(money + context.getString(R.string.payment_from))
                .setContentTitle(context.getString(R.string.receive_payment))
                .build();
//        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId++  , notification);
        }
    }
}
