package br.com.meu_controle.dados.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import br.com.meu_controle.dados.modelo.Conta;

@Database(entities = {Conta.class}, version = 2, exportSchema = false)
public abstract class BancoDados extends RoomDatabase {

    private static volatile BancoDados instance;

    private static final Migration MIGRACAO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE accounts ADD COLUMN ownerEmail TEXT");
            database.execSQL("ALTER TABLE accounts ADD COLUMN responsibleName TEXT");
        }
    };

    public abstract ContaDao contaDao();

    public static BancoDados obterInstancia(Context context) {
        if (instance == null) {
            synchronized (BancoDados.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BancoDados.class,
                            "meu_controle.db"
                    ).addMigrations(MIGRACAO_1_2).build();
                }
            }
        }
        return instance;
    }
}
