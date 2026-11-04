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

    @Query("SELECT * FROM categorias WHERE tipo = :tipo AND deleted = 0 "
            + "ORDER BY isPadrao DESC, nome ASC")
    List<Categoria> getByTipo(String tipo);

    @Query("SELECT * FROM categorias WHERE tipo = :tipo "
            + "AND (ownerEmail = :ownerEmail OR ownerEmail IS NULL) AND deleted = 0 "
            + "ORDER BY isPadrao DESC, nome ASC")
    List<Categoria> getByTipoAndOwner(String tipo, String ownerEmail);

    @Query("SELECT * FROM categorias WHERE deleted = 0 "
            + "ORDER BY tipo ASC, isPadrao DESC, nome ASC")
    List<Categoria> getAll();

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    Categoria getById(long id);

    @Query("SELECT COUNT(*) FROM categorias WHERE tipo = :tipo AND nome = :nome "
            + "AND id != :ignoreId AND deleted = 0")
    int contarPorNomeETipo(String tipo, String nome, long ignoreId);

    @Query("SELECT COUNT(*) FROM accounts WHERE category = :nome AND type = :tipo "
            + "AND deleted = 0")
    int contarContasUsandoCategoria(String nome, String tipo);

    @Insert
    long insert(Categoria categoria);

    @Update
    void update(Categoria categoria);

    @Query("UPDATE categorias SET deleted = 1, syncPending = 1, updatedAt = :agora "
            + "WHERE id = :id")
    void marcarExcluida(long id, long agora);

    @Query("UPDATE accounts SET category = :novoNome, syncPending = 1, updatedAt = :agora "
            + "WHERE category = :nomeAntigo AND type = :tipo AND deleted = 0")
    void renomearCategoriaEmContas(String nomeAntigo, String novoNome, String tipo, long agora);

    @Transaction
    default void atualizarComPropagacao(Categoria categoria, String nomeAntigo, long agora) {
        if (!nomeAntigo.equals(categoria.nome)) {
            renomearCategoriaEmContas(nomeAntigo, categoria.nome, categoria.tipo, agora);
        }
        update(categoria);
    }
}