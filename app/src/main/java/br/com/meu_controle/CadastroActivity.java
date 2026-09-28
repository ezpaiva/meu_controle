package br.com.meu_controle;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class CadastroActivity extends AppCompatActivity {

    private static final int TAMANHO_MINIMO_SENHA = 6;

    private EditText editNome;
    private EditText editEmail;
    private EditText editSenha;
    private EditText editConfirmarSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        // Respeita a barra de status / navegação (edge-to-edge)
        View rootView = findViewById(R.id.rootView);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha);
        MaterialButton btnCriarConta = findViewById(R.id.btnCriarConta);

        // "← Voltar" e "Já tenho uma conta" retornam para o Login
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.linkJaTenhoConta).setOnClickListener(v -> finish());

        btnCriarConta.setOnClickListener(v -> tentarCadastro());
    }

    private void tentarCadastro() {
        String nome = editNome.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String senha = editSenha.getText().toString();
        String confirmar = editConfirmarSenha.getText().toString();

        editNome.setError(null);
        editEmail.setError(null);
        editSenha.setError(null);
        editConfirmarSenha.setError(null);

        if (nome.isEmpty()) {
            editNome.setError(getString(R.string.error_nome));
            editNome.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError(getString(R.string.error_email));
            editEmail.requestFocus();
            return;
        }

        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            editSenha.setError(getString(R.string.error_senha_curta));
            editSenha.requestFocus();
            return;
        }

        if (!senha.equals(confirmar)) {
            editConfirmarSenha.setError(getString(R.string.error_senhas_diferentes));
            editConfirmarSenha.requestFocus();
            return;
        }

        // TODO: salvar o usuário no banco local (Room). Nunca guarde a senha em texto puro.
        Toast.makeText(this, R.string.msg_conta_criada, Toast.LENGTH_SHORT).show();
        finish(); // volta para o Login
    }
}
