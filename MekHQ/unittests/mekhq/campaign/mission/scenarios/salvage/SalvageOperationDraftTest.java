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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Dropship;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TeamAvailability;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TeamOption;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TechOrigin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

class SalvageOperationDraftTest {
    private static final int SCENARIO_ID = 12;

    private final Campaign campaign = mockCampaign();
    private final CampaignOptions options = new CampaignOptions();
    private final Scenario scenario = mock(Scenario.class);
    private final List<Formation> formations = new ArrayList<>();
    private final List<CombatTeam> combatTeams = new ArrayList<>();
    private final List<Person> idleTechs = new ArrayList<>();
    private final List<Integer> scenarioFormations = new ArrayList<>();
    private final List<UUID> scenarioTechs = new ArrayList<>();
    private int nextFormationId = 1;

    SalvageOperationDraftTest() {
        options.set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        options.set(CampaignOption.USE_ADVANCED_MEDICAL, false);
        options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(campaign.getActiveContracts()).thenReturn(List.of());
        when(campaign.getPlayerForce().isClanForce()).thenReturn(false);
        when(campaign.getPlayerForce().getAllFormations()).thenReturn(formations);
        when(campaign.getPlayerForce().getCombatTeamsAsList(campaign)).thenReturn(combatTeams);
        when(campaign.getPlayerForce().getHumanResources().getTechsExpanded(any(), any(), anyBoolean(), any(),
              anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(idleTechs);

        when(scenario.getId()).thenReturn(SCENARIO_ID);
        when(scenario.getName()).thenReturn("Raid");
        when(scenario.getSalvageFormations()).thenReturn(scenarioFormations);
        when(scenario.getSalvageTechs()).thenReturn(scenarioTechs);
    }

    // region Fixtures

    private Person person(String name, boolean isSupervisor) {
        Person person = mock(Person.class);
        UUID id = UUID.randomUUID();
        when(person.getId()).thenReturn(id);
        when(person.getFullName()).thenReturn(name);
        when(person.getRankName()).thenReturn("Sergeant");
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getMinutesLeft()).thenReturn(480);
        when(person.getSkillLevel(campaign, false, true)).thenReturn(SkillLevel.REGULAR);
        when(person.isSalvageSupervisor()).thenReturn(isSupervisor);
        when(person.isTechExpanded()).thenReturn(true);
        when(campaign.getPlayerForce().getHumanResources().getPerson(id)).thenReturn(person);
        return person;
    }

    /** An idle tech with work time left, who could join the operation if they qualify. */
    private Person idleTech(String name, boolean isSupervisor) {
        Person tech = person(name, isSupervisor);
        idleTechs.add(tech);
        return tech;
    }

    private Unit salvageUnit(List<Person> crew, boolean isSelfCrewed) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(40.0);
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Prime Mover");
        when(unit.getEntity()).thenReturn(tank);
        when(unit.canSalvage(anyBoolean())).thenReturn(true);
        when(unit.isRepairable()).thenReturn(true);
        when(unit.getCargoCapacityForSalvage()).thenReturn(20.0);
        when(unit.getCrew()).thenReturn(crew);
        when(unit.isSelfCrewed()).thenReturn(isSelfCrewed);
        return unit;
    }

    private Formation formation(String name, FormationType type, int salvageUnits, List<Unit> units,
          UUID toeTechId) {
        Formation formation = mock(Formation.class);
        int id = nextFormationId++;
        when(formation.getId()).thenReturn(id);
        when(formation.getFullName()).thenReturn(name);
        when(formation.getFormationType()).thenReturn(type);
        when(formation.getTechID()).thenReturn(toeTechId);
        when(formation.getScenarioId()).thenReturn(-1);
        when(formation.getSalvageUnitCount(any(), anyBoolean(), any())).thenReturn(salvageUnits);
        when(formation.getAllUnitsAsUnits(any(), eq(false))).thenReturn(units);
        when(campaign.getPlayerForce().getFormation(id)).thenReturn(formation);
        formations.add(formation);
        return formation;
    }

    private Formation salvageTeam(String name, List<Unit> units, UUID toeTechId) {
        return formation(name, FormationType.SALVAGE, units.size(), units, toeTechId);
    }

    private Formation combatTeam(String name, List<Unit> units) {
        Formation formation = formation(name, FormationType.STANDARD, units.size(), units, null);
        int formationId = formation.getId();
        CombatTeam combatTeam = mock(CombatTeam.class);
        when(combatTeam.getFormationId()).thenReturn(formationId);
        combatTeams.add(combatTeam);
        return formation;
    }

    private SalvageOperationDraft draft() {
        return new SalvageOperationDraft(campaign, scenario);
    }

    private static TeamOption option(SalvageOperationDraft draft, Formation formation) {
        for (TeamOption option : draft.getTeamOptions()) {
            if (option.formation() == formation) {
                return option;
            }
        }
        throw new AssertionError("No option for " + formation.getFullName());
    }

    private static List<Person> techsOf(List<SalvageTechCandidate> candidates) {
        List<Person> techs = new ArrayList<>();
        for (SalvageTechCandidate candidate : candidates) {
            techs.add(candidate.tech());
        }
        return techs;
    }

    // endregion Fixtures

    @Nested
    class Teams {
        @Test
        void salvageFormationsAreListedBeforeCombatTeamsAlphabetically() {
            Formation fire = combatTeam("Fire Lance", List.of(salvageUnit(List.of(), true)));
            Formation assault = combatTeam("Assault Lance", List.of(salvageUnit(List.of(), true)));
            Formation recoveryB = salvageTeam("Recovery B", List.of(salvageUnit(List.of(), true)), null);
            Formation recoveryA = salvageTeam("Recovery A", List.of(salvageUnit(List.of(), true)), null);

            List<Formation> listed = new ArrayList<>();
            for (TeamOption option : draft().getTeamOptions()) {
                listed.add(option.formation());
            }

            assertEquals(List.of(recoveryA, recoveryB, assault, fire), listed);
        }

        @Test
        void salvageFormationsNestedInSalvageFormationsAreNotListedSeparately() {
            Formation parent = salvageTeam("Recovery Company", List.of(salvageUnit(List.of(), true)), null);
            Formation child = salvageTeam("Recovery Lance", List.of(salvageUnit(List.of(), true)), null);
            when(child.getParentFormation()).thenReturn(parent);

            assertEquals(1, draft().getTeamOptions().size());
        }

        @Test
        void aCombatTeamThatIsAlsoASalvageFormationIsListedOnce() {
            Formation recovery = salvageTeam("Recovery", List.of(salvageUnit(List.of(), true)), null);
            int recoveryId = recovery.getId();
            CombatTeam combatTeam = mock(CombatTeam.class);
            when(combatTeam.getFormationId()).thenReturn(recoveryId);
            combatTeams.add(combatTeam);

            assertEquals(1, draft().getTeamOptions().size());
        }

        @Test
        void aCombatTeamContainingASalvageFormationIsNotListedAlongsideIt() {
            // CombatTeam.isEligible lets a standard combat team contain a Salvage sub-formation
            Formation parent = combatTeam("Lance", List.of(salvageUnit(List.of(), true)));
            Formation child = salvageTeam("Recovery", List.of(salvageUnit(List.of(), true)), null);
            when(child.getParentFormation()).thenReturn(parent);

            List<TeamOption> teamOptions = draft().getTeamOptions();

            assertEquals(1, teamOptions.size());
            assertSame(child, teamOptions.getFirst().formation());
        }

        @Test
        void availability() {
            Formation idle = salvageTeam("Idle", List.of(salvageUnit(List.of(), true)), null);
            Formation deployed = salvageTeam("Deployed", List.of(salvageUnit(List.of(), true)), null);
            when(deployed.isDeployed()).thenReturn(true);
            Formation empty = salvageTeam("Empty", List.of(), null);

            SalvageOperationDraft draft = draft();

            assertEquals(TeamAvailability.AVAILABLE, option(draft, idle).availability());
            assertEquals(TeamAvailability.DEPLOYED, option(draft, deployed).availability());
            assertEquals(TeamAvailability.NO_SALVAGE_UNITS, option(draft, empty).availability());
        }

        @Test
        void salvageFormationsFightingHereCanSalvageUnderRevisedRules() {
            options.set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_REVISED);
            Formation fighting = salvageTeam("Fighting", List.of(salvageUnit(List.of(), true)), null);
            when(fighting.isDeployed()).thenReturn(true);
            when(fighting.getScenarioId()).thenReturn(SCENARIO_ID);

            TeamOption option = option(draft(), fighting);

            assertEquals(TeamAvailability.FIGHTING_HERE, option.availability());
            assertTrue(option.availability().isStageable());
        }

        @Test
        void fightingFormationsCantSalvageUnderStrictRules() {
            Formation fighting = salvageTeam("Fighting", List.of(salvageUnit(List.of(), true)), null);
            when(fighting.isDeployed()).thenReturn(true);
            when(fighting.getScenarioId()).thenReturn(SCENARIO_ID);

            assertEquals(TeamAvailability.DEPLOYED, option(draft(), fighting).availability());
        }

        @Test
        void combatTeamsFightingHereCantSalvageEvenUnderRevisedRules() {
            options.set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_REVISED);
            Formation fighting = combatTeam("Fire Lance", List.of(salvageUnit(List.of(), true)));
            when(fighting.isDeployed()).thenReturn(true);
            when(fighting.getScenarioId()).thenReturn(SCENARIO_ID);

            assertEquals(TeamAvailability.DEPLOYED, option(draft(), fighting).availability());
        }

        @Test
        void aParentFightingHereCounts() {
            options.set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_REVISED);
            Formation parent = mock(Formation.class);
            when(parent.getScenarioId()).thenReturn(SCENARIO_ID);
            when(parent.getFormationType()).thenReturn(FormationType.STANDARD);
            Formation fighting = salvageTeam("Fighting", List.of(salvageUnit(List.of(), true)), null);
            when(fighting.isDeployed()).thenReturn(true);
            when(fighting.getParentFormation()).thenReturn(parent);

            assertEquals(TeamAvailability.FIGHTING_HERE, option(draft(), fighting).availability());
        }

        @Test
        void theScenariosCurrentTeamsStartStaged() {
            Formation assigned = salvageTeam("Assigned", List.of(salvageUnit(List.of(), true)), null);
            Formation other = salvageTeam("Other", List.of(salvageUnit(List.of(), true)), null);
            scenarioFormations.add(assigned.getId());

            SalvageOperationDraft draft = draft();

            assertTrue(draft.isStaged(option(draft, assigned)));
            assertFalse(draft.isStaged(option(draft, other)));
        }

        @Test
        void formationsThatNoLongerExistAreDropped() {
            salvageTeam("Team", List.of(salvageUnit(List.of(), true)), null);
            scenarioFormations.add(999);

            assertTrue(draft().getStagedTeams().isEmpty());
        }

        @Test
        void optionsKnowWhetherTheyAreSalvageFormations() {
            Formation recovery = salvageTeam("Recovery", List.of(salvageUnit(List.of(), true)), null);
            Formation fire = combatTeam("Fire Lance", List.of(salvageUnit(List.of(), true)));
            SalvageOperationDraft draft = draft();

            assertTrue(option(draft, recovery).isSalvageFormation());
            assertFalse(option(draft, fire).isSalvageFormation());
        }

        @Test
        void unavailableTeamsDontStartStaged() {
            Formation deployed = salvageTeam("Deployed", List.of(salvageUnit(List.of(), true)), null);
            when(deployed.isDeployed()).thenReturn(true);
            scenarioFormations.add(deployed.getId());

            assertTrue(draft().getStagedTeams().isEmpty());
        }

        @Test
        void unavailableTeamsCantBeStaged() {
            Formation empty = salvageTeam("Empty", List.of(), null);
            SalvageOperationDraft draft = draft();

            assertFalse(draft.stage(option(draft, empty)));
            assertTrue(draft.getStagedTeams().isEmpty());
        }

        @Test
        void stagingTwiceAndUnstagingTwiceDoNothing() {
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), null);
            SalvageOperationDraft draft = draft();
            TeamOption option = option(draft, team);

            assertTrue(draft.stage(option));
            assertFalse(draft.stage(option));
            assertTrue(draft.unstage(option));
            assertFalse(draft.unstage(option));
        }
    }

    @Nested
    class Techs {
        @Test
        void stagingATeamBringsItsTechAndCrew() {
            Person toeTech = person("Ana Rios", false);
            Person crewTech = person("Tomas Viel", false);
            Person driver = person("Driver", false);
            when(driver.isTechExpanded()).thenReturn(false);
            Person engineer = person("Engineer", false);
            when(engineer.isEngineer()).thenReturn(true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(crewTech, driver, engineer), false)),
                  toeTech.getId());
            SalvageOperationDraft draft = draft();

            draft.stage(option(draft, team));

            assertTrue(draft.isTechSelected(toeTech.getId()));
            assertTrue(draft.isTechSelected(crewTech.getId()));
            assertFalse(draft.isTechSelected(driver.getId()));
            assertFalse(draft.isTechSelected(engineer.getId()));
            assertEquals(TechOrigin.TEAM_TECH, draft.getTechOrigin(toeTech.getId()));
            assertEquals(TechOrigin.CREW, draft.getTechOrigin(crewTech.getId()));
        }

        @Test
        void selfCrewedUnitsBringNoCrew() {
            Person crewTech = person("Crew", false);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(crewTech), true)), null);
            SalvageOperationDraft draft = draft();

            draft.stage(option(draft, team));

            assertFalse(draft.isTechSelected(crewTech.getId()));
        }

        @Test
        void engineersAreNeverTeamTechs() {
            Person engineer = person("Engineer", false);
            when(engineer.isEngineer()).thenReturn(true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), engineer.getId());
            SalvageOperationDraft draft = draft();

            draft.stage(option(draft, team));

            assertTrue(draft.getSelectedTechs().isEmpty());
        }

        @Test
        void unstagingATeamTakesItsTechsAlong() {
            Person toeTech = person("Ana Rios", false);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), toeTech.getId());
            SalvageOperationDraft draft = draft();
            TeamOption option = option(draft, team);
            draft.stage(option);

            draft.unstage(option);

            assertFalse(draft.isTechSelected(toeTech.getId()));
        }

        @Test
        void techsSharedWithAnotherStagedTeamStay() {
            Person sharedTech = person("Shared", false);
            Formation first = salvageTeam("First", List.of(salvageUnit(List.of(), true)), sharedTech.getId());
            Formation second = salvageTeam("Second", List.of(salvageUnit(List.of(), true)), sharedTech.getId());
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, first));
            draft.stage(option(draft, second));

            draft.unstage(option(draft, first));

            assertTrue(draft.isTechSelected(sharedTech.getId()));
        }

        @Test
        void techsLeftBehindInACommittedPlanStayBehind() {
            Person kept = person("Kept", false);
            Person injured = person("Injured", false);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(kept, injured), false)), null);
            scenarioFormations.add(team.getId());
            scenarioTechs.add(kept.getId());

            SalvageOperationDraft draft = draft();

            assertTrue(draft.isTechSelected(kept.getId()));
            assertFalse(draft.isTechSelected(injured.getId()));
        }

        @Test
        void aHandPickedSupervisorSurvivesUnstagingHerTeam() {
            Person supervisor = idleTech("Supervisor", true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(supervisor), false)), null);
            SalvageOperationDraft draft = draft();
            TeamOption option = option(draft, team);

            draft.setTechSelected(supervisor.getId(), true);
            draft.stage(option);
            draft.unstage(option);

            assertTrue(draft.isTechSelected(supervisor.getId()));
            assertEquals(TechOrigin.SUPERVISOR, draft.getTechOrigin(supervisor.getId()));
        }

        @Test
        void aPreviouslyPickedSupervisorSurvivesUnstagingHerTeam() {
            Person supervisor = idleTech("Supervisor", true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(supervisor), false)), null);
            scenarioTechs.add(supervisor.getId());
            SalvageOperationDraft draft = draft();
            TeamOption option = option(draft, team);

            draft.stage(option);
            draft.unstage(option);

            assertTrue(draft.isTechSelected(supervisor.getId()));
        }

        @Test
        void teamTechsAreOnlyWorkedOutOnce() {
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(person("Crew", false)), false)), null);
            SalvageOperationDraft draft = draft();

            assertSame(draft.getTeamTechIds(team), draft.getTeamTechIds(team));
        }

        @Test
        void salvageSupervisorsAreOfferedButOtherIdleTechsAreNot() {
            Person supervisor = idleTech("Supervisor", true);
            Person ordinaryTech = idleTech("Ordinary", false);

            List<Person> offered = techsOf(draft().getTechCandidates());

            assertTrue(offered.contains(supervisor));
            assertFalse(offered.contains(ordinaryTech));
        }

        @Test
        void deployedEngineersAndTiredTechsAreNotOffered() {
            Person deployed = idleTech("Deployed", true);
            when(deployed.isDeployed()).thenReturn(true);
            Person engineer = idleTech("Engineer", true);
            when(engineer.isEngineer()).thenReturn(true);
            Person tired = idleTech("Tired", true);
            when(tired.getMinutesLeft()).thenReturn(0);

            assertTrue(draft().getTechCandidates().isEmpty());
        }

        @Test
        void techsAlreadyAssignedAreOfferedAndSelectedEvenIfBusy() {
            Person assigned = person("Assigned", false);
            when(assigned.isDeployed()).thenReturn(true);
            scenarioTechs.add(assigned.getId());

            SalvageOperationDraft draft = draft();

            assertTrue(techsOf(draft.getTechCandidates()).contains(assigned));
            assertTrue(draft.isTechSelected(assigned.getId()));
        }

        @Test
        void techsThatNoLongerExistAreDropped() {
            scenarioTechs.add(UUID.randomUUID());

            assertTrue(draft().getSelectedTechs().isEmpty());
        }

        @Test
        void engineersAlreadyAssignedAreDropped() {
            Person engineer = person("Engineer", false);
            when(engineer.isEngineer()).thenReturn(true);
            scenarioTechs.add(engineer.getId());

            SalvageOperationDraft draft = draft();

            assertFalse(draft.isTechSelected(engineer.getId()));
            assertFalse(techsOf(draft.getTechCandidates()).contains(engineer));
        }

        @Test
        void aStagedTeamsBusyCrewStillComeAlong() {
            Person busyCrew = person("Busy", false);
            when(busyCrew.isDeployed()).thenReturn(true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(busyCrew), false)), null);
            scenarioFormations.add(team.getId());

            SalvageOperationDraft draft = draft();

            assertTrue(draft.isTechSelected(busyCrew.getId()));
            assertEquals(TechOrigin.CREW, draft.getTechOrigin(busyCrew.getId()));
        }

        @Test
        void supervisorsCanBeAddedAndLeftBehind() {
            Person supervisor = idleTech("Supervisor", true);
            SalvageOperationDraft draft = draft();

            assertTrue(draft.setTechSelected(supervisor.getId(), true));
            assertFalse(draft.setTechSelected(supervisor.getId(), true), "already selected");
            assertEquals(TechOrigin.SUPERVISOR, draft.getTechOrigin(supervisor.getId()));
            assertTrue(draft.setTechSelected(supervisor.getId(), false));
            assertFalse(draft.isTechSelected(supervisor.getId()));
        }

        @Test
        void teamTechsCanBeLeftBehind() {
            Person toeTech = person("Ana Rios", false);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), toeTech.getId());
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));

            assertTrue(draft.setTechSelected(toeTech.getId(), false));
            assertTrue(techsOf(draft.getTechCandidates()).contains(toeTech), "still offered");
        }

        @Test
        void unknownTechsCantBeSelected() {
            assertFalse(draft().setTechSelected(UUID.randomUUID(), true));
        }

        @Test
        void candidatesAreSortedByMinutesLeftThenName() {
            Person busy = idleTech("Zed", true);
            when(busy.getMinutesLeft()).thenReturn(120);
            Person freeB = idleTech("Bea", true);
            Person freeA = idleTech("Abe", true);

            assertEquals(List.of(freeA, freeB, busy), techsOf(draft().getTechCandidates()));
        }

        @Test
        void candidateFactsAreWorkedOutOnce() {
            Person tech = idleTech("Jin Hale", true);
            when(tech.getHits()).thenReturn(2);
            when(tech.isPregnant()).thenReturn(true);
            Unit unit = mock(Unit.class);
            when(unit.getName()).thenReturn("Atlas AS7-D");
            when(tech.getTechUnits()).thenReturn(List.of(unit));

            SalvageTechCandidate candidate = draft().getTechCandidates().getFirst();

            assertTrue(candidate.isInjured());
            assertTrue(candidate.isPregnant());
            assertTrue(candidate.hasTechUnits());
            assertEquals(SkillLevel.REGULAR, candidate.skillLevel());
            assertTrue(candidate.matches("atlas"));
            assertTrue(candidate.matches("jin"));
        }

        @Test
        void advancedMedicalCountsInjuriesRatherThanHits() {
            options.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
            Person tech = idleTech("Hale", true);
            when(tech.getHits()).thenReturn(2);

            assertFalse(draft().getTechCandidates().getFirst().isInjured());
        }

        @Test
        void techsWithoutASkillLevelAreTreatedAsHavingNone() {
            Person tech = idleTech("Unskilled", true);
            when(tech.getSkillLevel(campaign, false, true)).thenReturn(null);

            assertEquals(SkillLevel.NONE, draft().getTechCandidates().getFirst().skillLevel());
        }
    }

    @Nested
    class Tallies {
        @Test
        void talliesSumTheStagedTeams() {
            Person techA = person("A", false);
            when(techA.getMinutesLeft()).thenReturn(200);
            Person techB = person("B", false);
            when(techB.getMinutesLeft()).thenReturn(100);
            Formation first = salvageTeam("First", List.of(salvageUnit(List.of(), true)), techA.getId());
            Formation second = salvageTeam("Second", List.of(salvageUnit(List.of(), true),
                  salvageUnit(List.of(), true)), techB.getId());
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, first));
            draft.stage(option(draft, second));

            assertEquals(3, draft.getSalvageUnitCount());
            assertEquals(20.0, draft.getBestCargoCapacity());
            assertEquals(40.0, draft.getBestTowCapacity());
            assertFalse(draft.hasNavalTug());
            assertEquals(300, draft.getSelectedTechMinutes());
            assertTrue(draft.isLowOnTechTime());
        }

        @Test
        void aTugInAStagedTeamIsNoted() {
            when(scenario.getBoardType()).thenReturn(Scenario.T_SPACE);
            Dropship dropship = mock(Dropship.class);
            MiscType tugType = mock(MiscType.class);
            when(tugType.hasFlag(MiscType.F_NAVAL_TUG_ADAPTOR)).thenReturn(true);
            MiscMounted tug = mock(MiscMounted.class);
            when(tug.getType()).thenReturn(tugType);
            when(tug.getEntity()).thenReturn(dropship);
            when(tug.isOperable()).thenReturn(true);
            List<MiscMounted> misc = List.of(tug);
            when(dropship.getMisc()).thenReturn(misc);
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(dropship);
            when(unit.canSalvage(anyBoolean())).thenReturn(true);
            when(unit.isRepairable()).thenReturn(true);
            Formation team = salvageTeam("Tugs", List.of(unit), null);
            SalvageOperationDraft draft = draft();

            assertFalse(draft.hasNavalTug(), "nothing staged yet");
            draft.stage(option(draft, team));

            assertTrue(draft.hasNavalTug());
        }

        @Test
        void noTeamsIsNeverLowOnTechTime() {
            assertFalse(draft().isLowOnTechTime());
        }

        @Test
        void enoughTechTimeIsNotLow() {
            Person tech = person("A", false);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), tech.getId());
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));

            assertEquals(480, draft.getSelectedTechMinutes());
            assertFalse(draft.isLowOnTechTime());
        }
    }

    @Nested
    class Commit {
        @Test
        void committingWritesTheTeamsAndTechs() {
            Person toeTech = person("Ana Rios", false);
            Person supervisor = idleTech("Supervisor", true);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), toeTech.getId());
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));
            draft.setTechSelected(supervisor.getId(), true);

            draft.commit();

            InOrder inOrder = inOrder(scenario);
            inOrder.verify(scenario).clearSalvageFormations();
            inOrder.verify(scenario).clearSalvageTechs();
            verify(scenario).addSalvageFormation(team.getId());
            verify(scenario).addSalvageTech(toeTech.getId());
            verify(scenario).addSalvageTech(supervisor.getId());
        }

        @Test
        void committingWithoutTeamsClearsTheOperation() {
            Person supervisor = idleTech("Supervisor", true);
            scenarioTechs.add(supervisor.getId());
            SalvageOperationDraft draft = draft();

            draft.commit();

            verify(scenario).clearSalvageFormations();
            verify(scenario).clearSalvageTechs();
            verify(scenario, never()).addSalvageTech(any());
        }

        @Test
        void nothingChangesUntilCommitted() {
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), null);
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));

            verify(scenario, never()).clearSalvageFormations();
            verify(scenario, never()).addSalvageFormation(anyInt());
        }

        @Test
        void salvageTeamsDeployOnStratCon() {
            options.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.NORMAL);
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), null);
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));

            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class,
                  CALLS_REAL_METHODS)) {
                utilities.when(() -> CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario))
                      .thenAnswer(invocation -> null);

                draft.commit();

                utilities.verify(() -> CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario));
            }
        }

        @Test
        void withoutStratConNothingDeploys() {
            Formation team = salvageTeam("Team", List.of(salvageUnit(List.of(), true)), null);
            SalvageOperationDraft draft = draft();
            draft.stage(option(draft, team));

            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class,
                  CALLS_REAL_METHODS)) {
                draft.commit();

                utilities.verify(() -> CamOpsSalvageUtilities.deploySalvageTeams(any(), any()), never());
            }
        }
    }

    @Nested
    class ScenarioFacts {
        @Test
        void battlefieldControlIsUnknownOutsideDynamicScenarios() {
            assertNull(draft().getBattlefieldControlType());
        }

        @Test
        void dynamicScenariosWithoutATemplateAssumeTheVictorHoldsTheField() {
            AtBDynamicScenario dynamicScenario =
                  mock(AtBDynamicScenario.class);
            when(dynamicScenario.getSalvageFormations()).thenReturn(List.of());
            when(dynamicScenario.getSalvageTechs()).thenReturn(List.of());

            assertEquals(ScenarioTemplate.BattlefieldControlType.VICTOR,
                  new SalvageOperationDraft(campaign, dynamicScenario).getBattlefieldControlType());
        }

        @Test
        void dynamicScenariosUseTheirTemplatesControl() {
            AtBDynamicScenario dynamicScenario =
                  mock(AtBDynamicScenario.class);
            when(dynamicScenario.getSalvageFormations()).thenReturn(List.of());
            when(dynamicScenario.getSalvageTechs()).thenReturn(List.of());
            ScenarioTemplate template =
                  mock(ScenarioTemplate.class);
            when(template.getBattlefieldControl())
                  .thenReturn(ScenarioTemplate.BattlefieldControlType.ENEMY);
            when(dynamicScenario.getTemplate()).thenReturn(template);

            assertEquals(ScenarioTemplate.BattlefieldControlType.ENEMY,
                  new SalvageOperationDraft(campaign, dynamicScenario).getBattlefieldControlType());
        }

        @Test
        void groundScenariosAreNotInSpace() {
            assertFalse(draft().isInSpace());
        }

        @Test
        void spaceScenariosAreInSpace() {
            when(scenario.getBoardType()).thenReturn(Scenario.T_SPACE);

            assertTrue(draft().isInSpace());
        }
    }
}
