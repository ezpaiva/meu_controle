package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;

import java.util.List;

public class LembreteViewModel extends AndroidViewModel {

    private final RepositorioLembretes repositorio;

    public LembreteViewModel(@NonNull Application application) {
        super(application);
        repositorio = new RepositorioLembretes(application);
    }

    public LiveData<List<Lembrete>> observarLembretes(String email) {
        return repositorio.observarTodos(email);
    }

    public LiveData<List<LembreteComConta>> observarLembretesComConta(String email) {
        return repositorio.observarTodosComConta(email);
    }

    public void inserir(Lembrete lembrete, RetornoRepositorio<Long> retorno) {
        repositorio.inserir(lembrete, retorno);
    }

    public void atualizar(Lembrete lembrete, RetornoRepositorio<Void> retorno) {
        repositorio.atualizar(lembrete, retorno);
    }

    public void excluir(Lembrete lembrete, RetornoRepositorio<Void> retorno) {
        repositorio.excluir(lembrete, retorno);
    }
}
