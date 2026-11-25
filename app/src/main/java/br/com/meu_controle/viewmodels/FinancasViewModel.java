package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioCategorias;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FiltroContas;

public class FinancasViewModel extends AndroidViewModel {
    private final RepositorioContas contas;
    private final RepositorioCategorias categorias;
    private final MutableLiveData<ResumoPrevisto> resumoPrevisto = new MutableLiveData<>();
    private String dataSelecionada;

    public FinancasViewModel(@NonNull Application application) {
        super(application);
        BancoDados banco = BancoDados.obterInstancia(application);
        RepositorioLembretes lembretes = new RepositorioLembretes(application);
        contas = new RepositorioContas(banco.contaDao(), lembretes);
        categorias = new RepositorioCategorias(banco.categoriaDao());
    }

    public LiveData<ResumoPrevisto> observarResumoPrevisto() {
        return resumoPrevisto;
    }

    public String obterDataSelecionada() {
        return dataSelecionada;
    }

    public void definirDataSelecionada(String dataIso) {
        dataSelecionada = dataIso;
    }

    public void calcularResumoPrevisto(List<Conta> contas) {
        FiltroContas.ResumoFinanceiro resumo =
                FiltroContas.preverAte(contas, dataSelecionada);
        resumoPrevisto.setValue(new ResumoPrevisto(
                resumo.receitas, resumo.despesas, resumo.saldo));
    }

    public static class ResumoPrevisto {
        public final long receitas;
        public final long despesas;
        public final long saldo;

        ResumoPrevisto(long receitas, long despesas, long saldo) {
            this.receitas = receitas;
            this.despesas = despesas;
            this.saldo = saldo;
        }
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
