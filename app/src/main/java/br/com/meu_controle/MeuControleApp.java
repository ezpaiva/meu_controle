package br.com.meu_controle;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.services.NotificationHelper;
import br.com.meu_controle.services.NotificationScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeuControleApp extends Application {

    private static Context contexto;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        contexto = getApplicationContext();
        NotificationHelper.criarCanal(contexto);
        EXECUTOR.execute(() -> {
            try {
                BancoDados bancoDados = BancoDados.obterInstancia(contexto);
                RepositorioLembretes repositorioLembretes = new RepositorioLembretes(contexto);
                android.content.SharedPreferences preferencias =
                        getSharedPreferences("lembretes", MODE_PRIVATE);
                if (!preferencias.getBoolean("contas_v7_processadas", false)) {
                    for (Conta conta : bancoDados.contaDao().getAll()) {
                        repositorioLembretes.reconciliarConta(conta);
                    }
                    preferencias.edit().putBoolean("contas_v7_processadas", true).apply();
                }
                String email = new SessaoUsuario(contexto).obterEmail();
                if (email != null && !email.trim().isEmpty()) {
                    new NotificationScheduler(contexto).rescheduleAll(email);
                }
            } catch (Exception error) {
                Log.e("MeuControleApp", "Falha ao restaurar lembretes agendados.", error);
            }
        });
    }

    public static Context obterContexto() {
        return contexto;
    }
}