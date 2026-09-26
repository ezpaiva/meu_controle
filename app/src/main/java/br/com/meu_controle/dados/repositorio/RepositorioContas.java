package br.com.meu_controle.dados.repositorio;

import android.os.Handler;
import android.os.Looper;
import android.database.sqlite.SQLiteException;

import br.com.meu_controle.dados.local.ContaDao;
import br.com.meu_controle.dados.modelo.Conta;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositorioContas {

    private final ContaDao contaDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RepositorioContas(ContaDao contaDao) {
        this.contaDao = contaDao;
    }

    public void buscarPorTipo(String type, RetornoRepositorio<List<Conta>> retorno) {
        executor.execute(() -> {
            try {
                List<Conta> contas = contaDao.getByType(type);
                mainHandler.post(() -> retorno.onSuccess(contas));
            } catch (SQLiteException error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }

    public void inserir(Conta conta, RetornoRepositorio<Long> retorno) {
        executor.execute(() -> {
            try {
                long id = contaDao.insert(conta);
                mainHandler.post(() -> retorno.onSuccess(id));
            } catch (SQLiteException error) {
                mainHandler.post(() -> retorno.onError(error));
            }
        });
    }
}
