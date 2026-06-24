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

package mekhq.campaign.personnel.skills;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * An implementation of {@link ActionCheck} for attribute checks.
 *
 * <p>This class encapsulates the logic needed to resolve an attribute check for a character's
 * specific {@link SkillAttribute}, accounting for necessary factors.</p>
 *
 * @author Hokk
 * @since 0.51.01
 */
public class AttributeCheck extends ActionCheck<AttributeCheck> {

    private final SkillAttribute firstAttribute;
    private final SkillAttribute secondAttribute;

    /**
     * Instantiates an attribute check from raw values. Prefer the other constructor variants.
     *
     * @param person       the {@link Person} performing the skill check
     * @param targetNumber target number for the skill check
     */
    public AttributeCheck(Person person, TargetRoll targetNumber,
          SkillAttribute firstAttribute, SkillAttribute secondAttribute) {
        super(person, targetNumber);
        if (firstAttribute == null || firstAttribute.isNone()) {
            throw new IllegalArgumentException("First attribute for an attribute check is not present");
        }
        this.firstAttribute = firstAttribute;
        this.secondAttribute = secondAttribute;
    }

    /**
     * Please see {@link Person#checkAttribute(SkillAttribute)} and use it instead.
     */
    public AttributeCheck(Person person, SkillAttribute attribute) {
        this(person, attribute, SkillAttribute.NONE);
    }

    /**
     * Please see {@link Person#checkAttributes(SkillAttribute, SkillAttribute)} and use it instead.
     */
    public AttributeCheck(Person person, SkillAttribute firstAttribute, SkillAttribute secondAttribute) {
        super(person, AttributeCheckUtility.determineTargetNumber(person, firstAttribute, secondAttribute, 0));
        this.firstAttribute = firstAttribute;
        this.secondAttribute = secondAttribute;
    }

    public boolean isEasierThan(AttributeCheck other) {
        return !targetNumber.cannotSucceed() && (targetNumber.getValue() < other.targetNumber.getValue());
    }

    @Override
    protected AttributeCheck getThis() {
        return this;
    }

    @Override
    protected boolean isCountUp() {
        return false;
    }

    @Override
    protected boolean hasNaturalAptitude() {
        return false;
    }

    @Override
    protected String getActionName() {
        String label = firstAttribute.getLabel();
        if (secondAttribute != null && secondAttribute != SkillAttribute.NONE) {
            label = label + "-" + secondAttribute.getLabel();
        }
        return label;
    }

}
