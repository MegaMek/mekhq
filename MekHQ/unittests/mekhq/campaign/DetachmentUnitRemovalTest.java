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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.List;

import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Removing a unit kept in a second detachment of a split force removes it, with its parts, from that detachment and
 * leaves the main force alone.
 */
class DetachmentUnitRemovalTest {
    private Campaign campaign;
    private Detachment mainDetachment;
    private Detachment secondDetachment;
    private Unit unitInSecondDetachment;
    private Unit unitInMainForce;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        Campaign realCampaign = scenario.getCampaign();
        PlayerForce realForce = realCampaign.getPlayerForce();
        mainDetachment = realForce.getForceDetachment();
        // The second detachment stands somewhere else, so its warehouse can keep parts
        secondDetachment = new Detachment();
        assertTrue(secondDetachment.setParent(new FixedLocation(mock(PlanetarySystem.class))));

        // A unit kept in the second detachment, with its parts in that detachment's warehouse
        unitInSecondDetachment = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        mainDetachment.getHangar().removeUnit(unitInSecondDetachment.getId());
        secondDetachment.getHangar().addUnit(unitInSecondDetachment);
        for (Part part : List.copyOf(unitInSecondDetachment.getParts())) {
            mainDetachment.getWarehouse().removePart(part);
            secondDetachment.getWarehouse().addPart(part);
        }
        // and a unit in the main force, whose parts share the second detachment's part numbers
        unitInMainForce = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        PlayerForce splitForce = spy(realForce);
        doReturn(List.of(mainDetachment, secondDetachment)).when(splitForce).getDetachments();
        campaign = spy(realCampaign);
        doReturn(splitForce).when(campaign).getPlayerForce();
    }

    @Test
    void removingAUnitInASecondDetachmentTakesItAndItsPartsFromThatDetachment() {
        int mainPartCount = mainDetachment.getWarehouse().getParts().size();

        boolean isRemoved = campaign.removeUnit(unitInSecondDetachment.getId());

        assertTrue(isRemoved);
        assertNull(secondDetachment.getHangar().getUnit(unitInSecondDetachment.getId()), "The unit left");
        assertTrue(secondDetachment.getWarehouse().getParts().isEmpty(), "The unit's parts left with it");
        assertSame(unitInMainForce, mainDetachment.getHangar().getUnit(unitInMainForce.getId()));
        assertEquals(mainPartCount, mainDetachment.getWarehouse().getParts().size(), "Nothing left the main force");
    }
}
