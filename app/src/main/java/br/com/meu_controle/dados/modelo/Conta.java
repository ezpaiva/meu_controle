package br.com.meu_controle.dados.modelo;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts", indices = {@Index(value = {"type", "dueDate"})})
public class Conta {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String description;
    public long amountInCents;
    public String dueDate;
    public String category;
    public String type;
    public String status;

    public Conta(String description, long amountInCents, String dueDate,
                 String category, String type, String status) {
        this.description = description;
        this.amountInCents = amountInCents;
        this.dueDate = dueDate;
        this.category = category;
        this.type = type;
        this.status = status;
    }
}
