package br.com.meu_controle.dados.repositorio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;

public class RepositorioLembretesValidacaoTest {

    private Conta contaComVencimento(String dueDate) {
        return new Conta("Aluguel", 150000, dueDate, "Moradia",
                TipoConta.PAGAR, TipoConta.PENDENTE, "user@example.com", "User");
    }

    @Test
    public void contaNulaNaoGeraLembrete() {
        assertFalse(RepositorioLembretes.deveGerarLembretes(null));
    }

    @Test
    public void vencimentoNuloOuEmBrancoNaoGeraLembrete() {
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento(null)));
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("")));
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("   ")));
    }

    @Test
    public void vencimentoForaDoFormatoIsoNaoGeraLembrete() {
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("10/12/2026")));
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("amanhã")));
    }

    @Test
    public void vencimentoComDiaInexistenteNaoGeraLembrete() {
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("2026-02-30")));
        assertFalse(RepositorioLembretes.deveGerarLembretes(contaComVencimento("2027-02-29")));
    }

    @Test
    public void vencimentoEmAnoBissextoValidoGeraLembrete() {
        assertTrue(RepositorioLembretes.deveGerarLembretes(contaComVencimento("2028-02-29")));
    }

    @Test
    public void receitaEDespesaGeramLembreteDoMesmoJeito() {
        Conta conta = contaComVencimento("2026-12-10");

        conta.type = TipoConta.RECEBER;
        assertTrue(RepositorioLembretes.deveGerarLembretes(conta));

        conta.type = TipoConta.PAGAR;
        assertTrue(RepositorioLembretes.deveGerarLembretes(conta));
    }
}