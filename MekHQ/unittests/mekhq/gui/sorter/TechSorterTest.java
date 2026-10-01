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
package mekhq.gui.sorter;

import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The order of the techs offered a task (issue #10220). Not a GUI test: the comparator is plain data.
 */
class TechSorterTest {
    private PartsScenario scenario;
    private Unit locust;
    private EquipmentPart damagedPart;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        damagedPart = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst();
        damagedPart.setHits(1);
    }

    /** A tech who is Elite at everything except the Mek skill this task uses, where they are Regular. */
    private Person regularAtMeksButEliteElsewhere() {
        Person tech = scenario.withTech(EXP_ELITE);
        tech.addSkill(SkillType.S_TECH_MEK, SkillType.getType(SkillType.S_TECH_MEK).getLevelFromExperience(EXP_REGULAR),
              0);
        return tech;
    }

    @Test
    void theLeastSkilledTechInTheTasksOwnSkillComesFirst() {
        Person eliteMekTech = scenario.withTech(EXP_ELITE);
        Person regularMekTech = regularAtMeksButEliteElsewhere();
        TechSorter sorter = new TechSorter(damagedPart);

        List<Person> techs = new ArrayList<>(List.of(eliteMekTech, regularMekTech));
        techs.sort(sorter);

        assertEquals(List.of(regularMekTech, eliteMekTech), techs,
              "Ranked by the Mek skill the task uses, not by the best skill overall");
    }

    @Test
    void equallySkilledTechsAreListedMostTimeFirst() {
        Person shortDayTech = scenario.withTech(EXP_REGULAR);
        shortDayTech.setMinutesLeft(30);
        Person fullDayTech = scenario.withTech(EXP_REGULAR);
        TechSorter sorter = new TechSorter(damagedPart);

        List<Person> techs = new ArrayList<>(List.of(shortDayTech, fullDayTech));
        techs.sort(sorter);

        assertEquals(List.of(fullDayTech, shortDayTech), techs);
    }

    @Test
    void twoTechsAssignedToTheUnitAreComparedTheSameWayRoundEitherWay() {
        Person eliteTech = scenario.withTech(EXP_ELITE);
        Person regularTech = scenario.withTech(EXP_REGULAR);
        eliteTech.addTechUnit(locust);
        regularTech.addTechUnit(locust);
        TechSorter sorter = new TechSorter(damagedPart);
        sorter.setAssignedFirst(true);

        int forwards = sorter.compare(regularTech, eliteTech);
        int backwards = sorter.compare(eliteTech, regularTech);

        assertTrue(forwards < 0, "The Regular comes first");
        assertEquals(Integer.signum(forwards), -Integer.signum(backwards), "Swapping the two flips the answer");
    }

    @Test
    void aTechAssignedToTheUnitComesFirstWhenTheCampaignAsks() {
        Person eliteAssignedTech = scenario.withTech(EXP_ELITE);
        eliteAssignedTech.addTechUnit(locust);
        Person regularTech = scenario.withTech(EXP_REGULAR);
        TechSorter sorter = new TechSorter(damagedPart);
        sorter.setAssignedFirst(true);

        List<Person> techs = new ArrayList<>(List.of(regularTech, eliteAssignedTech));
        techs.sort(sorter);

        assertEquals(List.of(eliteAssignedTech, regularTech), techs);
    }
    @Test
    void techsAlikeInEveryWayAreAlwaysInTheSameOrder() {
        Person firstTech = scenario.withTech(EXP_REGULAR);
        Person secondTech = scenario.withTech(EXP_REGULAR);
        TechSorter sorter = new TechSorter(damagedPart);

        int forwards = sorter.compare(firstTech, secondTech);

        assertTrue(forwards != 0, "Two different techs never compare as equal");
        assertEquals(-Integer.signum(forwards), Integer.signum(sorter.compare(secondTech, firstTech)));
    }
}
