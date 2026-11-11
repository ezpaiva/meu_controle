package br.com.meu_controle.services;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NotificationSchedulerTest {

    @Test
    public void dueDateProducesFourDistinctReminderDates() {
        List<LocalDate> dates = NotificationScheduler.calcularDatasAviso(
                LocalDate.of(2026, 11, 20));

        assertEquals(4, dates.size());
        assertEquals(4, new HashSet<>(dates).size());
        assertEquals(LocalDate.of(2026, 11, 13), dates.get(0));
        assertEquals(LocalDate.of(2026, 11, 17), dates.get(1));
        assertEquals(LocalDate.of(2026, 11, 19), dates.get(2));
        assertEquals(LocalDate.of(2026, 11, 20), dates.get(3));
    }

    @Test
    public void alarmIdentityIsStableAndDistinctForDifferentReminderIds() {
        assertEquals(NotificationScheduler.requestCode(31L),
                NotificationScheduler.requestCode(31L));
        assertNotEquals(NotificationScheduler.requestCode(31L),
                NotificationScheduler.requestCode(32L));
    }

    @Test
    public void alertsUseTheUserSelectedTimeOnEachDueDate() {
        ZoneId zone = ZoneId.of("UTC");
        long now = LocalDateTime.of(2026, 11, 1, 8, 0)
                .atZone(zone).toInstant().toEpochMilli();
        long alert = NotificationScheduler.calcularHorarioAviso(
                LocalDate.of(2026, 11, 13), LocalTime.of(18, 45), now, zone);

        assertEquals(LocalDateTime.of(2026, 11, 13, 18, 45)
                .atZone(zone).toInstant().toEpochMilli(), alert);
    }

    @Test
    public void eachReminderOffsetHasItsOwnAlarmIdentity() {
        assertNotEquals(NotificationScheduler.requestCode(50L, 0),
                NotificationScheduler.requestCode(50L, 1));
        assertNotEquals(NotificationScheduler.requestCode(50L, 2),
                NotificationScheduler.requestCode(50L, 3));
    }
}
