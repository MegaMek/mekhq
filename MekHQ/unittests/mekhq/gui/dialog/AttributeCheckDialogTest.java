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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.dialog.AttributeCheckDialog.AttributeOption;
import org.junit.jupiter.api.Test;

class AttributeCheckDialogTest {
    private static final List<SkillAttribute> CHECKABLE = List.of(SkillAttribute.BODY, SkillAttribute.CHARISMA,
          SkillAttribute.DEXTERITY, SkillAttribute.INTELLIGENCE, SkillAttribute.REFLEXES, SkillAttribute.STRENGTH,
          SkillAttribute.WILLPOWER);

    @Test
    void everyAttributeCanBeCheckedOnItsOwn() {
        List<AttributeOption> singles = AttributeCheckDialog.ATTRIBUTE_CHECK_OPTIONS.stream()
                                              .filter(option -> option.second() == null).toList();
        assertEquals(CHECKABLE, singles.stream().map(AttributeOption::first).toList());
    }

    @Test
    void everyPairOfDifferentAttributesIsOfferedOnce() {
        List<AttributeOption> pairs = AttributeCheckDialog.ATTRIBUTE_CHECK_OPTIONS.stream()
                                            .filter(option -> option.second() != null).toList();
        assertEquals(21, pairs.size(), "seven attributes make 21 pairs");

        Set<Set<SkillAttribute>> seen = new HashSet<>();
        for (AttributeOption pair : pairs) {
            assertTrue(pair.first() != pair.second(), pair.label());
            assertTrue(seen.add(Set.of(pair.first(), pair.second())), "duplicate pair " + pair.label());
        }
    }

    @Test
    void labelsJoinTheAttributeNames() {
        AttributeOption single = new AttributeOption(SkillAttribute.WILLPOWER, null);
        assertEquals(SkillAttribute.WILLPOWER.getLabel(), single.label());
        assertNull(single.second());
        assertEquals(SkillAttribute.BODY.getLabel() + "-" + SkillAttribute.WILLPOWER.getLabel(),
              new AttributeOption(SkillAttribute.BODY, SkillAttribute.WILLPOWER).label());
    }
}
