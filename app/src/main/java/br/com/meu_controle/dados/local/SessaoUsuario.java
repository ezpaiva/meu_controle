package br.com.meu_controle.dados.local;

import android.content.Context;
import android.content.SharedPreferences;

import br.com.meu_controle.dados.modelo.TipoPerfil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessaoUsuario {
    private static final String ARQUIVO = "sessao_usuario";
    private static final String CHAVE_NOME = "nome";
    private static final String CHAVE_EMAIL = "email";
    private static final String CHAVE_EMAIL_CONTATO = "email_contato";
    private static final String CHAVE_TIPO = "tipo_perfil";
    private static final String CHAVE_GRUPO_ID = "grupo_id";
    private static final String CHAVE_GRUPO_NOME = "grupo_nome";
    private static final String CHAVE_MEMBROS = "membros_familiares";
    private static final String CHAVE_NOTIFICACOES_HABILITADAS = "notificacoes_habilitadas";

    private final SharedPreferences preferencias;

    public SessaoUsuario(Context context) {
        preferencias = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public void salvarPerfil(String nome, String email, String tipo) {
        salvarPerfil(nome, email, tipo, null, null);
    }

    public void salvarPerfil(String nome, String email, String tipo, String grupoId,
                             String grupoNome) {
        salvarPerfil(nome, email, tipo, grupoId, grupoNome, obterEmailContato());
    }

    public void salvarPerfil(String nome, String email, String tipo, String grupoId,
                             String grupoNome, String emailContato) {
        preferencias.edit()
                .putString(CHAVE_NOME, nome)
                .putString(CHAVE_EMAIL, email)
                .putString(CHAVE_EMAIL_CONTATO,
                        emailContato == null || emailContato.trim().isEmpty()
                                ? email : emailContato)
                .putString(CHAVE_TIPO, tipo)
                .putString(CHAVE_GRUPO_ID, grupoId)
                .putString(CHAVE_GRUPO_NOME, grupoNome)
                .apply();
    }

    public void garantirPerfil(String email) {
        if (!preferencias.contains(CHAVE_EMAIL)) {
            salvarPerfil("Você", email, TipoPerfil.INDIVIDUAL);
        }
    }

    public String obterNome() {
        return preferencias.getString(CHAVE_NOME, "Você");
    }

    public String obterEmail() {
        return preferencias.getString(CHAVE_EMAIL, "");
    }

    public String obterEmailContato() {
        return preferencias.getString(CHAVE_EMAIL_CONTATO, obterEmail());
    }

    public String obterTipo() {
        return preferencias.getString(CHAVE_TIPO, TipoPerfil.INDIVIDUAL);
    }

    public String obterGrupoId() {
        return preferencias.getString(CHAVE_GRUPO_ID, null);
    }

    public String obterGrupoNome() {
        return preferencias.getString(CHAVE_GRUPO_NOME, "");
    }

    public void salvarGrupo(String grupoId, String grupoNome) {
        preferencias.edit().putString(CHAVE_GRUPO_ID, grupoId)
                .putString(CHAVE_GRUPO_NOME, grupoNome).apply();
    }

    public boolean perfilFamiliar() {
        return TipoPerfil.FAMILIAR.equals(obterTipo());
    }

    public boolean isNotificationsEnabled() {
        return preferencias.getBoolean(CHAVE_NOTIFICACOES_HABILITADAS, true);
    }

    public void setNotificationsEnabled(boolean habilitadas) {
        preferencias.edit().putBoolean(CHAVE_NOTIFICACOES_HABILITADAS, habilitadas).apply();
    }

    public List<String> obterMembros() {
        Set<String> membros = preferencias.getStringSet(CHAVE_MEMBROS, new LinkedHashSet<>());
        return new ArrayList<>(membros);
    }

    public boolean adicionarMembro(String email) {
        Set<String> membros = new LinkedHashSet<>(obterMembros());
        boolean adicionado = membros.add(email);
        if (adicionado) {
            preferencias.edit().putStringSet(CHAVE_MEMBROS, membros).apply();
        }
        return adicionado;
    }

    public void salvarMembros(List<String> emails) {
        preferencias.edit().putStringSet(CHAVE_MEMBROS,
                new LinkedHashSet<>(emails)).apply();
    }

    public void sair() {
        boolean notificacoesHabilitadas = isNotificationsEnabled();
        preferencias.edit().clear()
                .putBoolean(CHAVE_NOTIFICACOES_HABILITADAS, notificacoesHabilitadas)
                .apply();
    }
}
