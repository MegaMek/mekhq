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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptionsFreebieTracker;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Guards the one list of support vehicle capabilities that the mechanic count, the company generator, the support
 * team organiser and the campaign-option dialogs all read.
 *
 * <p>Issue #10059 was those places disagreeing: recovery vehicles were counted for their mechanics while nothing
 * built them. These tests pin the two halves that can drift, namely which capability is switched on by which
 * campaign option, and where its vehicles are built.</p>
 */
class SupportCapabilityTest {
    /** The sections {@code SupportPersonnelToTOE.organize} actually builds capability vehicles for. */
    private static final Set<SupportSection> SECTIONS_THAT_BUILD_VEHICLES =
          EnumSet.of(SupportSection.MAINTENANCE, SupportSection.MEDICAL);

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    @DisplayName("Salvage and medical vehicles join a section only while support teams are on")
    void sectionVehiclesJoinTheirSectionOnlyWithSupportTeams() {
        Campaign withTeams = campaignWithEverythingOn(true);
        Campaign withoutTeams = campaignWithEverythingOn(false);

        assertTrue(SupportCapability.SALVAGE.joinsSection(withTeams), "recovery vehicles join maintenance");
        assertTrue(SupportCapability.MEDICAL.joinsSection(withTeams), "MASH trucks join medical");
        assertFalse(SupportCapability.SALVAGE.joinsSection(withoutTeams),
              "with support teams off the recovery vehicles are built standalone");
        assertFalse(SupportCapability.MEDICAL.joinsSection(withoutTeams),
              "with support teams off the MASH trucks are built standalone");

        for (SupportCapability capability : EnumSet.of(SupportCapability.LOGISTICS, SupportCapability.COMMISSARY,
              SupportCapability.SECURITY)) {
            assertFalse(capability.joinsSection(withTeams), capability + " has no section to join");
            assertFalse(capability.joinsSection(withoutTeams), capability + " has no section to join");
        }
    }

    @Test
    @DisplayName("Every capability has somewhere that builds its vehicles")
    void everyCapabilityHasABuilder() {
        // A capability with no section is built standalone by the generator. One with a section is built by
        // organize, which only builds for maintenance and medical - a capability pointing anywhere else would be
        // counted for its mechanics and then never built, which is what issue #10059 was.
        for (SupportCapability capability : SupportCapability.values()) {
            SupportSection section = capability.crewSection();
            if (section == null) {
                continue;
            }
            assertTrue(SECTIONS_THAT_BUILD_VEHICLES.contains(section),
                  capability + " is crewed by the " + section + " section, which builds no vehicles");
        }
    }

    @Test
    @DisplayName("Each capability is switched on by the same option that opens its dialog")
    void capabilityOptionMatchesTheDialogTrigger() {
        assertTriggerMatches(campaignWithEverythingOff());
        assertTriggerMatches(campaignWithEverythingOn(true));
    }

    @Test
    @DisplayName("Infantry carry no mechanic demand; every vehicle capability does")
    void onlyVehicleCapabilitiesNeedMechanics() {
        assertFalse(SupportCapability.SECURITY.needsMechanics(), "the security detail is infantry");
        for (SupportCapability capability : EnumSet.complementOf(EnumSet.of(SupportCapability.SECURITY))) {
            assertTrue(capability.needsMechanics(), capability + " fields vehicles, which need mechanics");
        }
    }

    @Test
    @DisplayName("Each capability names the role that crews it, so the temporary crew check is made per role")
    void crewRoleMatchesWhatTheUnitIs() {
        assertEquals(PersonnelRole.SOLDIER, SupportCapability.SECURITY.crewRole(),
              "the security detail is infantry");
        for (SupportCapability capability : EnumSet.complementOf(EnumSet.of(SupportCapability.SECURITY))) {
            assertEquals(PersonnelRole.VEHICLE_CREW_GROUND, capability.crewRole(),
                  capability + " fields ground support vehicles");
        }
    }

    /** Checks every capability against the campaign-options snapshot that decides which dialog opens. */
    private static void assertTriggerMatches(Campaign campaign) {
        CampaignOptionsFreebieTracker tracker = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());
        assertEquals(tracker.useAdvancedSalvage(), SupportCapability.SALVAGE.isEnabled(campaign),
              "recovery vehicles follow the CamOps salvage option");
        assertEquals(tracker.useMASHTheatres(), SupportCapability.MEDICAL.isEnabled(campaign),
              "MASH trucks follow the MASH theatres option");
        assertEquals(tracker.useStratCon(), SupportCapability.LOGISTICS.isEnabled(campaign),
              "the convoy follows the StratCon option");
        assertEquals(tracker.useFatigue(), SupportCapability.COMMISSARY.isEnabled(campaign),
              "the canteens follow the fatigue option");
        assertEquals(tracker.trackPrisoners(), SupportCapability.SECURITY.isEnabled(campaign),
              "the security detail follows the prisoner capture style");
    }

    /** A campaign with every capability switched off, which is what a fresh test campaign starts as. */
    private static Campaign campaignWithEverythingOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, false);
        campaign.getCampaignOptions().set(CampaignOption.USE_MASH_THEATRES, false);
        campaign.getCampaignOptions().set(CampaignOption.USE_FATIGUE, false);
        campaign.getCampaignOptions().set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.DISABLED);
        campaign.getCampaignOptions().set(CampaignOption.PRISONER_CAPTURE_STYLE, PrisonerCaptureStyle.NONE);
        campaign.getCampaignOptions().set(CampaignOption.USE_SUPPORT_TEAMS, false);
        return campaign;
    }

    /** A campaign with every capability switched on, with support teams as given. */
    private static Campaign campaignWithEverythingOn(boolean useSupportTeams) {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, true);
        campaign.getCampaignOptions().set(CampaignOption.USE_MASH_THEATRES, true);
        campaign.getCampaignOptions().set(CampaignOption.USE_FATIGUE, true);
        campaign.getCampaignOptions().set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.NORMAL);
        campaign.getCampaignOptions().set(CampaignOption.PRISONER_CAPTURE_STYLE,
              PrisonerCaptureStyle.CAMPAIGN_OPERATIONS);
        campaign.getCampaignOptions().set(CampaignOption.USE_SUPPORT_TEAMS, useSupportTeams);
        return campaign;
    }
}
