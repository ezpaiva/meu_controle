package br.com.meu_controle.dados.repositorio;

import org.junit.Test;

import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositorioLembretesTest {

    @Test
    public void everyAccountWithAValidDueDateCanHaveAReminder() {
        Conta payable = new Conta("Internet", 10000, "2026-12-10", "Moradia",
                TipoConta.PAGAR, TipoConta.PENDENTE, "user@example.com", "User");
        assertTrue(RepositorioLembretes.deveGerarLembretes(payable));

        payable.status = TipoConta.CONCLUIDA;
        assertTrue(RepositorioLembretes.deveGerarLembretes(payable));

        payable.type = TipoConta.RECEBER;
        assertTrue(RepositorioLembretes.deveGerarLembretes(payable));

        payable.type = TipoConta.PAGAR;
        payable.dueDate = "data-invalida";
        assertFalse(RepositorioLembretes.deveGerarLembretes(payable));
    }
}
