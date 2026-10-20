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

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * With "scrap parts that are impossible for an elite technician to repair" on, Mass Repair scrapped every part beyond
 * repair. A Mek's centre torso cannot be replaced, so scrapping it wrecked the Mek for good (issue #360). It is now
 * left in place, the daily report says why, and the Mek stays repairable. A limb can still be scrapped and replaced.
 */
class MassRepairUnreplaceablePartTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int DAMAGED_STRUCTURE = 3;

    private final PartsScenario scenario = PartsScenario.create();
    private final Campaign campaign = scenario.getCampaign();
    private Unit wolverine;

    @BeforeEach
    void setUp() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        scenario.withTech(EXP_REGULAR);
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        scrapPartsBeyondRepair();
        FixedDieRolls.everyDieShows(6);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private void scrapPartsBeyondRepair() {
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.MRMS_USE_REPAIR, true);
        options.set(CampaignOption.MRMS_SCRAP_IMPOSSIBLE, true);
        List<MRMSOption> repairTypeOptions = new ArrayList<>();
        for (PartRepairType repairType : PartRepairType.getMRMSValidTypes()) {
            repairTypeOptions.add(new MRMSOption(repairType, true, EXP_ULTRA_GREEN, EXP_LEGENDARY, 4, 8, 0));
        }
        options.set(CampaignOption.MRMS_OPTIONS, repairTypeOptions);
    }

    private MekLocation locationOf(int location) {
        return PartsScenario.unitParts(wolverine, MekLocation.class).stream()
                     .filter(part -> part.getLoc() == location)
                     .findFirst()
                     .orElseThrow();
    }

    /** Damages a location and marks it beyond repair, as repeated failed repairs would. */
    private MekLocation damagedBeyondRepair(int location) {
        MekLocation mekLocation = locationOf(location);
        wolverine.getEntity().setInternal(DAMAGED_STRUCTURE, location);
        mekLocation.updateConditionFromEntity(false);
        mekLocation.setSkillMin(EXP_LEGENDARY + 1);
        return mekLocation;
    }

    /** How many times the daily report says the part cannot be replaced. */
    private long timesTheReportIsShown(MekLocation mekLocation) {
        String expectedReport = getFormattedTextAt("mekhq.resources.MRMS", "MRMS.CannotBeReplaced.report",
              getWarningColor(), mekLocation.getName(), wolverine.getName());
        return campaign.getDailyReportLog().getLines(TECHNICAL).stream()
                     .filter(expectedReport::equals)
                     .count();
    }

    @Test
    void aCentreTorsoBeyondRepairIsLeftInPlace() {
        MekLocation centreTorso = damagedBeyondRepair(Mek.LOC_CENTER_TORSO);

        MRMSService.performSingleUnitMRMS(campaign, wolverine);

        assertTrue(wolverine.getParts().contains(centreTorso), "The centre torso is still fitted");
        assertTrue(wolverine.isRepairable(), "The Wolverine can still be repaired");
        assertEquals(1, timesTheReportIsShown(centreTorso));
    }

    @Test
    void aLimbBeyondRepairIsStillScrapped() {
        MekLocation leftArm = damagedBeyondRepair(Mek.LOC_LEFT_ARM);

        MRMSService.performSingleUnitMRMS(campaign, wolverine);

        assertFalse(wolverine.getParts().contains(leftArm), "The left arm was scrapped to be replaced");
        assertEquals(0, timesTheReportIsShown(leftArm));
    }
}
