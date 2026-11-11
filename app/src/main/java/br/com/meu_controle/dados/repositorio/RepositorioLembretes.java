package br.com.meu_controle.dados.repositorio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.LembreteDao;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.services.NotificationScheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositorioLembretes {

    private static final String TAG = "RepositorioLembretes";
    private final Context context;
    private final LembreteDao lembreteDao;
    private final NotificationScheduler scheduler;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RepositorioLembretes(Context context) {
        this.context = context.getApplicationContext();
        this.lembreteDao = BancoDados.obterInstancia(this.context).lembreteDao();
        this.scheduler = new NotificationScheduler(this.context);
    }

    public LiveData<List<Lembrete>> observarTodos(String email) {
        return lembreteDao.observarTodos(email);
    }

    public LiveData<List<LembreteComConta>> observarTodosComConta(String email) {
        return lembreteDao.observarTodosComConta(email);
    }

    public void buscarPorConta(long contaId, RetornoRepositorio<Lembrete> retorno) {
        EXECUTOR.execute(() -> {
            try {
                List<Lembrete> lembretes = lembreteDao.buscarPorConta(contaId);
                Lembrete lembrete = lembretes.isEmpty() ? null : lembretes.get(0);
                mainHandler.post(() -> retorno.onSuccess(lembrete));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void buscarPendentes(String email, RetornoRepositorio<List<Lembrete>> retorno) {
        EXECUTOR.execute(() -> {
            try {
                List<Lembrete> pendentes = lembreteDao.listarPendentes(email);
                mainHandler.post(() -> retorno.onSuccess(pendentes));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void inserir(Lembrete lembrete, RetornoRepositorio<Long> retorno) {
        EXECUTOR.execute(() -> {
            try {
                if (lembrete.criadoEm <= 0) {
                    lembrete.criadoEm = System.currentTimeMillis();
                }
                long id = lembreteDao.inserir(lembrete);
                lembrete.id = id;
                scheduler.scheduleNotification(lembrete);
                mainHandler.post(() -> retorno.onSuccess(id));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void atualizar(Lembrete lembrete, RetornoRepositorio<Void> retorno) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.cancelNotification(lembrete.id);
                lembreteDao.atualizar(lembrete);
                scheduler.scheduleNotification(lembrete);
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void excluir(Lembrete lembrete, RetornoRepositorio<Void> retorno) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.cancelNotification(lembrete.id);
                lembreteDao.excluir(lembrete);
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void reconciliarConta(Conta conta) {
        List<Lembrete> anteriores = lembreteDao.buscarPorConta(conta.id);
        boolean ativo = anteriores.isEmpty() || anteriores.get(0).ativo;
        LocalTime hora = anteriores.isEmpty()
                ? LocalTime.of(9, 0)
                : Instant.ofEpochMilli(anteriores.get(0).dataHora)
                        .atZone(ZoneId.systemDefault()).toLocalTime();
        reconciliarConta(conta, ativo, hora.getHour(), hora.getMinute());
    }

    public void reconciliarConta(Conta conta, boolean ativo, int hora, int minuto) {
        if (conta == null || conta.deleted) {
            return;
        }
        cancelarEExcluirDaConta(conta.id);
        Lembrete lembrete = gerarLembreteConta(context, conta, ativo, hora, minuto);
        if (lembrete != null) {
            long id = lembreteDao.inserir(lembrete);
            lembrete.id = id;
            scheduler.scheduleNotification(lembrete);
        }
    }

    public void excluirPorConta(long contaId) {
        cancelarEExcluirDaConta(contaId);
    }

    public void reagendarPendentes(String email) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.rescheduleAll(email);
            } catch (Exception error) {
                Log.e(TAG, "Falha ao reagendar lembretes pendentes.", error);
            }
        });
    }

    public void cancelarTodos(String email) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.cancelAll(email);
            } catch (Exception error) {
                Log.e(TAG, "Falha ao cancelar os alarmes dos lembretes.", error);
            }
        });
    }

    private void cancelarEExcluirDaConta(long contaId) {
        for (Lembrete lembrete : lembreteDao.buscarPorConta(contaId)) {
            scheduler.cancelNotification(lembrete.id);
        }
        lembreteDao.excluirPorConta(contaId);
    }

    public static Lembrete gerarLembreteConta(Context context, Conta conta, boolean ativo,
                                               int hora, int minuto) {
        if (!deveGerarLembretes(conta) || hora < 0 || hora > 23
                || minuto < 0 || minuto > 59) {
            return null;
        }
        LocalDate vencimento;
        try {
            vencimento = LocalDate.parse(conta.dueDate);
        } catch (RuntimeException error) {
            Log.e(TAG, "Data de vencimento inválida para a conta " + conta.id, error);
            return null;
        }
        long dataHora = LocalDateTime.of(vencimento, LocalTime.of(hora, minuto))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String dataFormatada = vencimento.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        String mensagem = context.getString(R.string.reminder_account_due_on,
                conta.description, dataFormatada);
        return new Lembrete(context.getString(R.string.reminder_notification_title),
                mensagem, dataHora, ativo && !TipoConta.CONCLUIDA.equals(conta.status),
                conta.id, System.currentTimeMillis(), false, conta.ownerEmail);
    }

    public static boolean deveGerarLembretes(Conta conta) {
        if (conta == null || conta.dueDate == null || conta.dueDate.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate.parse(conta.dueDate);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }
}
