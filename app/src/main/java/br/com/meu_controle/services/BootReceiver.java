package br.com.meu_controle.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.com.meu_controle.dados.local.SessaoUsuario;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        android.content.BroadcastReceiver.PendingResult result = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                String email = new SessaoUsuario(appContext).obterEmail();
                if (email != null && !email.trim().isEmpty()) {
                    new NotificationScheduler(appContext).rescheduleAll(email);
                }
            } catch (Exception error) {
                Log.e(TAG, "Falha ao restaurar os alarmes após reinicialização.", error);
            } finally {
                result.finish();
            }
        });
    }
}
