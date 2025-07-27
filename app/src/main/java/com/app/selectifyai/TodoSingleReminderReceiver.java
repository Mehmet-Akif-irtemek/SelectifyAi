package com.app.selectifyai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TodoSingleReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String todoTitle = intent.getStringExtra("todo_title");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "todo_channel")
                .setSmallIcon(R.drawable.event_to_do_logo)
                .setContentTitle("Görev Hatırlatıcı")
                .setContentText(todoTitle != null ? todoTitle : "Bir görevin zamanı geldi!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
} 