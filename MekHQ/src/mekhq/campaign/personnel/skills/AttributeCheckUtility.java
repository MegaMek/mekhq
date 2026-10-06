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
package mekhq.campaign.personnel.skills;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjuryEffect;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * Utility class for executing attribute checks.
 *
 * <p>The {@link AttributeCheckUtility} class encapsulates the logic needed to perform attribute checks, including
 * calculating target numbers based on supplied attributes, applying external and miscellaneous modifiers, optionally
 * handling re-rolls using the Edge mechanic, and generating detailed results including margin of success or failure. It
 * supports both single-attribute and double-attribute checks and can generate localized, formatted result text suitable
 * for UI feedback or logs.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class AttributeCheckUtility {

    // only static methods
    private AttributeCheckUtility() {
    }

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AttributeCheckUtility";

    /**
     * The target number for an attribute check with one attribute.
     */
    protected static final int TARGET_NUMBER_ONE_LINKED_ATTRIBUTE = 12; // ATOW pg 39-41

    /**
     * The target number for an attribute check with two attributes.
     */
    protected static final int TARGET_NUMBER_TWO_LINKED_ATTRIBUTES = 18; // ATOW pg 39-41

    /**
     * Determines the target number for a roll based on the given person's attributes, the requested attributes, and a
     * miscellaneous modifier.
     *
     * @param person               the person whose attributes will be used to calculate the target number
     * @param firstSkillAttribute  the primary skill attribute to be used for calculations can be null
     * @param secondSkillAttribute the secondary skill attribute to be used for calculations
     * @param miscModifier         an additional modifier to be applied to the target number
     *
     * @return a {@link TargetRoll} object containing the calculated target number
     *
     * @author Illiani
     * @since 0.50.07
     */
    static TargetRoll determineTargetNumber(Person person, SkillAttribute firstSkillAttribute,
          @Nullable SkillAttribute secondSkillAttribute, int miscModifier) {

        if (firstSkillAttribute == null || firstSkillAttribute.isNoAttribute()) {
            throw new IllegalArgumentException("First attribute for an attribute check is not present");
        }

        final Attributes characterAttributes = person.getATOWAttributes();

        TargetRoll targetNumber = new TargetRoll();

        getBaseTargetNumber(targetNumber, secondSkillAttribute != null && !secondSkillAttribute.isNoAttribute());
        getAttributeModifiers(firstSkillAttribute, secondSkillAttribute, characterAttributes, targetNumber,
              person.getActiveInjuryEffects(), person.getOptions(), person.getAgeForAttributeModifiers());

        targetNumber.addModifier(miscModifier, getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.miscModifier"));

        return targetNumber;
    }

    /**
     * Calculates and applies attribute modifiers to the target roll based on the provided skill attributes and
     * character attributes. If the second skill attribute is not null, it also calculates and applies its modifier.
     *
     * @param firstSkillAttribute  the first skill attribute used for modifier calculation
     * @param secondSkillAttribute the second skill attribute used for modifier calculation; can be null
     * @param characterAttributes  the set of character attributes that provide the modifiers
     * @param targetNumber         the target roll object to which the calculated modifiers will be added
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void getAttributeModifiers(SkillAttribute firstSkillAttribute, SkillAttribute secondSkillAttribute,
          Attributes characterAttributes, TargetRoll targetNumber, List<InjuryEffect> injuryEffects,
          PersonnelOptions options, int ageForAttributeModifiers) {
        // Because we're adjusting the target number, positive is bad, negative is good
        int firstAttributeModifier = -characterAttributes.getAdjustedAttributeScore(firstSkillAttribute, injuryEffects,
              options, ageForAttributeModifiers);
        targetNumber.addModifier(firstAttributeModifier, firstSkillAttribute.getLabel());

        if (secondSkillAttribute != null && !secondSkillAttribute.isNoAttribute()) {
            int secondAttributeModifier = -characterAttributes.getAdjustedAttributeScore(secondSkillAttribute,
                  injuryEffects, options, ageForAttributeModifiers);
            targetNumber.addModifier(secondAttributeModifier, secondSkillAttribute.getLabel());
        }
    }

    /**
     * Modifies the base target number based on whether a single or double attribute check is performed.
     *
     * @param targetNumber           the {@link TargetRoll} object to which the modifier will be added
     * @param isDoubleAttributeCheck a boolean indicating whether the check involves two linked attributes (true) or one
     *                               linked attribute (false)
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void getBaseTargetNumber(TargetRoll targetNumber, boolean isDoubleAttributeCheck) {
        if (isDoubleAttributeCheck) {
            targetNumber.addModifier(TARGET_NUMBER_TWO_LINKED_ATTRIBUTES,
                  getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.twoLinkedAttributes"));
        } else {
            targetNumber.addModifier(TARGET_NUMBER_ONE_LINKED_ATTRIBUTE,
                  getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.oneLinkedAttribute"));
        }
    }

}
