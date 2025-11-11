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

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.options.OptionsConstants.UNOFFICIAL_EI_IMPLANT;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_ADDICTION;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechRating;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * Enumeration representing the various types of prosthetics and artificial body replacements available in the Alternate
 * Advanced Medical system.
 *
 * <p>Each {@code ProstheticType} entry defines attributes such as its base cost, required surgery level, associated
 * {@link InjuryType}, and availability across different eras and factions. These values are used by the MekHQ medical
 * framework to determine purchase cost, eligibility, and in-game behavior.</p>
 *
 * <p>Prosthetic types range from crude wooden limbs to advanced cloned or myomer replacements. Each type also
 * encodes its associated technology rating, factional exclusivity, and temporal availability.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public enum ProstheticType {
    WOODEN_ARM("WOODEN_ARM",
          1,
          2,
          AlternateInjuries.WOODEN_ARM,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    HOOK_HAND("HOOK_HAND",
          1,
          2,
          AlternateInjuries.HOOK_HAND,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    PEG_LEG("PEG_LEG",
          1,
          2,
          AlternateInjuries.PEG_LEG,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    WOODEN_FOOT("WOODEN_FOOT",
          1,
          2,
          AlternateInjuries.WOODEN_FOOT,
          Money.of(75),
          TechRating.F,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    SIMPLE_ARM("SIMPLE_ARM",
          2,
          2,
          AlternateInjuries.SIMPLE_ARM,
          Money.of(750),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_CLAW_HAND("SIMPLE_CLAW_HAND",
          2,
          2,
          AlternateInjuries.SIMPLE_CLAW_HAND,
          Money.of(750),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_LEG("SIMPLE_LEG",
          2,
          2,
          AlternateInjuries.SIMPLE_LEG,
          Money.of(250),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    SIMPLE_FOOT("SIMPLE_FOOT",
          2,
          2,
          AlternateInjuries.SIMPLE_FOOT,
          Money.of(250),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A,
          false,
          false),
    PROSTHETIC_ARM("PROSTHETIC_ARM",
          3,
          5,
          AlternateInjuries.PROSTHETIC_ARM,
          Money.of(7500),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_HAND("PROSTHETIC_HAND",
          3,
          5,
          AlternateInjuries.PROSTHETIC_HAND,
          Money.of(7500),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_LEG("PROSTHETIC_LEG",
          3,
          5,
          AlternateInjuries.PROSTHETIC_LEG,
          Money.of(10000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    PROSTHETIC_FOOT("PROSTHETIC_FOOT",
          3,
          5,
          AlternateInjuries.PROSTHETIC_FOOT,
          Money.of(10000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    ADVANCED_PROSTHETIC_ARM("ADVANCED_PROSTHETIC_ARM",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_ARM,
          Money.of(25000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_HAND("ADVANCED_PROSTHETIC_HAND",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_HAND,
          Money.of(25000),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_LEG("ADVANCED_PROSTHETIC_LEG",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_LEG,
          Money.of(17500),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    ADVANCED_PROSTHETIC_FOOT("ADVANCED_PROSTHETIC_FOOT",
          4,
          5,
          AlternateInjuries.ADVANCED_PROSTHETIC_FOOT,
          Money.of(17500),
          TechRating.C,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    MYOMER_ARM("MYOMER_ARM",
          5,
          5,
          AlternateInjuries.MYOMER_ARM,
          Money.of(200000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_HAND("MYOMER_HAND",
          5,
          5,
          AlternateInjuries.MYOMER_HAND,
          Money.of(100000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_LEG("MYOMER_LEG",
          5,
          5,
          AlternateInjuries.MYOMER_LEG,
          Money.of(125000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    MYOMER_FOOT("MYOMER_FOOT",
          5,
          5,
          AlternateInjuries.MYOMER_FOOT,
          Money.of(50000),
          TechRating.B,
          AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E,
          false,
          true),
    CLONED_ARM("CLONED_ARM",
          6,
          5,
          AlternateInjuries.CLONED_ARM,
          Money.of(500000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_HAND("CLONED_HAND",
          6,
          5,
          AlternateInjuries.CLONED_HAND,
          Money.of(300000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_LEG("CLONED_LEG",
          6,
          5,
          AlternateInjuries.CLONED_LEG,
          Money.of(350000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    CLONED_FOOT("CLONED_FOOT",
          6,
          5,
          AlternateInjuries.CLONED_FOOT,
          Money.of(50000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          true,
          false),
    EYE_IMPLANT("EYE_IMPLANT",
          2,
          2,
          AlternateInjuries.EYE_IMPLANT,
          Money.of(350),
          TechRating.E,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_EAR("BIONIC_EAR",
          3,
          5,
          AlternateInjuries.BIONIC_EAR,
          Money.of(100000),
          TechRating.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_EYE("BIONIC_EYE",
          4,
          5,
          AlternateInjuries.BIONIC_EYE,
          Money.of(220000),
          TechRating.D,
          AvailabilityValue.A, AvailabilityValue.C, AvailabilityValue.A,
          false,
          false),
    BIONIC_HEART("BIONIC_HEART",
          3,
          5,
          AlternateInjuries.BIONIC_HEART,
          Money.of(500000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B,
          false,
          false),
    BIONIC_LUNGS("BIONIC_LUNGS",
          4,
          5,
          AlternateInjuries.BIONIC_LUNGS,
          Money.of(800000),
          TechRating.D,
          AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C,
          false,
          false),
    BIONIC_ORGAN_OTHER("BIONIC_ORGAN_OTHER",
          4,
          5,
          AlternateInjuries.BIONIC_ORGAN_OTHER,
          Money.of(750000),
          TechRating.D,
          AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.C,
          false,
          false),
    COSMETIC_SURGERY("COSMETIC_SURGERY",
          2,
          2,
          AlternateInjuries.COSMETIC_SURGERY,
          Money.of(2500),
          TechRating.E,
          AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
          false,
          false),
    ELECTIVE_MYOMER_ARM("ELECTIVE_MYOMER_ARM",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_ARM,
          Money.of(300000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_ADDICTION)),
    ELECTIVE_MYOMER_HAND("ELECTIVE_MYOMER_HAND",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_HAND,
          Money.of(150000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_ADDICTION)),
    ELECTIVE_MYOMER_LEG("ELECTIVE_MYOMER_LEG",
          5,
          5,
          AlternateInjuries.ELECTIVE_MYOMER_LEG,
          Money.of(375000),
          TechRating.B,
          AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E,
          false,
          false,
          List.of(),
          List.of(COMPULSION_ADDICTION)),
    ENHANCED_IMAGING("ENHANCED_IMAGING",
          5,
          5,
          AlternateInjuries.ENHANCED_IMAGING,
          Money.of(1500000),
          TechRating.A,
          AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D,
          true,
          false,
          List.of(UNOFFICIAL_EI_IMPLANT),
          List.of(COMPULSION_ADDICTION));

    private final String lookupName;
    private final int prostheticType;
    private final int surgeryLevel;
    private final InjuryType injuryType;
    private final Money baseCost;
    private final TechRating technologyRating;
    private final AvailabilityValue availabilityEarly;
    private final AvailabilityValue availabilityMid;
    private final AvailabilityValue availabilityLate;
    private final boolean isComStarOnly;
    private final boolean isClanOnly;
    private final List<String> associatedPilotOptions;
    private final List<String> associatedPersonnelOptions;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ProstheticType";

    // Era boundaries
    private static final int EARLY_ERA_CUTOFF = 2800;
    private static final int LATE_ERA_START = 3051;

    // Availability cost multipliers
    private static final double AVAILABILITY_MULTIPLIER_A = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_B = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_C = 1.0;
    private static final double AVAILABILITY_MULTIPLIER_D = 1.25;
    private static final double AVAILABILITY_MULTIPLIER_E = 1.5;
    private static final double AVAILABILITY_MULTIPLIER_F = 10.0;
    private static final double AVAILABILITY_MULTIPLIER_F_STAR = 0.0;
    private static final double AVAILABILITY_MULTIPLIER_X = 0.0;


    /**
     * Constructs a new {@code ProstheticType} entry.
     *
     * @param lookupName        the resource key for localization and lookup
     * @param prostheticType    the prosthetic tier (as per ATOW)
     * @param surgeryLevel      the minimum medical skill or facility level required
     * @param injuryType        the injury this prosthetic 'inflicts'
     * @param baseCost          the base market price before modifiers (as per ATOW)
     * @param technologyRating  the required technology rating for construction (as per ATOW)
     * @param availabilityEarly availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid   availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate  availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly        whether this item is exclusive to Clan factions (as per ATOW)
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, int prostheticType, int surgeryLevel, InjuryType injuryType, Money baseCost,
          TechRating technologyRating, AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid,
          AvailabilityValue availabilityLate, boolean isClanOnly, boolean isComStarOnly) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.technologyRating = technologyRating;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.associatedPilotOptions = new ArrayList<>();
        this.associatedPersonnelOptions = new ArrayList<>();
    }

    /**
     * Constructs a new {@code ProstheticType} entry.
     *
     * @param lookupName                 the resource key for localization and lookup
     * @param prostheticType             the prosthetic tier (as per ATOW)
     * @param surgeryLevel               the minimum medical skill or facility level required
     * @param injuryType                 the injury this prosthetic 'inflicts'
     * @param baseCost                   the base market price before modifiers (as per ATOW)
     * @param technologyRating           the required technology rating for construction (as per ATOW)
     * @param availabilityEarly          availability rating for early eras (pre-2800) (as per ATOW)
     * @param availabilityMid            availability rating for middle eras (2800–3050) (as per ATOW)
     * @param availabilityLate           availability rating for late eras (3051+) (as per ATOW)
     * @param isClanOnly                 whether this item is exclusive to Clan factions (as per ATOW)
     * @param associatedPilotOptions     Any Pilot Options that should be added to the character when they receive this
     *                                   prosthetic
     * @param associatedPersonnelOptions Any Personnel Options that should be added to the character when they received
     *                                   this prosthetic
     *
     * @author Illiani
     * @since 0.50.10
     */
    ProstheticType(String lookupName, int prostheticType, int surgeryLevel, InjuryType injuryType, Money baseCost,
          TechRating technologyRating, AvailabilityValue availabilityEarly, AvailabilityValue availabilityMid,
          AvailabilityValue availabilityLate, boolean isClanOnly, boolean isComStarOnly,
          List<String> associatedPilotOptions, List<String> associatedPersonnelOptions) {
        this.lookupName = lookupName;
        this.prostheticType = prostheticType;
        this.surgeryLevel = surgeryLevel;
        this.injuryType = injuryType;
        this.baseCost = baseCost;
        this.technologyRating = technologyRating;
        this.availabilityEarly = availabilityEarly;
        this.availabilityMid = availabilityMid;
        this.availabilityLate = availabilityLate;
        this.isClanOnly = isClanOnly;
        this.isComStarOnly = isComStarOnly;
        this.associatedPilotOptions = associatedPilotOptions;
        this.associatedPersonnelOptions = associatedPersonnelOptions;
    }

    /** @return the prosthetic classification. */
    public int getProstheticType() {
        return prostheticType;
    }

    /** @return the minimum surgical skill required. */
    public int getSurgeryLevel() {
        return surgeryLevel;
    }

    /**
     * Retrieves all valid body locations this prosthetic can replace, as defined by its associated {@link InjuryType}.
     *
     * @return a set of {@link BodyLocation} values eligible for replacement.
     */
    public Set<BodyLocation> getEligibleLocations() {
        return injuryType.getAllowedLocations();
    }

    /** @return the {@link InjuryType} this prosthetic 'inflicts'. */
    public InjuryType getInjuryType() {
        return injuryType;
    }

    public List<String> getAssociatedPilotOptions() {
        return associatedPilotOptions;
    }

    public List<String> getAssociatedPersonnelOptions() {
        return associatedPersonnelOptions;
    }

    /**
     * Determines if the given faction can access this prosthetic type.
     *
     * @param campaignFaction the faction to check
     *
     * @return {@code true} if the faction can access this prosthetic type; otherwise {@code false}.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isAvailableToFaction(Faction campaignFaction) {
        if (!campaignFaction.isClan() && isClanOnly) {
            return false;
        }
        return campaignFaction.isComStarOrWoB() || !isComStarOnly;
    }

    /**
     * Checks if this prosthetic is available for purchase or use based on the current location and planetary tech
     * rating.
     *
     * @param currentLocation the campaign's current location
     * @param today           the in-game date
     *
     * @return {@code true} if available in the current location and era
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isAvailableInCurrentLocation(CurrentLocation currentLocation, LocalDate today) {
        int minimumTechRating = TechRating.E.getIndex();
        int prostheticTechLevel = technologyRating.getIndex();

        if (!currentLocation.isOnPlanet()) {
            // In transit: availability limited to rating E or lower
            return minimumTechRating >= prostheticTechLevel;
        }

        Planet planet = currentLocation.getPlanet();
        int planetTechRating = max(minimumTechRating, planet.getTechRating(today).getIndex());

        return planetTechRating >= prostheticTechLevel;
    }

    /**
     * Calculates the adjusted cost for this prosthetic based on the game year. The price may vary depending on its
     * availability in that era.
     *
     * @param gameYear the current in-game year
     *
     * @return the adjusted cost, or {@code null} if the item is not available
     *
     * @author Illiani
     * @since 0.50.10
     */
    public @Nullable Money getCost(int gameYear) {
        double availabilityMultiplier = getAvailabilityMultiplier(gameYear);
        if (availabilityMultiplier == 0.0) {
            return null;
        }
        return baseCost.multipliedBy(availabilityMultiplier);
    }

    /**
     * Returns the price multiplier for this prosthetic based on its availability rating in the specified year.
     *
     * @param gameYear the current in-game year
     *
     * @return a multiplier representing rarity and availability
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Determines which {@link AvailabilityValue} applies for a given year.
     *
     * @param gameYear the current in-game year
     *
     * @return the effective {@link AvailabilityValue} for that era
     *
     * @author Illiani
     * @since 0.50.10
     */
    private AvailabilityValue getAvailability(int gameYear) {
        if (gameYear < EARLY_ERA_CUTOFF) {
            return availabilityEarly;
        } else if (gameYear >= LATE_ERA_START) {
            return availabilityLate;
        } else {
            return availabilityMid;
        }
    }

    /**
     * Returns the localized display name for this prosthetic type.
     *
     * @return the translated name string
     */
    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "ProstheticType." + lookupName + ".name");
    }

    /**
     * Builds a localized tooltip summarizing key information about this prosthetic, including cost, surgical
     * requirements, and attribute modifiers.
     *
     * @param gameYear the current in-game year for cost and availability calculation
     *
     * @return a formatted tooltip string suitable for UI display
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getTooltip(int gameYear, boolean isUseKinderMode) {
        Map<SkillAttribute, Integer> attributeTotals = new EnumMap<>(SkillAttribute.class);
        InjuryEffect effect = injuryType.getInjuryEffect();
        int perception = effect.getPerceptionModifier();

        addToMap(attributeTotals, SkillAttribute.STRENGTH, effect.getStrengthModifier());
        addToMap(attributeTotals, SkillAttribute.BODY, effect.getBodyModifier());
        addToMap(attributeTotals, SkillAttribute.REFLEXES, effect.getReflexesModifier());
        addToMap(attributeTotals, SkillAttribute.DEXTERITY, effect.getDexterityModifier());
        addToMap(attributeTotals, SkillAttribute.INTELLIGENCE, effect.getIntelligenceModifier());
        addToMap(attributeTotals, SkillAttribute.WILLPOWER, effect.getWillpowerModifier());
        addToMap(attributeTotals, SkillAttribute.CHARISMA, effect.getCharismaModifier());

        List<String> tooltipPortion = new ArrayList<>();
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.skill", surgeryLevel));

        Money cost = getCost(gameYear);
        if (cost != null) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.cost",
                  cost.toAmountString()));
        }

        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.techLevel",
              technologyRating.getName()));

        int recoveryTime = (int) round(injuryType.getBaseRecoveryTime() * (isUseKinderMode ? 0.5 : 1.0));
        tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.recovery", recoveryTime));

        for (SkillAttribute attribute : SkillAttribute.values()) {
            int modifier = attributeTotals.getOrDefault(attribute, 0);
            if (modifier != 0) {
                tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "ProstheticType.tooltip.attribute", modifier, attribute));
            }
        }

        if (perception != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.perception", perception));
        }

        for (String option : associatedPilotOptions) {
            String label = switch (option) {
                case UNOFFICIAL_EI_IMPLANT -> getTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.label.ei");
                default -> option;
            };

            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.spa", label));

            if (option.equals(UNOFFICIAL_EI_IMPLANT)) {
                tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.ei", label));
            }
        }

        for (String option : associatedPersonnelOptions) {
            SpecialAbility ability = SpecialAbility.getAbility(option);
            String label = ability == null ? option : ability.getDisplayName();
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE, "ProstheticType.tooltip.spa", label));
        }

        return String.join(" ", tooltipPortion);
    }

    /**
     * Utility method for aggregating skill attribute modifiers.
     *
     * @param map   the aggregation map
     * @param key   the skill attribute being modified
     * @param value the modifier to add (ignored if zero)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addToMap(Map<SkillAttribute, Integer> map, SkillAttribute key, int value) {
        map.merge(key, value, Integer::sum);
    }
}
