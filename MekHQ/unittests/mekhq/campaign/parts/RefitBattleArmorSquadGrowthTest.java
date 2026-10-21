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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import megamek.Version;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartQuality;
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
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Refitting a battle armor squad to a design with more troopers buys a suit for each trooper the squad adds, as part
 * of the refit kit, rather than handing it out for free (issue #9405). The four-trooper point is a pinned copy of
 * {@code battlearmor/3058Uu/Elemental BA [Laser] (Sqd4).blk} from mm-data.
 */
class RefitBattleArmorSquadGrowthTest {
    private static final int SMALLER_SQUAD = 4;
    private static final int LARGER_SQUAD = 5;

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
        Entity pointOfFour = MHQTestUtilities.getEntityForUnitTesting("Elemental BA [Laser] (Sqd4)", true);
        assertNotNull(pointOfFour);
        elementals = campaign.addNewUnit(pointOfFour, false, 0, PartQuality.QUALITY_D);
        assertNotNull(elementals);
        for (int index = 1; index <= SMALLER_SQUAD; index++) {
            Person trooper = new Person("Trooper", Integer.toString(index), campaign);
            trooper.setPrimaryRoleDirect(PersonnelRole.BATTLE_ARMOUR);
            trooper.addSkill(SkillType.S_GUN_BA, 4, 0);
            trooper.addSkill(SkillType.S_ANTI_MEK, 5, 0);
            campaign.importPerson(trooper);
            elementals.addPilotOrSoldier(trooper);
            troopers.add(trooper);
        }
    }

    private Refit refitToFiveTroopers() {
        return new Refit(elementals, UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER.loadEntity(), false, false, false);
    }

    /** Runs the refit from start to finish, with the kit delivered at once. */
    private void completeRefitToFiveTroopers() throws Exception {
        Refit refit = refitToFiveTroopers();
        assertNull(refit.checkFixable());
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    private static List<MissingBattleArmorSuit> suitsToBuy(Refit refit) {
        List<MissingBattleArmorSuit> suits = new ArrayList<>();
        for (Part part : refit.getShoppingList()) {
            if (part instanceof MissingBattleArmorSuit suit) {
                suits.add(suit);
            }
        }
        return suits;
    }

    private SortedMap<Integer, Integer> suitsPerTrooper() {
        SortedMap<Integer, Integer> suits = new TreeMap<>();
        for (Part part : elementals.getParts()) {
            if ((part instanceof BattleArmorSuit) || (part instanceof MissingBattleArmorSuit)) {
                suits.merge(part.getLocation(), 1, Integer::sum);
            }
        }
        return suits;
    }

    private static SortedMap<Integer, Integer> oneSuitPerTrooper(int troopers) {
        SortedMap<Integer, Integer> suits = new TreeMap<>();
        for (int trooper = BattleArmor.LOC_TROOPER_1; trooper <= troopers; trooper++) {
            suits.put(trooper, 1);
        }
        return suits;
    }

    @Test
    void aLargerSquadMayBeRefitted() {
        Refit refit = refitToFiveTroopers();

        assertNull(refit.checkFixable(), "Growing the squad is allowed");
    }

    @Test
    void theKitBuysOneSuitForTheNewTrooper() {
        Refit refit = refitToFiveTroopers();

        List<MissingBattleArmorSuit> suitsToBuy = suitsToBuy(refit);
        assertEquals(1, suitsToBuy.size(), "Only the new trooper needs a suit bought");
        assertEquals(LARGER_SQUAD, suitsToBuy.getFirst().getLocation());
    }

    @Test
    void aSpareSuitIsUsedInsteadOfBuyingOneAndTheKitCostsLess() {
        Money priceWithASuitToBuy = refitToFiveTroopers().getCost();

        BattleArmor pointOfFive = (BattleArmor) UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER.loadEntity();
        BattleArmorSuit spareSuit = new BattleArmorSuit(pointOfFive, BattleArmor.LOC_TROOPER_1, campaign);
        spareSuit.setTrooper(-1);
        scenario.withSpare(spareSuit, 1);
        Refit refit = refitToFiveTroopers();

        assertTrue(suitsToBuy(refit).isEmpty(), "The spare suit in the warehouse is used");
        assertTrue(refit.getCost().isLessThan(priceWithASuitToBuy),
              "The kit costs " + refit.getCost() + " with a spare suit and " + priceWithASuitToBuy + " without");
    }

    @Test
    void afterTheRefitEveryTrooperHasOneSuitAndTheNewSuitIsEmpty() throws Exception {
        Map<UUID, Integer> slotBefore = new HashMap<>();
        for (Person trooper : troopers) {
            slotBefore.put(trooper.getId(), elementals.getTrooperSlots().findSlot(trooper.getId()));
        }

        completeRefitToFiveTroopers();

        assertEquals(LARGER_SQUAD, ((BattleArmor) elementals.getEntity()).getSquadSize());
        assertEquals(oneSuitPerTrooper(LARGER_SQUAD), suitsPerTrooper());
        boolean hasWorkingNewSuit = false;
        for (Part part : elementals.getParts()) {
            if ((part instanceof BattleArmorSuit) && (part.getLocation() == LARGER_SQUAD)) {
                hasWorkingNewSuit = true;
            }
        }
        assertTrue(hasWorkingNewSuit, "The new trooper has a working suit");
        assertNull(elementals.getTrooperSlots().getOccupant(LARGER_SQUAD), "Nobody wears the new suit yet");
        for (Person trooper : troopers) {
            assertEquals(slotBefore.get(trooper.getId()), elementals.getTrooperSlots().findSlot(trooper.getId()),
                  trooper.getFullName() + " keeps their suit");
        }
        assertEquals(0, scenario.countSpareParts(BattleArmorSuit.class), "The bought suit is used up");
    }

    @Test
    void theSuitToBuySurvivesAReload() throws Exception {
        Refit refit = refitToFiveTroopers();
        refit.begin();

        StringWriter xmlText = new StringWriter();
        try (PrintWriter xmlWriter = new PrintWriter(xmlText)) {
            refit.writeToXML(xmlWriter, 0);
        }
        Element refitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                     .parse(new ByteArrayInputStream(
                                           xmlText.toString().getBytes(StandardCharsets.UTF_8)))
                                     .getDocumentElement();
        Refit reloaded = Refit.generateInstanceFromXML(refitElement, new Version(), campaign, elementals, true);
        assertNotNull(reloaded);
        reloaded.fixReferences(campaign);

        assertEquals(PartsCensus.ofParts(refit.getShoppingList()), PartsCensus.ofParts(reloaded.getShoppingList()));
        assertEquals(1, suitsToBuy(reloaded).size());
    }

    @Test
    void theLargerSquadLoadsAgain() throws Exception {
        completeRefitToFiveTroopers();

        StringWriter unitXml = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(unitXml)) {
            elementals.writeToXML(printWriter, 0);
        }
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();

        assertNotNull(Unit.generateInstanceFromXML(unitElement, new Version(), campaign));
        elementals.initializeParts(true);
        elementals.runDiagnostic(false);
        elementals.resetPilotAndEntity();
        assertEquals(oneSuitPerTrooper(LARGER_SQUAD), suitsPerTrooper());
    }
}
