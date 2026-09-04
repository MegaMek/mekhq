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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers who the rank pass promotes when Assign Best Company Commander and Assign Best Officers are on: the best
 * person leads the command wherever the roll seated them, and each lance is led by the best of what is left.
 */
class RulesetRankAssignerSelectionTest {

    /** The Inner Sphere echelon that maps to a company, so the root is a company and its formations lances. */
    private static final int COMPANY_ECHELON = 4;

    @BeforeAll
    static void loadWhatRanksNeed() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        Ranks.initializeRankSystems();
        Factions.setInstance(Factions.loadDefault(true));
    }

    @Test
    void theBestPersonLeadsTheCommandWhereverTheyWereSeated() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation firstLance = lance(campaign, "First Lance");
        Formation secondLance = lance(campaign, "Second Lance");
        Person green = pilot(campaign, mekIn(campaign, firstLance), 1);
        Person regular = pilot(campaign, mekIn(campaign, firstLance), 3);
        Person veteran = pilot(campaign, mekIn(campaign, secondLance), 5);
        Person elite = pilot(campaign, mekIn(campaign, secondLance), 7);
        CommandGenerationOptions options = companyOptions();
        options.setAssignBestCompanyCommander(true);
        options.setAssignBestOfficers(true);

        RulesetRankAssigner.Result result = RulesetRankAssigner.applyAndReport(campaign, options);

        assertSame(elite, result.rootCommander(), "the best pilot in the command leads it");
        assertEquals(FormationLevel.COMPANY, result.officers().get(elite));
        assertEquals(FormationLevel.LANCE, result.officers().get(regular), "the best left in the first lance");
        assertTrue(!result.officers().containsKey(veteran),
              "the second lance is led by the commander sitting in it, so no separate officer");
        assertTrue(!result.officers().containsKey(green), "the green pilot leads nothing");
    }

    @Test
    void starColonelAndAboveNeedABloodnameOnlyInAClanCommand() {
        assertTrue(RulesetRankAssigner.needsBloodname(RulesetRankAssigner.STAR_COLONEL_RANK_INDEX, true));
        assertTrue(RulesetRankAssigner.needsBloodname(RulesetRankAssigner.STAR_COLONEL_RANK_INDEX + 1, true));
        assertTrue(!RulesetRankAssigner.needsBloodname(RulesetRankAssigner.STAR_COLONEL_RANK_INDEX - 1, true),
              "a Star Captain need not be Bloodnamed");
        assertTrue(!RulesetRankAssigner.needsBloodname(RulesetRankAssigner.STAR_COLONEL_RANK_INDEX, false),
              "an Inner Sphere colonel has no Bloodname to need");
    }

    @Test
    void withoutTheOptionsTheFirstPersonFoundLeadsAndTheirLanceNeedsNoOfficer() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation firstLance = lance(campaign, "First Lance");
        Formation secondLance = lance(campaign, "Second Lance");
        Person green = pilot(campaign, mekIn(campaign, firstLance), 1);
        pilot(campaign, mekIn(campaign, firstLance), 3);
        Person veteran = pilot(campaign, mekIn(campaign, secondLance), 5);
        Person elite = pilot(campaign, mekIn(campaign, secondLance), 7);

        RulesetRankAssigner.Result result = RulesetRankAssigner.applyAndReport(campaign, companyOptions());

        assertSame(green, result.rootCommander(), "first found, not best");
        assertNotSame(elite, result.rootCommander());
        assertEquals(FormationLevel.LANCE, result.officers().get(veteran), "the second lance takes its first pilot");
        assertEquals(2, result.officers().size(), "the commander leads the first lance, so one lance leader");
    }

    @Test
    void theEnginesDesignatedCommanderTakesThePostWhenNoOrderIsAsked() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation firstLance = lance(campaign, "First Lance");
        Formation secondLance = lance(campaign, "Second Lance");
        pilot(campaign, mekIn(campaign, firstLance), 1);
        pilot(campaign, mekIn(campaign, firstLance), 3);
        Person designated = pilot(campaign, mekIn(campaign, secondLance), 5);
        Person seatedBesideThem = pilot(campaign, mekIn(campaign, secondLance), 7);
        Formation root = campaign.getPlayerForce().getFormations();
        RulesetRankAssigner.Guidance guidance = new RulesetRankAssigner.Guidance(
              java.util.Map.of(root, FormationLevel.COMPANY), java.util.Map.of(root, designated));

        RulesetRankAssigner.Result result = RulesetRankAssigner.applyAndReport(campaign, companyOptions(), guidance);

        assertSame(designated, result.rootCommander(), "the engine's choice stands");
        assertEquals(FormationLevel.COMPANY, result.officers().get(designated));
        assertTrue(!result.officers().containsKey(seatedBesideThem),
              "the designated commander's own lance is led by them, so nobody else in it is promoted");
    }

    @Test
    void aRootThatIsOnlyAContainerHandsTheCommandToTheFormationBeneathIt() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation company = lance(campaign, "The Company");
        Formation lance = new Formation("Its Lance");
        campaign.getPlayerForce().addFormation(lance, company, campaign);
        Person first = pilot(campaign, mekIn(campaign, lance), 3);
        pilot(campaign, mekIn(campaign, lance), 5);
        Formation root = campaign.getPlayerForce().getFormations();
        java.util.Map<Formation, FormationLevel> levels = new java.util.IdentityHashMap<>();
        levels.put(root, null);
        levels.put(company, FormationLevel.COMPANY);
        levels.put(lance, FormationLevel.LANCE);
        RulesetRankAssigner.Guidance guidance = new RulesetRankAssigner.Guidance(levels, java.util.Map.of());

        RulesetRankAssigner.Result result = RulesetRankAssigner.applyAndReport(campaign, companyOptions(), guidance);

        assertSame(first, result.rootCommander(), "the company's commander commands, the root has no rank");
        assertEquals(FormationLevel.COMPANY, result.officers().get(first));
        assertEquals(1, result.officers().size(), "the company commander leads its only lance too");
    }

    private static CommandGenerationOptions companyOptions() {
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setAutomaticallyAssignRanks(true);
        options.getForceDescriptorSnapshot().setEchelon(COMPANY_ECHELON);
        return options;
    }

    private static Formation lance(Campaign campaign, String name) {
        Formation lance = new Formation(name);
        campaign.getPlayerForce().addFormation(lance, campaign.getPlayerForce().getFormations(), campaign);
        return lance;
    }

    private static Unit mekIn(Campaign campaign, Formation lance) {
        Unit unit = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        campaign.getPlayerForce().addUnitToFormation(unit, lance.getId(), campaign);
        return unit;
    }

    /** A MekWarrior seated in the unit, with the given gunnery and piloting skill level; higher is better. */
    private static Person pilot(Campaign campaign, Unit unit, int skillLevel) {
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, skillLevel, 0);
        person.addSkill(SkillType.S_PILOT_MEK, skillLevel, 0);
        campaign.importPerson(person);
        unit.addPilotOrSoldier(person);
        return person;
    }
}
