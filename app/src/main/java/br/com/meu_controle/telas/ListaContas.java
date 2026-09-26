package br.com.meu_controle.telas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());
        listaContas = findViewById(R.id.accounts_container);
        mensagemVazia = findViewById(R.id.empty_accounts_message);

        TextView titulo = findViewById(R.id.accounts_title);
        titulo.setText(TipoConta.RECEBER.equals(tipoConta)
                ? R.string.accounts_title_receivable
                : R.string.accounts_title_payable);

        Button botaoNovaConta = findViewById(R.id.new_account_button);
        botaoNovaConta.setOnClickListener(view -> abrirNovaConta());

        boolean exibindoReceitas = TipoConta.RECEBER.equals(tipoConta);
        Button botaoReceitas = findViewById(R.id.nav_receivables);
        Button botaoDespesas = findViewById(R.id.nav_payables);
        botaoReceitas.setEnabled(!exibindoReceitas);
        botaoDespesas.setEnabled(exibindoReceitas);
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
                mensagemVazia.setVisibility(contas.isEmpty() ? View.VISIBLE : View.GONE);
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
        TextView vencimento = linha.findViewById(R.id.account_due_date);
        TextView categoria = linha.findViewById(R.id.account_category);
        TextView situacao = linha.findViewById(R.id.account_status);

        descricao.setText(conta.description);
        valor.setText(FormatoFinanceiro.formatAmount(conta.amountInCents));
        vencimento.setText(FormatoFinanceiro.formatStoredDate(conta.dueDate));
        categoria.setText(conta.category);
        boolean concluida = TipoConta.CONCLUIDA.equals(conta.status);
        int textoSituacao = concluida
                ? (TipoConta.RECEBER.equals(tipoConta)
                    ? R.string.status_completed_receivable
                    : R.string.status_completed_payable)
                : R.string.status_pending;
        situacao.setText(getString(R.string.status_label, getString(textoSituacao)));
    }
}
