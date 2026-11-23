package br.com.meu_controle.dados.local;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.modelo.TipoConta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LembreteDaoTest {

    private static final String EMAIL_A = "a@exemplo.com";
    private static final String EMAIL_B = "b@exemplo.com";
    private static final String GRUPO = "grupo-123";

    private BancoDados db;
    private LembreteDao dao;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, BancoDados.class)
                .allowMainThreadQueries()
                .build();
        dao = db.lembreteDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ---------- helpers ----------

    private Lembrete novaLembrete(String email, String groupId, long dataHora) {
        Lembrete l = new Lembrete();
        l.titulo = "Pagar conta";
        l.mensagem = "Vencimento";
        l.dataHora = dataHora;
        l.data = "2026-11-25";
        l.hora = "09:00";
        l.ativo = true;
        l.criadoEm = System.currentTimeMillis();
        l.notificado = false;
        l.ownerEmail = email;
        l.groupId = groupId;
        l.createdByName = "Você";
        l.remoteId = UUID.randomUUID().toString();
        l.updatedAt = System.currentTimeMillis();
        l.syncPending = true;
        l.deleted = false;
        return l;
    }

    private Conta novaConta(String email, String categoria) {
        Conta c = new Conta();
        c.type = TipoConta.PAGAR;
        c.amountInCents = 1000L;
        c.description = "Conta de teste";
        c.dueDate = "2026-11-25";
        c.category = categoria;
        c.ownerEmail = email;
        c.groupId = null;
        c.remoteId = UUID.randomUUID().toString();
        c.updatedAt = System.currentTimeMillis();
        c.syncPending = true;
        c.deleted = false;
        return c;
    }

    /**
     * Espera o primeiro valor emitido pelo LiveData. observeForever precisa
     * rodar na main thread, por isso usamos runOnMainSync.
     */
    @SuppressWarnings("unchecked")
    private <T> T observar(LiveData<T> liveData) throws InterruptedException {
        final Object[] resultado = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                resultado[0] = t;
                latch.countDown();
            }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(observer));
        if (!latch.await(3, TimeUnit.SECONDS)) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    liveData.removeObserver(observer));
            throw new AssertionError("LiveData nunca emitiu valor.");
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.removeObserver(observer));
        return (T) resultado[0];
    }

    private void marcarDeletado(long id) {
        db.getOpenHelper().getWritableDatabase()
                .execSQL("UPDATE reminders SET deleted = 1 WHERE id = ?",
                        new Object[]{id});
    }

    // ---------- observarTodos ----------

    @Test
    public void observarTodos_filtraPorEmailEGrupoNulo() throws Exception {
        dao.inserir(novaLembrete(EMAIL_A, null, 1000L));
        dao.inserir(novaLembrete(EMAIL_B, null, 2000L));
        dao.inserir(novaLembrete(EMAIL_A, GRUPO, 3000L));

        List<Lembrete> lembretes = observar(dao.observarTodos(EMAIL_A));
        assertEquals(1, lembretes.size());
        assertEquals(EMAIL_A, lembretes.get(0).ownerEmail);
        assertEquals(null, lembretes.get(0).groupId);
    }

    @Test
    public void observarTodos_ignoraDeletados() throws Exception {
        dao.inserir(novaLembrete(EMAIL_A, null, 1000L));
        long idDeletado = dao.inserir(novaLembrete(EMAIL_A, null, 2000L));
        marcarDeletado(idDeletado);

        List<Lembrete> lembretes = observar(dao.observarTodos(EMAIL_A));
        assertEquals(1, lembretes.size());
        assertEquals(1000L, lembretes.get(0).dataHora);
    }

    @Test
    public void observarTodos_ordenaPorDataHoraCrescente() throws Exception {
        dao.inserir(novaLembrete(EMAIL_A, null, 5000L));
        dao.inserir(novaLembrete(EMAIL_A, null, 1000L));
        dao.inserir(novaLembrete(EMAIL_A, null, 3000L));

        List<Lembrete> lembretes = observar(dao.observarTodos(EMAIL_A));
        assertEquals(3, lembretes.size());
        assertEquals(1000L, lembretes.get(0).dataHora);
        assertEquals(3000L, lembretes.get(1).dataHora);
        assertEquals(5000L, lembretes.get(2).dataHora);
    }

    // ---------- observarPorEscopo ----------

    @Test
    public void observarPorEscopo_grupoTrazLembretesDoGrupo() throws Exception {
        dao.inserir(novaLembrete(EMAIL_A, GRUPO, 1000L));
        dao.inserir(novaLembrete(EMAIL_B, GRUPO, 2000L));
        dao.inserir(novaLembrete(EMAIL_A, null, 3000L)); // pessoal, não deve aparecer

        List<Lembrete> doGrupo = observar(dao.observarPorEscopo(EMAIL_A, GRUPO));
        assertEquals(2, doGrupo.size());
        for (Lembrete l : doGrupo) {
            assertEquals(GRUPO, l.groupId);
        }
    }

    @Test
    public void observarPorEscopo_pessoalTrazApenasDoDono() throws Exception {
        dao.inserir(novaLembrete(EMAIL_A, null, 1000L));
        dao.inserir(novaLembrete(EMAIL_B, null, 2000L));
        dao.inserir(novaLembrete(EMAIL_A, GRUPO, 3000L)); // grupo, não deve aparecer

        List<Lembrete> pessoais = observar(dao.observarPorEscopo(EMAIL_A, null));
        assertEquals(1, pessoais.size());
        assertEquals(EMAIL_A, pessoais.get(0).ownerEmail);
    }

    // ---------- observarTodosComConta / join ----------

    @Test
    public void observarTodosComConta_trazDescricaoDaContaAssociada() throws Exception {
        long contaId = db.contaDao().insert(novaConta(EMAIL_A, "Moradia"));

        Lembrete comConta = novaLembrete(EMAIL_A, null, 1000L);
        comConta.contaId = contaId;
        dao.inserir(comConta);

        // Lembrete sem conta: deve continuar aparecendo com contaDescricao nula
        dao.inserir(novaLembrete(EMAIL_A, null, 2000L));

        List<LembreteComConta> resultado = observar(dao.observarTodosComConta(EMAIL_A));
        assertEquals(2, resultado.size());
        assertNotNull(resultado.get(0).contaDescricao);
        assertEquals("Conta de teste", resultado.get(0).contaDescricao);
        assertEquals(null, resultado.get(1).contaDescricao);
    }

    // ---------- inserir / atualizar / excluir ----------

    @Test
    public void inserir_e_atualizar_refletemNaObservacao() throws Exception {
        long id = dao.inserir(novaLembrete(EMAIL_A, null, 1000L));

        // Busca pela observação, altera o título e atualiza
        List<Lembrete> antes = observar(dao.observarTodos(EMAIL_A));
        Lembrete carregado = antes.get(0);
        carregado.titulo = "Título alterado";
        carregado.updatedAt = System.currentTimeMillis() + 1000L;
        dao.atualizar(carregado);

        List<Lembrete> depois = observar(dao.observarTodos(EMAIL_A));
        assertEquals(1, depois.size());
        assertEquals("Título alterado", depois.get(0).titulo);
        assertEquals(id, depois.get(0).id);
    }

    @Test
    public void excluir_fisicoRemoveDefinitivamente() throws Exception {
        long id = dao.inserir(novaLembrete(EMAIL_A, null, 1000L));
        List<Lembrete> antes = observar(dao.observarTodos(EMAIL_A));
        assertEquals(1, antes.size());

        dao.excluir(antes.get(0));

        List<Lembrete> depois = observar(dao.observarTodos(EMAIL_A));
        assertTrue(depois.isEmpty());
        assertEquals(id, antes.get(0).id); // só confirma o id
    }

    // ---------- escopo cruzado ----------

    @Test
    public void observarComContaPorEscopo_naoVazaContaDeOutroUsuario() throws Exception {
        long contaA = db.contaDao().insert(novaConta(EMAIL_A, "Lazer"));
        long contaB = db.contaDao().insert(novaConta(EMAIL_B, "Saúde"));

        Lembrete lembreteA = novaLembrete(EMAIL_A, null, 1000L);
        lembreteA.contaId = contaA;
        dao.inserir(lembreteA);

        Lembrete lembreteB = novaLembrete(EMAIL_B, null, 2000L);
        lembreteB.contaId = contaB;
        dao.inserir(lembreteB);

        List<LembreteComConta> doA = observar(dao.observarComContaPorEscopo(EMAIL_A, null));

        // Se o escopo vazasse, viriam 2 linhas (uma de cada usuário).
        // A asserção de tamanho já prova o isolamento; a contaDescricao
        // confirma que o join está funcionando.
        assertEquals(1, doA.size());
        assertEquals("Conta de teste", doA.get(0).contaDescricao);
    }
}