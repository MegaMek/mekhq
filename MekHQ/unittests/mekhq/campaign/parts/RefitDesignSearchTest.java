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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.loaders.MekSummary;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Choose Refit Kit offers the designs of the same chassis that a refit kit can turn a unit into (issue #4076). A
 * conventional platoon's weight follows from its troopers and weapons, so requiring the same weight hid almost every
 * custom platoon a player saved. When nothing can be offered, the dialog says why.
 */
class RefitDesignSearchTest {
    private static final String BUNDLE_UNIT_NAME = "Foot Platoon (Flamer)";

    private static MekSummary summaryWeighing(double tons) {
        MekSummary summary = new MekSummary();
        summary.setTons(tons);
        return summary;
    }

    /** @return the laser platoon with one squad fewer, as a player might save it from Customize to Model */
    private static ConvInfantry platoonWithOneSquadFewer() {
        ConvInfantry platoon = (ConvInfantry) UnitFixture.FOOT_PLATOON_LASER.loadEntity();
        platoon.setSquadCount(platoon.getSquadCount() - 1);
        platoon.autoSetInternal();
        platoon.setModel("(Laser, Smaller)");
        return platoon;
    }

    @Test
    void aPlatoonOfTheSameChassisButAnotherWeightIsOffered() {
        Entity laserPlatoon = UnitFixture.FOOT_PLATOON_LASER.loadEntity();
        Entity smallerPlatoon = platoonWithOneSquadFewer();
        assertNotEquals(laserPlatoon.getWeight(), smallerPlatoon.getWeight(),
              "The two platoons must weigh differently for this test to mean anything");

        assertTrue(RefitDesignSearch.isWeightMatch(laserPlatoon, summaryWeighing(smallerPlatoon.getWeight())));
    }

    @Test
    void aMekOfAnotherWeightIsNotOffered() {
        Entity griffin = UnitFixture.GRIFFIN_GRF_1N.loadEntity();

        assertFalse(RefitDesignSearch.isWeightMatch(griffin, summaryWeighing(griffin.getWeight() + 5)));
        assertTrue(RefitDesignSearch.isWeightMatch(griffin, summaryWeighing(griffin.getWeight())));
    }

    @Test
    void battleArmorMatchesOnSquadSize() {
        Entity elementals = UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER.loadEntity();
        MekSummary sameSquadSize = new MekSummary();
        sameSquadSize.setTWWeight(5);
        MekSummary largerSquad = new MekSummary();
        largerSquad.setTWWeight(6);

        assertTrue(RefitDesignSearch.isWeightMatch(elementals, sameSquadSize));
        assertFalse(RefitDesignSearch.isWeightMatch(elementals, largerSquad));
    }

    @Test
    void soldiersLeftOverWhenAPlatoonShrinksStayInTheCampaign() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        ConvInfantry fullPlatoon = (ConvInfantry) platoon.getEntity();
        List<Person> soldiers = new ArrayList<>();
        for (int index = 1; index <= fullPlatoon.getShootingStrength(); index++) {
            Person soldier = new Person("Soldier", Integer.toString(index), campaign);
            soldier.setPrimaryRoleDirect(PersonnelRole.SOLDIER);
            soldier.addSkill(SkillType.S_SMALL_ARMS, 4, 0);
            campaign.importPerson(soldier);
            platoon.addPilotOrSoldier(soldier);
            soldiers.add(soldier);
        }
        Refit refit = new Refit(platoon, platoonWithOneSquadFewer(), true, false, false);
        assertNull(refit.checkFixable(), "A smaller platoon may be refitted");
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();

        int stillInThePlatoon = 0;
        for (Person soldier : soldiers) {
            assertNotNull(campaign.getPlayerForce().getHumanResources().getPerson(soldier.getId()),
                  soldier.getFullName() + " is still in the campaign");
            assertTrue(soldier.getStatus().isActive(), soldier.getFullName() + " is still active");
            if (soldier.getUnit() == platoon) {
                stillInThePlatoon++;
            }
        }
        assertTrue(stillInThePlatoon < soldiers.size(), "Some soldiers no longer fit the smaller platoon");
    }

    @Test
    void anEmptyListNamesEachReasonWithItsCount() {
        RefitDesignSearch.Result result = new RefitDesignSearch.Result(List.of(), 2, 1);

        String text = result.describeEmptyList(BUNDLE_UNIT_NAME, 3);

        assertFalse(text.contains("{"), text);
        assertTrue(text.contains("No refit kits are available for the Foot Platoon (Flamer)."), text);
        assertTrue(text.contains("2 designs of this chassis weigh a different amount"), text);
        assertTrue(text.contains("1 design is not allowed by the campaign's tech rules."), text);
        assertTrue(text.contains("3 designs cannot be refitted from this unit."), text);
    }

    @Test
    void anEmptyListWithNothingSkippedSaysNoOtherDesignsExist() {
        RefitDesignSearch.Result result = new RefitDesignSearch.Result(List.of(), 0, 0);

        String text = result.describeEmptyList(BUNDLE_UNIT_NAME, 0);

        assertTrue(text.contains("No other designs of this chassis were found."), text);
        assertEquals(2, text.split("<p>").length - 1, text);
    }
}
