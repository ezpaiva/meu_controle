package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.utilitarios.ErrosAutenticacao;

import java.util.HashMap;
import java.util.Map;

public class CadastroActivity extends AppCompatActivity {

    private static final int TAMANHO_MINIMO_SENHA = 6;

    private EditText editNome;
    private EditText editEmail;
    private EditText editSenha;
    private EditText editConfirmarSenha;
    private RadioGroup grupoTipoPerfil;
    private MaterialButton btnCriarConta;

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
        grupoTipoPerfil = findViewById(R.id.profile_type_group);
        btnCriarConta = findViewById(R.id.btnCriarConta);

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

        int perfilSelecionado = grupoTipoPerfil.getCheckedRadioButtonId();
        if (perfilSelecionado == -1) {
            Toast.makeText(this, R.string.error_profile_type, Toast.LENGTH_SHORT).show();
            return;
        }
        String tipoPerfil = perfilSelecionado == R.id.profile_family
                ? TipoPerfil.FAMILIAR : TipoPerfil.INDIVIDUAL;

        criarContaNoFirebase(nome, email, senha, tipoPerfil);
    }

    private void criarContaNoFirebase(String nome, String email, String senha, String tipoPerfil) {
        btnCriarConta.setEnabled(false);

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        btnCriarConta.setEnabled(true);
                        Toast.makeText(this,
                                ErrosAutenticacao.traduzir(task.getException(), true),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser usuario = task.getResult().getUser();
                    usuario.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(nome).build());
                    salvarPerfilRemoto(usuario, nome, email, tipoPerfil);
                });
    }

    private void salvarPerfilRemoto(FirebaseUser usuario, String nome, String email,
                                    String tipoPerfil) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("email", email);
        dados.put("tipoPerfil", tipoPerfil);

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .set(dados)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Conta criada, mas o perfil não foi salvo na nuvem.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.msg_conta_criada,
                                Toast.LENGTH_SHORT).show();
                    }
                    new SessaoUsuario(this).salvarPerfil(nome, email, tipoPerfil);
                    abrirTelaInicial();
                });
    }

    private void abrirTelaInicial() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}