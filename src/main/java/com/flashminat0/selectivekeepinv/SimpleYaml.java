package com.flashminat0.selectivekeepinv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny YAML reader for the mod's two-level schema. Two parser methods:
 *
 * <ul>
 *   <li>{@link #parse(Reader)} for scalar key/value sections, used by Config:
 *     <pre>
 *       thresholds:
 *         offhand: 10
 *         helmet: 11
 *     </pre>
 *   </li>
 *   <li>{@link #parseLists(Reader)} for list-of-strings sections, used by
 *       DeathMessageStore:
 *     <pre>
 *       same-dim-lines:
 *         - "first line"
 *         - "second line"
 *     </pre>
 *   </li>
 * </ul>
 *
 * <p>Both methods share:
 * <ul>
 *   <li>Quote-aware <code>#</code> comment stripping (a <code>#</code> inside
 *       a <code>"..."</code> or <code>'...'</code> value is kept as part of
 *       the value).</li>
 *   <li>Blank-line and whole-line-comment skipping.</li>
 *   <li>Optional wrapping quotes on string values are stripped (with
 *       <code>\"</code> and <code>\\</code> unescaping inside double quotes).</li>
 * </ul>
 *
 * <p>{@link #parse} is lenient (skips unrecognised lines); {@link #parseLists}
 * is strict (throws on lines that don't fit the schema) because users editing
 * death-msgs.yml benefit from an early loud error over a silent half-load.
 *
 * <p>Booleans accepted (case-insensitive) in {@link #parse}:
 * {@code true}, {@code false}, {@code yes}, {@code no}, {@code on}, {@code off}.
 * Integers are parsed via {@link Integer#parseInt}. Anything else stays a
 * String.
 *
 * <p>{@link #parseLists} treats every list item as an opaque string (no type
 * coercion), so a death message that's literally {@code "yes"} stays the
 * string {@code "yes"} rather than getting promoted to a boolean.
 */
public final class SimpleYaml {

    private SimpleYaml() {}

    // -----------------------------------------------------------------
    // Scalar k/v sections (Config)
    // -----------------------------------------------------------------

    public static Map<String, Map<String, Object>> parse(Reader reader) throws IOException {
        Map<String, Map<String, Object>> root = new LinkedHashMap<>();
        Map<String, Object> currentSection = null;
        BufferedReader br = new BufferedReader(reader);
        String raw;
        int lineNum = 0;
        while ((raw = br.readLine()) != null) {
            lineNum++;
            String line = stripComment(raw);
            if (line.trim().isEmpty()) continue;

            boolean indented = line.length() > 0 && Character.isWhitespace(line.charAt(0));
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new IOException("line " + lineNum
                        + ": expected 'key: value' or 'section:' but got: " + line.trim());
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();

            if (!indented) {
                currentSection = new LinkedHashMap<>();
                root.put(key, currentSection);
            } else {
                if (currentSection == null) {
                    throw new IOException("line " + lineNum
                            + ": key/value before any section header: " + line.trim());
                }
                currentSection.put(key, parseScalar(value));
            }
        }
        return root;
    }

    // -----------------------------------------------------------------
    // List-of-strings sections (DeathMessageStore)
    //
    // Strict: throws on shape mismatches with a line number so a user
    // editing the file can find what they broke.
    // -----------------------------------------------------------------

    public static Map<String, List<String>> parseLists(Reader reader) throws IOException {
        Map<String, List<String>> root = new LinkedHashMap<>();
        List<String> currentSection = null;
        BufferedReader br = new BufferedReader(reader);
        String raw;
        int lineNum = 0;
        while ((raw = br.readLine()) != null) {
            lineNum++;
            String line = stripComment(raw);
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean indented = line.length() > 0 && Character.isWhitespace(line.charAt(0));

            if (!indented) {
                if (!trimmed.endsWith(":")) {
                    throw new IOException("line " + lineNum
                            + ": expected a section header ending with ':' but got: "
                            + trimmed);
                }
                String key = trimmed.substring(0, trimmed.length() - 1).trim();
                if (key.isEmpty()) {
                    throw new IOException("line " + lineNum + ": empty section name");
                }
                currentSection = new ArrayList<>();
                root.put(key, currentSection);
            } else {
                if (!trimmed.startsWith("- ") && !trimmed.equals("-")) {
                    throw new IOException("line " + lineNum
                            + ": expected a list item ('- value') but got: " + trimmed);
                }
                if (currentSection == null) {
                    throw new IOException("line " + lineNum
                            + ": list item before any section header: " + trimmed);
                }
                String value = trimmed.length() <= 2 ? "" : trimmed.substring(2).trim();
                currentSection.add(unquote(value));
            }
        }
        return root;
    }

    // -----------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------

    /**
     * Drop everything from the first un-quoted '#' onwards. A '#' inside
     * a "..." or '...' region is treated as part of the value.
     */
    static String stripComment(String s) {
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < s.length()) {
                    i++;            // skip the escaped char
                    continue;
                }
                if (c == quote) quote = 0;
            } else {
                if (c == '"' || c == '\'') {
                    quote = c;
                } else if (c == '#') {
                    return s.substring(0, i);
                }
            }
        }
        return s;
    }

    /**
     * Parse a scalar config value. Recognizes booleans (case-insensitive,
     * including yes/no/on/off), integers, and quoted/unquoted strings.
     */
    static Object parseScalar(String s) {
        if (s.isEmpty()) return s;
        String lower = s.toLowerCase();
        if (lower.equals("true")  || lower.equals("yes") || lower.equals("on"))  return Boolean.TRUE;
        if (lower.equals("false") || lower.equals("no")  || lower.equals("off")) return Boolean.FALSE;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}
        return unquote(s);
    }

    /**
     * Strip wrapping "..." or '...' from a string, unescaping \" and \\
     * inside double quotes. No type coercion. Used by parseLists since a
     * list item that happens to be the literal string "yes" should remain
     * the string "yes", not get promoted to a boolean.
     */
    static String unquote(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if (first == '"' && last == '"') {
                return unescape(s.substring(1, s.length() - 1));
            }
            if (first == '\'' && last == '\'') {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                sb.append(s.charAt(++i));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
