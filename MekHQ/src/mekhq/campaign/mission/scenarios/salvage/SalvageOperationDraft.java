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

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.isForceDeployedToStratCon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * The player's plan for a scenario's salvage operation, made before the scenario starts: which formations recover the
 * wrecks (the salvage teams), and which techs go with them.
 *
 * <p>Staging a team brings its techs along: the formation's TO&amp;E tech and the tech crew of its units. The player
 * can add salvage supervisors, or leave any tech behind. Nothing reaches the scenario until {@link #commit()}, so a
 * cancelled plan leaves the scenario untouched.</p>
 *
 * <p>The draft holds no user interface; the salvage operation planner renders it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SalvageOperationDraft {
    /** Below this many minutes of tech time, few wrecks can be recovered, so the player is warned. */
    public static final int LOW_TECH_MINUTES = 360;

    private final Campaign campaign;
    private final Scenario scenario;
    private final AbstractSalvage salvageRules;
    private final boolean isInSpace;
    private final List<TeamOption> teamOptions;
    private final Set<Integer> stagedFormationIds = new LinkedHashSet<>();
    private final Set<UUID> selectedTechIds = new LinkedHashSet<>();
    private final Map<UUID, SalvageTechCandidate> techPool = new LinkedHashMap<>();

    /**
     * Whether a formation can join the salvage operation.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum TeamAvailability {
        /** The formation is idle and has units that can salvage. */
        AVAILABLE,
        /** A Salvage formation fighting in this scenario, which the salvage system lets salvage it afterward. */
        FIGHTING_HERE,
        /** The formation is deployed, to this or another scenario, or on StratCon. */
        DEPLOYED,
        /** None of the formation's units can take part in salvage operations. */
        NO_SALVAGE_UNITS;

        /**
         * @return {@code true} if the formation can be staged
         */
        public boolean isStageable() {
            return (this == AVAILABLE) || (this == FIGHTING_HERE);
        }
    }

    /**
     * A formation the player could use as a salvage team.
     *
     * @param formation    the formation
     * @param data         the formation's salvage capabilities
     * @param availability whether the formation can join the operation
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record TeamOption(Formation formation, SalvageFormationData data, TeamAvailability availability) {
        /**
         * @return {@code true} if the formation is set to 'Salvage', rather than being a combat team
         */
        public boolean isSalvageFormation() {
            return formation.getFormationType().isSalvage();
        }
    }

    /**
     * How a tech comes to be on the salvage operation.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum TechOrigin {
        /** The TO&amp;E tech of a staged team. */
        TEAM_TECH,
        /** Tech crew of a unit in a staged team. */
        CREW,
        /** A salvage supervisor, added by the player. */
        SUPERVISOR
    }

    /**
     * Starts a plan from the scenario's current salvage assignments.
     *
     * @param campaign the current campaign
     * @param scenario the scenario the salvage operation is for
     */
    public SalvageOperationDraft(Campaign campaign, Scenario scenario) {
        this.campaign = campaign;
        this.scenario = scenario;
        this.salvageRules = campaign.getCampaignOptions().get(CampaignOption.SALVAGE_SYSTEM).getSalvage();
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;
        this.teamOptions = buildTeamOptions();

        for (int formationId : scenario.getSalvageFormations()) {
            TeamOption option = findTeamOption(formationId);
            if ((option != null) && option.availability().isStageable()) {
                stagedFormationIds.add(formationId);
            }
        }

        buildTechPool();
        for (UUID techId : scenario.getSalvageTechs()) {
            if (techPool.containsKey(techId)) {
                selectedTechIds.add(techId);
            }
        }
        for (TeamOption option : getStagedTeams()) {
            selectedTechIds.addAll(getTeamTechIds(option.formation()));
        }
    }

    // region Scenario

    public Scenario getScenario() {
        return scenario;
    }

    public AbstractSalvage getSalvageRules() {
        return salvageRules;
    }

    public boolean isInSpace() {
        return isInSpace;
    }

    /**
     * @return who controls the battlefield once the scenario is over, or {@code null} if that isn't known in advance
     */
    public @Nullable ScenarioTemplate.BattlefieldControlType getBattlefieldControlType() {
        if (scenario instanceof AtBDynamicScenario dynamicScenario) {
            ScenarioTemplate template = dynamicScenario.getTemplate();
            return (template != null) ? template.getBattlefieldControl() :
                         ScenarioTemplate.BattlefieldControlType.VICTOR;
        }
        return null;
    }

    // endregion Scenario

    // region Teams

    /**
     * Lists every formation that could be a salvage team: Salvage formations first, then combat teams, each
     * alphabetically. Formations that can't join are included, with the reason, so the player can see why.
     */
    private List<TeamOption> buildTeamOptions() {
        LocalHangar hangar = campaign.getPlayerForce().getHangar();
        List<AbstractContract> activeContracts = campaign.getActiveContracts();
        List<Integer> alreadyAssignedFormations = scenario.getSalvageFormations();

        List<Formation> salvageFormations = new ArrayList<>();
        Set<Integer> listedIds = new LinkedHashSet<>();
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            Formation parentFormation = formation.getParentFormation();
            boolean isNestedInSalvageFormation = (parentFormation != null) &&
                                                       parentFormation.getFormationType().isSalvage();
            if (formation.getFormationType().isSalvage() && !isNestedInSalvageFormation) {
                salvageFormations.add(formation);
                listedIds.add(formation.getId());
            }
        }

        List<Formation> combatTeams = new ArrayList<>();
        for (CombatTeam combatTeam : campaign.getPlayerForce().getCombatTeamsAsList(campaign)) {
            Formation formation = campaign.getPlayerForce().getFormation(combatTeam.getFormationId());
            if ((formation != null) && listedIds.add(formation.getId())) {
                combatTeams.add(formation);
            }
        }

        salvageFormations.sort(Comparator.comparing(Formation::getFullName));
        combatTeams.sort(Comparator.comparing(Formation::getFullName));

        List<TeamOption> options = new ArrayList<>();
        for (Formation formation : salvageFormations) {
            options.add(new TeamOption(formation, SalvageFormationData.buildData(campaign, formation, isInSpace),
                  getAvailability(formation, hangar, activeContracts, alreadyAssignedFormations)));
        }
        for (Formation formation : combatTeams) {
            options.add(new TeamOption(formation, SalvageFormationData.buildData(campaign, formation, isInSpace),
                  getAvailability(formation, hangar, activeContracts, alreadyAssignedFormations)));
        }
        return options;
    }

    private TeamAvailability getAvailability(Formation formation, LocalHangar hangar,
          List<AbstractContract> activeContracts, List<Integer> alreadyAssignedFormations) {
        if (formation.getSalvageUnitCount(hangar, isInSpace, salvageRules) <= 0) {
            return TeamAvailability.NO_SALVAGE_UNITS;
        }

        // A formation already assigned to this operation skips the StratCon check. Otherwise, a player who assigns
        // a formation and then cancels at the last minute would find it unavailable for the operation it was
        // assigned to.
        boolean isDeployedToStratCon = !alreadyAssignedFormations.contains(formation.getId()) &&
                                             isForceDeployedToStratCon(activeContracts, formation.getId());
        if (!formation.isDeployed() && !isDeployedToStratCon) {
            return TeamAvailability.AVAILABLE;
        }

        // Some salvage systems let Salvage formations fight in a scenario and then salvage it
        if (salvageRules.canSalvageAfterFighting(formation) && isDeployedToThisScenario(formation)) {
            return TeamAvailability.FIGHTING_HERE;
        }
        return TeamAvailability.DEPLOYED;
    }

    /** Checks whether a formation, or any formation above it, is deployed to this scenario. */
    private boolean isDeployedToThisScenario(Formation formation) {
        for (Formation current = formation; current != null; current = current.getParentFormation()) {
            if (current.getScenarioId() == scenario.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return every formation that could be a salvage team, including those that can't join now
     */
    public List<TeamOption> getTeamOptions() {
        return teamOptions;
    }

    private @Nullable TeamOption findTeamOption(int formationId) {
        for (TeamOption option : teamOptions) {
            if (option.formation().getId() == formationId) {
                return option;
            }
        }
        return null;
    }

    /**
     * @return the staged teams, in the order they were listed
     */
    public List<TeamOption> getStagedTeams() {
        List<TeamOption> stagedTeams = new ArrayList<>();
        for (TeamOption option : teamOptions) {
            if (stagedFormationIds.contains(option.formation().getId())) {
                stagedTeams.add(option);
            }
        }
        return stagedTeams;
    }

    /**
     * @param option a team
     *
     * @return {@code true} if the team is staged
     */
    public boolean isStaged(TeamOption option) {
        return stagedFormationIds.contains(option.formation().getId());
    }

    /**
     * Stages a team, bringing its techs along.
     *
     * @param option the team
     *
     * @return {@code true} if the team was staged; {@code false} if it can't join the operation, or was already staged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean stage(TeamOption option) {
        if (!option.availability().isStageable() || !stagedFormationIds.add(option.formation().getId())) {
            return false;
        }
        for (UUID techId : getTeamTechIds(option.formation())) {
            addToPool(techId);
            selectedTechIds.add(techId);
        }
        return true;
    }

    /**
     * Unstages a team. Its techs leave with it, unless another staged team also brings them.
     *
     * @param option the team
     *
     * @return {@code true} if the team was unstaged; {@code false} if it wasn't staged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean unstage(TeamOption option) {
        if (!stagedFormationIds.remove(option.formation().getId())) {
            return false;
        }
        Set<UUID> stillBrought = new LinkedHashSet<>();
        for (TeamOption stagedTeam : getStagedTeams()) {
            stillBrought.addAll(getTeamTechIds(stagedTeam.formation()));
        }
        for (UUID techId : getTeamTechIds(option.formation())) {
            if (!stillBrought.contains(techId)) {
                selectedTechIds.remove(techId);
            }
        }
        return true;
    }

    // endregion Teams

    // region Techs

    /**
     * Gathers the techs who could join the operation: every idle, non-engineer tech with work time left, plus anyone
     * already assigned to this scenario's salvage.
     */
    private void buildTechPool() {
        for (Person tech : campaign.getPlayerForce()
                                 .getHumanResources()
                                 .getTechsExpanded(campaign.getPlayerForce().getHangar().getUnits(),
                                       campaign.getCampaignOptions(),
                                       campaign.getPlayerForce().isClanForce(),
                                       campaign.getLocalDate(),
                                       true,
                                       false,
                                       true)) {
            if (!tech.isDeployed() && (tech.getMinutesLeft() > 0) && !tech.isEngineer()) {
                techPool.put(tech.getId(), buildCandidate(tech));
            }
        }
        for (UUID techId : scenario.getSalvageTechs()) {
            addToPool(techId);
        }
        for (TeamOption option : getStagedTeams()) {
            for (UUID techId : getTeamTechIds(option.formation())) {
                addToPool(techId);
            }
        }
    }

    private void addToPool(UUID techId) {
        if (techPool.containsKey(techId)) {
            return;
        }
        Person tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);
        if ((tech != null) && !tech.isEngineer()) {
            techPool.put(techId, buildCandidate(tech));
        }
    }

    private SalvageTechCandidate buildCandidate(Person tech) {
        boolean isSecondary = CamOpsSalvageUtilities.isUseSecondaryTechSkill(tech);
        SkillLevel skillLevel = tech.getSkillLevel(campaign, isSecondary, true);
        boolean isInjured = campaign.getCampaignOptions().isUseAdvancedMedical() ?
                                  tech.hasInjuries(true) :
                                  (tech.getHits() > 0);
        boolean isClan = campaign.getPlayerForce().isClanForce();

        StringBuilder searchText = new StringBuilder();
        searchText.append(tech.getFullName()).append(' ')
              .append(tech.getRankName()).append(' ')
              .append(tech.getPrimaryRole().getLabel(isClan)).append(' ')
              .append(tech.getSecondaryRole().getLabel(isClan));
        for (Unit unit : tech.getTechUnits()) {
            searchText.append(' ').append(unit.getName());
        }

        return new SalvageTechCandidate(tech, (skillLevel == null) ? SkillLevel.NONE : skillLevel, isInjured,
              tech.isPregnant(), !tech.getTechUnits().isEmpty(), Math.max(0, tech.getMinutesLeft()),
              searchText.toString().toLowerCase(Locale.ROOT));
    }

    /**
     * The techs a team brings: its TO&amp;E tech, and the tech crew of its units that aren't self-crewed. Engineers
     * never salvage.
     *
     * @param formation the team's formation
     *
     * @return the IDs of the team's techs
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Set<UUID> getTeamTechIds(Formation formation) {
        Set<UUID> techIds = new LinkedHashSet<>();
        UUID toeTechId = formation.getTechID();
        if (toeTechId != null) {
            Person toeTech = campaign.getPlayerForce().getHumanResources().getPerson(toeTechId);
            if ((toeTech != null) && !toeTech.isEngineer()) {
                techIds.add(toeTechId);
            }
        }

        for (Unit unit : formation.getAllUnitsAsUnits(campaign.getPlayerForce().getHangar(), false)) {
            if (unit.isSelfCrewed()) {
                continue;
            }
            for (Person crewMember : unit.getCrew()) {
                if (crewMember.isTechExpanded() && !crewMember.isEngineer()) {
                    techIds.add(crewMember.getId());
                }
            }
        }
        return techIds;
    }

    /**
     * @param techId a tech
     *
     * @return how the tech comes to be on the operation: brought by a staged team (as its TO&amp;E tech or crew), or
     *       added as a salvage supervisor
     */
    public TechOrigin getTechOrigin(UUID techId) {
        for (TeamOption option : getStagedTeams()) {
            Formation formation = option.formation();
            if (techId.equals(formation.getTechID())) {
                return TechOrigin.TEAM_TECH;
            }
        }
        for (TeamOption option : getStagedTeams()) {
            if (getTeamTechIds(option.formation()).contains(techId)) {
                return TechOrigin.CREW;
            }
        }
        return TechOrigin.SUPERVISOR;
    }

    /**
     * Lists the techs the player can choose from: salvage supervisors, the techs staged teams bring, and anyone
     * already selected.
     *
     * @return the techs, most work time first, then by name
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<SalvageTechCandidate> getTechCandidates() {
        Set<UUID> teamTechIds = new LinkedHashSet<>();
        for (TeamOption option : getStagedTeams()) {
            teamTechIds.addAll(getTeamTechIds(option.formation()));
        }

        List<SalvageTechCandidate> candidates = new ArrayList<>();
        for (SalvageTechCandidate candidate : techPool.values()) {
            UUID techId = candidate.tech().getId();
            if (candidate.tech().isSalvageSupervisor() || teamTechIds.contains(techId) ||
                      selectedTechIds.contains(techId)) {
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingInt(SalvageTechCandidate::minutesLeft).reversed()
                              .thenComparing(candidate -> candidate.tech().getFullName()));
        return candidates;
    }

    /**
     * @param techId a tech
     *
     * @return {@code true} if the tech is going on the operation
     */
    public boolean isTechSelected(UUID techId) {
        return selectedTechIds.contains(techId);
    }

    /**
     * Adds a tech to the operation, or leaves them behind.
     *
     * @param techId     the tech
     * @param isSelected {@code true} to add the tech; {@code false} to leave them behind
     *
     * @return {@code true} if the selection changed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean setTechSelected(UUID techId, boolean isSelected) {
        if (!techPool.containsKey(techId)) {
            return false;
        }
        return isSelected ? selectedTechIds.add(techId) : selectedTechIds.remove(techId);
    }

    /**
     * @return the techs going on the operation, most work time first
     */
    public List<SalvageTechCandidate> getSelectedTechs() {
        List<SalvageTechCandidate> selectedTechs = new ArrayList<>();
        for (SalvageTechCandidate candidate : getTechCandidates()) {
            if (selectedTechIds.contains(candidate.tech().getId())) {
                selectedTechs.add(candidate);
            }
        }
        return selectedTechs;
    }

    // endregion Techs

    // region Tallies

    /**
     * @return the number of units in the staged teams that can take part in salvage operations
     */
    public int getSalvageUnitCount() {
        int count = 0;
        for (TeamOption option : getStagedTeams()) {
            count += option.data().salvageCapableUnits();
        }
        return count;
    }

    /**
     * @return the largest cargo capacity of any unit in the staged teams, in tons
     */
    public double getBestCargoCapacity() {
        double best = 0.0;
        for (TeamOption option : getStagedTeams()) {
            best = Math.max(best, option.data().maximumCargoCapacity());
        }
        return best;
    }

    /**
     * @return the largest tow capacity of any unit in the staged teams, in tons
     */
    public double getBestTowCapacity() {
        double best = 0.0;
        for (TeamOption option : getStagedTeams()) {
            best = Math.max(best, option.data().maximumTowCapacity());
        }
        return best;
    }

    /**
     * @return {@code true} if a unit in the staged teams has a working naval tug adaptor
     */
    public boolean hasNavalTug() {
        for (TeamOption option : getStagedTeams()) {
            if (option.data().hasTug()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the work time the selected techs have left between them, in minutes
     */
    public int getSelectedTechMinutes() {
        int minutes = 0;
        for (SalvageTechCandidate candidate : getSelectedTechs()) {
            minutes += candidate.minutesLeft();
        }
        return minutes;
    }

    /**
     * @return {@code true} if teams are staged, but their techs have too little time to recover much
     */
    public boolean isLowOnTechTime() {
        return !stagedFormationIds.isEmpty() && (getSelectedTechMinutes() < LOW_TECH_MINUTES);
    }

    // endregion Tallies

    /**
     * Writes the plan to the scenario, replacing its salvage formations and techs, and deploys the salvage teams on
     * StratCon. Without any teams, there is no salvage operation, so no techs are assigned either.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void commit() {
        scenario.clearSalvageFormations();
        scenario.clearSalvageTechs();
        if (stagedFormationIds.isEmpty()) {
            return;
        }

        for (int formationId : stagedFormationIds) {
            scenario.addSalvageFormation(formationId);
        }
        for (UUID techId : selectedTechIds) {
            scenario.addSalvageTech(techId);
        }

        if (campaign.getCampaignOptions().isUseStratCon()) {
            CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario);
        }
    }
}
