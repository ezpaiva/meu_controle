package br.com.meu_controle.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationSchedulerLimitesTest {

    // ---------- calcularDatasAviso ----------

    @Test
    public void avisosAtravessamAViradaDeMes() {
        List<LocalDate> datas = NotificationScheduler.calcularDatasAviso(LocalDate.of(2026, 3, 2));

        assertEquals(LocalDate.of(2026, 2, 23), datas.get(0));
        assertEquals(LocalDate.of(2026, 2, 27), datas.get(1));
        assertEquals(LocalDate.of(2026, 3, 1), datas.get(2));
        assertEquals(LocalDate.of(2026, 3, 2), datas.get(3));
    }

    @Test
    public void avisosConsideramAnoBissexto() {
        List<LocalDate> datas = NotificationScheduler.calcularDatasAviso(LocalDate.of(2028, 3, 1));

        assertEquals(LocalDate.of(2028, 2, 23), datas.get(0));
        assertEquals(LocalDate.of(2028, 2, 27), datas.get(1));
        assertEquals(LocalDate.of(2028, 2, 29), datas.get(2));
        assertEquals(LocalDate.of(2028, 3, 1), datas.get(3));
    }

    @Test
    public void avisosAtravessamAViradaDeAno() {
        List<LocalDate> datas = NotificationScheduler.calcularDatasAviso(LocalDate.of(2027, 1, 3));

        assertEquals(LocalDate.of(2026, 12, 27), datas.get(0));
        assertEquals(LocalDate.of(2026, 12, 31), datas.get(1));
        assertEquals(LocalDate.of(2027, 1, 2), datas.get(2));
        assertEquals(LocalDate.of(2027, 1, 3), datas.get(3));
    }

    @Test
    public void ultimoAvisoSempreCaiNoDiaDoVencimento() {
        LocalDate vencimento = LocalDate.of(2026, 8, 15);
        List<LocalDate> datas = NotificationScheduler.calcularDatasAviso(vencimento);

        assertEquals(vencimento, datas.get(datas.size() - 1));
    }

    // ---------- calcularHorarioAviso ----------

    @Test
    public void respeitaOFusoHorarioDoUsuario() {
        ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo"); // UTC-3, sem horário de verão desde 2019
        long agora = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli();

        long aviso = NotificationScheduler.calcularHorarioAviso(
                LocalDate.of(2026, 11, 13), LocalTime.of(18, 45), agora, saoPaulo);

        assertEquals(Instant.parse("2026-11-13T21:45:00Z").toEpochMilli(), aviso);
    }

    @Test
    public void dataFuturaMantemOHorarioEscolhido() {
        ZoneId utc = ZoneId.of("UTC");
        long agora = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli();

        long aviso = NotificationScheduler.calcularHorarioAviso(
                LocalDate.of(2099, 1, 1), LocalTime.of(9, 0), agora, utc);

        assertEquals(Instant.parse("2099-01-01T09:00:00Z").toEpochMilli(), aviso);
    }

    @Test
    public void dataPassadaDistanteNaoEhReagendadaParaAgora() {
        ZoneId utc = ZoneId.of("UTC");
        long agora = Instant.parse("2026-11-01T12:00:00Z").toEpochMilli();

        long aviso = NotificationScheduler.calcularHorarioAviso(
                LocalDate.of(2020, 1, 1), LocalTime.of(9, 0), agora, utc);

        assertEquals(Instant.parse("2020-01-01T09:00:00Z").toEpochMilli(), aviso);
    }

    @Test
    public void hojeComHorarioQueJaPassouAgendaParaUmMinutoDepois() {
        ZoneId utc = ZoneId.of("UTC");
        LocalDate hoje = LocalDate.now(utc);
        long agora = hoje.atTime(12, 0).atZone(utc).toInstant().toEpochMilli();

        long aviso = NotificationScheduler.calcularHorarioAviso(hoje, LocalTime.of(8, 0), agora, utc);

        assertEquals(agora + 60_000L, aviso);
    }

    @Test
    public void hojeComHorarioQueAindaVaiChegarMantemOHorario() {
        ZoneId utc = ZoneId.of("UTC");
        LocalDate hoje = LocalDate.now(utc);
        long agora = hoje.atTime(12, 0).atZone(utc).toInstant().toEpochMilli();

        long aviso = NotificationScheduler.calcularHorarioAviso(hoje, LocalTime.of(18, 0), agora, utc);

        assertEquals(hoje.atTime(18, 0).atZone(utc).toInstant().toEpochMilli(), aviso);
    }

    // ---------- requestCode ----------

    @Test
    public void requestCodeDeIdPequenoEOProprioId() {
        assertEquals(0, NotificationScheduler.requestCode(0L));
        assertEquals(1, NotificationScheduler.requestCode(1L));
        assertEquals(31, NotificationScheduler.requestCode(31L));
    }

    @Test
    public void requestCodeEstavelParaIdGrande() {
        assertEquals(NotificationScheduler.requestCode(Long.MAX_VALUE),
                NotificationScheduler.requestCode(Long.MAX_VALUE));
    }

    // Dois alarmes com o mesmo código se sobrescreveriam: nunca pode haver colisão
    @Test
    public void nenhumaColisaoEntreLembretesEIndicesDeAviso() {
        Set<Integer> codigos = new HashSet<>();
        int total = 0;
        for (long id = 1; id <= 2000; id++) {
            for (int indice = 0; indice < 4; indice++) {
                codigos.add(NotificationScheduler.requestCode(id, indice));
                total++;
            }
        }
        assertTrue(total > 0);
        assertEquals(total, codigos.size());
    }
}