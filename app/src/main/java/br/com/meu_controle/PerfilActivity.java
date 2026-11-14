package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;
import android.view.Gravity;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.GrupoFamiliar;
import br.com.meu_controle.dados.modelo.Usuario;
import br.com.meu_controle.dados.repositorio.ContaUsuarioRepository;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.utilitarios.NavegacaoInferior;
import br.com.meu_controle.viewmodels.ContaUsuarioViewModel;
import br.com.meu_controle.viewmodels.GrupoFamiliarViewModel;

public class PerfilActivity extends AppCompatActivity {
    private SessaoUsuario sessao;
    private LinearLayout listaMembros;
    private RepositorioLembretes repositorioLembretes;
    private GrupoFamiliarViewModel grupoViewModel;
    private ContaUsuarioViewModel contaUsuarioViewModel;
    private MaterialButton adicionarMembro;
    private MaterialButton sairGrupo;
    private TextView nomeGrupo;
    private TextView quantidadeMembros;
    private SwitchMaterial notificacoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        sessao = new SessaoUsuario(this);
        repositorioLembretes = new RepositorioLembretes(getApplicationContext());
        grupoViewModel = new ViewModelProvider(this).get(GrupoFamiliarViewModel.class);
        contaUsuarioViewModel = new ViewModelProvider(this)
                .get(ContaUsuarioViewModel.class);

        // Preenche os dados
        ((TextView) findViewById(R.id.profile_space_name)).setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        ((TextView) findViewById(R.id.profile_space_detail)).setText(sessao.perfilFamiliar()
                ? sessao.obterMembros().size() + " membros" : "Privado");
        ((TextView) findViewById(R.id.profile_name)).setText(sessao.obterNome());
        ((TextView) findViewById(R.id.profile_email)).setText(sessao.obterEmailContato());
        ((TextView) findViewById(R.id.profile_type)).setText(sessao.perfilFamiliar() ? "Perfil Familiar" : "Perfil Individual");
        ((TextView) findViewById(R.id.profile_description)).setText(sessao.perfilFamiliar() ?
                "Você gerencia movimentações em conjunto com os membros da Família." :
                "Suas receitas, despesas, categorias e lembretes são privados.");

        // Controle de visibilidade entre os perfis
        View secaoFamiliar = findViewById(R.id.family_group_section);
        View secaoIndividual = findViewById(R.id.individual_settings_section);

        if (sessao.perfilFamiliar()) {
            secaoFamiliar.setVisibility(View.VISIBLE);
            secaoIndividual.setVisibility(View.GONE);
            listaMembros = findViewById(R.id.family_members_list);
            adicionarMembro = findViewById(R.id.btn_invite_family);
            sairGrupo = findViewById(R.id.btn_leave_family);
            nomeGrupo = findViewById(R.id.family_group_name);
            quantidadeMembros = findViewById(R.id.family_member_count);
            adicionarMembro.setText(R.string.family_add_member);
            adicionarMembro.setContentDescription(getString(R.string.family_add_accessibility));
            adicionarMembro.setOnClickListener(v -> mostrarDialogAdicionarMembro());
            sairGrupo.setOnClickListener(v -> confirmarSaidaGrupo());
            observarGrupo();
        } else {
            secaoFamiliar.setVisibility(View.GONE);
            secaoIndividual.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.logout_button).setOnClickListener(view -> sair());

        findViewById(R.id.row_gerenciar_categorias).setOnClickListener(v -> {
            if (sessao.perfilFamiliar()
                    && (sessao.obterGrupoId() == null
                    || sessao.obterGrupoId().trim().isEmpty())) {
                Toast.makeText(this, R.string.category_requires_group,
                        Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(new Intent(this, GerenciarCategoriasActivity.class));
        });
        notificacoes = findViewById(R.id.notifications_switch);
        notificacoes.setChecked(sessao.isNotificationsEnabled());
        notificacoes.setOnCheckedChangeListener((button, habilitadas) -> {
            sessao.setNotificationsEnabled(habilitadas);
            if (habilitadas) {
                repositorioLembretes.reagendarPendentes(
                        sessao.obterEmail(), sessao.obterGrupoId());
            } else {
                repositorioLembretes.cancelarTodos(
                        sessao.obterEmail(), sessao.obterGrupoId());
            }
        });

        findViewById(R.id.btn_editar_perfil).setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfilActivity.class)));
        findViewById(R.id.btn_change_login_email).setOnClickListener(v ->
                mostrarDialogAlterarEmail());
        findViewById(R.id.btn_delete_account).setOnClickListener(v ->
                confirmarExclusaoConta());
        configurarNavegacaoInferior();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessao != null) {
            ((TextView) findViewById(R.id.profile_name)).setText(sessao.obterNome());
            ((TextView) findViewById(R.id.profile_email)).setText(sessao.obterEmailContato());
            if (notificacoes != null) {
                notificacoes.setChecked(sessao.isNotificationsEnabled());
            }
        }
        if (sessao != null && sessao.perfilFamiliar()) {
            grupoViewModel.carregarGrupo();
        }
    }

    private void observarGrupo() {
        grupoViewModel.observarGrupo().observe(this, grupo -> {
            boolean semGrupo = grupo == null;
            if (semGrupo && !sessao.perfilFamiliar()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            adicionarMembro.setVisibility(!semGrupo && ehCriadorDoGrupo(grupo)
                    ? View.VISIBLE : View.GONE);
            sairGrupo.setVisibility(semGrupo ? View.GONE : View.VISIBLE);
            nomeGrupo.setText(semGrupo
                    ? getString(R.string.family_first_account_group_hint)
                    : getString(R.string.family_group_name, grupo.nome));
            if (grupo != null) {
                ((TextView) findViewById(R.id.profile_space_name)).setText(grupo.nome);
            } else {
                ((TextView) findViewById(R.id.profile_space_name))
                        .setText(R.string.perfil_familiar_title);
            }
        });
        grupoViewModel.observarMembros().observe(this, membros -> {
            int total = membros == null ? 0 : membros.size();
            quantidadeMembros.setText(getString(R.string.family_member_count, total));
            renderizarMembros(membros == null ? java.util.Collections.emptyList() : membros);
        });
        grupoViewModel.observarCarregando().observe(this, carregando -> {
            boolean habilitado = !Boolean.TRUE.equals(carregando);
            if (adicionarMembro != null) adicionarMembro.setEnabled(habilitado);
            if (sairGrupo != null) sairGrupo.setEnabled(habilitado);
        });
        grupoViewModel.observarErro().observe(this, erro -> {
            if (erro != null && !erro.isEmpty()) {
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });
        grupoViewModel.observarMensagem().observe(this, mensagem -> {
            if (mensagem != null && !mensagem.isEmpty()) {
                Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderizarMembros(java.util.List<Usuario> membros) {
        listaMembros.removeAllViews();
        for (Usuario membro : membros) {
            adicionarLinhaMembro(membro);
        }
    }

    private void adicionarLinhaMembro(Usuario membro) {
        LinearLayout linha = new LinearLayout(this);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setPadding(0, 12, 0, 12);
        LinearLayout dados = new LinearLayout(this);
        dados.setOrientation(LinearLayout.VERTICAL);
        dados.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView nome = new TextView(this);
        nome.setText(membro.nome == null || membro.nome.trim().isEmpty()
                ? getString(R.string.family_member_email_missing) : membro.nome);
        nome.setTextColor(getColor(R.color.black));
        TextView email = new TextView(this);
        email.setText(membro.email == null
                ? getString(R.string.family_member_email_missing) : membro.email);
        email.setTextColor(getColor(R.color.gray_text));
        dados.addView(nome);
        dados.addView(email);
        linha.addView(dados);
        GrupoFamiliar grupo = grupoViewModel.observarGrupo().getValue();
        boolean criadorAtual = ehCriadorDoGrupo(grupo);
        if (membro.criador) {
            TextView badge = new TextView(this);
            badge.setText(R.string.family_creator_badge);
            badge.setTextColor(getColor(R.color.green_dark));
            linha.addView(badge);
        } else if (criadorAtual) {
            MaterialButton remover = new MaterialButton(this);
            remover.setText(R.string.family_remove);
            remover.setContentDescription(getString(R.string.family_remove_accessibility,
                    membro.nome == null ? membro.email : membro.nome));
            remover.setOnClickListener(v -> confirmarRemocao(membro));
            linha.addView(remover);
        }
        listaMembros.addView(linha);
        View divisor = new View(this);
        divisor.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divisor.setBackgroundColor(getColor(R.color.gray_border));
        listaMembros.addView(divisor);
    }

    private boolean ehCriadorDoGrupo(GrupoFamiliar grupo) {
        if (grupo == null || FirebaseAuth.getInstance().getCurrentUser() == null) return false;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid.equals(grupo.criadorId)) return true;
        String emailAtual = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        return grupo.criadorId == null && emailAtual != null && grupo.criadorEmail != null
                && emailAtual.trim().equalsIgnoreCase(grupo.criadorEmail.trim());
    }

    private void mostrarDialogAdicionarMembro() {
        EditText campo = new EditText(this);
        campo.setHint(R.string.family_email_hint);
        campo.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        new AlertDialog.Builder(this)
                .setTitle(R.string.family_add_title)
                .setView(campo)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        grupoViewModel.adicionarMembro(campo.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmarSaidaGrupo() {
        GrupoFamiliar grupo = grupoViewModel.observarGrupo().getValue();
        String nome = grupo == null ? getString(R.string.grupo_familiar_title) : grupo.nome;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.family_leave_confirmation, nome))
                .setPositiveButton(R.string.family_leave, (dialog, which) ->
                        grupoViewModel.sairDoGrupo())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void mostrarDialogAlterarEmail() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        int margem = Math.round(24 * getResources().getDisplayMetrics().density);
        campos.setPadding(margem, 0, margem, 0);
        EditText novoEmail = new EditText(this);
        novoEmail.setSingleLine(true);
        novoEmail.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        novoEmail.setHint(R.string.change_email_new);
        EditText senha = new EditText(this);
        senha.setSingleLine(true);
        senha.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        senha.setHint(R.string.change_email_password);
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
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.change_email_confirm_title)
                            .setMessage(R.string.change_email_confirm_message)
                            .setPositiveButton(R.string.action_confirm,
                                    (confirmacao, which) -> alterarEmail(email, password))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void alterarEmail(String novoEmail, String senha) {
        View botao = findViewById(R.id.btn_change_login_email);
        botao.setEnabled(false);
        contaUsuarioViewModel.alterarEmail(
                novoEmail, senha, new ContaUsuarioRepository.Retorno() {
                    @Override
                    public void onSuccess() {
                        FirebaseAuth.getInstance().signOut();
                        sessao.sair();
                        Toast.makeText(PerfilActivity.this, R.string.change_email_success,
                                Toast.LENGTH_LONG).show();
                        abrirLogin();
                    }

                    @Override
                    public void onError(Exception error) {
                        botao.setEnabled(true);
                        android.util.Log.e("PerfilActivity", "Falha ao alterar e-mail.", error);
                        Toast.makeText(PerfilActivity.this,
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
                .setPositiveButton(android.R.string.yes, (dialog, which) ->
                        pedirConfirmacaoFinalExclusao())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void pedirConfirmacaoFinalExclusao() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        int margem = Math.round(24 * getResources().getDisplayMetrics().density);
        campos.setPadding(margem, 0, margem, 0);
        EditText palavra = new EditText(this);
        palavra.setSingleLine(true);
        palavra.setHint(R.string.delete_account_type_word);
        EditText senha = new EditText(this);
        senha.setSingleLine(true);
        senha.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        senha.setHint(R.string.delete_account_password);
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
        View botao = findViewById(R.id.btn_delete_account);
        botao.setEnabled(false);
        contaUsuarioViewModel.excluirConta(
                senha, new ContaUsuarioRepository.Retorno() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(PerfilActivity.this, R.string.delete_account_success,
                                Toast.LENGTH_LONG).show();
                        abrirLogin();
                    }

                    @Override
                    public void onError(Exception error) {
                        botao.setEnabled(true);
                        android.util.Log.e("PerfilActivity", "Falha ao excluir conta.", error);
                        Toast.makeText(PerfilActivity.this,
                                error.getMessage() == null
                                        ? getString(R.string.delete_account_error)
                                        : error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void abrirLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmarRemocao(Usuario membro) {
        String nome = membro.nome == null ? membro.email : membro.nome;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.family_remove_confirmation, nome))
                .setPositiveButton(R.string.family_remove, (dialog, which) ->
                        grupoViewModel.removerMembro(membro.id))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void sair() {
        FirebaseAuth.getInstance().signOut();
        sessao.sair();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void configurarNavegacaoInferior() {
        NavegacaoInferior.configurar(this,
                (BottomNavigationView) findViewById(R.id.bottom_navigation),
                R.id.nav_perfil);
    }
}