package vn.edu.iuh.fit.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextNormalizer {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^\\p{Alnum}\\s]");

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        normalized = NON_ALPHANUM.matcher(normalized).replaceAll(" ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.toLowerCase(Locale.ROOT);
    }
}
