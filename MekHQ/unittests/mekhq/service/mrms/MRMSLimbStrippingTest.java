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
package mekhq.service.mrms;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Mek with a broken shoulder is stripped and its arm scrapped before other repairs. That work switches the unit to
 * salvage mode and carryover off for a while; both must go back afterwards, and Quick Strip must stop once the limb is
 * gone (issue #10217).
 */
class MRMSLimbStrippingTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_SHIFT_MINUTES = 480;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private MekLocation leftArm;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_SALVAGE, true);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        breakTheLeftShoulder();
        for (MekLocation location : PartsScenario.unitParts(locust, MekLocation.class)) {
            if (location.getLoc() == Mek.LOC_LEFT_ARM) {
                leftArm = location;
            }
        }
        assertNotNull(leftArm, "The Locust has a left arm");
        assertTrue(leftArm.onBadHipOrShoulder());
    }

    private void breakTheLeftShoulder() {
        Mek mek = (Mek) locust.getEntity();
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(Mek.LOC_LEFT_ARM); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(Mek.LOC_LEFT_ARM, slot);
            boolean isShoulder = (criticalSlot != null)
                                       && (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM)
                                       && (criticalSlot.getIndex() == Mek.ACTUATOR_SHOULDER);
            if (isShoulder) {
                criticalSlot.setHit(true);
            }
        }
        mek.setInternal(mek.getOInternal(Mek.LOC_LEFT_ARM) - 1, Mek.LOC_LEFT_ARM);
        locust.runDiagnostic(false);
    }

    @Test
    void aStoppedLimbStripLeavesCarryoverAndRepairModeAsTheyWere() {
        scenario.withTech(EXP_REGULAR);
        // Only the Locations row is on, so nothing inside the arm can be stripped and the strip stops early
        for (MRMSOption option : campaign.getCampaignOptions().get(CampaignOption.MRMS_OPTIONS)) {
            option.setActive(option.getType() == PartRepairType.GENERAL_LOCATION);
        }
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        assertTrue(configuredOptions.isAllowCarryover());

        MRMSService.mrmsUnits(campaign, List.of(locust), configuredOptions);

        assertTrue(configuredOptions.isAllowCarryover(), "Later units in the batch may still carry work over");
        assertFalse(locust.isSalvage(), "The Locust is back in repair mode");
    }

    @Test
    void quickStripStopsOnceTheEmptyLimbIsScrapped() {
        Person tech = scenario.withTech(EXP_REGULAR);
        for (Part part : List.copyOf(locust.getParts())) {
            boolean isInTheLeftArm = part.getLocation() == Mek.LOC_LEFT_ARM;
            if (isInTheLeftArm && !(part instanceof MekLocation)) {
                part.remove(false);
            }
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10),
              () -> MRMSService.performSingleLocationMRMS(campaign, locust, leftArm));

        assertEquals(FULL_SHIFT_MINUTES, tech.getMinutesLeft(), "Nobody works on the arm that was just scrapped");
        assertFalse(locust.isSalvage(), "The Locust is back in repair mode");
    }

    @Test
    void quickStripRunsWithMassRepairAndSalvageSwitchedOff() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, false);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_SALVAGE, false);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertTimeoutPreemptively(Duration.ofSeconds(10),
              () -> MRMSService.performSingleLocationMRMS(campaign, locust, leftArm));

        assertTrue(tech.getMinutesLeft() < FULL_SHIFT_MINUTES, "The tech starts stripping the arm");
        assertFalse(locust.isSalvage(), "The Locust is back in repair mode");
    }
}
