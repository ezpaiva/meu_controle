package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import br.com.meu_controle.dados.repositorio.PerfilUsuarioRepository;

public class PerfilUsuarioViewModel extends AndroidViewModel {

    private final PerfilUsuarioRepository repository;

    public PerfilUsuarioViewModel(@NonNull Application application) {
        super(application);
        repository = new PerfilUsuarioRepository(application);
    }

    public void carregar(String uid, String nomePadrao, String email,
                         PerfilUsuarioRepository.RetornoPerfil retorno) {
        repository.carregar(uid, nomePadrao, email, retorno);
    }

    public void salvar(String uid, String nome, String email, String perfil,
                       PerfilUsuarioRepository.RetornoPerfil retorno) {
        repository.salvar(uid, nome, email, perfil, retorno);
    }

    public void salvarEdicao(String uid, String nome, String emailContato, String perfil,
                             boolean notificacoesHabilitadas,
                             PerfilUsuarioRepository.RetornoPerfil retorno) {
        repository.salvarEdicao(uid, nome, emailContato, perfil,
                notificacoesHabilitadas, retorno);
    }
}
