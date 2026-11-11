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
import br.com.meu_controle.dados.modelo.Lembrete;

@Database(entities = {Conta.class, Categoria.class, Lembrete.class}, version = 7, exportSchema = false)
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

    private static final Migration MIGRACAO_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS reminders ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "titulo TEXT, "
                    + "mensagem TEXT, "
                    + "dataHora INTEGER NOT NULL, "
                    + "ativo INTEGER NOT NULL, "
                    + "contaId INTEGER, "
                    + "criadoEm INTEGER NOT NULL, "
                    + "notificado INTEGER NOT NULL, "
                    + "ownerEmail TEXT, "
                    + "FOREIGN KEY(contaId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_contaId ON reminders(contaId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dataHora ON reminders(dataHora)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_ownerEmail ON reminders(ownerEmail)");
        }
    };

    private static final Migration MIGRACAO_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM reminders WHERE contaId IS NOT NULL "
                    + "AND id NOT IN (SELECT MIN(id) FROM reminders "
                    + "WHERE contaId IS NOT NULL GROUP BY contaId)");
            database.execSQL("CREATE TABLE reminders_new ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "titulo TEXT NOT NULL, "
                    + "mensagem TEXT NOT NULL, "
                    + "dataHora INTEGER NOT NULL, "
                    + "ativo INTEGER NOT NULL, "
                    + "contaId INTEGER, "
                    + "criadoEm INTEGER NOT NULL, "
                    + "notificado INTEGER NOT NULL, "
                    + "ownerEmail TEXT, "
                    + "FOREIGN KEY(contaId) REFERENCES accounts(id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("INSERT INTO reminders_new "
                    + "(id, titulo, mensagem, dataHora, ativo, contaId, criadoEm, notificado, ownerEmail) "
                    + "SELECT id, COALESCE(titulo, 'Lembrete'), COALESCE(mensagem, ''), "
                    + "dataHora, ativo, contaId, criadoEm, notificado, ownerEmail FROM reminders");
            database.execSQL("DROP TABLE reminders");
            database.execSQL("ALTER TABLE reminders_new RENAME TO reminders");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_contaId "
                    + "ON reminders(contaId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dataHora "
                    + "ON reminders(dataHora)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_ownerEmail "
                    + "ON reminders(ownerEmail)");
        }
    };

    private static final Migration MIGRACAO_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM reminders WHERE contaId IS NOT NULL "
                    + "AND id NOT IN (SELECT MIN(id) FROM reminders "
                    + "WHERE contaId IS NOT NULL GROUP BY contaId)");
            database.execSQL("CREATE TABLE reminders_new ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "titulo TEXT NOT NULL, "
                    + "mensagem TEXT NOT NULL, "
                    + "dataHora INTEGER NOT NULL, "
                    + "data TEXT NOT NULL, "
                    + "hora TEXT NOT NULL, "
                    + "ativo INTEGER NOT NULL, "
                    + "contaId INTEGER, "
                    + "criadoEm INTEGER NOT NULL, "
                    + "notificado INTEGER NOT NULL, "
                    + "ownerEmail TEXT, "
                    + "FOREIGN KEY(contaId) REFERENCES accounts(id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("INSERT INTO reminders_new "
                    + "(id, titulo, mensagem, dataHora, data, hora, ativo, contaId, "
                    + "criadoEm, notificado, ownerEmail) "
                    + "SELECT id, COALESCE(titulo, 'Lembrete'), COALESCE(mensagem, ''), dataHora, "
                    + "COALESCE(strftime('%Y-%m-%d', dataHora / 1000, 'unixepoch', 'localtime'), "
                    + "'1970-01-01'), "
                    + "COALESCE(strftime('%H:%M', dataHora / 1000, 'unixepoch', 'localtime'), "
                    + "'09:00'), "
                    + "ativo, contaId, criadoEm, notificado, ownerEmail FROM reminders");
            database.execSQL("DROP TABLE reminders");
            database.execSQL("ALTER TABLE reminders_new RENAME TO reminders");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_contaId "
                    + "ON reminders(contaId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dataHora "
                    + "ON reminders(dataHora)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_ownerEmail "
                    + "ON reminders(ownerEmail)");
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
    public abstract LembreteDao lembreteDao();

    public static BancoDados obterInstancia(Context context) {
        if (instance == null) {
            synchronized (BancoDados.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BancoDados.class,
                                    "meu_controle.db")
                            .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4,
                                    MIGRACAO_4_5, MIGRACAO_5_6, MIGRACAO_6_7)
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