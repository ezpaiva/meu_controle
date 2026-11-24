package br.com.meu_controle;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditarPerfilNavigationTest {

    private SharedPreferences preferencias;
    private String tipoPerfilAnterior;

    @Before
    public void prepararPerfilIndividual() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferencias = context.getSharedPreferences("sessao_usuario", Context.MODE_PRIVATE);
        tipoPerfilAnterior = preferencias.getString("tipo_perfil", null);
        preferencias.edit().putString("tipo_perfil", "INDIVIDUAL").commit();
    }

    @After
    public void restaurarPerfil() {
        SharedPreferences.Editor editor = preferencias.edit();
        if (tipoPerfilAnterior == null) {
            editor.remove("tipo_perfil");
        } else {
            editor.putString("tipo_perfil", tipoPerfilAnterior);
        }
        editor.commit();
    }

    @Test
    public void abreEdicaoRetornaAoPerfilEExibeNavegacaoAcessivel() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<PerfilActivity> scenario =
                     ActivityScenario.launch(PerfilActivity.class)) {
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                    EditarPerfilActivity.class.getName(), null, false);
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.btn_editar_perfil).isShown());
                BottomNavigationView navigation =
                        activity.findViewById(R.id.bottom_navigation);
                assertTrue(navigation.isShown());
                assertEquals(5, navigation.getMenu().size());
                assertEquals(R.id.nav_perfil, navigation.getSelectedItemId());
                for (int indice = 0; indice < navigation.getMenu().size(); indice++) {
                    assertNotNull(navigation.getMenu().getItem(indice).getContentDescription());
                }
                assertTrue(navigation.getHeight()
                        >= Math.round(80 * activity.getResources().getDisplayMetrics().density));
                for (int indice = 0; indice < navigation.getMenu().size(); indice++) {
                    assertEquals(null, navigation.getMenu().getItem(indice).getIcon());
                }
                activity.findViewById(R.id.btn_editar_perfil).performClick();
            });

            Activity editorActivity = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            instrumentation.removeMonitor(monitor);
            assertNotNull(editorActivity);
            instrumentation.runOnMainSync(() -> {
                assertTrue(editorActivity.findViewById(R.id.profile_name_input).isShown());
                assertTrue(editorActivity.findViewById(R.id.profile_save_button).isShown());
                assertTrue(editorActivity.findViewById(R.id.profile_change_email_button).isShown());
                assertTrue(editorActivity.findViewById(R.id.profile_delete_account_button).isShown());
                MaterialToolbar toolbar = editorActivity.findViewById(R.id.edit_profile_toolbar);
                assertNotNull(toolbar.getNavigationIcon());
                assertNotNull(toolbar.getNavigationContentDescription());
                View botaoVoltar = null;
                for (int indice = 0; indice < toolbar.getChildCount(); indice++) {
                    View filho = toolbar.getChildAt(indice);
                    CharSequence descricao = filho.getContentDescription();
                    if (descricao != null && editorActivity.getString(R.string.profile_back)
                            .contentEquals(descricao)) {
                        botaoVoltar = filho;
                        break;
                    }
                }
                assertNotNull(botaoVoltar);
                botaoVoltar.performClick();
            });

            instrumentation.waitForIdleSync();
            scenario.onActivity(activity ->
                    assertTrue(activity.findViewById(R.id.profile_name).isShown()));
        }
    }
}
