package br.com.meu_controle.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Lembrete;

public final class NotificationHelper {

    public static final String CHANNEL_ID = "FINANCEIRO_LEMBRETES";
    private NotificationHelper() {
    }

    public static void criarCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.reminder_channel_description));
            channel.enableVibration(true);
            channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean mostrar(Context context, Lembrete lembrete,
                                  android.app.PendingIntent intent, int notificationId) {
        if (!new SessaoUsuario(context).isNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(lembrete.titulo)
                .setContentText(lembrete.mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(lembrete.mensagem))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true);
        if (intent != null) {
            notification.setContentIntent(intent);
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification.build());
        return true;
    }
}
