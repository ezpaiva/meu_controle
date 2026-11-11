package br.com.meu_controle.dados.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class LembreteComConta {
    @Embedded
    public Lembrete lembrete;

    @ColumnInfo(name = "contaDescricao")
    public String contaDescricao;
}
