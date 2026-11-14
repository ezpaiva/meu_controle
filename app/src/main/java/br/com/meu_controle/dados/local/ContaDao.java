package br.com.meu_controle.dados.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import br.com.meu_controle.dados.modelo.Conta;

import java.util.List;

@Dao
public interface ContaDao {

    @Query("SELECT * FROM accounts WHERE type = :type AND LOWER(ownerEmail) = LOWER(:ownerEmail) "
            + "AND groupId IS NULL AND deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getByTypeAndOwner(String type, String ownerEmail);

    @Query("SELECT * FROM accounts WHERE type = :type AND deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL AND LOWER(ownerEmail) = LOWER(:ownerEmail))) "
            + "ORDER BY dueDate ASC, id DESC")
    List<Conta> getByTypeAndScope(String type, String ownerEmail, String groupId);

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL AND LOWER(ownerEmail) = LOWER(:ownerEmail))) "
            + "ORDER BY dueDate ASC, id DESC")
    List<Conta> getAllByScope(String ownerEmail, String groupId);

    @Query("SELECT * FROM accounts WHERE LOWER(ownerEmail) = LOWER(:ownerEmail) "
            + "AND groupId IS NULL AND deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getAllByOwner(String ownerEmail);

    // USO INTERNO (reconciliação de lembretes). Não usar para listar contas nas telas,
    // porque traz as contas de todos os usuários do aparelho.
    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY dueDate ASC, id DESC")
    List<Conta> getAll();

    // Usado pelo NotificationReceiver (ele mesmo confere se a conta foi excluída)
    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    Conta getById(long id);

    @Insert
    long insert(Conta conta);

    @Update
    void update(Conta conta);

    // Exclusão lógica: a linha fica no banco até a exclusão ser enviada ao servidor
    @Query("UPDATE accounts SET deleted = 1, syncPending = 1, updatedAt = :agora WHERE id = :id")
    void marcarExcluida(long id, long agora);

    // --- Usados pela sincronização ---
    @Query("SELECT * FROM accounts WHERE syncPending = 1")
    List<Conta> getPendentes();

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId LIMIT 1")
    Conta getByRemoteId(String remoteId);

    // Remoção física, só para limpar depois que o servidor confirmou a exclusão
    @Query("DELETE FROM accounts WHERE id = :id")
    void deleteById(long id);

    // Só as pendências do usuário logado
    @Query("SELECT * FROM accounts WHERE syncPending = 1 AND LOWER(ownerEmail) = LOWER(:email)")
    List<Conta> getPendentesDoDono(String email);

    @Query("SELECT * FROM accounts WHERE syncPending = 1 AND groupId = :groupId")
    List<Conta> getPendentesDoGrupo(String groupId);

    @Query("SELECT * FROM accounts WHERE groupId = :groupId")
    List<Conta> getDoGrupo(String groupId);

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId AND groupId = :groupId LIMIT 1")
    Conta getByRemoteIdAndGroup(String remoteId, String groupId);

    @Query("DELETE FROM accounts WHERE groupId = :groupId")
    void excluirCacheDoGrupo(String groupId);

    @Query("DELETE FROM accounts WHERE LOWER(ownerEmail) = LOWER(:email) AND groupId IS NULL")
    void excluirPessoaisDoDono(String email);

    @Query("UPDATE accounts SET ownerEmail = :novoEmail WHERE LOWER(ownerEmail) = LOWER(:email)")
    void atualizarEmailDono(String email, String novoEmail);

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId AND groupId IS NULL LIMIT 1")
    Conta getByPersonalRemoteId(String remoteId);

    // Contas antigas sem dono passam a pertencer a quem sincroniza primeiro (uma única vez)
    @Query("UPDATE accounts SET ownerEmail = :email, syncPending = 1 WHERE ownerEmail IS NULL")
    void adotarContasSemDono(String email);

    // Só considera sincronizada se a conta não mudou durante o envio
    @Query("UPDATE accounts SET syncPending = 0 WHERE id = :id AND updatedAt = :atualizadoEm")
    void marcarSincronizada(long id, long atualizadoEm);
}