package br.com.meu_controle.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.com.meu_controle.MainActivity;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.TipoConta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_LEMBRETE_ID = "lembrete_id";
    public static final String EXTRA_ALERT_INDEX = "alert_index";
    private static final String TAG = "NotificationReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(EXTRA_LEMBRETE_ID, -1L);
        int indiceAlerta = intent.getIntExtra(EXTRA_ALERT_INDEX, 3);
        if (id < 0) {
            Log.w(TAG, "Alarme recebido sem identificador de lembrete.");
            return;
        }

        PendingResult result = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                BancoDados db = BancoDados.obterInstancia(appContext);
                Lembrete lembrete = db.lembreteDao().buscarPorId(id);
                if (lembrete == null || !lembrete.ativo || lembrete.notificado
                        || !new SessaoUsuario(appContext).isNotificationsEnabled()) {
                    return;
                }

                String emailAtual = new SessaoUsuario(appContext).obterEmail();
                if (lembrete.ownerEmail != null && !lembrete.ownerEmail.equals(emailAtual)) {
                    Log.i(TAG, "Notificação ignorada: o lembrete pertence a outra sessão.");
                    return;
                }

                if (lembrete.contaId != null) {
                    Conta conta = db.contaDao().getById(lembrete.contaId);
                    if (conta == null || conta.deleted || TipoConta.CONCLUIDA.equals(conta.status)) {
                        db.lembreteDao().marcarNotificado(lembrete.id);
                        return;
                    }
                }

                Intent abrirApp = new Intent(appContext, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent abrir = PendingIntent.getActivity(appContext,
                        NotificationScheduler.requestCode(lembrete.id, indiceAlerta),
                        abrirApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                NotificationHelper.criarCanal(appContext);
                if (NotificationHelper.mostrar(appContext, lembrete, abrir,
                        NotificationScheduler.requestCode(lembrete.id, indiceAlerta))) {
                    if (indiceAlerta == 3) {
                        db.lembreteDao().marcarNotificado(lembrete.id);
                    }
                } else {
                    Log.w(TAG, "Permissão de notificações ausente; lembrete mantido como pendente.");
                }
            } catch (Exception error) {
                Log.e(TAG, "Falha ao processar o alarme do lembrete " + id, error);
            } finally {
                result.finish();
            }
        });
    }
}
