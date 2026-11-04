package br.com.meu_controle;

import android.app.Application;
import android.content.Context;

public class MeuControleApp extends Application {

    private static Context contexto;

    @Override
    public void onCreate() {
        super.onCreate();
        contexto = getApplicationContext();
    }

    public static Context obterContexto() {
        return contexto;
    }
}