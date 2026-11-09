package br.com.meu_controle.utilitarios;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {
    private Paint paintReceitas, paintDespesas, paintCenter;
    private float receitas = 0f, despesas = 0f;

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintReceitas = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintReceitas.setColor(Color.parseColor("#2E7D32")); // Verde escuro
        paintReceitas.setStyle(Paint.Style.FILL);

        paintDespesas = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDespesas.setColor(Color.parseColor("#C62828")); // Vermelho
        paintDespesas.setStyle(Paint.Style.FILL);

        paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenter.setColor(Color.WHITE);
        paintCenter.setStyle(Paint.Style.FILL);
    }

    public void setData(float receitas, float despesas) {
        this.receitas = receitas;
        this.despesas = despesas;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float total = receitas + despesas;
        float padding = 20f;
        RectF rect = new RectF(padding, padding, getWidth() - padding, getHeight() - padding);

        if (total == 0) {
            Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            emptyPaint.setColor(Color.LTGRAY);
            canvas.drawArc(rect, 0, 360, true, emptyPaint);
        } else {
            float angleReceitas = (receitas / total) * 360f;
            float angleDespesas = (despesas / total) * 360f;

            // Começa do topo (-90 graus)
            canvas.drawArc(rect, -90, angleReceitas, true, paintReceitas);
            canvas.drawArc(rect, -90 + angleReceitas, angleDespesas, true, paintDespesas);
        }

        // Desenha o furo no meio (Donut)
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4.5f, paintCenter);
    }
}