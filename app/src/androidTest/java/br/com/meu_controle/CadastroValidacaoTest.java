package br.com.meu_controle;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class CadastroValidacaoTest {

    private void preencher(String nome, String email, String senha, String confirmar) {
        onView(withId(R.id.editNome)).perform(replaceText(nome), closeSoftKeyboard());
        onView(withId(R.id.editEmail)).perform(replaceText(email), closeSoftKeyboard());
        onView(withId(R.id.editSenha)).perform(replaceText(senha), closeSoftKeyboard());
        onView(withId(R.id.editConfirmarSenha)).perform(replaceText(confirmar), closeSoftKeyboard());
    }

    private void clicarCriarConta() {
        onView(withId(R.id.profile_family)).perform(click());
        onView(withId(R.id.btnCriarConta)).perform(click());
    }

    @Test
    public void nomeVazio_exibeErroNoCampoNome() {
        try (ActivityScenario<CadastroActivity> scenario =
                     ActivityScenario.launch(CadastroActivity.class)) {
            preencher("", "teste@exemplo.com", "senha123", "senha123");
            clicarCriarConta();
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editNome)).getError()));
        }
    }

    @Test
    public void emailInvalido_exibeErroNoCampoEmail() {
        try (ActivityScenario<CadastroActivity> scenario =
                     ActivityScenario.launch(CadastroActivity.class)) {
            preencher("Fulano", "nao-e-email", "senha123", "senha123");
            clicarCriarConta();
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editEmail)).getError()));
        }
    }

    @Test
    public void senhaCurta_exibeErroNoCampoSenha() {
        try (ActivityScenario<CadastroActivity> scenario =
                     ActivityScenario.launch(CadastroActivity.class)) {
            preencher("Fulano", "teste@exemplo.com", "abc", "abc");
            clicarCriarConta();
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editSenha)).getError()));
        }
    }

    @Test
    public void senhasDiferentes_exibeErroNoCampoConfirmar() {
        try (ActivityScenario<CadastroActivity> scenario =
                     ActivityScenario.launch(CadastroActivity.class)) {
            preencher("Fulano", "teste@exemplo.com", "senha123", "outra456");
            clicarCriarConta();
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editConfirmarSenha)).getError()));
        }
    }

    @Test
    public void formularioValido_naoExibeErrosNosCampos() {
        try (ActivityScenario<CadastroActivity> scenario =
                     ActivityScenario.launch(CadastroActivity.class)) {
            preencher("Fulano", "teste@exemplo.com", "senha123", "senha123");
            // Não clicamos no botão para não disparar Firebase.
            scenario.onActivity(a -> {
                assertNull(((EditText) a.findViewById(R.id.editNome)).getError());
                assertNull(((EditText) a.findViewById(R.id.editEmail)).getError());
                assertNull(((EditText) a.findViewById(R.id.editSenha)).getError());
                assertNull(((EditText) a.findViewById(R.id.editConfirmarSenha)).getError());
            });
        }
    }
}