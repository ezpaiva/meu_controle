package br.com.meu_controle.dados.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class ContaTest {

    private Conta novaConta() {
        return new Conta("Internet", 10000, "2026-12-10", "Moradia",
                TipoConta.PAGAR, TipoConta.PENDENTE, "user@example.com", "User");
    }

    @Test
    public void construtorGuardaOsCamposInformados() {
        Conta conta = novaConta();

        assertEquals("Internet", conta.description);
        assertEquals(10000L, conta.amountInCents);
        assertEquals("2026-12-10", conta.dueDate);
        assertEquals("Moradia", conta.category);
        assertEquals(TipoConta.PAGAR, conta.type);
        assertEquals(TipoConta.PENDENTE, conta.status);
        assertEquals("user@example.com", conta.ownerEmail);
        assertEquals("User", conta.responsibleName);
    }

    @Test
    public void contaNovaNasceComoPendenteDeSincronizacaoENaoExcluida() {
        Conta conta = novaConta();

        assertTrue(conta.syncPending);
        assertFalse(conta.deleted);
        assertNull(conta.groupId);
    }

    @Test
    public void remoteIdEhUmUuidValido() {
        Conta conta = novaConta();

        assertNotNull(conta.remoteId);
        assertEquals(conta.remoteId, UUID.fromString(conta.remoteId).toString());
    }

    @Test
    public void cadaContaTemUmRemoteIdDiferente() {
        assertNotEquals(novaConta().remoteId, novaConta().remoteId);
    }

    @Test
    public void updatedAtEhOMomentoDaCriacao() {
        long antes = System.currentTimeMillis();
        Conta conta = novaConta();
        long depois = System.currentTimeMillis();

        assertTrue(conta.updatedAt >= antes && conta.updatedAt <= depois);
    }

    // A Conta viaja entre telas (Intent) como Serializable
    @Test
    public void serializacaoPreservaTodosOsCampos() throws Exception {
        Conta original = novaConta();
        original.id = 42;
        original.groupId = "grupo-1";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream saida = new ObjectOutputStream(bytes)) {
            saida.writeObject(original);
        }
        Conta copia;
        try (ObjectInputStream entrada =
                     new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copia = (Conta) entrada.readObject();
        }

        assertEquals(42L, copia.id);
        assertEquals(original.description, copia.description);
        assertEquals(original.amountInCents, copia.amountInCents);
        assertEquals(original.dueDate, copia.dueDate);
        assertEquals(original.ownerEmail, copia.ownerEmail);
        assertEquals(original.remoteId, copia.remoteId);
        assertEquals(original.updatedAt, copia.updatedAt);
        assertEquals("grupo-1", copia.groupId);
        assertTrue(copia.syncPending);
        assertFalse(copia.deleted);
    }
}