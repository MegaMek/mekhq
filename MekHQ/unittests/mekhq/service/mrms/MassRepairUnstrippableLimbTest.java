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

import megamek.common.equipment.IArmorState;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mass Repair strips the parts out of a destroyed limb before anything else. When none of them could be taken out,
 * because the job was harder than the maximum target allowed, the unit's other repairs were never tried either, and
 * as nothing changed from one day to the next the unit was never repaired (issue #5420). A Wolverine WVR-6R with a
 * destroyed left torso still holding its SRM 6 stands in for the reporter's Vulcan: taking the launcher out needs a 6,
 * and Mass Repair allows at most 4.
 */
class MassRepairUnstrippableLimbTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int CENTRE_TORSO_ARMOR_LEFT = 4;
    private static final int PLENTY_OF_ARMOR = 500;

    private final PartsScenario scenario = PartsScenario.create();
    private final Campaign campaign = scenario.getCampaign();
    private Unit wolverine;
    private int fullCentreTorsoArmor;

    @BeforeEach
    void setUp() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        scenario.withTech(EXP_REGULAR);
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        fullCentreTorsoArmor = wolverine.getEntity().getArmor(Mek.LOC_CENTER_TORSO);
        stockArmor();
        destroyTheLeftTorso();
        wolverine.getEntity().setArmor(CENTRE_TORSO_ARMOR_LEFT, Mek.LOC_CENTER_TORSO);
        wolverine.runDiagnostic(false);
        FixedDieRolls.everyDieShows(6);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private void stockArmor() {
        Armor spareArmor = (Armor) PartsScenario.unitParts(wolverine, Armor.class).getFirst().clone();
        spareArmor.setAmount(PLENTY_OF_ARMOR);
        scenario.withSpare(spareArmor, 1);
    }

    private void destroyTheLeftTorso() {
        wolverine.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_TORSO);
        wolverine.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_TORSO);
        for (Part part : new ArrayList<>(wolverine.getParts())) {
            part.updateConditionFromEntity(false);
        }
    }

    private void allowTargetsUpTo(int maximumTargetNumber) {
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.MRMS_USE_REPAIR, true);
        options.set(CampaignOption.MRMS_USE_SALVAGE, true);
        options.set(CampaignOption.MRMS_USE_EXTRA_TIME, false);
        options.set(CampaignOption.MRMS_USE_RUSH_JOB, false);
        options.set(CampaignOption.MRMS_ALLOW_CARRYOVER, true);
        options.set(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY, false);
        options.set(CampaignOption.MRMS_SCRAP_IMPOSSIBLE, false);
        options.set(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST, false);
        options.set(CampaignOption.MRMS_REPLACE_POD, true);
        List<MRMSOption> repairTypeOptions = new ArrayList<>();
        for (PartRepairType repairType : PartRepairType.getMRMSValidTypes()) {
            repairTypeOptions.add(new MRMSOption(repairType, true, EXP_ULTRA_GREEN, EXP_LEGENDARY, 4,
                  maximumTargetNumber, 0));
        }
        options.set(CampaignOption.MRMS_OPTIONS, repairTypeOptions);
    }

    /** The daily report line telling the player the limb could not be stripped and other repairs went ahead. */
    private boolean isThePlayerToldTheLimbCannotBeStripped() {
        String expectedReport = getFormattedTextAt("mekhq.resources.MRMS", "MRMS.LimbCannotBeStripped.report",
              getWarningColor(), "Mek Left Torso", wolverine.getName());
        return campaign.getDailyReportLog().getLines(TECHNICAL).contains(expectedReport);
    }

    private boolean isTheLauncherStillFitted() {
        return PartsScenario.unitParts(wolverine, EquipmentPart.class).stream()
                     .anyMatch(part -> "SRM 6".equals(part.getName()) && (part.getLocation() == Mek.LOC_LEFT_TORSO));
    }

    @Test
    void otherRepairsGoAheadWhenTheLimbCannotBeStripped() {
        allowTargetsUpTo(4);

        MRMSService.performSingleUnitMRMS(campaign, wolverine);

        assertTrue(isTheLauncherStillFitted(), "Taking the SRM 6 out needs a 6, more than Mass Repair allows");
        assertEquals(fullCentreTorsoArmor, wolverine.getEntity().getArmor(Mek.LOC_CENTER_TORSO));
        assertTrue(isThePlayerToldTheLimbCannotBeStripped());
    }

    @Test
    void theLimbIsStillStrippedWhenItCanBe() {
        allowTargetsUpTo(8);

        MRMSService.performSingleUnitMRMS(campaign, wolverine);

        assertFalse(isTheLauncherStillFitted(), "The SRM 6 is taken out of the destroyed left torso");
        assertEquals(fullCentreTorsoArmor, wolverine.getEntity().getArmor(Mek.LOC_CENTER_TORSO));
        assertFalse(isThePlayerToldTheLimbCannotBeStripped());
    }
}
