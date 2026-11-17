package br.com.meu_controle.utilitarios;

import static org.junit.Assert.assertEquals;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import org.junit.Test;

public class ErrosAutenticacaoTest {

    private static final String SEM_REDE =
            "Sem conexão com a internet. Verifique sua rede e tente novamente.";
    private static final String EMAIL_EM_USO = "Já existe uma conta com este e-mail.";
    private static final String SENHA_FRACA = "Senha muito fraca. Use pelo menos 6 caracteres.";
    private static final String EMAIL_INVALIDO = "E-mail inválido.";
    private static final String CREDENCIAIS = "E-mail ou senha incorretos.";
    private static final String MUITAS_TENTATIVAS =
            "Muitas tentativas. Aguarde um pouco e tente de novo.";
    private static final String GENERICO = "Não foi possível concluir. Tente novamente.";

    @Test
    public void semInternetNoLoginENoCadastro() {
        Exception erro = new FirebaseNetworkException("rede");
        assertEquals(SEM_REDE, ErrosAutenticacao.traduzir(erro, false));
        assertEquals(SEM_REDE, ErrosAutenticacao.traduzir(erro, true));
    }

    @Test
    public void emailJaCadastrado() {
        Exception erro = new FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "em uso");
        assertEquals(EMAIL_EM_USO, ErrosAutenticacao.traduzir(erro, true));
    }

    // WeakPassword é subclasse de InvalidCredentials: a ordem dos "if" importa
    @Test
    public void senhaFracaTemPrioridadeSobreCredenciaisInvalidas() {
        Exception erro = new FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "fraca", "curta");
        assertEquals(SENHA_FRACA, ErrosAutenticacao.traduzir(erro, true));
        assertEquals(SENHA_FRACA, ErrosAutenticacao.traduzir(erro, false));
    }

    @Test
    public void credenciaisInvalidasMudamDeMensagemEntreLoginECadastro() {
        Exception erro = new FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "ruim");
        assertEquals(CREDENCIAIS, ErrosAutenticacao.traduzir(erro, false));
        assertEquals(EMAIL_INVALIDO, ErrosAutenticacao.traduzir(erro, true));
    }

    @Test
    public void usuarioInexistenteSeguePadraoDeCredenciais() {
        Exception erro = new FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "sem usuário");
        assertEquals(CREDENCIAIS, ErrosAutenticacao.traduzir(erro, false));
        assertEquals(EMAIL_INVALIDO, ErrosAutenticacao.traduzir(erro, true));
    }

    @Test
    public void excessoDeTentativas() {
        Exception erro = new FirebaseTooManyRequestsException("muitas");
        assertEquals(MUITAS_TENTATIVAS, ErrosAutenticacao.traduzir(erro, false));
    }

    @Test
    public void erroDesconhecidoOuNuloViraMensagemGenerica() {
        assertEquals(GENERICO, ErrosAutenticacao.traduzir(new RuntimeException("x"), false));
        assertEquals(GENERICO, ErrosAutenticacao.traduzir(null, true));
    }
}