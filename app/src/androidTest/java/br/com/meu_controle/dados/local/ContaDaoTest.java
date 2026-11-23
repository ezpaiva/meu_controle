package br.com.meu_controle.dados.local;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ContaDaoTest {

    private static final String EMAIL_A = "a@exemplo.com";
    private static final String EMAIL_B = "b@exemplo.com";
    private static final String GRUPO = "grupo-123";

    private BancoDados db;
    private ContaDao dao;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, BancoDados.class)
                .allowMainThreadQueries()
                .build();
        dao = db.contaDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ---------- helpers ----------
    private Conta novaConta(String tipo, String email, String groupId) {
        Conta c = new Conta();
        c.type = tipo;
        c.description = "Aluguel";
        c.amountInCents = 150_000L;
        c.dueDate = "2026-11-25";
        c.category = "Moradia";
        c.ownerEmail = email;
        c.groupId = groupId;
        c.remoteId = UUID.randomUUID().toString();
        c.updatedAt = System.currentTimeMillis();
        c.syncPending = true;
        c.deleted = false;
        return c;
    }

    // ---------- escopo ----------
    @Test
    public void getByTypeAndScope_naoVazaContasDeOutroUsuario() {
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null));
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_B, null));

        List<Conta> soA = dao.getByTypeAndScope(TipoConta.PAGAR, EMAIL_A, null);
        assertEquals(1, soA.size());
        assertEquals(EMAIL_A, soA.get(0).ownerEmail);
    }

    @Test
    public void getByTypeAndScope_filtraPorGrupo() {
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, GRUPO));
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null));

        List<Conta> grupo = dao.getByTypeAndScope(TipoConta.PAGAR, EMAIL_A, GRUPO);
        assertEquals(1, grupo.size());
        assertEquals(GRUPO, grupo.get(0).groupId);

        List<Conta> pessoal = dao.getByTypeAndScope(TipoConta.PAGAR, EMAIL_A, null);
        assertEquals(1, pessoal.size());
        assertNull(pessoal.get(0).groupId);
    }

    @Test
    public void getAllByScope_respeitaEscopoDoGrupo() {
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, GRUPO));
        dao.insert(novaConta(TipoConta.RECEBER, EMAIL_B, GRUPO));
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null));

        List<Conta> grupo = dao.getAllByScope(EMAIL_A, GRUPO);
        assertEquals(2, grupo.size());

        List<Conta> pessoal = dao.getAllByScope(EMAIL_A, null);
        assertEquals(1, pessoal.size());
    }

    // ---------- soft delete ----------
    @Test
    public void marcarExcluida_removeDasConsultasMasMantemLinha() {
        long id = dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null));

        dao.marcarExcluida(id, System.currentTimeMillis());

        assertTrue(dao.getByTypeAndScope(TipoConta.PAGAR, EMAIL_A, null).isEmpty());
        assertNotNull("Linha precisa sobreviver para a sincronização", dao.getById(id));
        assertTrue(dao.getById(id).deleted);
        assertTrue(dao.getById(id).syncPending);
    }

    // ---------- concorrência ----------
    @Test
    public void marcarSincronizada_ignoraSeContaMudouDuranteEnvio() {
        Conta c = novaConta(TipoConta.PAGAR, EMAIL_A, null);
        c.updatedAt = 1000L;
        long id = dao.insert(c);

        // Simula edição durante o envio
        Conta atualizada = dao.getById(id);
        atualizada.description = "Editada durante o envio";
        atualizada.updatedAt = 2000L;
        dao.update(atualizada);

        // Sincronizador tenta marcar usando o updatedAt antigo
        dao.marcarSincronizada(id, 1000L);

        Conta depois = dao.getById(id);
        assertTrue("Não deve marcar como sincronizada uma edição mais nova",
                depois.syncPending);
    }

    @Test
    public void marcarSincronizada_funcionaQuandoNaoHouveEdicao() {
        Conta c = novaConta(TipoConta.PAGAR, EMAIL_A, null);
        c.updatedAt = 5000L;
        long id = dao.insert(c);

        dao.marcarSincronizada(id, 5000L);

        Conta depois = dao.getById(id);
        org.junit.Assert.assertFalse(depois.syncPending);
    }

    // ---------- adoção e remoteId ----------
    @Test
    public void adotarContasSemDono_atribuiPrimeiroEmailESincroniza() {
        Conta orfa = novaConta(TipoConta.PAGAR, null, null);
        long id = dao.insert(orfa);

        dao.adotarContasSemDono(EMAIL_A);

        Conta depois = dao.getById(id);
        assertEquals(EMAIL_A, depois.ownerEmail);
        assertTrue(depois.syncPending);
    }

    @Test
    public void getByPersonalRemoteId_ignoraContasDeGrupo() {
        String remote = "remote-xyz";

        Conta pessoal = novaConta(TipoConta.PAGAR, EMAIL_A, null);
        pessoal.remoteId = remote;
        dao.insert(pessoal);

        Conta doGrupo = novaConta(TipoConta.PAGAR, EMAIL_A, GRUPO);
        doGrupo.remoteId = remote;
        dao.insert(doGrupo);

        Conta encontrada = dao.getByPersonalRemoteId(remote);
        assertNotNull(encontrada);
        assertNull(encontrada.groupId);
    }

    @Test
    public void getPendentesDoDono_trazApenasPendenciasDoUsuario() {
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null)); // pendente
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_B, null)); // pendente de outro
        Conta sincronizada = novaConta(TipoConta.RECEBER, EMAIL_A, null);
        sincronizada.syncPending = false;
        dao.insert(sincronizada);

        List<Conta> pendentes = dao.getPendentesDoDono(EMAIL_A);
        assertEquals(1, pendentes.size());
        assertEquals(EMAIL_A, pendentes.get(0).ownerEmail);
    }

    @Test
    public void excluirCacheDoGrupo_naoAfetaContasPessoais() {
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, GRUPO));
        dao.insert(novaConta(TipoConta.PAGAR, EMAIL_A, null));

        dao.excluirCacheDoGrupo(GRUPO);

        assertTrue(dao.getAllByScope(EMAIL_A, GRUPO).isEmpty());
        assertEquals(1, dao.getAllByScope(EMAIL_A, null).size());
    }
}