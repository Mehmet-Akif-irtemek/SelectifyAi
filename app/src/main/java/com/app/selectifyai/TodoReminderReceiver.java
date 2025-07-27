
package com.app.selectifyai;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class TodoReminderReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Context'e dil ayarını uygula
        Context localizedContext = createLocalizedContext(context);
        
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("kullanicilar")
                .document(uid)
                .collection("todos")
                .whereEqualTo("tamamlandi", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(localizedContext, "todo_channel")
                                .setSmallIcon(R.drawable.event_to_do_logo)
                                .setContentTitle(localizedContext.getString(R.string.todo_notification_title))
                                .setContentText(localizedContext.getString(R.string.todo_notification_text))
                                .setPriority(NotificationCompat.PRIORITY_HIGH);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(localizedContext);
                        notificationManager.notify(1, builder.build());
                    }
                });
    }
    
    private Context createLocalizedContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ayarlar", Context.MODE_PRIVATE);
        String dilKodu = prefs.getString("dil", "tr");
        
        Locale locale = new Locale(dilKodu);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }
}
