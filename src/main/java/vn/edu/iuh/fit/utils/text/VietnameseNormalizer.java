package vn.edu.iuh.fit.utils.text;

import java.text.Normalizer;
import java.util.Locale;

public final class VietnameseNormalizer {
    private VietnameseNormalizer() {}

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String lower = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        lower = lower.replace('đ', 'd');
        lower = lower.replaceAll("[^a-z0-9\s]", " ");
        lower = lower.replaceAll("\\s+", " ").trim();
        return lower;
    }
}
