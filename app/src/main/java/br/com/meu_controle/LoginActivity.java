package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.SessaoUsuario;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Respeita a barra de status / navegação (edge-to-edge)
        View rootView = findViewById(R.id.rootView);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        MaterialButton btnEntrar = findViewById(R.id.btnEntrar);
        View linkCriarConta = findViewById(R.id.linkCriarConta);

        btnEntrar.setOnClickListener(v -> tentarLogin());

        linkCriarConta.setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class)));
    }

    private void tentarLogin() {
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString();

        editEmail.setError(null);
        editSenha.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError(getString(R.string.error_email));
            editEmail.requestFocus();
            return;
        }

        if (senha.length() < 6) {
            editSenha.setError(getString(R.string.error_senha_curta));
            editSenha.requestFocus();
            return;
        }

        // A autenticação remota será conectada posteriormente; o perfil local já
        // permite manter o isolamento e a experiência offline do protótipo.
        new SessaoUsuario(this).garantirPerfil(email);
        abrirTelaInicial();
    }

    private void abrirTelaInicial() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}