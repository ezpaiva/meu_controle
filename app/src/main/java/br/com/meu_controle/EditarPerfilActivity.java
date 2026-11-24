package br.com.meu_controle;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.dados.repositorio.PerfilUsuarioRepository;
import br.com.meu_controle.viewmodels.PerfilUsuarioViewModel;
import br.com.meu_controle.viewmodels.ContaUsuarioViewModel;
import br.com.meu_controle.dados.repositorio.ContaUsuarioRepository;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfilActivity";

    private PerfilUsuarioViewModel viewModel;
    private ContaUsuarioViewModel contaUsuarioViewModel;
    private SessaoUsuario sessao;
    private TextInputLayout nomeLayout;
    private TextInputEditText nomeInput;
    private SwitchMaterial notificacoesSwitch;
    private MaterialButton salvarButton;
    private String tipoPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        viewModel = new ViewModelProvider(this).get(PerfilUsuarioViewModel.class);
        contaUsuarioViewModel = new ViewModelProvider(this).get(ContaUsuarioViewModel.class);
        sessao = new SessaoUsuario(this);
        nomeLayout = findViewById(R.id.profile_name_layout);
        nomeInput = findViewById(R.id.profile_name_input);
        notificacoesSwitch = findViewById(R.id.profile_notifications_switch);
        salvarButton = findViewById(R.id.profile_save_button);
        tipoPerfil = sessao.obterTipo();

        MaterialToolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        nomeInput.setText(sessao.obterNome());
        notificacoesSwitch.setChecked(sessao.isNotificationsEnabled());
        salvarButton.setOnClickListener(v -> salvarPerfil());
        findViewById(R.id.profile_change_email_button).setOnClickListener(v ->
                mostrarDialogAlterarEmail());
        findViewById(R.id.profile_delete_account_button).setOnClickListener(v ->
                confirmarExclusaoConta());

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            carregarPerfil(usuario);
        } else {
            Toast.makeText(this, R.string.profile_load_local, Toast.LENGTH_LONG).show();
        }
    }

    private void carregarPerfil(FirebaseUser usuario) {
        viewModel.carregar(usuario.getUid(),
                usuario.getDisplayName() == null ? sessao.obterNome() : usuario.getDisplayName(),
                usuario.getEmail(),
                new PerfilUsuarioRepository.RetornoPerfil() {
                    @Override
                    public void onSuccess(br.com.meu_controle.dados.modelo.Usuario perfil) {
                        nomeInput.setText(perfil.nome);
                        notificacoesSwitch.setChecked(perfil.notificacoesHabilitadas);
                        tipoPerfil = perfil.perfil;
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Falha ao carregar o perfil remoto.", error);
                        Toast.makeText(EditarPerfilActivity.this,
                                R.string.profile_load_local, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void salvarPerfil() {
        nomeLayout.setError(null);
        String nome = nomeInput.getText() == null
                ? "" : nomeInput.getText().toString().trim();
        String emailContato = sessao.obterEmailContato();

        if (nome.isEmpty()) {
            nomeLayout.setError(getString(R.string.error_nome));
            nomeInput.requestFocus();
            return;
        }
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.profile_session_expired, Toast.LENGTH_LONG).show();
            return;
        }

        salvarButton.setEnabled(false);
        salvarButton.setText(R.string.profile_saving);
        String perfil = tipoPerfil == null ? TipoPerfil.INDIVIDUAL : tipoPerfil;
        viewModel.salvarEdicao(usuario.getUid(), nome, emailContato, perfil,
                notificacoesSwitch.isChecked(),
                new PerfilUsuarioRepository.RetornoPerfil() {
                    @Override
                    public void onSuccess(br.com.meu_controle.dados.modelo.Usuario resultado) {
                        setResult(RESULT_OK);
                        Toast.makeText(EditarPerfilActivity.this,
                                R.string.profile_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Falha ao salvar o perfil.", error);
                        salvarButton.setEnabled(true);
                        salvarButton.setText(R.string.profile_save);
                        Toast.makeText(EditarPerfilActivity.this,
                                R.string.profile_save_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarDialogAlterarEmail() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        int margem = Math.round(24 * getResources().getDisplayMetrics().density);
        campos.setPadding(margem, 0, margem, 0);
        EditText novoEmail = new EditText(this);
        novoEmail.setSingleLine(true);
        novoEmail.setHint(R.string.change_email_new);
        novoEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText senha = new EditText(this);
        senha.setSingleLine(true);
        senha.setHint(R.string.change_email_password);
        senha.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campos.addView(novoEmail);
        campos.addView(senha);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_login_email)
                .setView(campos)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String email = novoEmail.getText().toString().trim();
                    String password = senha.getText().toString();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        novoEmail.setError(getString(R.string.error_email));
                        return;
                    }
                    if (password.isEmpty()) {
                        senha.setError(getString(R.string.change_email_password));
                        return;
                    }
                    dialog.dismiss();
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.change_email_confirm_title)
                            .setMessage(R.string.change_email_confirm_message)
                            .setPositiveButton(R.string.action_confirm,
                                    (confirmacao, which) -> alterarEmail(email, password))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }));
        dialog.show();
    }

    private void alterarEmail(String novoEmail, String senha) {
        contaUsuarioViewModel.alterarEmail(novoEmail, senha,
                new ContaUsuarioRepository.Retorno() {
                    @Override
                    public void onSuccess() {
                        FirebaseAuth.getInstance().signOut();
                        sessao.sair();
                        Toast.makeText(EditarPerfilActivity.this,
                                R.string.change_email_success, Toast.LENGTH_LONG).show();
                        abrirLogin();
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(EditarPerfilActivity.this,
                                error.getMessage() == null
                                        ? getString(R.string.change_email_error)
                                        : error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmarExclusaoConta() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(R.string.delete_account_confirm_message)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> pedirConfirmacaoFinalExclusao())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void pedirConfirmacaoFinalExclusao() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        int margem = Math.round(24 * getResources().getDisplayMetrics().density);
        campos.setPadding(margem, 0, margem, 0);
        EditText palavra = new EditText(this);
        palavra.setHint(R.string.delete_account_type_word);
        palavra.setSingleLine(true);
        EditText senha = new EditText(this);
        senha.setHint(R.string.delete_account_password);
        senha.setSingleLine(true);
        senha.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campos.addView(palavra);
        campos.addView(senha);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_confirm_title)
                .setView(campos)
                .setPositiveButton(R.string.delete_account, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (!"EXCLUIR".equals(palavra.getText().toString().trim())) {
                        palavra.setError(getString(R.string.delete_account_type_word));
                        return;
                    }
                    if (senha.getText().toString().isEmpty()) {
                        senha.setError(getString(R.string.delete_account_password));
                        return;
                    }
                    dialog.dismiss();
                    excluirConta(senha.getText().toString());
                }));
        dialog.show();
    }

    private void excluirConta(String senha) {
        contaUsuarioViewModel.excluirConta(senha, new ContaUsuarioRepository.Retorno() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditarPerfilActivity.this,
                        R.string.delete_account_success, Toast.LENGTH_LONG).show();
                abrirLogin();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(EditarPerfilActivity.this,
                        error.getMessage() == null
                                ? getString(R.string.delete_account_error)
                                : error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirLogin() {
        android.content.Intent intent = new android.content.Intent(
                this, LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
