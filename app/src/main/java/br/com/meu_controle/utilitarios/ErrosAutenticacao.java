package br.com.meu_controle.utilitarios;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public final class ErrosAutenticacao {

    private ErrosAutenticacao() {
    }

    public static String traduzir(Exception erro, boolean cadastro) {
        if (erro instanceof FirebaseNetworkException) {
            return "Sem conexão com a internet. Verifique sua rede e tente novamente.";
        }
        if (erro instanceof FirebaseAuthUserCollisionException) {
            return "Já existe uma conta com este e-mail.";
        }
        if (erro instanceof FirebaseAuthWeakPasswordException) {
            return "Senha muito fraca. Use pelo menos 6 caracteres.";
        }
        if (erro instanceof FirebaseAuthInvalidCredentialsException
                || erro instanceof FirebaseAuthInvalidUserException) {
            return cadastro ? "E-mail inválido." : "E-mail ou senha incorretos.";
        }
        if (erro instanceof FirebaseTooManyRequestsException) {
            return "Muitas tentativas. Aguarde um pouco e tente de novo.";
        }
        return "Não foi possível concluir. Tente novamente.";
    }
}