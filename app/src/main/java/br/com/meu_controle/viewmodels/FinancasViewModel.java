package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.repositorio.RepositorioCategorias;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;

public class FinancasViewModel extends AndroidViewModel {
    private final RepositorioContas contas;
    private final RepositorioCategorias categorias;

    public FinancasViewModel(@NonNull Application application) {
        super(application);
        BancoDados banco = BancoDados.obterInstancia(application);
        RepositorioLembretes lembretes = new RepositorioLembretes(application);
        contas = new RepositorioContas(banco.contaDao(), lembretes);
        categorias = new RepositorioCategorias(banco.categoriaDao());
    }

    public void buscarPorTipo(String tipo, String email, boolean familiar, String grupo,
                              RetornoRepositorio<List<Conta>> retorno) {
        contas.buscarPorTipo(tipo, email, familiar, grupo, retorno);
    }

    public void buscarTodas(String email, boolean familiar, String grupo,
                            RetornoRepositorio<List<Conta>> retorno) {
        contas.buscarTodas(email, familiar, grupo, retorno);
    }

    public void inserirConta(Conta conta, RetornoRepositorio<Long> retorno) {
        contas.inserir(conta, retorno);
    }

    public void inserirConta(Conta conta, boolean lembreteAtivo, int hora, int minuto,
                             RetornoRepositorio<Long> retorno) {
        contas.inserir(conta, lembreteAtivo, hora, minuto, retorno);
    }

    public void atualizarConta(Conta conta, RetornoRepositorio<Void> retorno) {
        contas.atualizar(conta, retorno);
    }

    public void atualizarConta(Conta conta, boolean lembreteAtivo, int hora, int minuto,
                               RetornoRepositorio<Void> retorno) {
        contas.atualizar(conta, lembreteAtivo, hora, minuto, retorno);
    }

    public void excluirConta(long id, RetornoRepositorio<Void> retorno) {
        contas.excluir(id, retorno);
    }

    public void buscarCategorias(String tipo, String email, boolean familiar, String grupo,
                                 RetornoRepositorio<List<Categoria>> retorno) {
        categorias.buscarPorTipo(tipo, email, familiar, grupo, retorno);
    }

    public void inserirCategoria(Categoria categoria, RetornoRepositorio<Long> retorno) {
        categorias.inserir(categoria, retorno);
    }

    public void atualizarCategoria(Categoria categoria, RetornoRepositorio<Void> retorno) {
        categorias.atualizar(categoria, retorno);
    }

    public void excluirCategoria(Categoria categoria, RetornoRepositorio<Void> retorno) {
        categorias.excluir(categoria, retorno);
    }
}
