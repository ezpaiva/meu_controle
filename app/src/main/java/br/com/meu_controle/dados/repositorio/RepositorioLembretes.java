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

    public LiveData<List<Lembrete>> observarTodos(String email, String groupId) {
        return lembreteDao.observarPorEscopo(email, groupId);
    }

    public LiveData<List<LembreteComConta>> observarTodosComConta(String email) {
        return lembreteDao.observarTodosComConta(email);
    }

    public LiveData<List<LembreteComConta>> observarTodosComConta(String email, String groupId) {
        return lembreteDao.observarComContaPorEscopo(email, groupId);
    }

    /**
     * RNF03 / RF21: sincronização é best-effort. Room é a fonte de verdade.
     *
     * Distingue três categorias:
     *  - SUCESSO ou estado normal (offline, sem sessão): onSuccess — tela não avisa.
     *  - BLOQUEIO DE ACESSO (grupo removido): onError com SecurityException —
     *    a Activity exibe a mensagem real ao usuário.
     *  - FALHA TRANSITÓRIA (timeout, rede): log + onSuccess — dado local preservado,
     *    nova tentativa ocorre no próximo onResume/onCreate.
     */
    public void sincronizar(RetornoRepositorio<Void> retorno) {
        try {
            br.com.meu_controle.dados.remoto.SincronizadorContas.sincronizar(
                    new br.com.meu_controle.dados.remoto.SincronizadorContas.Retorno() {
                        @Override
                        public void aoConcluir(boolean sucesso, String mensagem) {
                            if (sucesso) {
                                Log.d(TAG, "Sincronização: " + mensagem);
                            } else {
                                Log.w(TAG, "Falha transitória: " + mensagem
                                        + " (dados locais preservados).");
                            }
                            retorno.onSuccess(null);
                        }

                        @Override
                        public void aoBloquearAcesso(String motivo) {
                            Log.w(TAG, "Acesso ao grupo revogado: " + motivo);
                            retorno.onError(new SecurityException(motivo));
                        }
                    });
        } catch (RuntimeException e) {
            Log.e(TAG, "Sincronização indisponível; seguindo em modo offline.", e);
            retorno.onSuccess(null);
        }
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
        buscarPendentes(email, null, retorno);
    }

    public void buscarPendentes(String email, String groupId,
                                RetornoRepositorio<List<Lembrete>> retorno) {
        EXECUTOR.execute(() -> {
            try {
                List<Lembrete> pendentes = lembreteDao.listarPendentesPorEscopo(email, groupId);
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
                lembrete.updatedAt = System.currentTimeMillis();
                lembrete.syncPending = temGrupo(lembrete.groupId);
                long id = lembreteDao.inserir(lembrete);
                lembrete.id = id;
                scheduler.scheduleNotification(lembrete);
                sincronizarGrupoSeNecessario(lembrete);
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
                lembrete.updatedAt = System.currentTimeMillis();
                lembrete.syncPending = temGrupo(lembrete.groupId);
                lembreteDao.atualizar(lembrete);
                scheduler.scheduleNotification(lembrete);
                sincronizarGrupoSeNecessario(lembrete);
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
                if (temGrupo(lembrete.groupId)) {
                    lembreteDao.marcarExcluido(lembrete.id, System.currentTimeMillis());
                    lembrete.syncPending = true;
                    lembrete.deleted = true;
                } else {
                    lembreteDao.excluir(lembrete);
                }
                sincronizarGrupoSeNecessario(lembrete);
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

        Lembrete novo = gerarLembreteConta(context, conta, ativo, hora, minuto);
        List<Lembrete> existentes = lembreteDao.buscarPorConta(conta.id);

        // Conta que não gera lembrete (data inválida, por exemplo): remove o que existir
        if (novo == null) {
            cancelarEExcluirDaConta(conta.id);
            return;
        }

        long agora = System.currentTimeMillis();

        if (existentes.isEmpty()) {
            novo.updatedAt = agora;
            novo.syncPending = temGrupo(novo.groupId);
            novo.id = lembreteDao.inserir(novo);
            scheduler.scheduleNotification(novo);
            sincronizarGrupoSeNecessario(novo);
            return;
        }

        // contaId é único na tabela (inclusive para lembretes de grupo marcados como
        // excluídos): atualiza a linha existente em vez de excluir e inserir de novo
        Lembrete atual = existentes.get(0);
        scheduler.cancelNotification(atual.id);

        atual.titulo = novo.titulo;
        atual.mensagem = novo.mensagem;
        atual.dataHora = novo.dataHora;
        atual.data = novo.data;
        atual.hora = novo.hora;
        atual.ativo = novo.ativo;
        atual.notificado = false;
        atual.deleted = false;
        atual.ownerEmail = novo.ownerEmail;
        atual.groupId = novo.groupId;
        atual.createdByName = novo.createdByName;
        atual.contaRemoteId = novo.contaRemoteId;
        atual.updatedAt = agora;
        atual.syncPending = temGrupo(atual.groupId);

        lembreteDao.atualizar(atual);
        scheduler.scheduleNotification(atual);
        sincronizarGrupoSeNecessario(atual);
    }

    public void excluirPorConta(long contaId) {
        cancelarEExcluirDaConta(contaId);
    }

    public void reagendarPendentes(String email) {
        reagendarPendentes(email, null);
    }

    public void reagendarPendentes(String email, String groupId) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.rescheduleAll(email, groupId);
            } catch (Exception error) {
                Log.e(TAG, "Falha ao reagendar lembretes pendentes.", error);
            }
        });
    }

    public void cancelarTodos(String email) {
        cancelarTodos(email, null);
    }

    public void cancelarTodos(String email, String groupId) {
        EXECUTOR.execute(() -> {
            try {
                scheduler.cancelAll(email, groupId);
            } catch (Exception error) {
                Log.e(TAG, "Falha ao cancelar os alarmes dos lembretes.", error);
            }
        });
    }

    private void cancelarEExcluirDaConta(long contaId) {
        for (Lembrete lembrete : lembreteDao.buscarPorConta(contaId)) {
            scheduler.cancelNotification(lembrete.id);
            if (temGrupo(lembrete.groupId)) {
                lembrete.updatedAt = System.currentTimeMillis();
                lembrete.syncPending = true;
                lembrete.deleted = true;
                lembreteDao.marcarExcluido(lembrete.id, lembrete.updatedAt);
                sincronizarGrupoSeNecessario(lembrete);
            } else {
                lembreteDao.deleteById(lembrete.id);
            }
        }
    }

    /**
     * RNF03: sincronização pós-CRUD não pode derrubar a operação local.
     * Qualquer falha remota é logada e ignorada — o registro já está no Room.
     */
    private boolean temGrupo(String groupId) {
        return groupId != null && !groupId.trim().isEmpty();
    }

    private void sincronizarGrupoSeNecessario(Lembrete lembrete) {
        if (lembrete == null || !temGrupo(lembrete.groupId)) {
            return;
        }
        try {
            br.com.meu_controle.dados.remoto.SincronizadorContas.sincronizar();
        } catch (RuntimeException e) {
            Log.w(TAG, "Sincronização pós-CRUD falhou; operação local preservada.", e);
        }
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
        Lembrete lembrete = new Lembrete(context.getString(R.string.reminder_notification_title),
                mensagem, dataHora, ativo && !TipoConta.CONCLUIDA.equals(conta.status),
                conta.id, System.currentTimeMillis(), false, conta.ownerEmail);
        lembrete.groupId = conta.groupId;
        lembrete.createdByName = conta.responsibleName;
        lembrete.contaRemoteId = conta.remoteId;
        return lembrete;
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