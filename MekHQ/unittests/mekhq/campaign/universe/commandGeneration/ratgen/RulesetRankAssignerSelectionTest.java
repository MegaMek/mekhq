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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers who the rank pass promotes when Assign Best Company Commander and Assign Best Officers are on: the best
 * person leads the command wherever the roll seated them, and each lance is led by the best of what is left. Also
 * covers which rank system they are ranked on: always the one set in Campaign Options.
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

    @Test
    void everyoneRankedStaysOnTheCampaignsCustomRankSystem() {
        // Issue 10867: the rank pass moved everyone it ranked onto the faction's rank system, overriding the custom
        // one the player had chosen in Campaign Options.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        RankSystem custom = new RankSystem(Ranks.getRankSystemFromCode("DCMS"));
        custom.setCode("MYMERC");
        custom.setName("My Mercenaries");
        custom.setType(RankSystemType.CAMPAIGN);
        campaign.getPlayerForce().setRankSystem(custom);
        assertSame(custom, campaign.getPlayerForce().getRankSystem(), "the campaign is on the custom system");
        Formation firstLance = lance(campaign, "First Lance");
        Person commander = pilot(campaign, mekIn(campaign, firstLance), 5);
        Person lancemate = pilot(campaign, mekIn(campaign, firstLance), 3);

        RulesetRankAssigner.applyAndReport(campaign, companyOptions());

        for (Person person : java.util.List.of(commander, lancemate)) {
            assertSame(custom, person.getRankSystem(), person.getFullName() + " is ranked on the custom system");
            assertTrue(person.getRankNumeric() > 0, person.getFullName() + " is given a rank");
        }
    }

    @Test
    void aShipAddedAfterTheRankPassHasItsCrewRanked() {
        // Issue 10899: the troopships the lift top-up adds arrive after the rank pass, so nobody aboard had a rank.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit troopship = crewedDropShip(campaign);
        Person captain = troopship.getCommander();
        assertNotNull(captain, "the DropShip arrives with a captain");

        int ranked = RulesetRankAssigner.rankLateShipCrews(campaign, companyOptions(), List.of(troopship));

        assertTrue(ranked > 1, "the captain and the rest of the crew are ranked, not just one person");
        assertTrue(captain.getRankNumeric() > Rank.RWO_MAX, "the captain holds an officer's rank");
        assertTrue(captain.getRankNumeric() <= RulesetRankAssigner.LATE_SHIP_CAPTAIN_RANK_INDEX,
              "and no more than a company commander's, not a Vice Admiral's");
        for (Person person : troopship.getCrew()) {
            assertTrue(person.getRankNumeric() > 0, person.getFullName() + " is given a rank");
            assertSame(campaign.getPlayerForce().getRankSystem(), person.getRankSystem(),
                  person.getFullName() + " is ranked on the campaign's rank system");
        }
    }

    @Test
    void aShipAddedAfterTheRankPassIsLeftAloneWhenRanksAreOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit troopship = crewedDropShip(campaign);
        CommandGenerationOptions options = companyOptions();
        options.setAutomaticallyAssignRanks(false);

        assertEquals(0, RulesetRankAssigner.rankLateShipCrews(campaign, options, List.of(troopship)));
        for (Person person : troopship.getCrew()) {
            assertEquals(0, person.getRankNumeric(), person.getFullName() + " keeps no rank");
        }
    }

    @Test
    void aNullShipListRanksNobody() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        assertEquals(0, RulesetRankAssigner.rankLateShipCrews(campaign, companyOptions(), null));
    }

    /** A Union DropShip added and crewed the way the generator's lift stages add theirs. */
    private static Unit crewedDropShip(Campaign campaign) {
        MekSummary union = MekSummaryCache.getInstance().getMek("Union (2708)");
        Assumptions.assumeTrue(union != null, "the unit cache does not hold the Union (2708)");
        Entity entity = union.loadEntity();
        assertNotNull(entity, "the Union (2708) loads");
        Unit ship = campaign.addNewUnit(entity, true, 0, PartQuality.QUALITY_D);
        assertFalse(ship.getCrew().isEmpty(), "the DropShip arrives crewed");
        for (Person person : ship.getCrew()) {
            assertEquals(0, person.getRankNumeric(), "this test is only meaningful while the crew have no rank");
        }
        return ship;
    }

    private static CommandGenerationOptions companyOptions() {
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setAutomaticallyAssignRanks(true);
        // Each case turns on the selection option it is about; the baseline starts with none of them.
        options.setAssignBestCompanyCommander(false);
        options.setAssignBestOfficers(false);
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
