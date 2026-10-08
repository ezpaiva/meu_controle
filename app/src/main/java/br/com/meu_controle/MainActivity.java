package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SessaoUsuario sessao;
    private RepositorioContas repositorio;
    private TextView valorReceitas;
    private TextView valorDespesas;
    private TextView valorSaldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessao = new SessaoUsuario(this);
        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());
        valorReceitas = findViewById(R.id.dashboard_income);
        valorDespesas = findViewById(R.id.dashboard_expense);
        valorSaldo = findViewById(R.id.dashboard_balance);

        Button receivablesButton = findViewById(R.id.open_receivables_button);
        Button payablesButton = findViewById(R.id.open_payables_button);
        receivablesButton.setOnClickListener(view -> openAccounts(TipoConta.RECEBER));
        payablesButton.setOnClickListener(view -> openAccounts(TipoConta.PAGAR));
        findViewById(R.id.open_profile_button).setOnClickListener(view ->
                startActivity(new Intent(this, PerfilActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarEspacoAtivo();
        carregarResumo();
    }

    private void atualizarEspacoAtivo() {
        ((TextView) findViewById(R.id.dashboard_space_name)).setText(sessao.perfilFamiliar()
                ? R.string.active_space_family : R.string.active_space_individual);
        ((TextView) findViewById(R.id.dashboard_space_detail)).setText(sessao.perfilFamiliar()
                ? getString(R.string.active_space_members, sessao.obterMembros().size() + 1)
                : getString(R.string.active_space_private));
    }

    private void carregarResumo() {
        repositorio.buscarTodas(sessao.obterEmail(), sessao.perfilFamiliar(),
                new RetornoRepositorio<List<Conta>>() {
                    @Override
                    public void onSuccess(List<Conta> contas) {
                        long receitas = 0;
                        long despesas = 0;
                        for (Conta conta : contas) {
                            if (TipoConta.RECEBER.equals(conta.type)) {
                                receitas += conta.amountInCents;
                            } else if (TipoConta.PAGAR.equals(conta.type)) {
                                despesas += conta.amountInCents;
                            }
                        }
                        valorReceitas.setText(FormatoFinanceiro.formatAmount(receitas));
                        valorDespesas.setText(FormatoFinanceiro.formatAmount(despesas));
                        valorSaldo.setText(FormatoFinanceiro.formatAmount(receitas - despesas));
                        valorSaldo.setTextColor(getColor(receitas >= despesas
                                ? R.color.receivables_accent_dark
                                : R.color.payables_accent_dark));
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(MainActivity.this, R.string.load_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openAccounts(String type) {
        Intent intent = new Intent(this, ListaContas.class);
        intent.putExtra(ListaContas.EXTRA_TIPO_CONTA, type);
        startActivity(intent);
    }
}
