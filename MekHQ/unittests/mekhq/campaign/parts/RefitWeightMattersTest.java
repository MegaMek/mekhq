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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A targeting computer weighs a quarter (Inner Sphere) or a fifth (Clan) of the direct fire weapons of the unit it is
 * built for, so its weight matters: a unit takes only a targeting computer of the weight its design needs. A refit
 * worked the weight out from the unit's old design instead, so Order Parts ordered a targeting computer of the wrong
 * weight (a 0 ton one for a Summoner), and the refit never took it and asked for another.
 */
class RefitWeightMattersTest {
    private static final String TARGETING_COMPUTER = "ISTargeting Computer";

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    /** A Locust LCT-1V with a Large Laser and a targeting computer added, so the computer weighs 2 tons, not 1 */
    private static Entity locustWithATargetingComputer() throws LocationFullException {
        Entity design = UnitFixture.LOCUST_LCT_1V.loadEntity();
        design.addEquipment(EquipmentType.get("ISLargeLaser"), Mek.LOC_RIGHT_TORSO);
        design.addEquipment(EquipmentType.get(TARGETING_COMPUTER), Mek.LOC_LEFT_TORSO);
        return design;
    }

    private static double targetingComputerWeightOn(Entity design) {
        return EquipmentType.get(TARGETING_COMPUTER).getTonnage(design, 1.0);
    }

    private static List<MissingEquipmentPart> targetingComputersToBuy(Refit refit) {
        return refit.getShoppingList()
                     .stream()
                     .filter(MissingEquipmentPart.class::isInstance)
                     .map(MissingEquipmentPart.class::cast)
                     .filter(part -> TARGETING_COMPUTER.equals(part.getType().getInternalName()))
                     .toList();
    }

    private IAcquisitionWork targetingComputerOrder() {
        return campaign.getPlayerForce()
                     .getShoppingList()
                     .getPartList()
                     .stream()
                     .filter(order -> (order.getNewEquipment() instanceof EquipmentPart orderedPart)
                           && TARGETING_COMPUTER.equals(orderedPart.getType().getInternalName()))
                     .findFirst()
                     .orElseThrow();
    }

    @Test
    void theDesignsGiveTheTargetingComputerDifferentWeights() throws LocationFullException {
        double weightOnNewDesign = targetingComputerWeightOn(locustWithATargetingComputer());

        assertEquals(2.0, weightOnNewDesign);
        assertNotEquals(weightOnNewDesign, targetingComputerWeightOn(locust.getEntity()),
              "The test needs the old design to give the targeting computer another weight");
    }

    @Test
    void theRefitBuysATargetingComputerOfTheWeightTheNewDesignNeeds() throws LocationFullException {
        Refit refit = new Refit(locust, locustWithATargetingComputer(), true, false, false);

        List<MissingEquipmentPart> toBuy = targetingComputersToBuy(refit);

        assertEquals(1, toBuy.size(), refit.getShoppingList().toString());
        assertTrue(toBuy.get(0).isWeightMatters());
        assertEquals(2.0, toBuy.get(0).getTonnage());
    }

    @Test
    void orderPartsOrdersATargetingComputerOfTheWeightTheNewDesignNeeds() throws LocationFullException {
        RefitPartOrders.orderMissingParts(campaign, new Refit(locust, locustWithATargetingComputer(), true, false,
              false));

        Part orderedComputer = (Part) targetingComputerOrder().getNewEquipment();

        assertEquals(2.0, orderedComputer.getTonnage());
    }

    @Test
    void theRefitTakesTheTargetingComputerOrderedAhead() throws LocationFullException {
        RefitPartOrders.orderMissingParts(campaign, new Refit(locust, locustWithATargetingComputer(), true, false,
              false));
        targetingComputerOrder().find(5, 1.0);

        Refit refit = new Refit(locust, locustWithATargetingComputer(), true, false, false);

        assertEquals(List.of(), targetingComputersToBuy(refit),
              "The targeting computer on its way is the one the refit needs, so it does not ask for another");
    }
}
