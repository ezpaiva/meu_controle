package br.com.meu_controle.dados.repositorio;

import android.os.Handler;
import android.os.Looper;

import br.com.meu_controle.dados.local.ContaDao;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.remoto.SincronizadorContas;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositorioContas {

    private final ContaDao contaDao;
    private final RepositorioLembretes repositorioLembretes;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RepositorioContas(ContaDao contaDao) {
        this(contaDao, null);
    }

    public RepositorioContas(ContaDao contaDao, RepositorioLembretes repositorioLembretes) {
        this.contaDao = contaDao;
        this.repositorioLembretes = repositorioLembretes;
    }

    public void buscarPorTipo(String type, String ownerEmail, boolean compartilhar,
                              RetornoRepositorio<List<Conta>> retorno) {
        executor.execute(() -> {
            try {
                List<Conta> contas = contaDao.getByTypeAndOwner(type, ownerEmail);
                mainHandler.post(() -> retorno.onSuccess(contas));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void buscarTodas(String ownerEmail, boolean compartilhar,
                            RetornoRepositorio<List<Conta>> retorno) {
        executor.execute(() -> {
            try {
                List<Conta> contas = contaDao.getAllByOwner(ownerEmail);
                mainHandler.post(() -> retorno.onSuccess(contas));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void inserir(Conta conta, RetornoRepositorio<Long> retorno) {
        inserir(conta, null, 9, 0, retorno);
    }

    public void inserir(Conta conta, boolean lembreteAtivo, int hora, int minuto,
                        RetornoRepositorio<Long> retorno) {
        inserir(conta, Boolean.valueOf(lembreteAtivo), hora, minuto, retorno);
    }

    private void inserir(Conta conta, Boolean lembreteAtivo, int hora, int minuto,
                         RetornoRepositorio<Long> retorno) {
        executor.execute(() -> {
            try {
                if (conta.remoteId == null) {
                    conta.remoteId = UUID.randomUUID().toString();
                }
                conta.updatedAt = System.currentTimeMillis();
                conta.syncPending = true;
                conta.deleted = false;
                long id = contaDao.insert(conta);
                conta.id = id;
                if (repositorioLembretes != null) {
                    if (lembreteAtivo == null) {
                        repositorioLembretes.reconciliarConta(conta);
                    } else {
                        repositorioLembretes.reconciliarConta(conta,
                                lembreteAtivo, hora, minuto);
                    }
                }
                SincronizadorContas.sincronizar();
                mainHandler.post(() -> retorno.onSuccess(id));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void atualizar(Conta conta, RetornoRepositorio<Void> retorno) {
        atualizar(conta, null, 9, 0, retorno);
    }

    public void atualizar(Conta conta, boolean lembreteAtivo, int hora, int minuto,
                          RetornoRepositorio<Void> retorno) {
        atualizar(conta, Boolean.valueOf(lembreteAtivo), hora, minuto, retorno);
    }

    private void atualizar(Conta conta, Boolean lembreteAtivo, int hora, int minuto,
                           RetornoRepositorio<Void> retorno) {
        executor.execute(() -> {
            try {
                conta.updatedAt = System.currentTimeMillis();
                conta.syncPending = true;
                contaDao.update(conta);
                if (repositorioLembretes != null) {
                    if (lembreteAtivo == null) {
                        repositorioLembretes.reconciliarConta(conta);
                    } else {
                        repositorioLembretes.reconciliarConta(conta,
                                lembreteAtivo, hora, minuto);
                    }
                }
                SincronizadorContas.sincronizar();
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void excluir(long id, RetornoRepositorio<Void> retorno) {
        executor.execute(() -> {
            try {
                if (repositorioLembretes != null) {
                    repositorioLembretes.excluirPorConta(id);
                }
                contaDao.marcarExcluida(id, System.currentTimeMillis());
                SincronizadorContas.sincronizar();
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }
}