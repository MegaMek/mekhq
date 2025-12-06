/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.logging.MMLogger;

/**
 * Represents blood groups and their genetic inheritance logic in the MekHQ campaign system.
 *
 * <p>
 * This enum defines blood groups with their associated probabilities of occurrence, Rh Factor, genetic composition
 * (alleles), and utility methods for determining inherited blood groups and localized labels. BloodGroup is designed to
 * model real-world blood group inheritance and probability.
 * </p>
 */
public enum BloodGroup {
    AA_POSITIVE(7, true, Allele.A, Allele.A),
    AA_NEGATIVE(1, false, Allele.A, Allele.A),
    AO_POSITIVE(27, true, Allele.A, Allele.O),
    AO_NEGATIVE(5, false, Allele.A, Allele.O),

    AB_POSITIVE(4, true, Allele.A, Allele.B),
    AB_NEGATIVE(1, false, Allele.A, Allele.B),

    BB_POSITIVE(1, true, Allele.B, Allele.B),
    BB_NEGATIVE(1, false, Allele.B, Allele.B),
    BO_POSITIVE(9, true, Allele.B, Allele.O),
    BO_NEGATIVE(1, false, Allele.B, Allele.O),

    OO_POSITIVE(37, true, Allele.O, Allele.O),
    OO_NEGATIVE(6, false, Allele.O, Allele.O);

    private final String label;
    private final int chance;  // Represents the probability of occurrence for the blood group.
    private final boolean hasPositiveRhFactor;  // Indicates if the blood group has a positive Rh factor.
    private final List<Allele> alleles;  // Genetic composition of the blood group.

    /**
     * Constructor to initialize the blood group.
     *
     * @param chance              the chance of occurrence of this blood group.
     * @param hasPositiveRhFactor {@code true} if the blood group has a positive Rh factor, {@code false} otherwise.
     * @param alleles             the alleles that define the genetic composition of the blood group.
     */
    BloodGroup(int chance, boolean hasPositiveRhFactor, Allele... alleles) {
        this.label = generateLabel();
        this.chance = chance;
        this.hasPositiveRhFactor = hasPositiveRhFactor;
        this.alleles = List.of(alleles);  // Store alleles as an immutable list.
    }

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    public String getLabel() {
        return label;
    }

    /**
     * Gets the chance value for this blood group.
     *
     * @return the chance value as an integer.
     */
    public int getChance() {
        return chance;
    }

    /**
     * Checks whether this blood group has a positive Rh factor.
     *
     * @return {@code true} if the blood group has a positive Rh factor, {@code false} otherwise.
     */
    public boolean hasPositiveRhFactor() {
        return hasPositiveRhFactor;
    }

    /**
     * Checks if this blood group is O_NEGATIVE.
     *
     * @return {@code true} if the blood group is O_NEGATIVE, {@code false} otherwise.
     */
    public boolean isUniversalDonor() {
        return this == OO_NEGATIVE;
    }

    /**
     * Checks if this blood group is AB_POSITIVE.
     *
     * @return {@code true} if the blood group is AB_POSITIVE, {@code false} otherwise.
     */
    public boolean isUniversalRecipient() {
        return this == AB_POSITIVE;
    }

    /**
     * Retrieves the alleles associated with this blood group.
     *
     * @return a list of {@link Allele} values that represent the genetic composition.
     */
    public List<Allele> getAlleles() {
        return alleles;
    }

    /**
     * Retrieves the formatted label for this blood group.
     *
     * @return the localized name of the blood group.
     */
    private String generateLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Determines the inherited blood group of a child based on the blood groups of the mother and the father. This is
     * determined by randomly selecting an allele from each parent's blood group and accounting for whether the Rh
     * factor is positive or negative.
     *
     * @param motherBloodGroup The blood group of the mother.
     * @param fatherBloodGroup The blood group of the father.
     *
     * @return The resulting {@link BloodGroup} inherited by the child.
     *
     *       <p>The inheritance is calculated as follows:</p>
     *       <ul>
     *           <li>An allele is randomly chosen from each parent's blood group using their genetic composition.</li>
     *           <li>The Rh Factor is determined to be negative only if both parents have a negative Rh Factor.
     *               Otherwise, it is positive (dominant).</li>
     *           <li>The alleles are combined, and the resulting blood group is calculated based on standard
     *               inheritance rules.</li>
     *       </ul>
     *       <p>
     *       For example:
     *       <ul>
     *           <li>If the mother's blood group is A_POSITIVE and the father's is B_NEGATIVE, the inherited blood
     *               group may be AB_POSITIVE, AB_NEGATIVE, AO_POSITIVE, or AO_NEGATIVE.</li>
     *           <li>If both parents have O_NEGATIVE, the child will always inherit OO_NEGATIVE (recessive).</li>
     *       </ul>
     */
    public static BloodGroup getInheritedBloodGroup(BloodGroup motherBloodGroup, BloodGroup fatherBloodGroup) {
        Allele motherAllele = ObjectUtility.getRandomItem(motherBloodGroup.getAlleles());
        Allele fatherAllele = ObjectUtility.getRandomItem(fatherBloodGroup.getAlleles());

        boolean inheritedRhFactorIsNegative = !motherBloodGroup.hasPositiveRhFactor() &&
                                                    !fatherBloodGroup.hasPositiveRhFactor();

        // Combine alleles to form a key for selecting the blood group
        String alleleKey = (motherAllele.name() + fatherAllele.name()).chars()
                                 .sorted() // Sort allele characters alphabetically (e.g., "AB" or "AO")
                                 .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                 .toString();

        return switch (alleleKey) {
            case "AA" -> inheritedRhFactorIsNegative ? AA_NEGATIVE : AA_POSITIVE;
            case "AB" -> inheritedRhFactorIsNegative ? AB_NEGATIVE : AB_POSITIVE;
            case "AO" -> inheritedRhFactorIsNegative ? AO_NEGATIVE : AO_POSITIVE;
            case "BB" -> inheritedRhFactorIsNegative ? BB_NEGATIVE : BB_POSITIVE;
            case "BO" -> inheritedRhFactorIsNegative ? BO_NEGATIVE : BO_POSITIVE;
            default -> inheritedRhFactorIsNegative ? OO_NEGATIVE : OO_POSITIVE;
        };
    }

    /**
     * Selects a random {@link BloodGroup} based on predefined probabilities for each blood group. The method generates
     * a random integer (0-99) and uses the cumulative probability of all blood groups to determine which group is
     * selected.
     *
     * @return A randomly selected {@link BloodGroup} instance, weighted by its probability (chance). If an error occurs
     *       where probabilities do not sum to 100, it returns {@link BloodGroup#OO_POSITIVE}.
     */
    public static BloodGroup getRandomBloodGroup() {
        int roll = randomInt(100);
        int cumulativeChance = 0;

        for (BloodGroup bloodGroup : BloodGroup.values()) {
            cumulativeChance += bloodGroup.getChance();
            if (roll < cumulativeChance) {
                return bloodGroup;
            }
        }

        MMLogger logger = MMLogger.create(BloodGroup.class);
        logger.error("Blood group probabilities do not sum to 100. Returning {}.", OO_POSITIVE);

        return OO_POSITIVE;
    }

    /**
     * Converts a string into a {@link BloodGroup}.
     *
     * @param text the string to convert. Expected to match blood group names or ordinal values.
     *
     * @return the corresponding {@link BloodGroup} or defaults to {@code O_POSITIVE}.
     */
    public static BloodGroup fromString(String text) {
        try {
            return BloodGroup.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        try {
            return BloodGroup.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        MMLogger logger = MMLogger.create(BloodGroup.class);
        logger.error("Unknown BloodGroup ordinal: {} - returning {}.", text, OO_POSITIVE);

        return OO_POSITIVE;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * Represents the genetic alleles used in determining blood groups.
     */
    public enum Allele {
        A, B, O
    }
}
