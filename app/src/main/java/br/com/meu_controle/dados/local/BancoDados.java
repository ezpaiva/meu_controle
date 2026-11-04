package br.com.meu_controle.dados.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;

@Database(entities = {Conta.class, Categoria.class}, version = 4, exportSchema = false)
public abstract class BancoDados extends RoomDatabase {

    private static volatile BancoDados instance;

    private static final Migration MIGRACAO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE accounts ADD COLUMN ownerEmail TEXT");
            database.execSQL("ALTER TABLE accounts ADD COLUMN responsibleName TEXT");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_type_dueDate "
                    + "ON accounts(type, dueDate)");
        }
    };

    private static final Migration MIGRACAO_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE accounts ADD COLUMN remoteId TEXT");
            database.execSQL("ALTER TABLE accounts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE accounts ADD COLUMN syncPending INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE accounts ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            // Contas que já existiam ganham identidade e entram na fila de envio
            database.execSQL("UPDATE accounts SET "
                    + "remoteId = lower(hex(randomblob(16))), "
                    + "updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000, "
                    + "syncPending = 1");
        }
    };

    private static final Migration MIGRACAO_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS categorias ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "nome TEXT, "
                    + "tipo TEXT, "
                    + "ownerEmail TEXT, "
                    + "isPadrao INTEGER NOT NULL DEFAULT 0, "
                    + "updatedAt INTEGER NOT NULL DEFAULT 0, "
                    + "syncPending INTEGER NOT NULL DEFAULT 0, "
                    + "deleted INTEGER NOT NULL DEFAULT 0)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_categorias_tipo "
                    + "ON categorias(tipo)");
            semearCategoriasPadrao(database);
        }
    };

    private static void semearCategoriasPadrao(SupportSQLiteDatabase db) {
        String[] despesas = {"Alimentação", "Transporte", "Moradia", "Saúde",
                "Lazer", "Educação", "Outros"};
        String[] receitas = {"Salário", "Freelance", "Investimentos", "Vendas", "Outros"};
        long agora = System.currentTimeMillis();

        for (String nome : despesas) {
            db.execSQL("INSERT INTO categorias "
                            + "(nome, tipo, ownerEmail, isPadrao, updatedAt, syncPending, deleted) "
                            + "VALUES (?, 'PAGAR', NULL, 1, ?, 0, 0)",
                    new Object[]{nome, agora});
        }
        for (String nome : receitas) {
            db.execSQL("INSERT INTO categorias "
                            + "(nome, tipo, ownerEmail, isPadrao, updatedAt, syncPending, deleted) "
                            + "VALUES (?, 'RECEBER', NULL, 1, ?, 0, 0)",
                    new Object[]{nome, agora});
        }
    }

    public abstract ContaDao contaDao();
    public abstract CategoriaDao categoriaDao();

    public static BancoDados obterInstancia(Context context) {
        if (instance == null) {
            synchronized (BancoDados.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BancoDados.class,
                                    "meu_controle.db")
                            .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    semearCategoriasPadrao(db);
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
    }
}