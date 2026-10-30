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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AcquisitionTargetTextTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ActionCheck";

    @BeforeAll
    static void initializeSkillTypes() {
        SkillType.initializeTypes();
    }

    private static SkillCheck administrationCheck(TargetRoll target) {
        return new SkillCheck(mock(Person.class), SkillType.getType(SkillType.S_ADMIN), target);
    }

    /** The player's report: a Regular admin, availability F and a contract restriction add up to 13. */
    private static TargetRoll targetOfThirteen() {
        TargetRoll target = new TargetRoll(6, "Regular");
        target.addModifier(5, "availability (F*)");
        target.addModifier(2, "Contract");
        return target;
    }

    @Test
    void reachableTargetShowsTheNumberToRoll() {
        SkillCheck check = administrationCheck(new TargetRoll(9, "Elite"));

        assertEquals("9+", AcquisitionTargetText.describeTarget(check));
    }

    @Test
    void targetOfTwelveCanStillBeRolled() {
        SkillCheck check = administrationCheck(new TargetRoll(12, "Green"));

        assertEquals("12+", AcquisitionTargetText.describeTarget(check));
    }

    @Test
    void targetAboveTwelveReadsAsImpossibleAndKeepsTheNumber() {
        SkillCheck check = administrationCheck(targetOfThirteen());

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "acquisition.target.outOfReach", 13),
              AcquisitionTargetText.describeTarget(check));
    }

    @Test
    void decidedTargetKeepsItsOwnWording() {
        SkillCheck check = administrationCheck(new TargetRoll(TargetRoll.AUTOMATIC_FAIL, "No skill"));

        assertEquals("Automatic Failure", AcquisitionTargetText.describeTarget(check));
    }

    @Test
    void reachableTargetTooltipIsJustTheModifiers() {
        TargetRoll target = new TargetRoll(9, "Elite");
        SkillCheck check = administrationCheck(target);

        assertEquals(target.getDesc(), AcquisitionTargetText.describeModifiers(check, null));
    }

    @Test
    void targetAboveTwelveTooltipKeepsTheModifiersAndSaysWhy() {
        TargetRoll target = targetOfThirteen();
        SkillCheck check = administrationCheck(target);

        String expected = "<html>" + target.getDesc() + "<br>"
                                + getFormattedTextAt(RESOURCE_BUNDLE, "acquisition.target.outOfReachReason", 13)
                                + "</html>";
        assertEquals(expected, AcquisitionTargetText.describeModifiers(check, null));
    }

    @Test
    void staffNoteIsAddedUnderTheModifiersRatherThanReplacingThem() {
        TargetRoll target = new TargetRoll(9, "Regular");
        SkillCheck check = administrationCheck(target);
        String staffNote = getFormattedTextAt(RESOURCE_BUNDLE, "acquisition.modifier.noPersonnel");

        assertEquals("<html>" + target.getDesc() + "<br>" + staffNote + "</html>",
              AcquisitionTargetText.describeModifiers(check, staffNote));
    }
}
