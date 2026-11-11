package br.com.meu_controle.dados.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;

import java.util.List;

@Dao
public interface LembreteDao {

    @Insert
    long inserir(Lembrete lembrete);

    @Update
    void atualizar(Lembrete lembrete);

    @Delete
    void excluir(Lembrete lembrete);

    @Query("SELECT * FROM reminders WHERE ownerEmail = :email OR ownerEmail IS NULL "
            + "ORDER BY dataHora ASC")
    LiveData<List<Lembrete>> observarTodos(String email);

    @Query("SELECT r.*, a.description AS contaDescricao FROM reminders r "
            + "LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.ownerEmail = :email OR r.ownerEmail IS NULL ORDER BY r.dataHora ASC")
    LiveData<List<LembreteComConta>> observarTodosComConta(String email);

    @Query("SELECT * FROM reminders WHERE ownerEmail = :email OR ownerEmail IS NULL "
            + "ORDER BY dataHora ASC")
    List<Lembrete> listarTodos(String email);

    @Query("SELECT r.* FROM reminders r LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.notificado = 0 "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL) "
            + "AND (r.contaId IS NULL OR (a.deleted = 0 AND a.status != 'CONCLUIDA')) "
            + "ORDER BY r.dataHora ASC")
    List<Lembrete> listarPendentes(String email);

    @Query("SELECT r.* FROM reminders r LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.ativo = 1 AND r.notificado = 0 "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL) "
            + "AND (r.contaId IS NULL OR (a.deleted = 0 AND a.status != 'CONCLUIDA')) "
            + "ORDER BY r.dataHora ASC")
    List<Lembrete> listarAtivosPendentes(String email);

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    Lembrete buscarPorId(long id);

    @Query("SELECT * FROM reminders WHERE contaId = :contaId")
    List<Lembrete> buscarPorConta(long contaId);

    @Query("DELETE FROM reminders WHERE contaId = :contaId")
    void excluirPorConta(long contaId);

    @Query("UPDATE reminders SET notificado = 1 WHERE id = :id")
    void marcarNotificado(long id);
}
