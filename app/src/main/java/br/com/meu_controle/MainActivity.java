package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.telas.ListaContas;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button receivablesButton = findViewById(R.id.open_receivables_button);
        Button payablesButton = findViewById(R.id.open_payables_button);
        receivablesButton.setOnClickListener(view -> openAccounts(TipoConta.RECEBER));
        payablesButton.setOnClickListener(view -> openAccounts(TipoConta.PAGAR));
    }

    private void openAccounts(String type) {
        Intent intent = new Intent(this, ListaContas.class);
        intent.putExtra(ListaContas.EXTRA_TIPO_CONTA, type);
        startActivity(intent);
    }
}