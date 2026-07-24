/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.enums.Alphabet;
import mekhq.campaign.universe.enums.ForceNamingMethod;

/**
 * Produces unique, player-parseable names for the {@link mekhq.campaign.force.Formation} nodes the
 * Command Generator mirrors into the campaign TO&amp;E, driven by the Formation Naming Method the
 * player picked on the Command Designer's Setup tab.
 *
 * <p>The Force Generator ruleset engine names formations per parent, so a battalion rolls
 * "A Company, B Company" under every battalion and "Battle Lance, Fire Lance" inside every company —
 * once several companies land in one TO&amp;E the player cannot tell which lance belongs to which
 * formation when deploying. This namer replaces those per-parent names with campaign-wide unique
 * callsigns:</p>
 * <ul>
 *   <li><b>Company-tier</b> formations (Company, Binary/Trinary, ComStar Level III) allocate the next
 *       unused word of the selected naming alphabet — "Able Company", "Baker Company", continuing
 *       across battalions and across later generator runs.</li>
 *   <li><b>Lance-tier</b> formations (Lance, Star/Nova, Level II) are numbered under their company's
 *       designator — "Able-1 Battle Lance", "Able-2 Fire Lance". A lance with no company above it
 *       allocates its own alphabet word instead ("Able Battle Lance").</li>
 *   <li><b>Battalion-tier and above</b> keep the ruleset's name ("First Battalion"); on a clash with
 *       an existing formation the sequence token is advanced to the next free value
 *       ("Second Battalion").</li>
 * </ul>
 *
 * <p>All comparisons are case-insensitive against every formation name already in the campaign, so
 * generating a second command continues the sequences instead of restarting them.</p>
 */
public class FormationNamer {

    private static final MMLogger LOGGER = MMLogger.create(FormationNamer.class);

    /** Company-tier formations sit at this {@link FormationLevel#getDepth() depth}. */
    private static final int COMPANY_DEPTH = 2;

    /** Spelled ordinals matching the ruleset engine's {@code {ordinal}} token values. */
    private static final List<String> SPELLED_ORDINALS = List.of("First", "Second", "Third", "Fourth",
          "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth");

    private static final Pattern NUMERIC_ORDINAL = Pattern.compile("(\\d+)(st|nd|rd|th)");

    /**
     * The name chosen for a formation, plus the alphabet designator allocated to it (when one was).
     * The designator is fed back as {@code parentDesignator} when naming the formation's children.
     *
     * @param name       the unique display name for the {@link mekhq.campaign.force.Formation}
     * @param designator the alphabet word allocated to a company-tier formation (e.g. "Able"), or
     *                   {@code null} when the tier does not allocate one
     */
    public record NamedFormation(String name, @Nullable String designator) {}

    private final ForceNamingMethod namingMethod;
    private final Set<String> usedNames = new HashSet<>();
    private final Set<String> usedDesignators = new HashSet<>();
    private final List<List<String>> sequences = new ArrayList<>();
    private boolean warnedAlphabetExhausted;

    /**
     * @param namingMethod  the alphabet style selected on the Setup tab; {@code null} falls back to
     *                      {@link ForceNamingMethod#CCB_1943}
     * @param existingNames every formation name already present in the campaign TO&amp;E, used to
     *                      continue designator sequences across generator runs (may be empty)
     */
    public FormationNamer(@Nullable ForceNamingMethod namingMethod, Collection<String> existingNames) {
        this.namingMethod = (namingMethod == null) ? ForceNamingMethod.CCB_1943 : namingMethod;

        List<String> ccb = new ArrayList<>();
        List<String> icao = new ArrayList<>();
        List<String> english = new ArrayList<>();
        List<String> greek = new ArrayList<>();
        for (Alphabet letter : Alphabet.values()) {
            ccb.add(letter.getCCB1943());
            icao.add(letter.getICAO1956());
            english.add(letter.getEnglish());
            greek.add(letter.getGreek());
        }
        sequences.add(SPELLED_ORDINALS);
        sequences.add(ccb);
        sequences.add(icao);
        sequences.add(english);
        sequences.add(greek);

        for (String existingName : existingNames) {
            usedNames.add(normalize(existingName));
            markDesignatorFromExistingName(existingName);
        }
        LOGGER.debug("[CommandGen][Naming] namer initialised: method={} existingNames={} usedDesignators={}",
              this.namingMethod.name(), existingNames.size(), usedDesignators);
    }

    /**
     * Names one formation node mirrored from the generated force tree.
     *
     * @param engineName          the ruleset engine's name for the node (from
     *                            {@code ForceDescriptor.parseName()}); blank falls back to a generic
     *                            name
     * @param level               the formation level the node maps to, or {@code null} when the
     *                            echelon has no mapping (treated as battalion-tier: name kept,
     *                            de-duplicated)
     * @param parentDesignator    the alphabet designator of the enclosing company-tier formation, or
     *                            {@code null} when there is none
     * @param ordinalWithinParent 1-based position of this formation among its formation siblings,
     *                            used for the lance number in "Able-1 Battle Lance"
     * @return the unique name plus the designator allocated to it (if any)
     */
    public NamedFormation nameFormation(String engineName, @Nullable FormationLevel level,
          @Nullable String parentDesignator, int ordinalWithinParent) {
        String baseName = (engineName == null || engineName.isBlank()) ? "Formation" : engineName.trim();
        int depth = (level == null) ? Integer.MAX_VALUE : level.getDepth();

        NamedFormation named;
        if (depth == COMPANY_DEPTH) {
            named = nameCompanyTier(baseName);
        } else if (depth < COMPANY_DEPTH) {
            named = nameLanceTier(baseName, parentDesignator, ordinalWithinParent);
        } else {
            named = new NamedFormation(uniqueUpperTierName(baseName), null);
        }
        usedNames.add(normalize(named.name()));
        LOGGER.debug("[CommandGen][Naming] '{}' (level={} parentDesignator={} ordinal={}) -> '{}'",
              engineName, level, parentDesignator, ordinalWithinParent, named.name());
        return named;
    }

    /**
     * Company-tier: allocate the next free alphabet word and prepend it to the engine name with its
     * own designator token stripped ("1/A Company" becomes "Able Company").
     */
    private NamedFormation nameCompanyTier(String baseName) {
        String designator = allocateDesignator();
        if (designator == null) {
            return new NamedFormation(appendCounterUntilFree(stripDesignatorToken(baseName)), null);
        }
        String name = ensureFree(designator + " " + stripDesignatorToken(baseName));
        return new NamedFormation(name, designator);
    }

    /**
     * Lance-tier: "&lt;company designator&gt;-&lt;n&gt; &lt;flavour&gt;" under a company, or an own
     * alphabet word when the lance hangs directly under the command root. The callsign is passed on
     * as the designator so nested formations extend it ("Able-1-1 Squad").
     */
    private NamedFormation nameLanceTier(String baseName, @Nullable String parentDesignator,
          int ordinalWithinParent) {
        String flavour = stripDesignatorToken(baseName);
        if (parentDesignator != null) {
            String callsign = parentDesignator + "-" + Math.max(1, ordinalWithinParent);
            return new NamedFormation(ensureFree(callsign + " " + flavour), callsign);
        }
        String designator = allocateDesignator();
        if (designator == null) {
            return new NamedFormation(appendCounterUntilFree(flavour), null);
        }
        return new NamedFormation(ensureFree(designator + " " + flavour), designator);
    }

    /**
     * Battalion-tier and above: keep the engine name; on a clash, advance its sequence token
     * ("First Battalion" to "Second Battalion", "1st Battalion" to "2nd Battalion") or, when no
     * sequence token is present, append a counter.
     */
    private String uniqueUpperTierName(String baseName) {
        if (!isUsed(baseName)) {
            return baseName;
        }
        String bumped = bumpSequenceToken(baseName);
        if (bumped != null) {
            return bumped;
        }
        return appendCounterUntilFree(baseName);
    }

    /**
     * Advances the first sequence token found in {@code name} (spelled ordinal, alphabet word, or
     * "1st"-style numeric ordinal) to the next value that yields an unused name.
     *
     * @return the bumped name, or {@code null} if no token could produce a free name
     */
    private @Nullable String bumpSequenceToken(String name) {
        String[] tokens = name.split("\\s+");
        for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
            String token = tokens[tokenIndex];

            for (List<String> sequence : sequences) {
                int position = indexOfIgnoreCase(sequence, token);
                if (position < 0) {
                    continue;
                }
                for (int next = position + 1; next < sequence.size(); next++) {
                    String candidate = replaceToken(tokens, tokenIndex, sequence.get(next));
                    if (!isUsed(candidate)) {
                        return candidate;
                    }
                }
            }

            Matcher numericOrdinal = NUMERIC_ORDINAL.matcher(token);
            if (numericOrdinal.matches()) {
                int number = Integer.parseInt(numericOrdinal.group(1));
                for (int next = number + 1; next < number + 1000; next++) {
                    String candidate = replaceToken(tokens, tokenIndex, cardinalOrdinal(next));
                    if (!isUsed(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /** The first free alphabet word of the selected naming method, or {@code null} when exhausted. */
    private @Nullable String allocateDesignator() {
        for (Alphabet letter : Alphabet.values()) {
            String word = namingMethod.getValue(letter);
            if (usedDesignators.add(normalize(word))) {
                return word;
            }
        }
        if (!warnedAlphabetExhausted) {
            warnedAlphabetExhausted = true;
            LOGGER.warn("[CommandGen][Naming] all 26 {} designators are in use; "
                        + "falling back to numbered names", namingMethod.name());
        }
        return null;
    }

    /**
     * Marks the designator of an existing formation name as used so a later run continues the
     * sequence. Recognises both plain designators ("Able Company") and callsigns ("Able-1 Battle
     * Lance" marks "Able").
     */
    private void markDesignatorFromExistingName(String existingName) {
        String firstToken = existingName.trim().split("\\s+")[0];
        int dash = firstToken.indexOf('-');
        if (dash > 0) {
            firstToken = firstToken.substring(0, dash);
        }
        for (Alphabet letter : Alphabet.values()) {
            if (namingMethod.getValue(letter).equalsIgnoreCase(firstToken)) {
                usedDesignators.add(normalize(firstToken));
                return;
            }
        }
    }

    /**
     * Strips a leading or trailing designator token from an engine name so the naming method's word
     * can take its place: "A Company" and "1/A Company" become "Company", "Trinary Bravo" becomes
     * "Trinary", while flavour words are kept ("Battle Lance" is unchanged).
     */
    private String stripDesignatorToken(String name) {
        String[] tokens = name.split("\\s+");
        if (tokens.length < 2) {
            return name;
        }
        if (isDesignatorToken(tokens[0])) {
            return String.join(" ", List.of(tokens).subList(1, tokens.length));
        }
        if (isDesignatorToken(tokens[tokens.length - 1])) {
            return String.join(" ", List.of(tokens).subList(0, tokens.length - 1));
        }
        return name;
    }

    /**
     * Whether {@code token} is a designator rather than flavour: a single letter, a word of any
     * naming alphabet, a spelled or numeric ordinal, or a compound of those ("1/A", "A-1"). Roman
     * numerals are deliberately not designators so ComStar names like "Level III" survive intact.
     */
    private boolean isDesignatorToken(String token) {
        String[] parts = token.split("[/-]");
        if (parts.length > 1) {
            for (String part : parts) {
                if (!isSimpleDesignator(part)) {
                    return false;
                }
            }
            return true;
        }
        return isSimpleDesignator(token);
    }

    private boolean isSimpleDesignator(String token) {
        if (token.isEmpty()) {
            return false;
        }
        if (token.length() == 1 && Character.isLetterOrDigit(token.charAt(0))) {
            return true;
        }
        if (token.chars().allMatch(Character::isDigit)) {
            return true;
        }
        if (NUMERIC_ORDINAL.matcher(token).matches()) {
            return true;
        }
        for (List<String> sequence : sequences) {
            if (indexOfIgnoreCase(sequence, token) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Returns {@code name} unchanged when free, otherwise with an appended counter. */
    private String ensureFree(String name) {
        return isUsed(name) ? appendCounterUntilFree(name) : name;
    }

    private String appendCounterUntilFree(String name) {
        for (int counter = 2; ; counter++) {
            String candidate = name + " (" + counter + ")";
            if (!isUsed(candidate)) {
                return candidate;
            }
        }
    }

    private boolean isUsed(String name) {
        return usedNames.contains(normalize(name));
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static String replaceToken(String[] tokens, int index, String replacement) {
        String[] copy = tokens.clone();
        copy[index] = replacement;
        return String.join(" ", copy);
    }

    private static int indexOfIgnoreCase(List<String> words, String token) {
        for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
            if (words.get(wordIndex).equalsIgnoreCase(token)) {
                return wordIndex;
            }
        }
        return -1;
    }

    /** Formats {@code number} as a numeric ordinal: 1 to "1st", 2 to "2nd", 11 to "11th". */
    private static String cardinalOrdinal(int number) {
        int modHundred = number % 100;
        String suffix;
        if (modHundred >= 11 && modHundred <= 13) {
            suffix = "th";
        } else {
            suffix = switch (number % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
        return number + suffix;
    }
}
