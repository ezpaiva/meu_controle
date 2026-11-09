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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RepositorioContas(ContaDao contaDao) {
        this.contaDao = contaDao;
    }

    public void buscarPorTipo(String type, String ownerEmail, boolean compartilhar,
                              RetornoRepositorio<List<Conta>> retorno) {
        executor.execute(() -> {
            try {
                List<Conta> contas = compartilhar
                        ? contaDao.getByType(type)
                        : contaDao.getByTypeAndOwner(type, ownerEmail);
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
                List<Conta> contas = compartilhar
                        ? contaDao.getAll()
                        : contaDao.getAllByOwner(ownerEmail);
                mainHandler.post(() -> retorno.onSuccess(contas));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void inserir(Conta conta, RetornoRepositorio<Long> retorno) {
        executor.execute(() -> {
            try {
                if (conta.remoteId == null) {
                    conta.remoteId = UUID.randomUUID().toString();
                }
                conta.updatedAt = System.currentTimeMillis();
                conta.syncPending = true;
                conta.deleted = false;
                long id = contaDao.insert(conta);
                SincronizadorContas.sincronizar();
                mainHandler.post(() -> retorno.onSuccess(id));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void atualizar(Conta conta, RetornoRepositorio<Void> retorno) {
        executor.execute(() -> {
            try {
                conta.updatedAt = System.currentTimeMillis();
                conta.syncPending = true;
                contaDao.update(conta);
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
                contaDao.marcarExcluida(id, System.currentTimeMillis());
                SincronizadorContas.sincronizar();
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }
}