package br.com.meu_controle;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.utilitarios.ErrosAutenticacao;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editSenha;
    private MaterialButton btnEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Login automático: sessão do Firebase ativa e perfil já salvo no aparelho
        if (FirebaseAuth.getInstance().getCurrentUser() != null
                && !new SessaoUsuario(this).obterEmail().isEmpty()) {
            abrirTelaInicial();
            return;
        }

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
        btnEntrar = findViewById(R.id.btnEntrar);
        View linkCriarConta = findViewById(R.id.linkCriarConta);
        findViewById(R.id.linkEsqueciSenha).setOnClickListener(v -> recuperarSenha());

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

        btnEntrar.setEnabled(false);

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        btnEntrar.setEnabled(true);
                        Toast.makeText(this,
                                ErrosAutenticacao.traduzir(task.getException(), false),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    carregarPerfilEEntrar(task.getResult().getUser(), email);
                });
    }

    // Recupera nome e tipo de perfil do Firestore para o aparelho atual
    private void carregarPerfilEEntrar(FirebaseUser usuario, String email) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    String nome = usuario.getDisplayName() != null
                            ? usuario.getDisplayName() : "Você";
                    String tipo = TipoPerfil.INDIVIDUAL;

                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().exists()) {
                        String nomeRemoto = task.getResult().getString("nome");
                        String tipoRemoto = task.getResult().getString("tipoPerfil");
                        if (nomeRemoto != null) nome = nomeRemoto;
                        if (tipoRemoto != null) tipo = tipoRemoto;
                    }

                    new SessaoUsuario(this).salvarPerfil(nome, email, tipo);
                    abrirTelaInicial();
                });
    }

    private void abrirTelaInicial() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void recuperarSenha() {
        String email = editEmail.getText().toString().trim();
        editEmail.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Digite seu e-mail para recuperar a senha");
            editEmail.requestFocus();
            return;
        }

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Se existir uma conta com este e-mail, enviamos um link para redefinir a senha. Verifique também o spam.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                ErrosAutenticacao.traduzir(task.getException(), true),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}