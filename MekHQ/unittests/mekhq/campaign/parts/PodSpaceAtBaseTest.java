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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.UnitPartsTransfer;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Pod work on an Omni unit at a base works on the unit's own pod-mounted parts, kept at the base, not on main-force
 * parts that share their numbers (issue #10322).
 */
class PodSpaceAtBaseTest {
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private LocalWarehouse baseWarehouse;
    private Unit omniAtBase;
    private Unit omniInMainForce;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();

        omniAtBase = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);
        campaign.getPlayerForce().getHangar().removeUnit(omniAtBase.getId());
        base.getBaseHangar().addUnit(omniAtBase);
        UnitPartsTransfer.moveUnitParts(campaign, omniAtBase, baseWarehouse);
        // The same design in the main force, whose parts take the numbers the base's parts had
        omniInMainForce = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);
    }

    private Part podMountedPart(Unit unit) {
        for (Part part : unit.getParts()) {
            if (part.isOmniPodded() && (part.getLocation() >= 0)) {
                return part;
            }
        }
        throw new IllegalStateException(unit.getName() + " has no pod-mounted part");
    }

    private PodSpace podSpaceFor(Unit unit, Part part) {
        PodSpace podSpace = new PodSpace(part.getLocation(), unit);
        podSpace.updateConditionFromEntity(false);
        return podSpace;
    }

    @Test
    void podWorkAtABaseUsesTheBasesStock() {
        Part podPart = podMountedPart(omniAtBase);

        assertSame(baseWarehouse, podSpaceFor(omniAtBase, podPart).getWarehouse());
    }

    @Test
    void podSpaceAtABaseSeesDamageToItsOwnPart() {
        Part damagedPart = podMountedPart(omniAtBase);
        damagedPart.setHits(1);
        Part mainPartWithTheSameNumber = mainWarehouse.getPart(damagedPart.getId());
        assertFalse((mainPartWithTheSameNumber != null) && mainPartWithTheSameNumber.needsFixing());

        assertTrue(podSpaceFor(omniAtBase, damagedPart).needsFixing());
    }

    @Test
    void salvagingAPodSpaceAtABaseLeavesTheMainForceAlone() {
        Part podPart = podMountedPart(omniAtBase);
        int mainForcePartCount = omniInMainForce.getParts().size();

        podSpaceFor(omniAtBase, podPart).remove(true);

        assertFalse(omniAtBase.getParts().contains(podPart), "The base unit's pod part came off");
        assertEquals(mainForcePartCount, omniInMainForce.getParts().size(), "The main-force Omni keeps its parts");
    }
}
