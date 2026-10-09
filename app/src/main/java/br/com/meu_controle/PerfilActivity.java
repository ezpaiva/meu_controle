package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import br.com.meu_controle.dados.local.SessaoUsuario;

public class PerfilActivity extends AppCompatActivity {
    private SessaoUsuario sessao;
    private LinearLayout listaMembros;
    private EditText campoConvite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        sessao = new SessaoUsuario(this);
        ((TextView) findViewById(R.id.profile_name)).setText(sessao.obterNome());
        ((TextView) findViewById(R.id.profile_email)).setText(sessao.obterEmail());
        ((TextView) findViewById(R.id.profile_type)).setText(sessao.perfilFamiliar()
                ? R.string.profile_family : R.string.profile_individual);
        ((TextView) findViewById(R.id.profile_description)).setText(sessao.perfilFamiliar()
                ? R.string.profile_family_description : R.string.profile_individual_description);

        View grupoFamiliar = findViewById(R.id.family_group);
        grupoFamiliar.setVisibility(sessao.perfilFamiliar() ? View.VISIBLE : View.GONE);
        listaMembros = findViewById(R.id.family_members);
        campoConvite = findViewById(R.id.invite_email);
        MaterialButton botaoConvidar = findViewById(R.id.invite_button);
        botaoConvidar.setOnClickListener(view -> convidarMembro());
        findViewById(R.id.profile_back).setOnClickListener(view -> finish());
        findViewById(R.id.logout_button).setOnClickListener(view -> sair());
        atualizarMembros();
    }

    private void convidarMembro() {
        String email = campoConvite.getText().toString().trim().toLowerCase();
        campoConvite.setError(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoConvite.setError(getString(R.string.error_email));
            return;
        }
        if (email.equalsIgnoreCase(sessao.obterEmail())) {
            campoConvite.setError(getString(R.string.invite_own_email));
            return;
        }
        if (!sessao.adicionarMembro(email)) {
            campoConvite.setError(getString(R.string.invite_duplicate));
            return;
        }
        campoConvite.setText("");
        atualizarMembros();
        Toast.makeText(this, R.string.invite_success, Toast.LENGTH_SHORT).show();
    }

    private void atualizarMembros() {
        listaMembros.removeAllViews();
        adicionarLinhaMembro(getString(R.string.profile_you, sessao.obterNome()));
        for (String membro : sessao.obterMembros()) {
            adicionarLinhaMembro(membro);
        }
    }

    private void adicionarLinhaMembro(String membro) {
        TextView linha = new TextView(this);
        linha.setText(membro);
        linha.setTextColor(getColor(R.color.text_primary));
        linha.setTextSize(14);
        int espaco = Math.round(12 * getResources().getDisplayMetrics().density);
        linha.setPadding(0, espaco, 0, espaco);
        listaMembros.addView(linha);
    }

    private void sair() {
        sessao.sair();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
