package br.com.meu_controle.dados.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import br.com.meu_controle.dados.modelo.Conta;

import java.util.List;

@Dao
public interface ContaDao {

    @Query("SELECT * FROM accounts WHERE type = :type ORDER BY dueDate ASC, id DESC")
    List<Conta> getByType(String type);

    @Insert
    long insert(Conta conta);
}
