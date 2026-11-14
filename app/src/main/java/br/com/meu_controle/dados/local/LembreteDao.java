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

    @Query("SELECT * FROM reminders WHERE groupId IS NULL AND deleted = 0 "
            + "AND (ownerEmail = :email OR ownerEmail IS NULL) "
            + "ORDER BY dataHora ASC")
    LiveData<List<Lembrete>> observarTodos(String email);

    @Query("SELECT * FROM reminders WHERE deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL "
            + "AND (ownerEmail = :email OR ownerEmail IS NULL))) ORDER BY dataHora ASC")
    LiveData<List<Lembrete>> observarPorEscopo(String email, String groupId);

    @Query("SELECT r.*, a.description AS contaDescricao FROM reminders r "
            + "LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.groupId IS NULL AND r.deleted = 0 "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL) ORDER BY r.dataHora ASC")
    LiveData<List<LembreteComConta>> observarTodosComConta(String email);

    @Query("SELECT r.*, a.description AS contaDescricao FROM reminders r "
            + "LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.deleted = 0 AND ((:groupId IS NOT NULL AND r.groupId = :groupId) OR "
            + "(:groupId IS NULL AND r.groupId IS NULL "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL))) "
            + "ORDER BY r.dataHora ASC")
    LiveData<List<LembreteComConta>> observarComContaPorEscopo(String email, String groupId);

    @Query("SELECT * FROM reminders WHERE groupId IS NULL AND deleted = 0 "
            + "AND (ownerEmail = :email OR ownerEmail IS NULL) "
            + "ORDER BY dataHora ASC")
    List<Lembrete> listarTodos(String email);

    @Query("SELECT r.* FROM reminders r LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.notificado = 0 AND r.deleted = 0 AND r.groupId IS NULL "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL) "
            + "AND (r.contaId IS NULL OR (a.deleted = 0 AND a.status != 'CONCLUIDA')) "
            + "ORDER BY r.dataHora ASC")
    List<Lembrete> listarPendentes(String email);

    @Query("SELECT r.* FROM reminders r LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.notificado = 0 AND r.deleted = 0 "
            + "AND ((:groupId IS NOT NULL AND r.groupId = :groupId) OR "
            + "(:groupId IS NULL AND r.groupId IS NULL "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL))) "
            + "AND (r.contaId IS NULL OR (a.deleted = 0 AND a.status != 'CONCLUIDA')) "
            + "ORDER BY r.dataHora ASC")
    List<Lembrete> listarPendentesPorEscopo(String email, String groupId);

    @Query("SELECT r.* FROM reminders r LEFT JOIN accounts a ON r.contaId = a.id "
            + "WHERE r.ativo = 1 AND r.notificado = 0 AND r.deleted = 0 AND r.groupId IS NULL "
            + "AND (r.ownerEmail = :email OR r.ownerEmail IS NULL) "
            + "AND (r.contaId IS NULL OR (a.deleted = 0 AND a.status != 'CONCLUIDA')) "
            + "ORDER BY r.dataHora ASC")
    List<Lembrete> listarAtivosPendentes(String email);

    @Query("SELECT * FROM reminders WHERE ativo = 1 AND notificado = 0 AND deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL "
            + "AND (ownerEmail = :email OR ownerEmail IS NULL))) ORDER BY dataHora ASC")
    List<Lembrete> listarAtivosPendentesPorEscopo(String email, String groupId);

    @Query("SELECT * FROM reminders WHERE deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL "
            + "AND (ownerEmail = :email OR ownerEmail IS NULL)))")
    List<Lembrete> listarTodosPorEscopo(String email, String groupId);

    @Query("SELECT * FROM reminders WHERE groupId = :groupId")
    List<Lembrete> getDoGrupo(String groupId);

    @Query("SELECT * FROM reminders WHERE syncPending = 1 AND groupId = :groupId")
    List<Lembrete> getPendentesDoGrupo(String groupId);

    @Query("SELECT * FROM reminders WHERE remoteId = :remoteId AND groupId = :groupId LIMIT 1")
    Lembrete getByRemoteIdAndGroup(String remoteId, String groupId);

    @Query("DELETE FROM reminders WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE reminders SET deleted = 1, syncPending = 1, updatedAt = :updatedAt "
            + "WHERE id = :id")
    void marcarExcluido(long id, long updatedAt);

    @Query("UPDATE reminders SET notificado = 1, syncPending = 1, updatedAt = :updatedAt "
            + "WHERE id = :id")
    void marcarNotificadoComSync(long id, long updatedAt);

    @Query("DELETE FROM reminders WHERE groupId = :groupId")
    void excluirCacheDoGrupo(String groupId);

    @Query("DELETE FROM reminders WHERE LOWER(ownerEmail) = LOWER(:email) AND groupId IS NULL")
    void excluirPessoaisDoDono(String email);

    @Query("UPDATE reminders SET ownerEmail = :novoEmail WHERE LOWER(ownerEmail) = LOWER(:email)")
    void atualizarEmailDono(String email, String novoEmail);

    @Query("DELETE FROM reminders")
    void excluirTodos();

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    Lembrete buscarPorId(long id);

    @Query("SELECT * FROM reminders WHERE contaId = :contaId")
    List<Lembrete> buscarPorConta(long contaId);

    @Query("DELETE FROM reminders WHERE contaId = :contaId")
    void excluirPorConta(long contaId);

    @Query("UPDATE reminders SET notificado = 1 WHERE id = :id")
    void marcarNotificado(long id);
}
