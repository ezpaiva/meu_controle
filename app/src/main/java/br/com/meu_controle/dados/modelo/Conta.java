package br.com.meu_controle.dados.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.UUID;

@Entity(tableName = "accounts", indices = {@Index(value = {"type", "dueDate"})})
public class Conta implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String description;
    public long amountInCents;
    public String dueDate;
    public String category;
    public String type;
    public String status;
    public String ownerEmail;
    public String responsibleName;

    // --- Campos de sincronização ---
    public String remoteId;

    @ColumnInfo(defaultValue = "0")
    public long updatedAt;

    @ColumnInfo(defaultValue = "0")
    public boolean syncPending;

    @ColumnInfo(defaultValue = "0")
    public boolean deleted;

    public Conta(String description, long amountInCents, String dueDate,
                 String category, String type, String status,
                 String ownerEmail, String responsibleName) {
        this.description = description;
        this.amountInCents = amountInCents;
        this.dueDate = dueDate;
        this.category = category;
        this.type = type;
        this.status = status;
        this.ownerEmail = ownerEmail;
        this.responsibleName = responsibleName;

        this.remoteId = UUID.randomUUID().toString();
        this.updatedAt = System.currentTimeMillis();
        this.syncPending = true;
        this.deleted = false;
    }
}