package com.github.chagall.notificationlistenerexample;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.chagall.notificationlistenerexample.util.Actions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * MIT License
 * <p>
 * Copyright (c) 2016 Fábio Alves Martins Pereira (Chagall)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class NotificationListenerExampleService extends NotificationListenerService {
    private String tag = getClass().getSimpleName();

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */
    private static final class ApplicationPackageNames {
        static final String FACEBOOK_PACK_NAME = "com.facebook.katana";
        static final String FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca";
        static final String WHATSAPP_PACK_NAME = "com.whatsapp";
        static final String INSTAGRAM_PACK_NAME = "com.instagram.android";
        static final String WECHAT_PACK_NAME = "com.tencent.mm";
        static final String ALIPAY_PACK_NAME = "com.eg.android.AlipayGphone";
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    static final class InterceptedNotificationCode {
        static final int TEST_CODE = 0;
        static final int FACEBOOK_CODE = 1;
        static final int WHATSAPP_CODE = 2;
        static final int INSTAGRAM_CODE = 3;
        static final int WECHAT_CODE = 4;
        static final int ALIPAY_CODE = 5;
        static final int OTHER_NOTIFICATIONS_CODE = 6; // We ignore all notification with code
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(tag, "onNotificationPosted: " + sbn.getPackageName());
        handleNotification(sbn.getNotification());

        int notificationCode = matchNotificationCode(sbn);

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {
            Intent intent = new Intent(Actions.ACTION_NOTIFICATION);
            intent.putExtra(Actions.KEY_NOTIFICATION_CODE, notificationCode);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        int notificationCode = matchNotificationCode(sbn);

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            StatusBarNotification[] activeNotifications = this.getActiveNotifications();

            if (activeNotifications != null && activeNotifications.length > 0) {
                for (StatusBarNotification activeNotification : activeNotifications) {
                    if (notificationCode == matchNotificationCode(activeNotification)) {
                        Intent intent = new Intent(Actions.ACTION_NOTIFICATION);
                        intent.putExtra(Actions.KEY_NOTIFICATION_CODE, notificationCode);
                        sendBroadcast(intent);
                        break;
                    }
                }
            }
        }
    }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (ApplicationPackageNames.FACEBOOK_PACK_NAME.equals(packageName)
                || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)) {
            return (InterceptedNotificationCode.FACEBOOK_CODE);
        } else if (ApplicationPackageNames.INSTAGRAM_PACK_NAME.equals(packageName)) {
            return (InterceptedNotificationCode.INSTAGRAM_CODE);
        } else if (ApplicationPackageNames.WHATSAPP_PACK_NAME.equals(packageName)) {
            return (InterceptedNotificationCode.WHATSAPP_CODE);
        } else if (ApplicationPackageNames.WECHAT_PACK_NAME.equals(packageName)) {
            return (InterceptedNotificationCode.WECHAT_CODE);
        } else if (ApplicationPackageNames.ALIPAY_PACK_NAME.equals(packageName)) {
            return (InterceptedNotificationCode.ALIPAY_CODE);
        } else if (getPackageName().equals(packageName)) {
            return (InterceptedNotificationCode.TEST_CODE);
        } else {
            return (InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }

    private void handleNotification(Notification notification) {
        if (notification == null) {
            Log.e(tag, "notification is null");
            return;
        }
        PendingIntent pendingIntent = null;
        // 当 API > 18 时，使用 extras 获取通知的详细信息
        if (Build.VERSION.SDK_INT >= 19) {
            Bundle extras = notification.extras;
            if (extras != null) {
                String title = extras.getString(Notification.EXTRA_TITLE, "");
                String content = extras.getString(Notification.EXTRA_TEXT, "");

                Log.i(tag, "title: " + title + ", content: " + content);
                if (!TextUtils.isEmpty(content) && content.contains("[微信红包]")) {
                    pendingIntent = notification.contentIntent;
                }
            }
        } else {
            // 当 API = 18 时，利用反射获取内容字段
            List<String> textList = getNotificationText(notification);
            if (textList != null && textList.size() > 0) {
                for (String text : textList) {
                    if (!TextUtils.isEmpty(text) && text.contains("[微信红包]")) {
                        pendingIntent = notification.contentIntent;
                        break;
                    }
                }
            }
        }
        // send pendingIntent to open wechat
        try {
            if (pendingIntent != null) {
                pendingIntent.send();
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private List<String> getNotificationText(Notification notification) {
        if (null == notification) {
            return null;
        }
        RemoteViews views = notification.bigContentView;
        if (views == null) {
            views = notification.contentView;
        }
        if (views == null) {
            return null;
        }
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);
            if (actions == null) {
                return null;
            }
            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;
                // View ID
                parcel.readInt();
                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();
                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }
}
