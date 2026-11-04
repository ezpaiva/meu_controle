package br.com.meu_controle.dados.repositorio;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.meu_controle.dados.local.CategoriaDao;
import br.com.meu_controle.dados.modelo.Categoria;

public class RepositorioCategorias {

    private final CategoriaDao categoriaDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RepositorioCategorias(CategoriaDao categoriaDao) {
        this.categoriaDao = categoriaDao;
    }

    public void buscarPorTipo(String tipo, String ownerEmail, boolean compartilhar,
                              RetornoRepositorio<List<Categoria>> retorno) {
        executor.execute(() -> {
            try {
                List<Categoria> categorias = compartilhar
                        ? categoriaDao.getByTipo(tipo)
                        : categoriaDao.getByTipoAndOwner(tipo, ownerEmail);
                mainHandler.post(() -> retorno.onSuccess(categorias));
            } catch (Exception e) {
                mainHandler.post(() -> retorno.onError(e));
            }
        });
    }

    public void inserir(Categoria categoria, RetornoRepositorio<Long> retorno) {
        executor.execute(() -> {
            try {
                int duplicatas = categoriaDao.contarPorNomeETipo(
                        categoria.tipo, categoria.nome, -1);
                if (duplicatas > 0) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Já existe uma categoria com esse nome.")));
                    return;
                }
                categoria.updatedAt = System.currentTimeMillis();
                categoria.syncPending = true;
                categoria.deleted = false;
                long id = categoriaDao.insert(categoria);
                mainHandler.post(() -> retorno.onSuccess(id));
            } catch (Exception e) {
                mainHandler.post(() -> retorno.onError(e));
            }
        });
    }

    public void atualizar(Categoria categoria, RetornoRepositorio<Void> retorno) {
        executor.execute(() -> {
            try {
                if (categoria.isPadrao) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Categorias padrão não podem ser editadas.")));
                    return;
                }
                Categoria original = categoriaDao.getById(categoria.id);
                if (original == null) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Categoria não encontrada.")));
                    return;
                }
                int duplicatas = categoriaDao.contarPorNomeETipo(
                        categoria.tipo, categoria.nome, categoria.id);
                if (duplicatas > 0) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Já existe uma categoria com esse nome.")));
                    return;
                }
                long agora = System.currentTimeMillis();
                categoria.updatedAt = agora;
                categoria.syncPending = true;
                categoriaDao.atualizarComPropagacao(categoria, original.nome, agora);
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> retorno.onError(e));
            }
        });
    }

    public void excluir(Categoria categoria, RetornoRepositorio<Void> retorno) {
        executor.execute(() -> {
            try {
                if (categoria.isPadrao) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Categorias padrão não podem ser excluídas.")));
                    return;
                }
                int usos = categoriaDao.contarContasUsandoCategoria(
                        categoria.nome, categoria.tipo);
                if (usos > 0) {
                    mainHandler.post(() -> retorno.onError(
                            new IllegalStateException("Existem " + usos
                                    + " conta(s) usando esta categoria. Edite-as antes de excluir.")));
                    return;
                }
                categoriaDao.marcarExcluida(categoria.id, System.currentTimeMillis());
                mainHandler.post(() -> retorno.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> retorno.onError(e));
            }
        });
    }
}