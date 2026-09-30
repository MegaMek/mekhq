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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A multi-day repair that cannot go ahead today is held rather than attempted: an impossible roll always fails, which
 * raises the task's difficulty and can destroy its reserved part. Overtime counts only when it is worked (issue
 * #10209).
 */
class RepairTaskHoldTest {
    private static final int MINUTES_ALREADY_SPENT = 30;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit wolverine;
    private Person tech;
    private Part damagedWeapon;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        tech = scenario.withTech(SkillType.EXP_REGULAR);
        damagedWeapon = PartsScenario.unitParts(wolverine, EquipmentPart.class).getFirst();
        damagedWeapon.setHits(1);
        damagedWeapon.setTech(tech);
        damagedWeapon.addTimeSpent(MINUTES_ALREADY_SPENT);
    }

    private void assertTaskUntouched() {
        assertEquals(MINUTES_ALREADY_SPENT, damagedWeapon.getTimeSpent(), "The work already done is kept");
        assertEquals(1, damagedWeapon.getHits(), "Nothing was rolled, so nothing changed");
        assertEquals(SkillType.EXP_GREEN, damagedWeapon.getSkillMin(), "A failed roll would have raised this");
    }

    @Test
    void aTaskOnADeployedUnitWaitsWithoutRolling() {
        wolverine.setScenarioId(5);
        int minutesBefore = tech.getMinutesLeft();

        campaign.fixPart(damagedWeapon, tech);

        assertTaskUntouched();
        assertSame(tech, damagedWeapon.getTech(), "The tech stays on the task until the unit is back");
        assertEquals(minutesBefore, tech.getMinutesLeft(), "No time is spent");
    }

    @Test
    void aTaskWhoseTechHasNoTimeLeftKeepsItsProgress() {
        tech.setMinutesLeft(0);

        campaign.fixPart(damagedWeapon, tech);

        assertTaskUntouched();
        assertSame(tech, damagedWeapon.getTech(), "The task carries on tomorrow");
    }

    @Test
    void aTaskThatCanNoLongerGoAheadIsStoppedButKeepsItsProgress() {
        damagedWeapon.setHits(0);

        campaign.fixPart(damagedWeapon, tech);

        assertNull(damagedWeapon.getTech(), "The tech is taken off a task that is no longer needed");
        assertEquals(MINUTES_ALREADY_SPENT, damagedWeapon.getTimeSpent());
    }

    @Test
    void overtimeCountsOnlyWhenItIsWorked() {
        campaign.setOvertime(true);
        tech.setOvertimeLeft(0);
        tech.setMinutesLeft(1);
        assertTrue(damagedWeapon.getTimeLeft() > 1, "The repair runs past today");

        campaign.fixPart(damagedWeapon, tech);

        assertFalse(damagedWeapon.hasWorkedOvertime(), "No overtime was worked, so there is no +3 overtime penalty");
    }
}
