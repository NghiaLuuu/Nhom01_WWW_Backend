package vn.edu.iuh.nhom01_www.aivector;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizes common Vietnamese location aliases for vector metadata filters. */
final class VietnameseLocationNormalizer {

    private static final Pattern DIRECTIONAL_QUERY = Pattern.compile(
            "(?iu)\\btừ\\s+(.+?)\\s+(?:đến|đi)\\s+(.+?)(?:[?.!,;:]|$)");
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("sai gon", "tp hcm"),
            Map.entry("ho chi minh", "tp hcm"),
            Map.entry("thanh pho ho chi minh", "tp hcm"),
            Map.entry("tp hcm", "tp hcm"),
            Map.entry("tphcm", "tp hcm"),
            Map.entry("hcm", "tp hcm"));

    String normalize(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(location, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
        return ALIASES.getOrDefault(decomposed, decomposed);
    }

    Optional<DirectionalLocation> directionalLocations(String query) {
        String candidate = query == null ? "" : query;
        // A multi-leg request has no unambiguous pair for an exact metadata filter. Keep
        // semantic retrieval available rather than falsely treating the first/last city as one route.
        if (countOccurrences(candidate.toLowerCase(Locale.ROOT), "đến") > 1
                || candidate.toLowerCase(Locale.ROOT).contains(" qua ")) {
            return Optional.empty();
        }
        Matcher matcher = DIRECTIONAL_QUERY.matcher(candidate);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String departure = normalize(matcher.group(1));
        String arrival = normalize(matcher.group(2));
        return departure.isBlank() || arrival.isBlank() ? Optional.empty()
                : Optional.of(new DirectionalLocation(departure, arrival));
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    record DirectionalLocation(String departure, String arrival) {
    }
}
