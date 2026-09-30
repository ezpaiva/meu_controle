package br.com.meu_controle.telas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;

import java.util.List;

public class ListaContas extends AppCompatActivity {

    public static final String EXTRA_TIPO_CONTA = "tipo_conta";

    private String tipoConta;
    private RepositorioContas repositorio;
    private LinearLayout listaContas;
    private TextView mensagemVazia;
    private View cartaoListaVazia;
    private MaterialCardView cartaoResumo;
    private TextView rotuloResumo;
    private TextView valorResumo;
    private TextView quantidadeResumo;
    private MaterialButton botaoNovaConta;
    private boolean exibindoReceitas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);

        tipoConta = getIntent().getStringExtra(EXTRA_TIPO_CONTA);
        if (!TipoConta.RECEBER.equals(tipoConta) && !TipoConta.PAGAR.equals(tipoConta)) {
            Toast.makeText(this, R.string.load_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        exibindoReceitas = TipoConta.RECEBER.equals(tipoConta);

        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());
        listaContas = findViewById(R.id.accounts_container);
        mensagemVazia = findViewById(R.id.empty_accounts_message);
        cartaoListaVazia = findViewById(R.id.empty_accounts_card);
        cartaoResumo = findViewById(R.id.accounts_summary_card);
        rotuloResumo = findViewById(R.id.accounts_summary_label);
        valorResumo = findViewById(R.id.accounts_summary_amount);
        quantidadeResumo = findViewById(R.id.accounts_summary_count);

        TextView titulo = findViewById(R.id.accounts_title);
        titulo.setText(exibindoReceitas
                ? R.string.accounts_title_receivable
                : R.string.accounts_title_payable);

        botaoNovaConta = findViewById(R.id.new_account_button);
        configurarResumoEAcao();
        botaoNovaConta.setOnClickListener(view -> abrirNovaConta());

        Button botaoReceitas = findViewById(R.id.nav_receivables);
        Button botaoDespesas = findViewById(R.id.nav_payables);
        botaoReceitas.setSelected(exibindoReceitas);
        botaoDespesas.setSelected(!exibindoReceitas);
        botaoReceitas.setOnClickListener(view -> abrirContas(TipoConta.RECEBER));
        botaoDespesas.setOnClickListener(view -> abrirContas(TipoConta.PAGAR));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repositorio != null) {
            carregarContas();
        }
    }

    private void abrirNovaConta() {
        Intent intent = new Intent(this, NovaConta.class);
        intent.putExtra(EXTRA_TIPO_CONTA, tipoConta);
        startActivity(intent);
    }

    private void configurarResumoEAcao() {
        int corAcento = getColor(exibindoReceitas
                ? R.color.receivables_accent
                : R.color.payables_accent);
        cartaoResumo.setCardBackgroundColor(corAcento);
        rotuloResumo.setText(exibindoReceitas
                ? R.string.total_receivables
                : R.string.total_payables);
        botaoNovaConta.setText(exibindoReceitas
                ? R.string.new_receivable
                : R.string.new_payable);
        if (exibindoReceitas) {
            botaoNovaConta.setBackgroundTintList(ColorStateList.valueOf(corAcento));
            botaoNovaConta.setTextColor(getColor(R.color.white));
            botaoNovaConta.setStrokeWidth(0);
        } else {
            botaoNovaConta.setBackgroundTintList(
                    ColorStateList.valueOf(getColor(R.color.surface)));
            botaoNovaConta.setTextColor(corAcento);
            botaoNovaConta.setStrokeColor(ColorStateList.valueOf(corAcento));
            botaoNovaConta.setStrokeWidth(Math.round(
                    1 * getResources().getDisplayMetrics().density));
        }
    }

    private void abrirContas(String tipo) {
        if (tipo.equals(tipoConta)) {
            return;
        }
        Intent intent = new Intent(this, ListaContas.class);
        intent.putExtra(EXTRA_TIPO_CONTA, tipo);
        startActivity(intent);
        finish();
    }

    private void carregarContas() {
        repositorio.buscarPorTipo(tipoConta, new RetornoRepositorio<List<Conta>>() {
            @Override
            public void onSuccess(List<Conta> contas) {
                listaContas.removeAllViews();
                boolean listaVazia = contas.isEmpty();
                cartaoListaVazia.setVisibility(listaVazia ? View.VISIBLE : View.GONE);
                mensagemVazia.setVisibility(listaVazia ? View.VISIBLE : View.GONE);
                long totalCentavos = 0;
                for (Conta conta : contas) {
                    totalCentavos += conta.amountInCents;
                }
                valorResumo.setText(FormatoFinanceiro.formatAmount(totalCentavos));
                quantidadeResumo.setText(getResources().getQuantityString(
                        R.plurals.account_record_count, contas.size(), contas.size()));
                LayoutInflater inflater = LayoutInflater.from(ListaContas.this);
                for (Conta conta : contas) {
                    View linha = inflater.inflate(R.layout.item_account, listaContas, false);
                    preencherConta(linha, conta);
                    listaContas.addView(linha);
                }
            }

            @Override
            public void onError(Exception erro) {
                Toast.makeText(ListaContas.this, R.string.load_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void preencherConta(View linha, Conta conta) {
        TextView descricao = linha.findViewById(R.id.account_description);
        TextView valor = linha.findViewById(R.id.account_amount);
        TextView metadata = linha.findViewById(R.id.account_metadata);
        TextView situacao = linha.findViewById(R.id.account_status);

        descricao.setText(conta.description);
        valor.setText(FormatoFinanceiro.formatAmount(conta.amountInCents));
        metadata.setText(getString(
                R.string.account_metadata,
                conta.category,
                FormatoFinanceiro.formatStoredDate(conta.dueDate)));
        valor.setTextColor(getColor(exibindoReceitas
                ? R.color.receivables_accent_dark
                : R.color.payables_accent_dark));
        boolean concluida = TipoConta.CONCLUIDA.equals(conta.status);
        int textoSituacao = concluida
                ? (TipoConta.RECEBER.equals(tipoConta)
                    ? R.string.status_completed_receivable
                    : R.string.status_completed_payable)
                : R.string.status_pending;
        situacao.setText(getString(R.string.status_label, getString(textoSituacao)));
        situacao.setBackgroundResource(concluida
                ? R.drawable.bg_status_completed
                : R.drawable.bg_status_pending);
        situacao.setTextColor(getColor(concluida
                ? R.color.status_completed_text
                : R.color.status_pending_text));
    }
}
