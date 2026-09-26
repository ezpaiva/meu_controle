package br.com.meu_controle.dados.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import br.com.meu_controle.dados.modelo.Conta;

@Database(entities = {Conta.class}, version = 1, exportSchema = false)
public abstract class BancoDados extends RoomDatabase {

    private static volatile BancoDados instance;

    public abstract ContaDao contaDao();

    public static BancoDados obterInstancia(Context context) {
        if (instance == null) {
            synchronized (BancoDados.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BancoDados.class,
                            "meu_controle.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
