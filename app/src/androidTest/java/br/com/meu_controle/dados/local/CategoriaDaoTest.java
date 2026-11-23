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

import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CategoriaDaoTest {

    private static final String EMAIL = "user@exemplo.com";
    private static final String GRUPO = "grupo-1";

    private BancoDados db;
    private CategoriaDao categoriaDao;
    private ContaDao contaDao;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, BancoDados.class)
                .allowMainThreadQueries()
                .build();
        categoriaDao = db.categoriaDao();
        contaDao = db.contaDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private Categoria novaCategoria(String nome, String tipo, String email, String groupId) {
        Categoria c = new Categoria(nome, tipo, email);
        c.groupId = groupId;
        c.isPadrao = false;
        c.updatedAt = System.currentTimeMillis();
        return c;
    }

    private Conta novaConta(String tipo, String email, String categoria) {
        Conta c = new Conta();
        c.type = tipo;
        c.description = "Conta de teste";
        c.amountInCents = 1000L;
        c.dueDate = "2026-11-25";
        c.category = categoria;
        c.ownerEmail = email;
        c.groupId = null;
        c.remoteId = java.util.UUID.randomUUID().toString();
        c.updatedAt = System.currentTimeMillis();
        c.syncPending = true;
        return c;
    }

    // ---------- escopo ----------
    @Test
    public void getByTipoAndScope_pessoalNaoVazaCategoriasDeOutros() {
        categoriaDao.insert(novaCategoria("Minha", TipoConta.PAGAR, EMAIL, null));
        categoriaDao.insert(novaCategoria("Alheia", TipoConta.PAGAR, "outro@exemplo.com", null));

        List<Categoria> minhas = categoriaDao.getByTipoAndScope(TipoConta.PAGAR, EMAIL, null);
        assertEquals(1, minhas.size());
        assertEquals("Minha", minhas.get(0).nome);
    }

    @Test
    public void getByTipoAndScope_grupoTrazPadroesECategoriasDoGrupo() {
        Categoria padrao = new Categoria("Alimentação", TipoConta.PAGAR, null);
        padrao.isPadrao = true;
        categoriaDao.insert(padrao);
        categoriaDao.insert(novaCategoria("Do grupo", TipoConta.PAGAR, EMAIL, GRUPO));

        List<Categoria> resultado = categoriaDao.getByTipoAndScope(TipoConta.PAGAR, EMAIL, GRUPO);
        assertEquals(2, resultado.size());
    }

    // ---------- unicidade ----------
    @Test
    public void contarPorNomeETipo_detectaDuplicataNoMesmoEscopo() {
        long id = categoriaDao.insert(novaCategoria("Lazer", TipoConta.PAGAR, EMAIL, null));

        int colisoes = categoriaDao.contarPorNomeETipo(
                TipoConta.PAGAR, "Lazer", -1L, null, EMAIL);
        assertEquals(1, colisoes);

        // Ignorando o próprio id, a contagem zera
        int comIgnore = categoriaDao.contarPorNomeETipo(
                TipoConta.PAGAR, "Lazer", id, null, EMAIL);
        assertEquals(0, comIgnore);
    }

    @Test
    public void contarPorNomeETipo_permiteMesmoNomeEmEscoposDiferentes() {
        categoriaDao.insert(novaCategoria("Lazer", TipoConta.PAGAR, EMAIL, null));
        categoriaDao.insert(novaCategoria("Lazer", TipoConta.PAGAR, EMAIL, GRUPO));

        assertEquals(1, categoriaDao.contarPorNomeETipo(
                TipoConta.PAGAR, "Lazer", -1L, null, EMAIL));
        assertEquals(1, categoriaDao.contarPorNomeETipo(
                TipoConta.PAGAR, "Lazer", -1L, GRUPO, EMAIL));
    }

    // ---------- propagação de renomeação ----------
    @Test
    public void renomearCategoriaEmContas_propagaParaContasDoMesmoEscopo() {
        contaDao.insert(novaConta(TipoConta.PAGAR, EMAIL, "Alimentação"));
        contaDao.insert(novaConta(TipoConta.RECEBER, EMAIL, "Alimentação")); // tipo diferente

        categoriaDao.renomearCategoriaEmContas(
                "Alimentação", "Comida", TipoConta.PAGAR, null, EMAIL,
                System.currentTimeMillis());

        List<Conta> todas = contaDao.getAllByScope(EMAIL, null);
        long pagarRenomeadas = todas.stream()
                .filter(c -> TipoConta.PAGAR.equals(c.type) && "Comida".equals(c.category))
                .count();
        long receberIntactas = todas.stream()
                .filter(c -> TipoConta.RECEBER.equals(c.type) && "Alimentação".equals(c.category))
                .count();
        assertEquals(1, pagarRenomeadas);
        assertEquals(1, receberIntactas);
    }

    @Test
    public void atualizarComPropagacao_renomeiaEAtualiza() {
        Categoria c = novaCategoria("Antiga", TipoConta.PAGAR, EMAIL, null);
        long id = categoriaDao.insert(c);
        contaDao.insert(novaConta(TipoConta.PAGAR, EMAIL, "Antiga"));

        Categoria carregada = categoriaDao.getById(id);
        carregada.nome = "Nova";
        categoriaDao.atualizarComPropagacao(carregada, "Antiga", System.currentTimeMillis());

        List<Conta> contas = contaDao.getAllByScope(EMAIL, null);
        assertEquals("Nova", contas.get(0).category);
    }

    // ---------- soft delete ----------
    @Test
    public void marcarExcluida_removeDasConsultas() {
        long id = categoriaDao.insert(novaCategoria("Temporária", TipoConta.PAGAR, EMAIL, null));

        categoriaDao.marcarExcluida(id, System.currentTimeMillis());

        assertTrue(categoriaDao.getByTipoAndScope(TipoConta.PAGAR, EMAIL, null).isEmpty());
        Categoria bruta = categoriaDao.getById(id);
        assertNotNull(bruta);
        assertTrue(bruta.deleted);
        assertTrue(bruta.syncPending);
    }

    @Test
    public void contarContasUsandoCategoria_detectaUso() {
        categoriaDao.insert(novaCategoria("Lazer", TipoConta.PAGAR, EMAIL, null));
        contaDao.insert(novaConta(TipoConta.PAGAR, EMAIL, "Lazer"));

        int usos = categoriaDao.contarContasUsandoCategoria(
                "Lazer", TipoConta.PAGAR, null, EMAIL);
        assertEquals(1, usos);
    }

    @Test
    public void excluirCacheDoGrupo_naoAfetaPessoais() {
        categoriaDao.insert(novaCategoria("Grupo", TipoConta.PAGAR, EMAIL, GRUPO));
        categoriaDao.insert(novaCategoria("Pessoal", TipoConta.PAGAR, EMAIL, null));

        categoriaDao.excluirCacheDoGrupo(GRUPO);

        List<Categoria> restantes = categoriaDao.getByTipoAndScope(TipoConta.PAGAR, EMAIL, null);
        assertEquals(1, restantes.size());
        assertEquals("Pessoal", restantes.get(0).nome);
    }

    @Test
    public void getDoGrupo_retornaApenasCategoriasDoGrupo() {
        categoriaDao.insert(novaCategoria("A", TipoConta.PAGAR, EMAIL, GRUPO));
        categoriaDao.insert(novaCategoria("B", TipoConta.RECEBER, EMAIL, GRUPO));
        categoriaDao.insert(novaCategoria("C", TipoConta.PAGAR, EMAIL, null));

        List<Categoria> doGrupo = categoriaDao.getDoGrupo(GRUPO);
        assertEquals(2, doGrupo.size());
    }

    @Test
    public void excluirPessoaisDoDono_removeSomenteAsPessoais() {
        categoriaDao.insert(novaCategoria("Pessoal", TipoConta.PAGAR, EMAIL, null));
        categoriaDao.insert(novaCategoria("Do grupo", TipoConta.PAGAR, EMAIL, GRUPO));

        categoriaDao.excluirPessoaisDoDono(EMAIL);

        assertTrue(categoriaDao.getByTipoAndScope(TipoConta.PAGAR, EMAIL, null).isEmpty());
        assertEquals(1, categoriaDao.getDoGrupo(GRUPO).size());
    }
}