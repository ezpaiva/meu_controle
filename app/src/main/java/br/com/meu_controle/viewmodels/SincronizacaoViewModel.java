package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;

public class SincronizacaoViewModel extends AndroidViewModel {

    private final RepositorioContas repositorio;

    public SincronizacaoViewModel(@NonNull Application application) {
        super(application);
        RepositorioLembretes lembretes = new RepositorioLembretes(application);
        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(application).contaDao(), lembretes);
    }

    public void sincronizar(RetornoRepositorio<Void> retorno) {
        repositorio.sincronizar(retorno);
    }
}
