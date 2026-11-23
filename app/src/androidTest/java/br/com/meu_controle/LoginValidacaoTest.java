package br.com.meu_controle;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class LoginValidacaoTest {

    @Before
    public void limparSessao() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences("sessao_usuario", Context.MODE_PRIVATE)
                .edit().clear().commit();
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {
            // Firebase indisponível neste ambiente: o auto-redirect simplesmente não ocorre
        }
    }

    private void preencher(String email, String senha) {
        onView(withId(R.id.editEmail)).perform(replaceText(email), closeSoftKeyboard());
        onView(withId(R.id.editSenha)).perform(replaceText(senha), closeSoftKeyboard());
    }

    @Test
    public void emailInvalido_exibeErroNoCampoEmail() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            preencher("nao-e-email", "senha123");
            onView(withId(R.id.btnEntrar)).perform(click());
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editEmail)).getError()));
        }
    }

    @Test
    public void senhaCurta_exibeErroNoCampoSenha() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            preencher("teste@exemplo.com", "123");
            onView(withId(R.id.btnEntrar)).perform(click());
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editSenha)).getError()));
        }
    }

    @Test
    public void emailVazio_exibeErroNoCampoEmail() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            preencher("", "senha123");
            onView(withId(R.id.btnEntrar)).perform(click());
            scenario.onActivity(a -> assertNotNull(
                    ((EditText) a.findViewById(R.id.editEmail)).getError()));
        }
    }
}