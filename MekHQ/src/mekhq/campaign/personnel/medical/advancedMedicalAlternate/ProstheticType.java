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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechRating;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

public enum ProstheticType {
    WOODEN_ARM("WOODEN_ARM",
          1,
          2,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.WOODEN_ARM,
          Money.of(75),
          TechRating.A,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    HOOK_HAND("HOOK_HAND",
          1,
          2,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.HOOK_HAND,
          Money.of(75),
          TechRating.A,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    PEG_LEG("PEG_LEG",
          1,
          2,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.PEG_LEG,
          Money.of(75),
          TechRating.A,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    WOODEN_FOOT("WOODEN_FOOT",
          1,
          2,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.WOODEN_FOOT,
          Money.of(75),
          TechRating.A,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    SIMPLE_ARM("SIMPLE_ARM",
          2,
          2,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.SIMPLE_ARM,
          Money.of(750),
          TechRating.B,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_CLAW_HAND("SIMPLE_CLAW_HAND",
          2,
          2,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.SIMPLE_CLAW_HAND,
          Money.of(750),
          TechRating.B,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_LEG("SIMPLE_LEG",
          2,
          2,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.SIMPLE_LEG,
          Money.of(250),
          TechRating.B,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_FOOT("SIMPLE_FOOT",
          2,
          2,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.SIMPLE_FOOT,
          Money.of(250),
          TechRating.B,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    PROSTHETIC_ARM("PROSTHETIC_ARM",
          3,
          5,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.PROSTHETIC_ARM,
          Money.of(7500),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_HAND("PROSTHETIC_HAND",
          3,
          5,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.PROSTHETIC_HAND,
          Money.of(7500),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_LEG("PROSTHETIC_LEG",
          3,
          5,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.PROSTHETIC_LEG,
          Money.of(10000),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_FOOT("PROSTHETIC_FOOT",
          3,
          5,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.PROSTHETIC_FOOT,
          Money.of(10000),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    ADVANCED_PROSTHETIC_ARM("ADVANCED_PROSTHETIC_ARM",
          4,
          5,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.ADVANCED_PROSTHETIC_ARM,
          Money.of(25000),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_HAND("ADVANCED_PROSTHETIC_HAND",
          4,
          5,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.ADVANCED_PROSTHETIC_HAND,
          Money.of(25000),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_LEG("ADVANCED_PROSTHETIC_LEG",
          4,
          5,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.ADVANCED_PROSTHETIC_LEG,
          Money.of(17500),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_FOOT("ADVANCED_PROSTHETIC_FOOT",
          4,
          5,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.ADVANCED_PROSTHETIC_FOOT,
          Money.of(17500),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    MYOMER_ARM("MYOMER_ARM",
          5,
          5,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.MYOMER_ARM,
          Money.of(200000),
          TechRating.E,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    MYOMER_HAND("MYOMER_HAND",
          5,
          5,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.MYOMER_HAND,
          Money.of(100000),
          TechRating.E,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    MYOMER_LEG("MYOMER_LEG",
          5,
          5,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.MYOMER_LEG,
          Money.of(125000),
          TechRating.E,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    MYOMER_FOOT("MYOMER_FOOT",
          5,
          5,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.MYOMER_FOOT,
          Money.of(50000),
          TechRating.E,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    CLONED_ARM("CLONED_ARM",
          6,
          5,
          List.of(LEFT_ARM, RIGHT_ARM),
          AlternateInjuries.CLONED_ARM,
          Money.of(500000),
          TechRating.E,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    CLONED_HAND("CLONED_HAND",
          6,
          5,
          List.of(LEFT_HAND, RIGHT_HAND),
          AlternateInjuries.CLONED_HAND,
          Money.of(300000),
          TechRating.E,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    CLONED_LEG("CLONED_LEG",
          6,
          5,
          List.of(LEFT_LEG, RIGHT_LEG),
          AlternateInjuries.CLONED_LEG,
          Money.of(350000),
          TechRating.E,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    CLONED_FOOT("CLONED_FOOT",
          6,
          5,
          List.of(LEFT_FOOT, RIGHT_FOOT),
          AlternateInjuries.CLONED_FOOT,
          Money.of(50000),
          TechRating.E,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false),
    EYE_IMPLANT("EYE_IMPLANT",
          2,
          2,
          List.of(EYES),
          AlternateInjuries.EYE_IMPLANT,
          Money.of(350),
          TechRating.B,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_EAR("BIONIC_EAR",
          3,
          5,
          List.of(EARS),
          AlternateInjuries.BIONIC_EAR,
          Money.of(100000),
          TechRating.C,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_EYE("BIONIC_EYE",
          4,
          5,
          List.of(EYES),
          AlternateInjuries.BIONIC_EYE,
          Money.of(220000),
          TechRating.C,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_HEART("BIONIC_HEART",
          3,
          5,
          List.of(HEART),
          AlternateInjuries.BIONIC_HEART,
          Money.of(500000),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_LUNGS("BIONIC_LUNGS",
          4,
          5,
          List.of(LUNGS),
          AlternateInjuries.BIONIC_LUNGS,
          Money.of(800000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    BIONIC_ORGAN_OTHER("BIONIC_ORGAN_OTHER",
          4,
          5,
          List.of(ORGANS),
          AlternateInjuries.BIONIC_ORGAN_OTHER,
          Money.of(750000),
          TechRating.C,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false),
    COSMETIC_SURGERY("COSMETIC_SURGERY",
          2,
          2,
          List.of(), // This is a special case used to heal burns
          AlternateInjuries.COSMETIC_SURGERY,
          Money.of(2500),
          TechRating.A,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false);

    private final String lookupName;
    private final int prostheticType;
    private final int surgeryLevel;
    private final List<BodyLocation> eligibleLocations;
    private final InjuryType injuryType;
    private final Money baseCost;
    private final TechRating technologyRating;
    private final AvailabilityValue availabilityEarly;
    private final AvailabilityValue availabilityMid;
    private final AvailabilityValue availabilityLate;
    private final boolean isComStarOnly;
    private final boolean isClanOnly;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ProstheticType";

    // Availability multipliers
    private static final int EARLY_ERA_CUTOFF = 2800;
    private static final int LATE_ERA_START = 3051;

    private static final double AVAILABILITY_MULTIPLIER_A = 1.0; // Very Common
    private static final double AVAILABILITY_MULTIPLIER_B = 1.0; // Common
    private static final double AVAILABILITY_MULTIPLIER_C = 1.0; // Uncommon
    private static final double AVAILABILITY_MULTIPLIER_D = 1.25; // Rare
    private static final double AVAILABILITY_MULTIPLIER_E = 1.5; // Very Rare
    private static final double AVAILABILITY_MULTIPLIER_F = 10.0; // Unique
    private static final double AVAILABILITY_MULTIPLIER_F_STAR = 0.0; // Unavailable
    private static final double AVAILABILITY_MULTIPLIER_X = 0.0; // Unavailable

    ProstheticType(String lookupName, int prostheticType, int surgeryLevel, List<BodyLocation> eligibleLocations,
          InjuryType injuryType,
          Money baseCost, TechRating technologyRating, AvailabilityValue availabilityEarly,
          AvailabilityValue availabilityMid, AvailabilityValue availabilityLate, boolean isClanOnly,
          boolean isComStarOnly) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.eligibleLocations = eligibleLocations;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.technologyRating = technologyRating;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
    }

    public int getProstheticType() {
        return prostheticType;
    }

    public int getSurgeryLevel() {
        return surgeryLevel;
    }

    public List<BodyLocation> getEligibleLocations() {
        return eligibleLocations;
    }

    public InjuryType getInjuryType() {
        return injuryType;
    }

    public Money getBaseCost() {
        return baseCost;
    }

    public TechRating getTechnologyRating() {
        return technologyRating;
    }

    public AvailabilityValue getAvailabilityEarly() {
        return availabilityEarly;
    }

    public AvailabilityValue getAvailabilityMid() {
        return availabilityMid;
    }

    public AvailabilityValue getAvailabilityLate() {
        return availabilityLate;
    }

    public boolean isClanOnly() {
        return isClanOnly;
    }

    public boolean isComStarOnly() {
        return isComStarOnly;
    }

    public boolean isAvailableToFaction(Faction campaignFaction) {
        if (!campaignFaction.isClan() && isClanOnly) {
            return false;
        }

        return campaignFaction.isComStarOrWoB() || !isComStarOnly;
    }

    public boolean isAvailableInCurrentLocation(CurrentLocation currentLocation, LocalDate today) {
        // If the campaign is in transit, we treat them as Technology Rating B
        if (!currentLocation.isOnPlanet()) {
            return !TechRating.B.isBetterThan(technologyRating);
        }
        // Otherwise, we check based on the Technology Rating of the current planet
        Planet planet = currentLocation.getPlanet();
        TechRating techRating = planet.getTechRating(today);
        return !techRating.isBetterThan(technologyRating);
    }

    public @Nullable Money getCost(int gameYear) {
        double availabilityMultiplier = getAvailabilityMultiplier(gameYear);
        if (availabilityMultiplier == 0.0) { // Not available for purchase
            return null;
        }

        return baseCost.multipliedBy(availabilityMultiplier);
    }

    public double getAvailabilityMultiplier(int gameYear) {
        AvailabilityValue availability = getAvailability(gameYear);
        return switch (availability) {
            case A -> AVAILABILITY_MULTIPLIER_A;
            case B -> AVAILABILITY_MULTIPLIER_B;
            case C -> AVAILABILITY_MULTIPLIER_C;
            case D -> AVAILABILITY_MULTIPLIER_D;
            case E -> AVAILABILITY_MULTIPLIER_E;
            case F -> AVAILABILITY_MULTIPLIER_F;
            case F_STAR -> AVAILABILITY_MULTIPLIER_F_STAR;
            case X -> AVAILABILITY_MULTIPLIER_X;
        };
    }

    private AvailabilityValue getAvailability(int gameYear) {
        if (gameYear < EARLY_ERA_CUTOFF) {
            return availabilityEarly;
        } else if (gameYear >= LATE_ERA_START) {
            return availabilityLate;
        } else {
            return availabilityMid;
        }
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "ProstheticType." + lookupName + ".name");
    }

    public String getTooltip() {
        // Map attributes to their aggregated modifiers
        Map<SkillAttribute, Integer> attributeTotals = new EnumMap<>(SkillAttribute.class);

        // Aggregate modifiers
        InjuryEffect effect = injuryType.getInjuryEffect();
        int perception = effect.getPerceptionModifier();
        addToMap(attributeTotals, SkillAttribute.STRENGTH, effect.getStrengthModifier());
        addToMap(attributeTotals, SkillAttribute.BODY, effect.getBodyModifier());
        addToMap(attributeTotals, SkillAttribute.REFLEXES, effect.getReflexesModifier());
        addToMap(attributeTotals, SkillAttribute.DEXTERITY, effect.getDexterityModifier());
        addToMap(attributeTotals, SkillAttribute.INTELLIGENCE, effect.getIntelligenceModifier());
        addToMap(attributeTotals, SkillAttribute.WILLPOWER, effect.getWillpowerModifier());
        addToMap(attributeTotals, SkillAttribute.CHARISMA, effect.getCharismaModifier());

        // Build tooltip
        List<String> tooltipPortion = new ArrayList<>();
        tooltipPortion.add(getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.general"));

        if (perception != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ProstheticType.tooltip.perception", perception));
        }

        for (SkillAttribute attribute : SkillAttribute.values()) {
            int modifier = attributeTotals.getOrDefault(attribute, 0);
            if (modifier != 0) {
                tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "ProstheticType.tooltip.attribute", modifier, attribute));
            }
        }

        return String.join(" ", tooltipPortion);
    }

    private static void addToMap(Map<SkillAttribute, Integer> map, SkillAttribute key, int value) {
        map.merge(key, value, Integer::sum);
    }
}
