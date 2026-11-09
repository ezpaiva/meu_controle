package br.com.meu_controle.dados.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "categorias", indices = {@Index(value = {"tipo"})})
public class Categoria implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String nome;
    public String tipo;          // TipoConta.RECEBER ou TipoConta.PAGAR
    public String ownerEmail;    // NULL para categorias padrão

    @ColumnInfo(defaultValue = "0")
    public boolean isPadrao;

    @ColumnInfo(defaultValue = "0")
    public long updatedAt;

    @ColumnInfo(defaultValue = "0")
    public boolean syncPending;

    @ColumnInfo(defaultValue = "0")
    public boolean deleted;

    public Categoria(String nome, String tipo, String ownerEmail) {
        this.nome = nome;
        this.tipo = tipo;
        this.ownerEmail = ownerEmail;
        this.isPadrao = false;
        this.updatedAt = System.currentTimeMillis();
        this.syncPending = true;
        this.deleted = false;
    }
}