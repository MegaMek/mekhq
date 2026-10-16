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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import megamek.Version;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.missing.MissingBattleArmorSuit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Refitting a battle armor squad to a design with fewer troopers leaves it with only the troopers, suits and parts the
 * smaller squad has, and the squad still loads afterwards (issue #3646). In 49.12 cutting an Elemental point from five
 * troopers to fewer ended in an "array out of bounds" error and a save that would not load. The three-trooper point is
 * a pinned copy of {@code battlearmor/3058Uu/Elemental BA [Laser] (Sqd3).blk} from mm-data.
 */
class RefitBattleArmorSquadShrinkTest {
    private static final int SMALLER_SQUAD = 3;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit elementals;
    private final List<Person> troopers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        elementals = scenario.withUnit(UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER);
        BattleArmor point = (BattleArmor) elementals.getEntity();
        for (int index = 1; index <= point.getSquadSize(); index++) {
            Person trooper = new Person("Trooper", Integer.toString(index), campaign);
            trooper.setPrimaryRoleDirect(PersonnelRole.BATTLE_ARMOUR);
            trooper.addSkill(SkillType.S_GUN_BA, 4, 0);
            trooper.addSkill(SkillType.S_ANTI_MEK, 5, 0);
            campaign.importPerson(trooper);
            elementals.addPilotOrSoldier(trooper);
            troopers.add(trooper);
        }
    }

    private static Entity smallerPoint() {
        Entity point = MHQTestUtilities.getEntityForUnitTesting("Elemental BA [Laser] (Sqd3)", true);
        assertNotNull(point);
        return point;
    }

    /** Refits the point to three troopers, with the last trooper's suit destroyed beforehand. */
    private void refitToThreeTroopers() throws Exception {
        for (Part part : elementals.getParts()) {
            if ((part instanceof BattleArmorSuit suit) && (suit.getTrooper() == 5)) {
                suit.remove(false);
                break;
            }
        }
        Refit refit = new Refit(elementals, smallerPoint(), false, false, false);
        assertNull(refit.checkFixable(), "A smaller squad may be refitted");
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    private SortedMap<Integer, Integer> suitsPerTrooper() {
        SortedMap<Integer, Integer> suits = new TreeMap<>();
        for (Part part : elementals.getParts()) {
            if (part instanceof BattleArmorSuit suit) {
                suits.merge(suit.getTrooper(), 1, Integer::sum);
            } else if (part instanceof MissingBattleArmorSuit missingSuit) {
                suits.merge(missingSuit.getTrooper(), 1, Integer::sum);
            }
        }
        return suits;
    }

    private static SortedMap<Integer, Integer> oneSuitPerTrooper(int troopers) {
        SortedMap<Integer, Integer> suits = new TreeMap<>();
        for (int trooper = 1; trooper <= troopers; trooper++) {
            suits.put(trooper, 1);
        }
        return suits;
    }

    @Test
    void theSmallerSquadHasOnlyItsOwnTroopersSuitsAndParts() throws Exception {
        refitToThreeTroopers();

        assertEquals(SMALLER_SQUAD, ((BattleArmor) elementals.getEntity()).getSquadSize());
        assertEquals(oneSuitPerTrooper(SMALLER_SQUAD), suitsPerTrooper());
        List<String> partsBeyondTheSquad = new ArrayList<>();
        for (Part part : elementals.getParts()) {
            if (part.getLocation() > SMALLER_SQUAD) {
                partsBeyondTheSquad.add(part.getName() + " in location " + part.getLocation());
            }
        }
        assertTrue(partsBeyondTheSquad.isEmpty(), partsBeyondTheSquad.toString());
    }

    @Test
    void theSmallerSquadKeepsNoMoreTroopersThanItHasSuits() throws Exception {
        refitToThreeTroopers();

        assertTrue(elementals.getCrew().size() <= SMALLER_SQUAD, "Crew of " + elementals.getCrew().size());
        for (int slot = SMALLER_SQUAD + 1; slot <= 5; slot++) {
            assertNull(elementals.getTrooperSlots().getOccupant(slot), "Nobody sits in removed suit " + slot);
        }
        BattleArmor point = (BattleArmor) elementals.getEntity();
        assertDoesNotThrow(() -> elementals.getTrooperSlots().findWearersOfLostSuits(point));
    }

    @Test
    void troopersLeftWithoutASuitStayInTheCampaignUnassigned() throws Exception {
        refitToThreeTroopers();

        int stillInTheSquad = 0;
        for (Person trooper : troopers) {
            assertNotNull(campaign.getPlayerForce().getHumanResources().getPerson(trooper.getId()), trooper.getFullName() + " is still in the campaign");
            assertTrue(trooper.getStatus().isActive(), trooper.getFullName() + " is still active");
            if (trooper.getUnit() == elementals) {
                stillInTheSquad++;
            } else {
                assertNull(trooper.getUnit(), trooper.getFullName() + " is unassigned, not moved elsewhere");
            }
        }
        assertEquals(elementals.getCrew().size(), stillInTheSquad);
    }

    @Test
    void theSmallerSquadLoadsAgain() throws Exception {
        refitToThreeTroopers();
        StringWriter unitXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(unitXml);
        elementals.writeToXML(printWriter, 0);
        printWriter.flush();
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();

        assertNotNull(Unit.generateInstanceFromXML(unitElement, new Version(), campaign));
        // What loading the campaign does to each unit once its parts are read
        assertDoesNotThrow(() -> {
            elementals.initializeParts(true);
            elementals.runDiagnostic(false);
            elementals.resetPilotAndEntity();
        });
        assertEquals(oneSuitPerTrooper(SMALLER_SQUAD), suitsPerTrooper());
    }
}
