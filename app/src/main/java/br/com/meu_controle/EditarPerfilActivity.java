package br.com.meu_controle;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfilActivity";

    private PerfilUsuarioViewModel viewModel;
    private SessaoUsuario sessao;
    private TextInputLayout nomeLayout;
    private TextInputLayout emailLayout;
    private TextInputEditText nomeInput;
    private TextInputEditText emailInput;
    private SwitchMaterial notificacoesSwitch;
    private MaterialButton salvarButton;
    private String tipoPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        viewModel = new ViewModelProvider(this).get(PerfilUsuarioViewModel.class);
        sessao = new SessaoUsuario(this);
        nomeLayout = findViewById(R.id.profile_name_layout);
        emailLayout = findViewById(R.id.profile_email_layout);
        nomeInput = findViewById(R.id.profile_name_input);
        emailInput = findViewById(R.id.profile_email_input);
        notificacoesSwitch = findViewById(R.id.profile_notifications_switch);
        salvarButton = findViewById(R.id.profile_save_button);
        tipoPerfil = sessao.obterTipo();

        MaterialToolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        nomeInput.setText(sessao.obterNome());
        emailInput.setText(sessao.obterEmailContato());
        notificacoesSwitch.setChecked(sessao.isNotificationsEnabled());
        salvarButton.setOnClickListener(v -> salvarPerfil());

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
                        emailInput.setText(perfil.emailContato);
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
        emailLayout.setError(null);

        String nome = nomeInput.getText() == null
                ? "" : nomeInput.getText().toString().trim();
        String emailContato = emailInput.getText() == null
                ? "" : emailInput.getText().toString().trim();

        if (nome.isEmpty()) {
            nomeLayout.setError(getString(R.string.error_nome));
            nomeInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailContato).matches()) {
            emailLayout.setError(getString(R.string.error_email));
            emailInput.requestFocus();
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
}
