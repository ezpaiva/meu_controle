package br.com.meu_controle.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;

import java.util.List;

public class LembreteViewModel extends AndroidViewModel {

    private static final String TAG = "LEMBRETES_VM";

    private final RepositorioLembretes repositorio;
    private final SessaoUsuario sessao;

    public LembreteViewModel(@NonNull Application application) {
        super(application);
        repositorio = new RepositorioLembretes(application);
        sessao = new SessaoUsuario(application);
    }

    public LiveData<List<Lembrete>> observarLembretes(String email) {
        return repositorio.observarTodos(email);
    }

    public LiveData<List<Lembrete>> observarLembretes(String email, String groupId) {
        return repositorio.observarTodos(email, groupId);
    }

    public LiveData<List<LembreteComConta>> observarLembretesComConta(String email) {
        return repositorio.observarTodosComConta(email);
    }

    public LiveData<List<LembreteComConta>> observarLembretesComConta(
            String email, String groupId) {
        return repositorio.observarTodosComConta(email, groupId);
    }

    /**
     * Sincronização best-effort (RNF03 / RF21).
     *
     * Regras centralizadas aqui, na camada de apresentação:
     *  - Sem sessão válida (email ou groupId vazios)  -> sucesso imediato, sem chamar remoto.
     *  - Repositório devolve onError apenas em falha real; falha de rede deve ser
     *    traduzida para onSuccess pelo RepositorioLembretes (offline é estado normal).
     */
    public void sincronizar(RetornoRepositorio<Void> retorno) {
        String email = sessao.obterEmail();
        String groupId = sessao.obterGrupoId();

        if (email == null || email.trim().isEmpty()
                || groupId == null || groupId.trim().isEmpty()) {
            Log.d(TAG, "Sincronização ignorada: sessão sem email/grupo válido "
                    + "(email=" + email + ", groupId=" + groupId + ")");
            retorno.onSuccess(null);   // estado normal, não é erro
            return;
        }

        repositorio.sincronizar(retorno);
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