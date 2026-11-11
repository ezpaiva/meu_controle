package br.com.meu_controle.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Lembrete;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    private static final String ACTION_REMINDER =
            "br.com.meu_controle.action.SHOW_FINANCIAL_REMINDER";
    private static final int ALERT_COUNT = 4;
    private final Context context;
    private final AlarmManager alarmManager;

    public NotificationScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleNotification(Lembrete lembrete) {
        if (!lembrete.ativo || lembrete.notificado
                || !new SessaoUsuario(context).isNotificationsEnabled()) {
            return;
        }
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager indisponível; não foi possível agendar o lembrete " + lembrete.id);
            return;
        }
        LocalDate vencimento = LocalDate.parse(lembrete.data);
        LocalTime hora = LocalTime.parse(lembrete.hora);
        List<LocalDate> datasAviso = calcularDatasAviso(vencimento);
        long agora = System.currentTimeMillis();
        for (int indice = 0; indice < datasAviso.size(); indice++) {
            LocalDate dataAviso = datasAviso.get(indice);
            long quando = calcularHorarioAviso(dataAviso, hora, agora, ZoneId.systemDefault());
            if (quando <= agora) {
                continue;
            }
            PendingIntent pendingIntent = obterPendingIntent(lembrete.id, indice,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pendingIntent);
            } catch (SecurityException error) {
                Log.e(TAG, "Permissão insuficiente para agendar o lembrete " + lembrete.id, error);
            }
        }
    }

    public void cancelNotification(long lembreteId) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager indisponível; não foi possível cancelar o lembrete " + lembreteId);
            return;
        }
        for (int indice = 0; indice < ALERT_COUNT; indice++) {
            PendingIntent pendingIntent = obterPendingIntent(lembreteId, indice,
                    PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
            NotificationManagerCompat.from(context).cancel(requestCode(lembreteId, indice));
        }
        NotificationManagerCompat.from(context).cancel((int) lembreteId);
        Intent legado = new Intent(context, NotificationReceiver.class)
                .setAction(ACTION_REMINDER)
                .putExtra(NotificationReceiver.EXTRA_LEMBRETE_ID, lembreteId);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent alarmeLegado = PendingIntent.getBroadcast(
                context, requestCode(lembreteId), legado, flags);
        if (alarmeLegado != null) {
            alarmManager.cancel(alarmeLegado);
            alarmeLegado.cancel();
        }
    }

    public void rescheduleAll(String ownerEmail) {
        if (!new SessaoUsuario(context).isNotificationsEnabled()) {
            return;
        }
        List<Lembrete> pendentes = BancoDados.obterInstancia(context)
                .lembreteDao().listarAtivosPendentes(ownerEmail);
        for (Lembrete lembrete : pendentes) {
            cancelNotification(lembrete.id);
            scheduleNotification(lembrete);
        }
    }

    public void cancelAll(String ownerEmail) {
        List<Lembrete> lembretes = BancoDados.obterInstancia(context)
                .lembreteDao().listarTodos(ownerEmail);
        for (Lembrete lembrete : lembretes) {
            cancelNotification(lembrete.id);
        }
    }

    public void cancelForAccount(long contaId) {
        List<Lembrete> lembretes = BancoDados.obterInstancia(context)
                .lembreteDao().buscarPorConta(contaId);
        for (Lembrete lembrete : lembretes) {
            cancelNotification(lembrete.id);
        }
    }

    public static List<LocalDate> calcularDatasAviso(LocalDate vencimento) {
        List<LocalDate> datas = new ArrayList<>();
        for (int dias : Arrays.asList(7, 3, 1, 0)) {
            datas.add(vencimento.minusDays(dias));
        }
        return datas;
    }

    public static long calcularHorarioAviso(LocalDate data, LocalTime hora,
                                            long agoraMillis, ZoneId zoneId) {
        LocalDateTime horario = LocalDateTime.of(data, hora);
        long instante = horario.atZone(zoneId).toInstant().toEpochMilli();
        if (data.equals(LocalDate.now(zoneId))
                && instante <= agoraMillis) {
            return agoraMillis + 60_000L;
        }
        return instante;
    }

    private PendingIntent obterPendingIntent(long lembreteId, int indice, int flags) {
        Intent intent = new Intent(context, NotificationReceiver.class)
                .setAction(ACTION_REMINDER)
                .setData(Uri.parse("meucontrole://lembrete/" + lembreteId + "/" + indice))
                .putExtra(NotificationReceiver.EXTRA_LEMBRETE_ID, lembreteId)
                .putExtra(NotificationReceiver.EXTRA_ALERT_INDEX, indice);
        int pendingFlags = flags;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode(lembreteId, indice),
                intent, pendingFlags);
    }

    public static int requestCode(long lembreteId) {
        return (int) (lembreteId ^ (lembreteId >>> 32));
    }

    public static int requestCode(long lembreteId, int indice) {
        return 31 * requestCode(lembreteId) + indice;
    }
}
