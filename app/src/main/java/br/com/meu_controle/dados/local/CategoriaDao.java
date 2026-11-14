package br.com.meu_controle.dados.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import br.com.meu_controle.dados.modelo.Categoria;

import java.util.List;

@Dao
public interface CategoriaDao {

    @Query("SELECT * FROM categorias WHERE tipo = :tipo AND deleted = 0 AND groupId IS NULL "
            + "ORDER BY isPadrao DESC, nome ASC")
    List<Categoria> getByTipo(String tipo);

    @Query("SELECT * FROM categorias WHERE tipo = :tipo "
            + "AND groupId IS NULL AND (ownerEmail = :ownerEmail OR ownerEmail IS NULL) AND deleted = 0 "
            + "ORDER BY isPadrao DESC, nome ASC")
    List<Categoria> getByTipoAndOwner(String tipo, String ownerEmail);

    @Query("SELECT * FROM categorias WHERE tipo = :tipo AND deleted = 0 AND "
            + "((:groupId IS NOT NULL AND (groupId = :groupId OR isPadrao = 1)) OR "
            + "(:groupId IS NULL AND groupId IS NULL "
            + "AND (LOWER(ownerEmail) = LOWER(:ownerEmail) OR ownerEmail IS NULL))) "
            + "ORDER BY isPadrao DESC, nome ASC")
    List<Categoria> getByTipoAndScope(String tipo, String ownerEmail, String groupId);

    @Query("SELECT * FROM categorias WHERE deleted = 0 "
            + "ORDER BY tipo ASC, isPadrao DESC, nome ASC")
    List<Categoria> getAll();

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    Categoria getById(long id);

    @Query("SELECT * FROM categorias WHERE groupId = :groupId")
    List<Categoria> getDoGrupo(String groupId);

    @Query("SELECT * FROM categorias WHERE remoteId = :remoteId AND groupId = :groupId LIMIT 1")
    Categoria getByRemoteIdAndGroup(String remoteId, String groupId);

    @Query("SELECT * FROM categorias WHERE syncPending = 1 AND groupId = :groupId")
    List<Categoria> getPendentesDoGrupo(String groupId);

    @Query("DELETE FROM categorias WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE categorias SET syncPending = 0 WHERE id = :id AND updatedAt = :updatedAt")
    void marcarSincronizada(long id, long updatedAt);

    @Query("DELETE FROM categorias WHERE groupId = :groupId")
    void excluirCacheDoGrupo(String groupId);

    @Query("DELETE FROM categorias WHERE LOWER(ownerEmail) = LOWER(:email) AND groupId IS NULL")
    void excluirPessoaisDoDono(String email);

    @Query("UPDATE categorias SET ownerEmail = :novoEmail WHERE LOWER(ownerEmail) = LOWER(:email)")
    void atualizarEmailDono(String email, String novoEmail);

    @Query("SELECT COUNT(*) FROM categorias WHERE tipo = :tipo AND nome = :nome "
            + "AND id != :ignoreId AND deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL AND ownerEmail = :ownerEmail))")
    int contarPorNomeETipo(String tipo, String nome, long ignoreId,
                           String groupId, String ownerEmail);

    @Query("SELECT COUNT(*) FROM accounts WHERE category = :nome AND type = :tipo "
            + "AND deleted = 0 AND ((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL AND ownerEmail = :ownerEmail))")
    int contarContasUsandoCategoria(String nome, String tipo, String groupId, String ownerEmail);

    @Insert
    long insert(Categoria categoria);

    @Update
    void update(Categoria categoria);

    @Query("UPDATE categorias SET deleted = 1, syncPending = 1, updatedAt = :agora "
            + "WHERE id = :id")
    void marcarExcluida(long id, long agora);

    @Query("UPDATE accounts SET category = :novoNome, syncPending = 1, updatedAt = :agora "
            + "WHERE category = :nomeAntigo AND type = :tipo AND deleted = 0 AND "
            + "((:groupId IS NOT NULL AND groupId = :groupId) OR "
            + "(:groupId IS NULL AND groupId IS NULL AND ownerEmail = :ownerEmail))")
    void renomearCategoriaEmContas(String nomeAntigo, String novoNome, String tipo,
                                   String groupId, String ownerEmail, long agora);

    @Transaction
    default void atualizarComPropagacao(Categoria categoria, String nomeAntigo, long agora) {
        if (!nomeAntigo.equals(categoria.nome)) {
            renomearCategoriaEmContas(nomeAntigo, categoria.nome, categoria.tipo,
                    categoria.groupId, categoria.ownerEmail, agora);
        }
        update(categoria);
    }
}