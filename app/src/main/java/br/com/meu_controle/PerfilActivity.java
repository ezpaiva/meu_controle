package br.com.meu_controle;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.telas.ListaContas;

public class PerfilActivity extends AppCompatActivity {
    private SessaoUsuario sessao;
    private LinearLayout listaMembros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        sessao = new SessaoUsuario(this);

        // Preenche os dados
        ((TextView) findViewById(R.id.profile_space_name)).setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        ((TextView) findViewById(R.id.profile_space_detail)).setText(sessao.perfilFamiliar() ? (sessao.obterMembros().size() + 1) + " membros" : "Privado");
        ((TextView) findViewById(R.id.profile_name)).setText(sessao.obterNome());
        ((TextView) findViewById(R.id.profile_email)).setText(sessao.obterEmail());
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
            atualizarMembros();

            findViewById(R.id.btn_invite_family).setOnClickListener(v ->
                    Toast.makeText(this, "Convite enviado!", Toast.LENGTH_SHORT).show()
            );
        } else {
            secaoFamiliar.setVisibility(View.GONE);
            secaoIndividual.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.logout_button).setOnClickListener(view -> sair());

        findViewById(R.id.row_gerenciar_categorias).setOnClickListener(v ->
                startActivity(new Intent(this, GerenciarCategoriasActivity.class)));

        configurarNavegacaoInferior();
    }

    private void atualizarMembros() {
        listaMembros.removeAllViews();
        adicionarLinhaMembro(sessao.obterNome() + " (você)");
        for (String membro : sessao.obterMembros()) {
            adicionarLinhaMembro(membro);
        }
    }

    private void adicionarLinhaMembro(String membro) {
        TextView linha = new TextView(this);
        linha.setText(membro);
        linha.setTextColor(getColor(R.color.black));
        linha.setTextSize(14);
        linha.setPadding(0, 24, 0, 24);
        listaMembros.addView(linha);

        // Adiciona linha divisória sutil entre membros
        View divisor = new View(this);
        divisor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divisor.setBackgroundColor(getColor(R.color.gray_border));
        listaMembros.addView(divisor);
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
        findViewById(R.id.nav_dashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)); finish();
        });
        findViewById(R.id.nav_receitas).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaContas.class);
            i.putExtra(ListaContas.EXTRA_TIPO_CONTA, TipoConta.RECEBER);
            startActivity(i); finish();
        });
        findViewById(R.id.nav_despesas).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaContas.class);
            i.putExtra(ListaContas.EXTRA_TIPO_CONTA, TipoConta.PAGAR);
            startActivity(i); finish();
        });
        findViewById(R.id.nav_calendario).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarioActivity.class)); finish();
        });

        TextView navAtivo = findViewById(R.id.nav_perfil);
        navAtivo.setTextColor(getColor(R.color.receivables_accent_dark));
        navAtivo.setTypeface(null, Typeface.BOLD);
    }
}