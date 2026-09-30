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

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A spare set aside for an overnight replacement is given back when its unit leaves the campaign, and a spare left
 * reserved by a unit that already left is freed when the campaign loads (issue #10212).
 */
class ReservedSparesTest {
    private static final String MEDIUM_LASER = "Medium Laser";
    private static final int SPARE_LASERS = 2;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(EXP_REGULAR);
    }

    /**
     * Destroys the Locust's medium laser, stocks spares for it and starts an overnight replacement, which reserves one.
     */
    private MissingPart replacementInProgress() {
        EquipmentPart laser = null;
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if (MEDIUM_LASER.equals(equipmentPart.getName())) {
                laser = equipmentPart;
            }
        }
        assertNotNull(laser, "The Locust LCT-1V fixture has a Medium Laser");
        Part spareLaser = laser.clone();
        laser.remove(false);
        scenario.withSpare(spareLaser, SPARE_LASERS);
        MissingPart missingLaser = PartsScenario.unitParts(locust, MissingPart.class).getFirst();
        missingLaser.setTech(tech);
        missingLaser.reservePart();
        assertNotNull(missingLaser.getReplacementPart(), "The replacement reserves one spare laser");
        return missingLaser;
    }

    private int countReservedParts() {
        int reservedParts = 0;
        for (Part part : campaign.getPlayerForce().getWarehouse().getParts()) {
            if (part.isReservedForReplacement()) {
                reservedParts++;
            }
        }
        return reservedParts;
    }

    private int countSpareLasers() {
        int spareLasers = 0;
        for (Part part : campaign.getPlayerForce().getWarehouse().getSpareParts()) {
            if (MEDIUM_LASER.equals(part.getName())) {
                spareLasers += part.getQuantity();
            }
        }
        return spareLasers;
    }

    @Test
    void aUnitThatLeavesGivesBackTheSpareItsReplacementReserved() {
        replacementInProgress();
        assertEquals(1, countReservedParts());

        campaign.removeUnit(locust.getId());

        assertEquals(0, countReservedParts(), "Nothing stays reserved for a unit that is gone");
        assertEquals(SPARE_LASERS, countSpareLasers(), "Both lasers can be used as spares again");
    }

    @Test
    void loadingFreesASpareReservedByATaskThatNoLongerExists() {
        MissingPart missingLaser = replacementInProgress();
        Part heldSpare = missingLaser.getReplacementPart();
        Part orphanedSpare = heldSpare.clone();
        orphanedSpare.setReservedBy(tech);
        campaign.getPlayerForce().getWarehouse().addPart(orphanedSpare, false);
        assertEquals(2, countReservedParts());

        int freedCount = ReservedSpares.releaseOrphanedReservations(campaign);

        assertEquals(1, freedCount);
        assertFalse(orphanedSpare.isReservedForReplacement(), "No task holds this spare, so it is freed");
        assertTrue(heldSpare.isReservedForReplacement(), "The Locust replacement still holds its spare");
    }
}
