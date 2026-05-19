package com.flashminat0.selectivekeepinv;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime holder for the eight message pools used at respawn. Two factories:
 * <ul>
 *   <li>{@link #defaults()}: built-in pools from {@link DeathMessages}.</li>
 *   <li>{@link #fromFile(File)}: loads + validates a custom death-msgs.yml.
 *       Throws {@link InvalidDeathMessageFile} on any structural problem.</li>
 * </ul>
 *
 * <p>The store is set once at preInit. {@link EventHandler} reads pools
 * directly from the active instance held by {@code SelectiveKeepInv}.
 */
public class DeathMessageStore {

    public final String[] allLines;
    public final String[] allLinesWithXp;
    public final String[] allLinesNoXp;
    public final String[] sameDimLines;
    public final String[] diffDimLines;
    public final String[] xpRollLucky;
    public final String[] xpRollMid;
    public final String[] xpRollBrutal;

    /** Map from on-disk section name to the corresponding pool array. */
    private static final String K_ALL          = "all-lines";
    private static final String K_ALL_WITH_XP  = "all-lines-with-xp";
    private static final String K_ALL_NO_XP    = "all-lines-no-xp";
    private static final String K_SAME_DIM     = "same-dim-lines";
    private static final String K_DIFF_DIM     = "diff-dim-lines";
    private static final String K_XP_LUCKY     = "xp-roll-lucky";
    private static final String K_XP_MID       = "xp-roll-mid";
    private static final String K_XP_BRUTAL    = "xp-roll-brutal";

    /** Sections that get String.format(line, distance). MUST have exactly one %s. */
    private static final List<String> FORMATTED_SECTIONS = Arrays.asList(K_SAME_DIM);

    /** All sections that exist. */
    private static final List<String> ALL_SECTIONS = Arrays.asList(
            K_ALL, K_ALL_WITH_XP, K_ALL_NO_XP,
            K_SAME_DIM, K_DIFF_DIM,
            K_XP_LUCKY, K_XP_MID, K_XP_BRUTAL
    );

    private DeathMessageStore(String[] allLines, String[] allLinesWithXp, String[] allLinesNoXp,
                              String[] sameDimLines, String[] diffDimLines,
                              String[] xpRollLucky, String[] xpRollMid, String[] xpRollBrutal) {
        this.allLines       = allLines;
        this.allLinesWithXp = allLinesWithXp;
        this.allLinesNoXp   = allLinesNoXp;
        this.sameDimLines   = sameDimLines;
        this.diffDimLines   = diffDimLines;
        this.xpRollLucky    = xpRollLucky;
        this.xpRollMid      = xpRollMid;
        this.xpRollBrutal   = xpRollBrutal;
    }

    /** Defaults: pulls from the constant arrays in {@link DeathMessages}. */
    public static DeathMessageStore defaults() {
        return new DeathMessageStore(
                DeathMessages.ALL_LINES,
                DeathMessages.ALL_LINES_WITH_XP,
                DeathMessages.ALL_LINES_NO_XP,
                DeathMessages.SAME_DIM_LINES,
                DeathMessages.DIFF_DIM_LINES,
                DeathMessages.XP_ROLL_LUCKY,
                DeathMessages.XP_ROLL_MID,
                DeathMessages.XP_ROLL_BRUTAL
        );
    }

    /**
     * Load + validate from disk. Throws on any structural problem so the
     * caller can decide what to do (typically: revert the flag and queue an
     * op-visible warning).
     */
    public static DeathMessageStore fromFile(File file) throws InvalidDeathMessageFile {
        Map<String, List<String>> root;
        try (Reader r = new FileReader(file)) {
            root = SimpleYaml.parseLists(r);
        } catch (IOException e) {
            // parseLists throws IOException for both shape errors and real IO.
            throw new InvalidDeathMessageFile(e.getMessage());
        } catch (Exception e) {
            throw new InvalidDeathMessageFile("malformed YAML: " + e.getMessage());
        }

        // Every section must be present.
        for (String section : ALL_SECTIONS) {
            if (!root.containsKey(section)) {
                throw new InvalidDeathMessageFile("missing required section: " + section);
            }
        }

        // Every pool must be non-empty (we randomFrom them, an empty array
        // would throw IllegalArgumentException at runtime).
        for (String section : ALL_SECTIONS) {
            List<String> lines = root.get(section);
            if (lines == null || lines.isEmpty()) {
                throw new InvalidDeathMessageFile("section '" + section + "' is empty");
            }
        }

        // Sections that go through String.format(line, %s) must have exactly
        // one %s. The unformatted ones must have none.
        for (String section : ALL_SECTIONS) {
            boolean formatted = FORMATTED_SECTIONS.contains(section);
            List<String> lines = root.get(section);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.trim().isEmpty()) {
                    throw new InvalidDeathMessageFile(
                            "section '" + section + "' line " + (i + 1) + " is blank");
                }
                int s = countOccurrences(line, "%s");
                int specifiers = countPercentSpecifiers(line);
                if (formatted) {
                    if (s != 1 || specifiers != 1) {
                        throw new InvalidDeathMessageFile(
                                "section '" + section + "' line " + (i + 1)
                                + " must contain exactly one %s placeholder (and no other %X). Got: "
                                + line);
                    }
                } else {
                    if (specifiers != 0) {
                        throw new InvalidDeathMessageFile(
                                "section '" + section + "' line " + (i + 1)
                                + " is not formatted but contains a % specifier. Got: " + line);
                    }
                }
            }
        }

        return new DeathMessageStore(
                toArray(root.get(K_ALL)),
                toArray(root.get(K_ALL_WITH_XP)),
                toArray(root.get(K_ALL_NO_XP)),
                toArray(root.get(K_SAME_DIM)),
                toArray(root.get(K_DIFF_DIM)),
                toArray(root.get(K_XP_LUCKY)),
                toArray(root.get(K_XP_MID)),
                toArray(root.get(K_XP_BRUTAL))
        );
    }

    /**
     * Write the built-in pools to a YAML file (used to seed death-msgs.yml
     * the first time override-corny-msgs is enabled).
     */
    public static void writeDefaultsToFile(File file) throws IOException {
        DeathMessageStore d = defaults();
        Map<String, String[]> sections = new LinkedHashMap<>();
        sections.put(K_ALL,         d.allLines);
        sections.put(K_ALL_WITH_XP, d.allLinesWithXp);
        sections.put(K_ALL_NO_XP,   d.allLinesNoXp);
        sections.put(K_SAME_DIM,    d.sameDimLines);
        sections.put(K_DIFF_DIM,    d.diffDimLines);
        sections.put(K_XP_LUCKY,    d.xpRollLucky);
        sections.put(K_XP_MID,      d.xpRollMid);
        sections.put(K_XP_BRUTAL,   d.xpRollBrutal);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (Writer w = new FileWriter(file)) {
            w.write("# Selective Keep Inventory death messages\n");
            w.write("# Generated from the built-in pools. Edit freely.\n");
            w.write("# A line in same-dim-lines must contain exactly one %s (the distance);\n");
            w.write("# all other sections must contain none.\n");
            w.write("# On a server restart the file is validated. If anything is wrong,\n");
            w.write("# the override flag is reset to false and a warning is shown to ops.\n");
            for (Map.Entry<String, String[]> e : sections.entrySet()) {
                w.write("\n");
                w.write(e.getKey());
                w.write(":\n");
                for (String line : e.getValue()) {
                    w.write("  - ");
                    w.write(quote(line));
                    w.write("\n");
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private static String quote(String s) {
        // We don't expect embedded " in the pools but escape defensively.
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\');
            sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    private static String[] toArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** Count any printf format specifier: % not followed by another %. */
    private static int countPercentSpecifiers(String s) {
        int count = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '%' && s.charAt(i + 1) != '%') count++;
        }
        return count;
    }

    /** Thrown by {@link #fromFile} on any structural problem. */
    public static class InvalidDeathMessageFile extends Exception {
        public InvalidDeathMessageFile(String message) {
            super(message);
        }
    }
}
