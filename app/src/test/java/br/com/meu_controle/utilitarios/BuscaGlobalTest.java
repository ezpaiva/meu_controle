package br.com.meu_controle.utilitarios;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.modelo.TipoConta;

import static org.junit.Assert.assertEquals;

public class BuscaGlobalTest {
    @Test
    public void buscaContasIgnoraMaiusculas() {
        Conta conta = new Conta("Internet fibra", 100,
                "2026-11-23", "Moradia", TipoConta.PAGAR,
                TipoConta.PENDENTE, "a@b.com", "A");
        List<Conta> resultado = BuscaGlobal.filtrarContas(Arrays.asList(conta), "INTERNET");
        assertEquals(1, resultado.size());
    }

    @Test
    public void buscaContasPorCategoria() {
        Conta conta = new Conta("Plano", 100,
                "2026-11-23", "Tecnologia", TipoConta.PAGAR,
                TipoConta.PENDENTE, "a@b.com", "A");
        assertEquals(1, BuscaGlobal.filtrarContas(Arrays.asList(conta), "tec").size());
    }

    @Test
    public void buscaCategoriasPorNome() {
        Categoria categoria = new Categoria("Internet", TipoConta.PAGAR, "a@b.com");
        assertEquals(1, BuscaGlobal.filtrarCategorias(
                Arrays.asList(categoria), "net").size());
    }

    @Test
    public void buscaLembretesPorTituloEMensagem() {
        Lembrete lembrete = new Lembrete("Pagar Internet", "Conta de fibra",
                System.currentTimeMillis() + 100000, true, null,
                System.currentTimeMillis(), false, "a@b.com");
        LembreteComConta item = new LembreteComConta();
        item.lembrete = lembrete;
        item.contaDescricao = "Plano residencial";
        assertEquals(1, BuscaGlobal.filtrarLembretes(
                Arrays.asList(item), "fibra").size());
    }
}
