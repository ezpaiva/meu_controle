package br.com.meu_controle.utilitarios;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.meu_controle.CalendarioActivity;
import br.com.meu_controle.MainActivity;
import br.com.meu_controle.PerfilActivity;
import br.com.meu_controle.R;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.telas.ListaContas;

public final class NavegacaoInferior {

    private NavegacaoInferior() {
    }

    public static void configurar(AppCompatActivity activity, BottomNavigationView barra,
                                  int abaAtiva) {
        configurar(activity, barra, abaAtiva, false);
    }

    public static void configurar(AppCompatActivity activity, BottomNavigationView barra,
                                  int abaAtiva, boolean navegarAoSelecionarAbaAtiva) {
        for (int indice = 0; indice < barra.getMenu().size(); indice++) {
            android.view.MenuItem item = barra.getMenu().getItem(indice);
            item.setContentDescription(item.getTitle());
        }
        barra.setSelectedItemId(abaAtiva);
        barra.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == abaAtiva && !navegarAoSelecionarAbaAtiva) {
                return true;
            }

            Intent destino;
            if (id == R.id.nav_dashboard) {
                destino = new Intent(activity, MainActivity.class);
            } else if (id == R.id.nav_receitas || id == R.id.nav_despesas) {
                destino = new Intent(activity, ListaContas.class);
                destino.putExtra(ListaContas.EXTRA_TIPO_CONTA,
                        id == R.id.nav_receitas
                                ? TipoConta.RECEBER : TipoConta.PAGAR);
            } else if (id == R.id.nav_calendario) {
                destino = new Intent(activity, CalendarioActivity.class);
            } else if (id == R.id.nav_perfil) {
                destino = new Intent(activity, PerfilActivity.class);
            } else {
                return false;
            }

            activity.startActivity(destino);
            activity.finish();
            return true;
        });
    }
}
