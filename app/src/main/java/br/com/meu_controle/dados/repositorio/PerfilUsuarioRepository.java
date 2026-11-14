package br.com.meu_controle.dados.repositorio;

import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.dados.modelo.Usuario;

public class PerfilUsuarioRepository {

    public interface RetornoPerfil {
        void onSuccess(Usuario usuario);
        void onError(Exception error);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final SessaoUsuario sessao;

    public PerfilUsuarioRepository(Context context) {
        sessao = new SessaoUsuario(context.getApplicationContext());
    }

    public void carregar(String uid, String nomePadrao, String email,
                         RetornoPerfil retorno) {
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documento -> {
                    Usuario usuario = new Usuario();
                    usuario.id = uid;
                    usuario.nome = valor(documento.getString("nome"), nomePadrao);
                    usuario.email = normalizar(email);
                    usuario.emailContato = valor(documento.getString("emailContato"),
                            valor(documento.getString("email"), usuario.email));
                    usuario.perfil = valor(documento.getString("perfil"),
                            valor(documento.getString("tipoPerfil"), TipoPerfil.INDIVIDUAL));
                    usuario.grupoId = documento.exists() ? documento.getString("grupoId") : null;
                    Boolean notificacoes = documento.getBoolean("notificacoesHabilitadas");
                    usuario.notificacoesHabilitadas = notificacoes == null
                            ? sessao.isNotificationsEnabled() : notificacoes;
                    String nomeGrupo = documento.getString("grupoNome");
                    if (nomeGrupo == null && usuario.grupoId != null
                            && usuario.grupoId.equals(sessao.obterGrupoId())) {
                        nomeGrupo = sessao.obterGrupoNome();
                    }
                    sessao.salvarPerfil(usuario.nome, usuario.email,
                            usuario.perfil, usuario.grupoId, nomeGrupo, usuario.emailContato);
                    sessao.setNotificationsEnabled(usuario.notificacoesHabilitadas);
                    retorno.onSuccess(usuario);
                })
                .addOnFailureListener(retorno::onError);
    }

    public void salvar(String uid, String nome, String email, String perfil,
                       RetornoPerfil retorno) {
        Usuario usuario = new Usuario();
        usuario.id = uid;
        usuario.nome = nome;
        usuario.email = normalizar(email);
        usuario.emailContato = usuario.email;
        usuario.perfil = perfil;
        usuario.grupoId = null;
        usuario.notificacoesHabilitadas = sessao.isNotificationsEnabled();
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", usuario.nome);
        dados.put("email", usuario.email);
        dados.put("emailContato", usuario.emailContato);
        dados.put("perfil", usuario.perfil);
        dados.put("tipoPerfil", usuario.perfil);
        dados.put("grupoId", null);
        dados.put("notificacoesHabilitadas", usuario.notificacoesHabilitadas);
        firestore.collection("usuarios").document(uid).set(dados, SetOptions.merge())
                .addOnSuccessListener(ignorado -> {
                    sessao.salvarPerfil(usuario.nome, usuario.email, usuario.perfil,
                            null, null, usuario.emailContato);
                    retorno.onSuccess(usuario);
                })
                .addOnFailureListener(retorno::onError);
    }

    public void salvarEdicao(String uid, String nome, String emailContato, String perfil,
                             boolean notificacoesHabilitadas, RetornoPerfil retorno) {
        Usuario usuario = new Usuario();
        usuario.id = uid;
        usuario.nome = nome.trim();
        usuario.email = sessao.obterEmail();
        usuario.emailContato = normalizar(emailContato);
        usuario.perfil = perfil;
        usuario.grupoId = sessao.obterGrupoId();
        usuario.notificacoesHabilitadas = notificacoesHabilitadas;

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", usuario.nome);
        dados.put("emailContato", usuario.emailContato);
        dados.put("perfil", usuario.perfil);
        dados.put("tipoPerfil", usuario.perfil);
        dados.put("grupoId", usuario.grupoId);
        dados.put("notificacoesHabilitadas", usuario.notificacoesHabilitadas);

        firestore.collection("usuarios").document(uid).set(dados, SetOptions.merge())
                .addOnSuccessListener(ignorado -> {
                    sessao.salvarPerfil(usuario.nome, usuario.email, usuario.perfil,
                            usuario.grupoId, sessao.obterGrupoNome(), usuario.emailContato);
                    sessao.setNotificationsEnabled(usuario.notificacoesHabilitadas);
                    retorno.onSuccess(usuario);
                })
                .addOnFailureListener(retorno::onError);
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String valor(String valor, String alternativa) {
        return valor == null || valor.trim().isEmpty() ? alternativa : valor;
    }
}
