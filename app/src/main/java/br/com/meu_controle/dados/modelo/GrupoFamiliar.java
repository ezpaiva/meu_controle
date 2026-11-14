package br.com.meu_controle.dados.modelo;

import java.util.ArrayList;
import java.util.List;

public class GrupoFamiliar {
    public String id;
    public String nome;
    public String criadorEmail;
    public String criadorId;
    public long dataCriacao;
    public boolean ativo;
    public List<String> membros = new ArrayList<>();

    public GrupoFamiliar() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCriadorEmail() {
        return criadorEmail;
    }

    public void setCriadorEmail(String criadorEmail) {
        this.criadorEmail = criadorEmail;
    }

    public long getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(long dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
