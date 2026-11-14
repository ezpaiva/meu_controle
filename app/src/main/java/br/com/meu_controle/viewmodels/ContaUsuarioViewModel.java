package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import br.com.meu_controle.dados.repositorio.ContaUsuarioRepository;

public class ContaUsuarioViewModel extends AndroidViewModel {

    private final ContaUsuarioRepository repository;

    public ContaUsuarioViewModel(@NonNull Application application) {
        super(application);
        repository = new ContaUsuarioRepository(application);
    }

    public void alterarEmail(String novoEmail, String senha,
                             ContaUsuarioRepository.Retorno retorno) {
        repository.alterarEmail(novoEmail, senha, retorno);
    }

    public void excluirConta(String senha, ContaUsuarioRepository.Retorno retorno) {
        repository.excluirConta(senha, retorno);
    }
}
