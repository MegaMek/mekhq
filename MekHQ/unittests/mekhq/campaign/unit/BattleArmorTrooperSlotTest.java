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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.Version;
import megamek.common.battleArmor.BattleArmor;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Each person in a battle armor squad wears one particular suit, keeps it, and is the one who leaves when that suit is
 * salvaged (issue #10257).
 */
class BattleArmorTrooperSlotTest {
    private Campaign campaign;
    private Unit squad;
    private BattleArmor battleArmor;
    private final List<Person> troopers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        squad = scenario.withUnit(UnitFixture.IS_STANDARD_BATTLE_ARMOR_LASER);
        battleArmor = (BattleArmor) squad.getEntity();
        troopers.clear();
        for (int index = 1; index <= battleArmor.getSquadSize(); index++) {
            Person trooper = new Person("Trooper", Integer.toString(index), campaign);
            trooper.setPrimaryRoleDirect(PersonnelRole.BATTLE_ARMOUR);
            trooper.addSkill(SkillType.S_GUN_BA, 4, 0);
            trooper.addSkill(SkillType.S_ANTI_MEK, 5, 0);
            campaign.importPerson(trooper);
            squad.addPilotOrSoldier(trooper);
            troopers.add(trooper);
        }
    }

    private Integer suitOf(Person trooper) {
        return squad.getTrooperSlots().findSlot(trooper.getId());
    }

    private BattleArmorSuit suit(int slot) {
        for (BattleArmorSuit suit : PartsScenario.unitParts(squad, BattleArmorSuit.class)) {
            if (suit.getTrooper() == slot) {
                return suit;
            }
        }
        throw new IllegalStateException("No suit in trooper slot " + slot);
    }

    @Test
    void everyTrooperWearsTheirOwnSuitAndEverySuitIsManned() {
        Set<Integer> wornSuits = new HashSet<>();
        for (Person trooper : troopers) {
            assertNotNull(suitOf(trooper), trooper.getFullName() + " wears a suit");
            wornSuits.add(suitOf(trooper));
        }

        assertEquals(battleArmor.getSquadSize(), wornSuits.size(), "No two troopers share a suit");
        for (int slot = BattleArmor.LOC_TROOPER_1; slot <= battleArmor.getSquadSize(); slot++) {
            assertEquals(1, battleArmor.getInternal(slot), "Suit " + slot + " is manned");
        }
    }

    @Test
    void troopersKeepTheirSuitsWhenOneLosesArmor() {
        List<Integer> suitsBefore = new ArrayList<>();
        for (Person trooper : troopers) {
            suitsBefore.add(suitOf(trooper));
        }
        int strippedSlot = suitOf(troopers.getFirst());
        battleArmor.setArmor(0, strippedSlot);

        squad.resetPilotAndEntity();

        for (int index = 0; index < troopers.size(); index++) {
            assertEquals(suitsBefore.get(index), suitOf(troopers.get(index)));
        }
    }

    @Test
    void aTrooperWhoLeavesEmptiesTheirOwnSuit() {
        Person leaving = troopers.get(1);
        int emptiedSlot = suitOf(leaving);

        squad.remove(leaving, false);

        assertNull(squad.getTrooperSlots().getOccupant(emptiedSlot));
        assertEquals(0, battleArmor.getInternal(emptiedSlot), "The suit is now empty");
        assertEquals(troopers.size() - 1, squad.getCrew().size());
    }

    @Test
    void salvagingASuitRemovesThePersonWearingIt() {
        Person wearer = troopers.getFirst();
        int salvagedSlot = suitOf(wearer);

        suit(salvagedSlot).remove(true);

        assertFalse(squad.getCrew().contains(wearer), "The person in the salvaged suit leaves the squad");
        assertEquals(troopers.size() - 1, squad.getCrew().size(), "Nobody else leaves");
        assertTrue(squad.getCrew().containsAll(troopers.subList(1, troopers.size())));
    }

    @Test
    void suitAssignmentsSurviveASaveAndLoad() throws Exception {
        StringWriter unitXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(unitXml);
        squad.writeToXML(printWriter, 0);
        printWriter.flush();
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();

        Unit loadedSquad = Unit.generateInstanceFromXML(unitElement, new Version(), campaign);

        for (Person trooper : troopers) {
            UUID loadedWearer = loadedSquad.getTrooperSlots().getOccupant(suitOf(trooper));
            assertEquals(trooper.getId(), loadedWearer, trooper.getFullName() + " is still in the same suit");
        }
    }
}
