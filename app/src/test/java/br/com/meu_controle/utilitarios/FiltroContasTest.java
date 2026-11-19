package br.com.meu_controle.utilitarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FiltroContasTest {

    private Conta despesa(String descricao, String categoria) {
        return new Conta(descricao, 1000, "2026-12-10", categoria,
                TipoConta.PAGAR, TipoConta.PENDENTE, "user@example.com", "User");
    }

    private List<Conta> exemplo() {
        return Arrays.asList(
                despesa("Mercado", "Alimentação"),
                despesa("Ônibus", "Transporte"),
                despesa("Padaria", "Alimentação"),
                despesa("Sem categoria", null));
    }

    // ---------- porCategoria ----------

    @Test
    public void categoriaNulaDevolveTodasAsContas() {
        assertEquals(4, FiltroContas.porCategoria(exemplo(), null).size());
    }

    @Test
    public void opcaoTodasCategoriasDevolveTodasAsContas() {
        assertEquals(4, FiltroContas.porCategoria(exemplo(), FiltroContas.TODAS_CATEGORIAS).size());
    }

    @Test
    public void filtraSomenteAsContasDaCategoriaEscolhida() {
        List<Conta> resultado = FiltroContas.porCategoria(exemplo(), "Alimentação");

        assertEquals(2, resultado.size());
        assertEquals("Mercado", resultado.get(0).description);
        assertEquals("Padaria", resultado.get(1).description);
    }

    @Test
    public void categoriaSemContasDevolveListaVazia() {
        assertTrue(FiltroContas.porCategoria(exemplo(), "Lazer").isEmpty());
    }

    @Test
    public void contaSemCategoriaNaoCasaComNenhumFiltro() {
        List<Conta> resultado = FiltroContas.porCategoria(exemplo(), "Transporte");

        assertEquals(1, resultado.size());
        assertEquals("Ônibus", resultado.get(0).description);
    }

    @Test
    public void naoAlteraAListaOriginal() {
        List<Conta> original = new ArrayList<>(exemplo());
        FiltroContas.porCategoria(original, "Alimentação");

        assertEquals(4, original.size());
    }

    // ---------- opcoesDeCategoria ----------

    @Test
    public void primeiraOpcaoETodasCategorias() {
        List<String> opcoes = FiltroContas.opcoesDeCategoria(
                Collections.emptyList(), Collections.emptyList());

        assertEquals(Collections.singletonList(FiltroContas.TODAS_CATEGORIAS), opcoes);
    }

    @Test
    public void mantemAOrdemDasCategoriasCadastradas() {
        List<String> opcoes = FiltroContas.opcoesDeCategoria(
                Arrays.asList("Moradia", "Alimentação", "Saúde"), Collections.emptyList());

        assertEquals(Arrays.asList("Todas categorias", "Moradia", "Alimentação", "Saúde"), opcoes);
    }

    @Test
    public void naoRepeteCategoriasCadastradas() {
        List<String> opcoes = FiltroContas.opcoesDeCategoria(
                Arrays.asList("Moradia", "Moradia"), Collections.emptyList());

        assertEquals(Arrays.asList("Todas categorias", "Moradia"), opcoes);
    }

    @Test
    public void incluiCategoriasUsadasPorContasMasNaoCadastradas() {
        List<Conta> contas = Arrays.asList(
                despesa("A", "Zebra"), despesa("B", "abelha"), despesa("C", "Moradia"));

        List<String> opcoes = FiltroContas.opcoesDeCategoria(
                Collections.singletonList("Moradia"), contas);

        // Moradia já está cadastrada; as outras entram ordenadas sem diferenciar caixa
        assertEquals(Arrays.asList("Todas categorias", "Moradia", "abelha", "Zebra"), opcoes);
    }

    @Test
    public void ignoraCategoriasNulasEmBrancoEMantemSoUmaVezCadaUma() {
        List<Conta> contas = Arrays.asList(
                despesa("A", null), despesa("B", "  "), despesa("C", "Lazer"), despesa("D", "Lazer"));

        List<String> opcoes = FiltroContas.opcoesDeCategoria(
                Arrays.asList(null, "", "Saúde"), contas);

        assertEquals(Arrays.asList("Todas categorias", "Saúde", "Lazer"), opcoes);
    }
}