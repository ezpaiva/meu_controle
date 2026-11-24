package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.ConviteFamiliar;
import br.com.meu_controle.dados.modelo.GrupoFamiliar;
import br.com.meu_controle.dados.modelo.Usuario;
import br.com.meu_controle.dados.remoto.SincronizadorContas;
import br.com.meu_controle.dados.repositorio.GrupoFamiliarRepository;
import br.com.meu_controle.dados.repositorio.PerfilUsuarioRepository;
import br.com.meu_controle.utilitarios.ErrosAutenticacao;
import br.com.meu_controle.viewmodels.PerfilUsuarioViewModel;
import br.com.meu_controle.viewmodels.GrupoFamiliarViewModel;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText editEmail;
    private EditText editSenha;
    private MaterialButton btnEntrar;
    private PerfilUsuarioViewModel perfilViewModel;
    private GrupoFamiliarViewModel grupoViewModel;
    private boolean verificandoConvites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        perfilViewModel = new ViewModelProvider(this).get(PerfilUsuarioViewModel.class);
        grupoViewModel = new ViewModelProvider(this).get(GrupoFamiliarViewModel.class);

        FirebaseUser sessaoAtual = FirebaseAuth.getInstance().getCurrentUser();
        if (sessaoAtual != null && !new SessaoUsuario(this).obterEmail().isEmpty()) {
            carregarPerfilEEntrar(sessaoAtual, sessaoAtual.getEmail());
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

    private void carregarPerfilEEntrar(FirebaseUser usuario, String email) {
        String nome = usuario.getDisplayName() == null ? "Você" : usuario.getDisplayName();
        perfilViewModel.carregar(usuario.getUid(), nome, email,
                new PerfilUsuarioRepository.RetornoPerfil() {
                    @Override
                    public void onSuccess(br.com.meu_controle.dados.modelo.Usuario resultado) {
                        abrirTelaInicial();
                    }

                    @Override
                    public void onError(Exception error) {
                        if (btnEntrar != null) btnEntrar.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.load_error), Toast.LENGTH_LONG).show();
                        if (btnEntrar == null
                                && !new SessaoUsuario(LoginActivity.this)
                                .obterEmail().isEmpty()) {
                            abrirTelaInicial();
                        }
                    }
                });
    }

    private void abrirTelaInicial() {
        if (verificandoConvites) return;
        verificandoConvites = true;
        grupoViewModel.listarConvites(
                new GrupoFamiliarRepository.RetornoConvites() {
                    @Override
                    public void onSuccess(java.util.List<ConviteFamiliar> convites) {
                        processarConvites(convites, 0);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Não foi possível verificar convites familiares.", error);
                        Toast.makeText(LoginActivity.this, R.string.family_invite_load_error,
                                Toast.LENGTH_LONG).show();
                        navegarParaInicio();
                    }
                });
    }

    private void processarConvites(java.util.List<ConviteFamiliar> convites, int indice) {
        if (indice >= convites.size()) {
            SincronizadorContas.sincronizar();
            navegarParaInicio();
            return;
        }
        ConviteFamiliar convite = convites.get(indice);
        String remetente = convite.emailOrigem == null
                ? getString(R.string.grupo_familiar_title) : convite.emailOrigem;
        String grupo = convite.grupoNome == null
                ? getString(R.string.grupo_familiar_title) : convite.grupoNome;
        new AlertDialog.Builder(this)
                .setTitle(R.string.family_invite_pending_title)
                .setMessage(getString(R.string.family_invite_pending_message, remetente, grupo))
                .setPositiveButton(R.string.family_invite_accept, (dialog, which) ->
                        grupoViewModel.aceitarConvite(
                                convite, new GrupoFamiliarRepository.RetornoGrupo() {
                                    @Override
                                    public void onSuccess(GrupoFamiliar group,
                                                          java.util.List<Usuario> members) {
                                        Toast.makeText(LoginActivity.this,
                                                R.string.family_invite_accepted,
                                                Toast.LENGTH_SHORT).show();
                                        processarConvites(convites, indice + 1);
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        Log.e(TAG, "Falha ao aceitar convite familiar.", error);
                                        Toast.makeText(LoginActivity.this,
                                                error.getMessage() == null
                                                        ? getString(R.string.family_invite_error)
                                                        : error.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        processarConvites(convites, indice + 1);
                                    }
                                }))
                .setNegativeButton(R.string.family_invite_decline, (dialog, which) ->
                        grupoViewModel.recusarConvite(
                                convite,
                                () -> processarConvites(convites, indice + 1),
                                error -> {
                                    Log.e(TAG, "Falha ao recusar convite familiar.", error);
                                    Toast.makeText(LoginActivity.this,
                                            R.string.family_invite_error,
                                            Toast.LENGTH_LONG).show();
                                    processarConvites(convites, indice + 1);
                                }))
                .setOnCancelListener(dialog -> navegarParaInicio())
                .show();
    }

    private void navegarParaInicio() {
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