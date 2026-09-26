package br.com.meu_controle.utilitarios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class FormatoFinanceiro {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
                    .withResolverStyle(ResolverStyle.STRICT);

    private FormatoFinanceiro() {
    }

    public static long parseAmountToCents(String text) {
        String normalized = text.trim().replace(" ", "");
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(',', '.');
        }

        try {
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.signum() <= 0 || amount.scale() > 2) {
                throw new IllegalArgumentException("Valor fora do formato aceito.");
            }
            return amount.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (NumberFormatException | ArithmeticException error) {
            throw new IllegalArgumentException("Valor fora do formato aceito.", error);
        }
    }

    public static String formatAmount(long amountInCents) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return currency.format(BigDecimal.valueOf(amountInCents, 2));
    }

    public static String parseDisplayDate(String displayDate) {
        try {
            return LocalDate.parse(displayDate, DISPLAY_DATE).toString();
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Data fora do formato dd/MM/aaaa.", error);
        }
    }

    public static String formatStoredDate(String storedDate) {
        return LocalDate.parse(storedDate).format(DISPLAY_DATE);
    }
}
