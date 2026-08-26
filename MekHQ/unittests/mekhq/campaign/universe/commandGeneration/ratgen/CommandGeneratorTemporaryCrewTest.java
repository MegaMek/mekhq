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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.EnumSet;
import mekhq.campaign.campaignOptions.CampaignOption;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.TemporaryCrewRole;
import testUtilities.MHQTestUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers the generated command's units arriving fully crewed.
 *
 * <p>Where a role draws on the temporary crew pool the assembler deliberately puts one named person
 * aboard and leaves the rest of the seats empty. Something then has to fill them, or every infantry
 * platoon and battle armour squad the generator produces arrives with a single soldier in it - which is
 * what happened, and what MegaMek showed as a squad of empty suits.</p>
 */
class CommandGeneratorTemporaryCrewTest {

    /** Recruiting a soldier rolls their skills, which needs the skill table loaded. */
    @BeforeAll
    static void loadSkillTypes() {
        SkillType.initializeTypes();
    }

    /** A 28-strong foot platoon: enough seats that one named soldier leaves an obvious hole. */
    private static Unit footPlatoonWithOneSoldier(Campaign campaign) {
        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Foot Platoon (DCMS) (Laser 2620+)",
              true);
        assertNotNull(entity, "the test platoon must load");
        Unit unit = campaign.addNewUnit(entity, false, 0, PartQuality.QUALITY_D);

        Person soldier = campaign.getPlayerForce().getHumanResources().newPerson(campaign, PersonnelRole.SOLDIER);
        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, soldier, true, true);
        unit.addPilotOrSoldier(soldier);
        return unit;
    }

    /**
     * The regression. With Temporary Crews on for infantry, the stage has to leave the platoon fully
     * crewed. Before the fix it left it exactly as it found it: the pool was sized from the roles
     * already in it, which on a freshly generated campaign is none, and the crew was never seated even
     * when the pool did hold some.
     */
    @Test
    void aPlatoonLeftShortByTheAssemblerIsCrewedInFull() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_BLOB_INFANTRY, true);
        Unit platoon = footPlatoonWithOneSoldier(campaign);

        int seats = platoon.getFullCrewSize();
        assertTrue(seats > 1, "the test platoon must have more than one seat, has " + seats);
        assertEquals(1, platoon.getTotalCrewSize(), "one named soldier is aboard to begin with");

        CommandGenerator.topUpTemporaryCrewPools(campaign);

        assertEquals(seats, platoon.getTotalCrewSize(),
              "every seat must be filled, by a named soldier or from the temporary crew pool");
        assertTrue(platoon.isFullyCrewed(), "the platoon must report itself fully crewed");
    }

    /**
     * With the option off the seats are meant to be held by named people, so the stage must not quietly
     * conjure temporary crew to paper over a genuinely short-handed unit.
     */
    @Test
    void aPlatoonIsLeftAloneWhenTemporaryCrewIsOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_BLOB_INFANTRY, false);
        Unit platoon = footPlatoonWithOneSoldier(campaign);

        CommandGenerator.topUpTemporaryCrewPools(campaign);

        assertEquals(0, platoon.getTotalTempCrew(),
              "no temporary crew may be assigned when the campaign does not use them");
        assertEquals(1, platoon.getTotalCrewSize(), "the platoon keeps only its named soldier");
    }
    /**
     * The designer's toggles are campaign settings, and the assembler reads them as each unit is crewed, so
     * the choices must land on the campaign: every chosen role on, every other role off - even one the
     * campaign had on before, because the toggles show and replace the campaign's settings.
     */
    @Test
    void theDesignersChoicesAreWrittenToTheCampaign() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_BLOB_VESSEL_CREW, true);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setTemporaryCrewRoles(EnumSet.of(TemporaryCrewRole.INFANTRY, TemporaryCrewRole.VTOL_CREW));

        CommandGenerator.applyTemporaryCrewChoices(campaign, options);

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        assertTrue(campaignOptions.get(CampaignOption.USE_BLOB_INFANTRY), "a chosen role is switched on");
        assertTrue(campaignOptions.get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL), "a chosen role is switched on");
        assertFalse(campaignOptions.get(CampaignOption.USE_BLOB_VESSEL_CREW),
              "a role left unticked is switched off, even one the campaign had on");
    }

    /**
     * Every toggle must map to a role the crew assembler recognises. A toggle for a role the assembler does
     * not know would be a checkbox that does nothing, which is the failure this feature exists to remove.
     */
    @Test
    void everyToggleIsRecognisedByTheCrewAssembler() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setTemporaryCrewRoles(EnumSet.allOf(TemporaryCrewRole.class));

        CommandGenerator.applyTemporaryCrewChoices(campaign, options);

        for (TemporaryCrewRole toggle : TemporaryCrewRole.values()) {
            assertTrue(campaign.getPlayerForce().getHumanResources()
                             .isBlobCrewEnabled(toggle.getPersonnelRole(), campaign.getCampaignOptions()),
                  toggle + " must switch temporary crew on for " + toggle.getPersonnelRole());
        }
    }
}
