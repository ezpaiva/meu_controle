package br.com.meu_controle.dados.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import br.com.meu_controle.dados.modelo.Conta;

import java.util.List;

@Dao
public interface ContaDao {

    @Query("SELECT * FROM accounts WHERE type = :type AND deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getByType(String type);

    @Query("SELECT * FROM accounts WHERE type = :type AND (ownerEmail = :ownerEmail OR ownerEmail IS NULL) AND deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getByTypeAndOwner(String type, String ownerEmail);

    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getAll();

    @Query("SELECT * FROM accounts WHERE (ownerEmail = :ownerEmail OR ownerEmail IS NULL) AND deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getAllByOwner(String ownerEmail);

    @Insert
    long insert(Conta conta);

    @Update
    void update(Conta conta);

    // Exclusão lógica: a linha fica no banco até a exclusão ser enviada ao servidor
    @Query("UPDATE accounts SET deleted = 1, syncPending = 1, updatedAt = :agora WHERE id = :id")
    void marcarExcluida(long id, long agora);

    // --- Usados pela sincronização (Etapa 4) ---
    @Query("SELECT * FROM accounts WHERE syncPending = 1")
    List<Conta> getPendentes();

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId LIMIT 1")
    Conta getByRemoteId(String remoteId);

    // Remoção física, só para limpar depois que o servidor confirmou a exclusão
    @Query("DELETE FROM accounts WHERE id = :id")
    void deleteById(long id);

    // Só as pendências do usuário logado (ou sem dono), para não misturar contas
    // de pessoas diferentes que usem o mesmo aparelho
    @Query("SELECT * FROM accounts WHERE syncPending = 1 AND (ownerEmail = :email OR ownerEmail IS NULL)")
    List<Conta> getPendentesDoDono(String email);

    // Só considera sincronizada se a conta não mudou durante o envio
    @Query("UPDATE accounts SET syncPending = 0 WHERE id = :id AND updatedAt = :atualizadoEm")
    void marcarSincronizada(long id, long atualizadoEm);
}