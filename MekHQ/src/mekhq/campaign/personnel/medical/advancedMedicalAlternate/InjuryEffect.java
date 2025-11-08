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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public enum InjuryEffect {
    NONE("NONE"),
    BLINDED("BLINDED", getWarningColor(), -3, 0, 0, 0, 0, 0, 0, 0, false),
    COMPOUND_FRACTURE("COMPOUND_FRACTURE", getNegativeColor(), 0, 0, 0, -3, -3, 0, 0, 0, false),
    DEAFENED("DEAFENED", getWarningColor(), -3, 0, 0, 0, 0, 0, 0, 0, false),
    FRACTURE_JAW("FRACTURE_JAW", getWarningColor(), 0, 0, 0, 0, 0, 0, 0, -3, false),
    FRACTURE_LIMB("FRACTURE_LIMB", getWarningColor(), 0, 0, 0, -2, -2, 0, 0, 0, false),
    FRACTURE_RIB("FRACTURE_RIB", getWarningColor(), 0, -1, -1, -1, 0, 0, 0, 0, false),
    FRACTURE_SKULL("FRACTURE_SKULL", getWarningColor(), 0, 0, 0, 0, 0, -2, -2, -2, false),
    INTERNAL_BLEEDING("INTERNAL_BLEEDING", getNegativeColor(), 0, 0, 0, 0, 0, 0, 0, 0, true),
    PUNCTURED("PUNCTURED", getNegativeColor(), 0, -2, -2, -2, 0, 0, 0, 0, false),
    SEVERED("SEVERED", getNegativeColor(), 0, 0, 0, -5, -5, 0, 0, 0, false),
    BLOOD_LOSS("BLOOD_LOSS", getNegativeColor(), 0, -1, -1, -1, -1, -1, -1, -1, false),
    DISEASE_DEADLY("DISEASE_DEADLY", getNegativeColor(), 0, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_GROWTHS_SLIGHT("DISEASE_GROWTHS_SLIGHT", getWarningColor(), 0, 0, -1, 0, -1, 0, 0, 0, false),
    DISEASE_GROWTHS_MODERATE("DISEASE_GROWTHS_MODERATE", getWarningColor(), 0, 0, -2, 0, -2, 0, 0, 0, false),
    DISEASE_GROWTHS_SEVERE("DISEASE_GROWTHS_SEVERE", getNegativeColor(), 0, 0, -3, 0, -3, 0, 0, 0, false),
    DISEASE_INFECTION_SLIGHT("DISEASE_INFECTION_SLIGHT", getWarningColor(), 0, 0, -1, 0, 0, 0, 0, -1, false),
    DISEASE_INFECTION_MODERATE("DISEASE_INFECTION_MODERATE", getWarningColor(), 0, 0, -2, 0, 0, 0, 0, -2, false),
    DISEASE_INFECTION_SEVERE("DISEASE_INFECTION_SEVERE", getNegativeColor(), 0, 0, -3, 0, 0, 0, 0, -3, false),
    DISEASE_HEARING_SLIGHT("DISEASE_HEARING_SLIGHT", getWarningColor(), -1, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_HEARING_MODERATE("DISEASE_HEARING_MODERATE", getWarningColor(), -2, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_HEARING_SEVERE("DISEASE_HEARING_SEVERE", getNegativeColor(), -3, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_WEAKNESS_SLIGHT("DISEASE_WEAKNESS_SLIGHT", getWarningColor(), 0, -1, -1, 0, 0, 0, 0, 0, false),
    DISEASE_WEAKNESS_MODERATE("DISEASE_WEAKNESS_MODERATE", getWarningColor(), 0, -2, -2, 0, 0, 0, 0, 0, false),
    DISEASE_WEAKNESS_SEVERE("DISEASE_WEAKNESS_SEVERE", getNegativeColor(), 0, -3, -3, 0, 0, 0, 0, 0, false),
    DISEASE_SORES_SLIGHT("DISEASE_SORES_SLIGHT", getWarningColor(), 0, 0, 0, 0, 0, 0, 0, -1, false),
    DISEASE_SORES_MODERATE("DISEASE_SORES_MODERATE", getWarningColor(), 0, 0, 0, 0, 0, 0, 0, -2, false),
    DISEASE_SORES_SEVERE("DISEASE_SORES_SEVERE", getNegativeColor(), 0, 0, 0, 0, 0, 0, 0, -3, false),
    DISEASE_FLU_SLIGHT("DISEASE_FLU_SLIGHT", getWarningColor(), 0, -1, -1, 0, 0, 0, 0, 0, false),
    DISEASE_FLU_MODERATE("DISEASE_FLU_MODERATE", getWarningColor(), 0, -2, -2, 0, 0, 0, 0, 0, false),
    DISEASE_FLU_SEVERE("DISEASE_FLU_SEVERE", getNegativeColor(), 0, -3, -3, 0, 0, 0, 0, 0, false),
    DISEASE_SIGHT_SLIGHT("DISEASE_SIGHT_SLIGHT", getWarningColor(), -1, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_SIGHT_MODERATE("DISEASE_SIGHT_MODERATE", getWarningColor(), -2, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_SIGHT_SEVERE("DISEASE_SIGHT_SEVERE", getNegativeColor(), -3, 0, 0, 0, 0, 0, 0, 0, false),
    DISEASE_TREMORS_SLIGHT("DISEASE_TREMORS_SLIGHT", getWarningColor(), 0, 0, -1, -1, 0, 0, 0, 0, false),
    DISEASE_TREMORS_MODERATE("DISEASE_TREMORS_MODERATE", getWarningColor(), 0, 0, -2, -2, 0, 0, 0, 0, false),
    DISEASE_TREMORS_SEVERE("DISEASE_TREMORS_SEVERE", getNegativeColor(), 0, 0, -3, -3, 0, 0, 0, 0, false),
    DISEASE_BREATHING_SLIGHT("DISEASE_BREATHING_SLIGHT", getWarningColor(), 0, -1, -1, 0, 0, 0, 0, 0, false),
    DISEASE_BREATHING_MODERATE("DISEASE_BREATHING_MODERATE", getWarningColor(), 0, -2, -2, 0, 0, 0, 0, 0, false),
    DISEASE_BREATHING_SEVERE("DISEASE_BREATHING_SEVERE", getNegativeColor(), 0, -3, -3, 0, 0, 0, 0, 0, false),
    DISEASE_HEMOPHILIA_SLIGHT("DISEASE_HEMOPHILIA_SLIGHT", getWarningColor(), 0, -0, -1, 0, 0, 0, 0, 0, false),
    DISEASE_HEMOPHILIA_MODERATE("DISEASE_HEMOPHILIA_MODERATE", getWarningColor(), 0, 0, -2, 0, 0, 0, 0, 0, false),
    DISEASE_HEMOPHILIA_SEVERE("DISEASE_HEMOPHILIA_SEVERE", getNegativeColor(), 0, 0, -3, 0, 0, 0, 0, 0, true),
    DISEASE_VENEREAL_SLIGHT("DISEASE_VENEREAL_SLIGHT", getWarningColor(), 0, 0, 0, 0, 0, 0, 0, -1, false),
    DISEASE_VENEREAL_MODERATE("DISEASE_VENEREAL_MODERATE", getWarningColor(), 0, 0, 0, 0, 0, 0, 0, -2, false),
    DISEASE_VENEREAL_SEVERE("DISEASE_VENEREAL_SEVERE", getNegativeColor(), 0, 0, 0, 0, 0, 0, 0, -3, false);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.InjuryEffect";

    private final String lookupName;
    private final String textColor;
    private final int perceptionModifier;
    private final int strengthModifier;
    private final int bodyModifier;
    private final int reflexesModifier;
    private final int dexterityModifier;
    private final int intelligenceModifier;
    private final int willpowerModifier;
    private final int charismaModifier;
    private final boolean inflictsPostScenarioInjury;

    InjuryEffect(String lookupName) {
        this.lookupName = lookupName;
        this.textColor = "";
        this.perceptionModifier = 0;
        this.strengthModifier = 0;
        this.bodyModifier = 0;
        this.reflexesModifier = 0;
        this.dexterityModifier = 0;
        this.intelligenceModifier = 0;
        this.willpowerModifier = 0;
        this.charismaModifier = 0;
        this.inflictsPostScenarioInjury = false;
    }

    InjuryEffect(String lookupName, String textColor, int perceptionModifier, int strengthModifier, int bodyModifier,
          int reflexesModifier, int dexterityModifier, int intelligenceModifier, int willpowerModifier,
          int charismaModifier, boolean inflictsPostScenarioInjury) {
        this.lookupName = lookupName;
        this.textColor = textColor;
        this.perceptionModifier = perceptionModifier;
        this.strengthModifier = strengthModifier;
        this.bodyModifier = bodyModifier;
        this.reflexesModifier = reflexesModifier;
        this.dexterityModifier = dexterityModifier;
        this.intelligenceModifier = intelligenceModifier;
        this.willpowerModifier = willpowerModifier;
        this.charismaModifier = charismaModifier;
        this.inflictsPostScenarioInjury = inflictsPostScenarioInjury;
    }

    public int getPerceptionModifier() {
        return perceptionModifier;
    }

    public int getStrengthModifier() {
        return strengthModifier;
    }

    public int getBodyModifier() {
        return bodyModifier;
    }

    public int getReflexesModifier() {
        return reflexesModifier;
    }

    public int getDexterityModifier() {
        return dexterityModifier;
    }

    public int getIntelligenceModifier() {
        return intelligenceModifier;
    }

    public int getWillpowerModifier() {
        return willpowerModifier;
    }

    public int getCharismaModifier() {
        return charismaModifier;
    }

    public boolean isInflictsPostScenarioInjury() {
        return inflictsPostScenarioInjury;
    }

    public String getTextColor() {
        return textColor;
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "InjuryEffect." + lookupName + ".name");
    }

    public static String getTooltip(List<InjuryEffect> injuryEffects) {
        // Map attributes to their aggregated modifiers
        Map<SkillAttribute, Integer> attributeTotals = new EnumMap<>(SkillAttribute.class);
        int perceptionTotal = 0;
        int postScenarioInjuries = 0;

        // Aggregate modifiers
        for (InjuryEffect effect : injuryEffects) {
            perceptionTotal += effect.getPerceptionModifier();
            addToMap(attributeTotals, SkillAttribute.STRENGTH, effect.getStrengthModifier());
            addToMap(attributeTotals, SkillAttribute.BODY, effect.getBodyModifier());
            addToMap(attributeTotals, SkillAttribute.REFLEXES, effect.getReflexesModifier());
            addToMap(attributeTotals, SkillAttribute.DEXTERITY, effect.getDexterityModifier());
            addToMap(attributeTotals, SkillAttribute.INTELLIGENCE, effect.getIntelligenceModifier());
            addToMap(attributeTotals, SkillAttribute.WILLPOWER, effect.getWillpowerModifier());
            addToMap(attributeTotals, SkillAttribute.CHARISMA, effect.getCharismaModifier());

            if (effect.isInflictsPostScenarioInjury()) {
                postScenarioInjuries++;
            }
        }

        // Build tooltip
        List<String> tooltipPortion = new ArrayList<>();

        if (perceptionTotal != 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "InjuryEffect.tooltip.perception", perceptionTotal));
        }

        for (SkillAttribute attribute : SkillAttribute.values()) {
            int modifier = attributeTotals.getOrDefault(attribute, 0);
            if (modifier != 0) {
                tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "InjuryEffect.tooltip.attribute", modifier, attribute));
            }
        }

        if (postScenarioInjuries > 0) {
            tooltipPortion.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "InjuryEffect.tooltip.inflictsHit", postScenarioInjuries));
        }

        return String.join(" ", tooltipPortion);
    }

    private static void addToMap(Map<SkillAttribute, Integer> map, SkillAttribute key, int value) {
        map.merge(key, value, Integer::sum);
    }

    public static String getEffectsLabel(List<InjuryEffect> injuryEffects) {
        // Count occurrences of each effect
        Map<InjuryEffect, Long> effectCounts = injuryEffects.stream()
                                                     .collect(Collectors.groupingBy(
                                                           Function.identity(),
                                                           LinkedHashMap::new,
                                                           Collectors.counting()
                                                     ));

        // Format each effect with color and count
        StringBuilder effects = new StringBuilder();
        for (Map.Entry<InjuryEffect, Long> entry : effectCounts.entrySet()) {
            if (!effects.isEmpty()) {
                effects.append(", ");
            }
            effects.append(formatEffect(entry.getKey(), entry.getValue()));
        }

        return "<html>" + effects + "</html>";
    }

    private static String formatEffect(InjuryEffect effect, long count) {
        String effectName = spanOpeningWithCustomColor(effect.getTextColor()) + effect + CLOSING_SPAN_TAG;
        return count > 1 ? effectName + " (x" + count + ")" : effectName;
    }
}
