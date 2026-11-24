package br.com.meu_controle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import br.com.meu_controle.dados.modelo.GrupoFamiliar;
import br.com.meu_controle.dados.modelo.Usuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.repositorio.GrupoFamiliarRepository;
import br.com.meu_controle.dados.modelo.ConviteFamiliar;

public class GrupoFamiliarViewModel extends AndroidViewModel {

    private final GrupoFamiliarRepository repository;
    private final MutableLiveData<GrupoFamiliar> grupo = new MutableLiveData<>();
    private final MutableLiveData<List<Usuario>> membros =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> carregando = new MutableLiveData<>(false);
    private final MutableLiveData<String> erro = new MutableLiveData<>();
    private final MutableLiveData<String> mensagem = new MutableLiveData<>();

    public GrupoFamiliarViewModel(@NonNull Application application) {
        super(application);
        repository = new GrupoFamiliarRepository(application);
    }

    public LiveData<GrupoFamiliar> observarGrupo() { return grupo; }
    public LiveData<List<Usuario>> observarMembros() { return membros; }
    public LiveData<Boolean> observarCarregando() { return carregando; }
    public LiveData<String> observarErro() { return erro; }
    public LiveData<String> observarMensagem() { return mensagem; }

    public void carregarGrupo() {
        executar(() -> repository.carregarGrupo(retorno(null)));
    }

    public void adicionarMembro(String email) {
        executar(() -> repository.adicionarMembro(email, retorno("Convite enviado.")));
    }

    public void removerMembro(String id) {
        executar(() -> repository.removerMembro(id, retorno("Grupo familiar atualizado.")));
    }

    public void sairDoGrupo() {
        executar(() -> repository.sairDoGrupo(retorno("Você saiu do grupo familiar.")));
    }

    public void criarGrupo(String nome, Conta primeiraConta,
                           GrupoFamiliarRepository.RetornoGrupo retorno) {
        executar(() -> repository.criarGrupo(nome, primeiraConta, retorno));
    }

    public void listarConvites(GrupoFamiliarRepository.RetornoConvites retorno) {
        repository.listarConvitesPendentes(retorno);
    }

    public void aceitarConvite(ConviteFamiliar convite,
                               GrupoFamiliarRepository.RetornoGrupo retorno) {
        executar(() -> repository.aceitarConvite(convite, retorno));
    }

    public void recusarConvite(ConviteFamiliar convite, Runnable sucesso,
                               java.util.function.Consumer<Exception> falha) {
        repository.recusarConvite(convite, sucesso, falha);
    }

    private void executar(Runnable operacao) {
        erro.setValue(null);
        mensagem.setValue(null);
        carregando.setValue(true);
        operacao.run();
    }

    private GrupoFamiliarRepository.RetornoGrupo retorno(String mensagemSucesso) {
        return new GrupoFamiliarRepository.RetornoGrupo() {
            @Override
            public void onSuccess(GrupoFamiliar resultado, List<Usuario> lista) {
                carregando.postValue(false);
                grupo.postValue(resultado);
                membros.postValue(lista);
                if (mensagemSucesso != null) mensagem.postValue(mensagemSucesso);
            }

            @Override
            public void onError(Exception errorValue) {
                carregando.postValue(false);
                erro.postValue(errorValue.getMessage() == null
                        ? "Não foi possível atualizar o grupo familiar."
                        : errorValue.getMessage());
            }
        };
    }
}
