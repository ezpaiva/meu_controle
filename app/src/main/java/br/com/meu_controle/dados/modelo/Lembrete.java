package br.com.meu_controle.dados.modelo;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity(tableName = "reminders",
        foreignKeys = @ForeignKey(entity = Conta.class,
                parentColumns = "id",
                childColumns = "contaId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "contaId", unique = true),
                @Index("dataHora"), @Index("ownerEmail")})
public class Lembrete {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String titulo;
    @NonNull
    public String mensagem;
    public long dataHora;
    @NonNull
    public String data;
    @NonNull
    public String hora;
    public boolean ativo;
    public Long contaId;
    public long criadoEm;
    public boolean notificado;
    public String ownerEmail;

    public Lembrete(@NonNull String titulo, @NonNull String mensagem, long dataHora, boolean ativo,
                    Long contaId, long criadoEm, boolean notificado, String ownerEmail) {
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.dataHora = dataHora;
        ZoneId zoneId = ZoneId.systemDefault();
        this.data = Instant.ofEpochMilli(dataHora).atZone(zoneId).toLocalDate().toString();
        this.hora = Instant.ofEpochMilli(dataHora).atZone(zoneId).toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        this.ativo = ativo;
        this.contaId = contaId;
        this.criadoEm = criadoEm;
        this.notificado = notificado;
        this.ownerEmail = ownerEmail;
    }
}
