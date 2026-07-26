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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import megamek.client.ratgenerator.FormationNamingConvention.DesignatorStyle;
import megamek.client.ratgenerator.FormationNamingConvention.Tier;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.enums.Alphabet;
import mekhq.campaign.universe.enums.ForceNamingMethod;

/**
 * Produces unique, player-parseable names for the {@link mekhq.campaign.force.Formation} nodes the
 * Command Generator mirrors into the campaign TO&amp;E.
 *
 * <p>A generated formation name has two parts, and only one of them belongs to this class:</p>
 * <ul>
 *   <li><b>Flavour</b> - "Battle Lance", "Assault Cluster", "Trinary [Battle]", "IV-alpha". Produced
 *       by the faction ruleset, carries canon meaning, and is preserved verbatim. This class never
 *       invents flavour.</li>
 *   <li><b>Designator</b> - "Alpha", "First", "1". Purely positional, and the part the ruleset cannot
 *       express because it depends on where a formation sits among its siblings.</li>
 * </ul>
 *
 * <p>Which designator each echelon takes is declared per faction in the ruleset's
 * {@code <formationNaming>} element and resolved through {@link Ruleset#findNamingTier(int)}, so
 * Inner Sphere companies take the player's chosen alphabet, Clan galaxies take Greek letters, and
 * ComStar Level IV formations keep the ruleset's name untouched. In particular, a ComStar Greek
 * suffix denotes <em>branch specialisation</em> rather than sequence position - "IV-alpha" and
 * "IV-beta" are different kinds of formation - so those names are never advanced or prefixed.</p>
 *
 * <p><b>Naming happens per sibling group, not per formation.</b> {@link #nameSiblings} receives every
 * formation that shares a parent and names them together, which is what allows designator sequences to
 * restart under each parent ("Alpha, Beta, Gamma" in <em>every</em> battalion rather than running
 * A-to-Z across a regiment) and what lets a collision be resolved consistently for the whole group
 * instead of leaving the first sibling bare and suffixing the second.</p>
 *
 * <p>Uniqueness comes from qualification rather than counters. A tier declaring
 * {@code qualifyWith="parent"} prefixes its parent's designator, giving "1/Alpha Company" and
 * "1/Alpha-1 Battle Lance"; the numeric counter fallback ("... (2)") exists only for names that
 * cannot be qualified, and a name reaching a player with one indicates a ruleset data gap.</p>
 */
public class FormationNamer {

    private static final MMLogger LOGGER = MMLogger.create(FormationNamer.class);

    /** Spelled ordinals matching the ruleset engine's {@code {ordinal}} token values. */
    private static final List<String> SPELLED_ORDINALS = List.of("First", "Second", "Third", "Fourth",
          "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth");

    private static final String[] ROMAN_NUMERALS = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII",
                                                     "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI",
                                                     "XVII", "XVIII", "XIX", "XX" };

    private static final Pattern NUMERIC_ORDINAL = Pattern.compile("(\\d+)(st|nd|rd|th)");

    /** Separator between a parent designator and a letter-style child designator: "1/Alpha". */
    private static final String QUALIFIER_SEPARATOR = "/";

    /** Separator between a parent designator and a numeric child designator: "1/Alpha-2". */
    private static final String NUMERIC_QUALIFIER_SEPARATOR = "-";

    /**
     * Looks up the naming rule for an echelon. Exists so tests can drive the namer without loading the
     * production ruleset data set, and so the namer has no hard dependency on {@link Ruleset}'s static
     * registry.
     */
    @FunctionalInterface
    public interface NamingTierResolver {
        /**
         * @param factionCode the faction whose convention applies, or {@code null} when unknown
         * @param echelon     the echelon to resolve
         *
         * @return the rule for {@code echelon}, or {@code null} to keep the ruleset's own name
         */
        @Nullable
        Tier resolve(@Nullable String factionCode, int echelon);
    }

    /**
     * One formation awaiting a name.
     *
     * @param engineName the ruleset engine's name for the node, from
     *                   {@code ForceDescriptor.parseName()}; blank falls back to a generic name
     * @param level      the formation level the node maps to, or {@code null} when the echelon has no
     *                   mapping
     * @param echelon    the ruleset echelon, used to resolve the naming rule; {@code null} when the
     *                   node carries none, in which case the ruleset's name is kept
     * @param unitType   the {@link megamek.common.units.UnitType} constant this formation is built
     *                   from, or {@code null} for a mixed or unspecified formation. Each unit type
     *                   counts separately, so a regiment's armor and infantry contingents both begin
     *                   at the start of the sequence instead of continuing its battalions'
     * @param faction    the faction code whose convention applies, or {@code null} when unknown
     */
    public record FormationRequest(String engineName, @Nullable FormationLevel level,
                                   @Nullable Integer echelon, @Nullable Integer unitType,
                                   @Nullable String faction) {}

    /** Identifies one independent designator sequence within a sibling group. */
    private record SequenceKey(@Nullable Integer echelon, @Nullable Integer unitType) {}

    /**
     * The name chosen for a formation, plus the designator its children should be qualified with.
     *
     * @param name       the unique display name for the {@link mekhq.campaign.force.Formation}
     * @param designator the token children prefix themselves with (e.g. "1/Alpha"), or {@code null}
     *                   when this tier contributes none
     */
    public record NamedFormation(String name, @Nullable String designator) {}

    private final ForceNamingMethod namingMethod;
    private final NamingTierResolver tierResolver;
    private boolean alwaysNumberRegiments;
    private final Set<String> usedNames = new HashSet<>();
    private final List<List<String>> sequences = new ArrayList<>();
    private boolean warnedAlphabetExhausted;

    /**
     * @param namingMethod  the alphabet style selected on the Setup tab; {@code null} falls back to
     *                      {@link ForceNamingMethod#CCB_1943}
     * @param existingNames every formation name already present in the campaign TO&amp;E, so a second
     *                      generator run does not reuse names the first one took (may be empty)
     */
    public FormationNamer(@Nullable ForceNamingMethod namingMethod, Collection<String> existingNames) {
        this(namingMethod, existingNames, FormationNamer::resolveFromRuleset);
    }

    /**
     * @param namingMethod  the alphabet style selected on the Setup tab; {@code null} falls back to
     *                      {@link ForceNamingMethod#CCB_1943}
     * @param existingNames every formation name already present in the campaign TO&amp;E
     * @param tierResolver  supplies the per-echelon naming rule; production callers use the
     *                      {@link #FormationNamer(ForceNamingMethod, Collection)} constructor, which
     *                      reads the faction rulesets
     */
    public FormationNamer(@Nullable ForceNamingMethod namingMethod, Collection<String> existingNames,
          NamingTierResolver tierResolver) {
        this.namingMethod = (namingMethod == null) ? ForceNamingMethod.CCB_1943 : namingMethod;
        this.tierResolver = tierResolver;

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
        }
        LOGGER.debug("[CommandGen][Naming] namer initialised: method={} existingNames={}",
              this.namingMethod.name(), existingNames.size());
    }

    /**
     * Overrides the faction convention at regiment level so regiments take a numeric ordinal - "1st Mek
     * Regiment", "2nd Mek Regiment" - instead of the selected naming alphabet. Because each unit type
     * counts as its own arm, an armor regiment alongside two Mek regiments is "1st Armor Regiment"
     * rather than "3rd".
     *
     * <p>Keyed on {@link FormationLevel#REGIMENT} rather than on an echelon number, so it applies to
     * Inner Sphere and Periphery regiments without catching the Clan Cluster or ComStar Level IV that
     * share their echelon number.</p>
     *
     * @param alwaysNumberRegiments the player's "Always number regiments" selection
     */
    public void setAlwaysNumberRegiments(boolean alwaysNumberRegiments) {
        this.alwaysNumberRegiments = alwaysNumberRegiments;
    }

    private static @Nullable Tier resolveFromRuleset(@Nullable String factionCode, int echelon) {
        Ruleset ruleset = Ruleset.findRuleset(factionCode);
        return (ruleset == null) ? null : ruleset.findNamingTier(echelon);
    }

    /**
     * Names every formation sharing one parent, in tree order.
     *
     * <p>Designator sequences restart with each call, which is what makes company letters restart in
     * each battalion. Candidates whose resulting name is already taken are skipped, so a second
     * generator run adding a fourth company to an existing battalion continues that battalion's
     * sequence rather than colliding with it.</p>
     *
     * @param siblings         the formations sharing a parent, in the order they appear in the tree
     * @param parentDesignator the enclosing formation's designator, or {@code null} at the top of the
     *                         command
     *
     * @return one {@link NamedFormation} per request, in the same order
     */
    public List<NamedFormation> nameSiblings(List<FormationRequest> siblings,
          @Nullable String parentDesignator) {
        if (siblings.isEmpty()) {
            return List.of();
        }

        // A sibling group is not necessarily homogeneous - a regiment holds its battalions alongside
        // attached armor, aerospace and infantry contingents. Each (echelon, unit type) pair counts as
        // its own arm and runs its own sequence, so the armor contingent starts at the beginning rather
        // than continuing the number the Mek battalions reached. Tree order is restored afterwards.
        Map<SequenceKey, List<Integer>> positionsBySequence = new LinkedHashMap<>();
        for (int position = 0; position < siblings.size(); position++) {
            FormationRequest sibling = siblings.get(position);
            positionsBySequence.computeIfAbsent(new SequenceKey(sibling.echelon(), sibling.unitType()),
                  key -> new ArrayList<>()).add(position);
        }

        List<NamedFormation> named = new ArrayList<>(Collections.nCopies(siblings.size(), null));
        for (List<Integer> positions : positionsBySequence.values()) {
            List<FormationRequest> subgroup = new ArrayList<>(positions.size());
            for (int position : positions) {
                subgroup.add(siblings.get(position));
            }
            List<NamedFormation> subgroupNames = nameOneSequence(subgroup, parentDesignator);
            for (int index = 0; index < positions.size(); index++) {
                named.set(positions.get(index), subgroupNames.get(index));
            }
        }
        return named;
    }

    /**
     * Names siblings that share one echelon and one unit type, and therefore share one naming rule and
     * one designator sequence.
     */
    private List<NamedFormation> nameOneSequence(List<FormationRequest> siblings,
          @Nullable String parentDesignator) {
        List<NamedFormation> named = new ArrayList<>(siblings.size());
        Tier tier = resolveTier(siblings.get(0));
        DesignatorStyle style = (tier == null) ? DesignatorStyle.ENGINE : tier.designatorStyle();
        boolean qualified = (tier != null) && tier.qualifiedByParent() && (parentDesignator != null);

        // Naval formations share echelon numbers with ground ones - a naval Division sits at the same
        // echelon as a Regiment - but they carry no unit type, which is what tells them apart here. The
        // option is about ground regiments, so a Division of WarShips keeps the naming alphabet.
        boolean isGroundFormation = siblings.get(0).unitType() != null;
        if (alwaysNumberRegiments && isGroundFormation
              && (siblings.get(0).level() == FormationLevel.REGIMENT)) {
            LOGGER.debug("[CommandGen][Naming] numbering regiments on player request (was {})", style);
            style = DesignatorStyle.NUMERIC_ORDINAL;
            qualified = false;
            // tier is deliberately left alone: it still decides whether the engine name carries a
            // designator token to strip, so a ruleset name like "First Regiment" becomes
            // "1st Regiment" rather than "1st First Regiment".
        }

        if (style == DesignatorStyle.ENGINE) {
            if (!hasInternalDuplicates(siblings)) {
                return nameFromEngine(siblings, parentDesignator, qualified);
            }
            // The ruleset gave several siblings the same name, so there is nothing to preserve by
            // keeping them. Designate the whole group with the player's alphabet - rather than leaving
            // the first bare and suffixing the rest - and keep each engine name intact as the flavour.
            LOGGER.debug("[CommandGen][Naming] echelon {} siblings share a ruleset name; designating the"
                        + " group instead of keeping it", siblings.get(0).echelon());
            style = DesignatorStyle.ALPHABET;
            qualified = false;
        }

        // A designator marks a formation's position under its own parent, so the sequence restarts with
        // every group and identical names under different parents are expected rather than a clash -
        // "Alpha Company" exists in each battalion, and the tree says which one is meant. The only place
        // an existing campaign name is worth avoiding is the top of the command, where a clash really
        // would be two formations sharing one parent in the TO&E.
        boolean avoidExistingNames = (parentDesignator == null);
        boolean keepWholeEngineName = (tier == null) || (tier.designatorStyle() == DesignatorStyle.ENGINE);
        int allocationIndex = 0;
        for (FormationRequest sibling : siblings) {
            String engineName = baseNameOf(sibling);
            String flavour = keepWholeEngineName ? engineName : stripDesignatorToken(engineName);
            Allocation allocation = allocate(style, allocationIndex, parentDesignator, qualified, flavour,
                  avoidExistingNames);
            allocationIndex = allocation.nextIndex();
            usedNames.add(normalize(allocation.named().name()));
            named.add(allocation.named());
            LOGGER.debug("[CommandGen][Naming] '{}' (echelon={} style={} parent={}) -> '{}'",
                  sibling.engineName(), sibling.echelon(), style, parentDesignator,
                  allocation.named().name());
        }
        return named;
    }

    /** Whether two or more of {@code siblings} carry the same ruleset name. */
    private boolean hasInternalDuplicates(List<FormationRequest> siblings) {
        Set<String> seen = new HashSet<>();
        for (FormationRequest sibling : siblings) {
            if (!seen.add(normalize(baseNameOf(sibling)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The engine's own names are already correct for this echelon and unique among the siblings, so
     * they are kept as-is. At the top of the command - the one place a repeat is really two formations
     * sharing a parent in the TO&amp;E - a name already taken is qualified with the parent designator,
     * and only if that still fails does a counter get appended.
     */
    private List<NamedFormation> nameFromEngine(List<FormationRequest> siblings,
          @Nullable String parentDesignator, boolean qualifiedByDeclaration) {
        boolean avoidExistingNames = (parentDesignator == null);
        List<NamedFormation> named = new ArrayList<>(siblings.size());
        for (FormationRequest sibling : siblings) {
            String engineName = baseNameOf(sibling);
            boolean mustQualify = qualifiedByDeclaration || (avoidExistingNames && isUsed(engineName));
            String candidate = engineName;
            if (mustQualify && (parentDesignator != null)) {
                candidate = parentDesignator + QUALIFIER_SEPARATOR + engineName;
            }
            if (avoidExistingNames && isUsed(candidate)) {
                LOGGER.warn("[CommandGen][Naming] '{}' could not be made unique by qualification"
                            + " (parent={}); falling back to a numbered name. This usually means the"
                            + " faction ruleset gives several sibling formations the same name.",
                      engineName, parentDesignator);
                candidate = appendCounterUntilFree(candidate);
            }
            usedNames.add(normalize(candidate));
            // An engine-named tier contributes no designator of its own, so children continue to
            // qualify against whatever this formation's parent supplied.
            named.add(new NamedFormation(candidate, parentDesignator));
            LOGGER.debug("[CommandGen][Naming] '{}' (echelon={} style=ENGINE parent={}) -> '{}'",
                  sibling.engineName(), sibling.echelon(), parentDesignator, candidate);
        }
        return named;
    }

    /** A chosen name plus the sequence position to resume allocating from. */
    private record Allocation(NamedFormation named, int nextIndex) {}

    /**
     * Takes sequence position {@code startIndex}, which is the formation's position among its
     * siblings.
     *
     * @param avoidExistingNames when {@code true}, positions whose resulting name already exists in the
     *                           campaign are skipped. Only meaningful at the top of the command; within
     *                           the tree a repeated name is how the scheme is supposed to look, and
     *                           skipping would push each battalion's companies further down the
     *                           alphabet than the one before it
     */
    private Allocation allocate(DesignatorStyle style, int startIndex,
          @Nullable String parentDesignator, boolean qualified, String flavour,
          boolean avoidExistingNames) {
        for (int index = startIndex; index < sequenceLength(style); index++) {
            String displayToken = displayToken(style, index);
            String childToken = childToken(style, index);
            String qualifiedDisplay = qualify(displayToken, parentDesignator, qualified, style);
            String qualifiedChild = qualify(childToken, parentDesignator, qualified, style);
            String candidate = flavour.isBlank() ? qualifiedDisplay : (qualifiedDisplay + " " + flavour);
            if (!avoidExistingNames || !isUsed(candidate)) {
                return new Allocation(new NamedFormation(candidate, qualifiedChild), index + 1);
            }
        }

        if (!warnedAlphabetExhausted) {
            warnedAlphabetExhausted = true;
            LOGGER.warn("[CommandGen][Naming] ran out of {} designators; falling back to numbered names",
                  style);
        }
        String fallback = appendCounterUntilFree(flavour.isBlank() ? "Formation" : flavour);
        return new Allocation(new NamedFormation(fallback, parentDesignator), startIndex);
    }

    private String qualify(String token, @Nullable String parentDesignator, boolean qualified,
          DesignatorStyle style) {
        if (!qualified || (parentDesignator == null)) {
            return token;
        }
        // A numeric child reads as a sub-index of its parent ("1/Alpha-2"); a word-style child reads as
        // a new level of the path ("1/Alpha").
        String separator = (style == DesignatorStyle.NUMBER)
              ? NUMERIC_QUALIFIER_SEPARATOR
              : QUALIFIER_SEPARATOR;
        return parentDesignator + separator + token;
    }

    private int sequenceLength(DesignatorStyle style) {
        return switch (style) {
            case ALPHABET, GREEK -> Alphabet.values().length;
            case ROMAN -> ROMAN_NUMERALS.length;
            // Ordinals and plain numbers are unbounded in practice; cap the search so a pathological
            // campaign cannot spin here.
            case ORDINAL, NUMERIC_ORDINAL, NUMBER -> 1000;
            case ENGINE -> 0;
        };
    }

    /** The token shown to the player: "Alpha", "First", "I", "2". */
    private String displayToken(DesignatorStyle style, int index) {
        return switch (style) {
            case ALPHABET -> namingMethod.getValue(Alphabet.values()[index]);
            case GREEK -> Alphabet.values()[index].getGreek();
            case ORDINAL -> (index < SPELLED_ORDINALS.size())
                  ? SPELLED_ORDINALS.get(index)
                  : cardinalOrdinal(index + 1);
            case NUMERIC_ORDINAL -> cardinalOrdinal(index + 1);
            case ROMAN -> ROMAN_NUMERALS[index];
            case NUMBER -> Integer.toString(index + 1);
            case ENGINE -> "";
        };
    }

    /**
     * The token children qualify against. Ordinals differ from their display form on purpose:
     * "First Battalion" reads correctly as prose, but its companies want the compact "1/Alpha
     * Company" rather than "First/Alpha Company".
     */
    private String childToken(DesignatorStyle style, int index) {
        return ((style == DesignatorStyle.ORDINAL) || (style == DesignatorStyle.NUMERIC_ORDINAL))
              ? Integer.toString(index + 1)
              : displayToken(style, index);
    }

    private static String baseNameOf(FormationRequest request) {
        String engineName = request.engineName();
        return (engineName == null || engineName.isBlank()) ? "Formation" : engineName.trim();
    }

    private @Nullable Tier resolveTier(FormationRequest request) {
        if (request.echelon() == null) {
            return null;
        }
        try {
            return tierResolver.resolve(request.faction(), request.echelon());
        } catch (Exception resolutionFailure) {
            LOGGER.warn(resolutionFailure, "[CommandGen][Naming] could not resolve a naming rule for"
                        + " faction {} echelon {}; keeping the ruleset's own names",
                  request.faction(), request.echelon());
            return null;
        }
    }

    /**
     * Strips a leading or trailing designator token so the faction's own designator can take its
     * place: "A Company" and "1/A Company" become "Company", "Trinary Bravo" becomes "Trinary", while
     * flavour words are kept ("Battle Lance" is unchanged).
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
     * Whether {@code token} is a designator rather than flavour: a single letter, a word of any naming
     * alphabet, a spelled or numeric ordinal, or a compound of those ("1/A", "A-1"). Roman numerals
     * are deliberately not designators, so ComStar names like "Level III" and "IV-alpha" survive
     * intact.
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
