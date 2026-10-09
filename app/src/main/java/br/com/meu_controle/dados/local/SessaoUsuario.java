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
    private static final String CHAVE_TIPO = "tipo_perfil";
    private static final String CHAVE_MEMBROS = "membros_familiares";

    private final SharedPreferences preferencias;

    public SessaoUsuario(Context context) {
        preferencias = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public void salvarPerfil(String nome, String email, String tipo) {
        preferencias.edit()
                .putString(CHAVE_NOME, nome)
                .putString(CHAVE_EMAIL, email)
                .putString(CHAVE_TIPO, tipo)
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

    public String obterTipo() {
        return preferencias.getString(CHAVE_TIPO, TipoPerfil.INDIVIDUAL);
    }

    public boolean perfilFamiliar() {
        return TipoPerfil.FAMILIAR.equals(obterTipo());
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

    public void sair() {
        preferencias.edit().clear().apply();
    }
}
