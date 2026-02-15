/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratCon;

import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.UNRESOLVED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.adapter.DateAdapter;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * Class that handles scenario metadata and interaction at the StratCon level
 *
 * @author NickAragua
 */
public class StratConScenario implements IStratConDisplayable {
    private static final MMLogger LOGGER = MMLogger.create(StratConScenario.class);

    /**
     * Represents the possible states of a StratCon scenario
     */
    public enum ScenarioState {
        NONEXISTENT,
        UNRESOLVED,
        PRIMARY_FORCES_COMMITTED,
        AWAITING_REINFORCEMENTS,
        REINFORCEMENTS_COMMITTED,
        COMPLETED,
        IGNORED,
        DEFEATED;

        private final static Map<ScenarioState, String> scenarioStateNames;

        static {
            scenarioStateNames = new HashMap<>();
            scenarioStateNames.put(ScenarioState.NONEXISTENT, "Shouldn't be seen");
            scenarioStateNames.put(ScenarioState.UNRESOLVED, "Unresolved");
            scenarioStateNames.put(ScenarioState.PRIMARY_FORCES_COMMITTED, "Primary forces committed");
            scenarioStateNames.put(ScenarioState.AWAITING_REINFORCEMENTS,
                  "Forces committed reinforcement interception not resolved");
            scenarioStateNames.put(ScenarioState.COMPLETED, "Victory");
            scenarioStateNames.put(ScenarioState.IGNORED, "Ignored");
            scenarioStateNames.put(ScenarioState.DEFEATED, "Defeat");
        }

        public String getScenarioStateName() {
            return scenarioStateNames.get(this);
        }
    }

    private AtBDynamicScenario backingScenario;

    private int backingScenarioID;
    private ScenarioState currentState = ScenarioState.UNRESOLVED;
    private int requiredPlayerLances;
    private boolean turningPoint;
    private boolean isStrategicObjective;
    private LocalDate deploymentDate;
    private LocalDate actionDate;
    private LocalDate returnDate;
    private StratConCoords coords;
    private int numDefensivePoints;
    private boolean ignoreForceAutoAssignment;
    private int leadershipPointsUsed;
    private Set<Integer> failedReinforcements = new HashSet<>();
    private ArrayList<Integer> primaryForceIDs = new ArrayList<>();

    /**
     * Add a force to the backing scenario. Do our best to add the force as a "primary" force, as defined in the
     * scenario template.
     *
     * @param forceID ID of the force to add.
     */
    public void addPrimaryForce(int forceID) {
        backingScenario.addForce(forceID, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        primaryForceIDs.add(forceID);
    }

    /**
     * Add a force to the backing scenario, trying to associate it with the given template. Does some scenario and force
     * house-keeping, fires a deployment changed event.
     */
    public void addForce(Formation formation, String templateID, Campaign campaign) {
        if (!getBackingScenario().getForceIDs().contains(formation.getId())) {
            backingScenario.addForce(formation.getId(), templateID);
            formation.setScenarioId(getBackingScenarioID(), campaign);

            for (UUID unitID : formation.getAllUnits(true)) {
                Unit unit = campaign.getUnit(unitID);

                if (unit == null) {
                    return;
                }

                addPlayerTransportRelationships(unit);
            }

            MekHQ.triggerEvent(new DeploymentChangedEvent(formation, getBackingScenario()));
        }
    }

    /**
     * Add an individual unit to the backing scenario, trying to associate it with the given template. Performs
     * housekeeping on the unit and scenario and invokes a deployment changed event.
     */
    public void addUnit(Unit unit, String templateID, boolean useLeadership) {
        if (!backingScenario.containsPlayerUnit(unit.getId())) {
            backingScenario.addUnit(unit.getId(), templateID);

            addPlayerTransportRelationships(unit);

            unit.setScenarioId(getBackingScenarioID());

            if (useLeadership) {
                int baseBattleValue = unit.getEntity().calculateBattleValue(true, true);
                leadershipPointsUsed += baseBattleValue;
            }

            MekHQ.triggerEvent(new DeploymentChangedEvent(unit, getBackingScenario()));
        }
    }

    /**
     * Establishes transport relationships between the specified unit and any assigned transport units in the campaign.
     * Each transport assignment of the given unit is checked, and if valid transport is found, a transport relationship
     * is added to the backing scenario.
     *
     * @param unit the {@code Unit} for which transport relationships will be established. This unit will be checked for
     *             active transport assignments.
     */
    private void addPlayerTransportRelationships(Unit unit) {
        for (CampaignTransportType transportType : CampaignTransportType.values()) {
            ITransportAssignment transportAssignment = unit.getTransportAssignment(transportType);
            if (transportAssignment != null) {
                Unit transport = transportAssignment.getTransport();

                if (transport == null) {
                    LOGGER.warn("Unit {} has a transport assigned, but the transported unit doesn't exist",
                          unit.getId());
                    continue;
                }

                UUID transportId = transport.getId();

                backingScenario.addPlayerTransportRelationship(transportId, unit.getId());
            }
        }
    }

    public List<Integer> getAssignedForces() {
        return backingScenario.getForceIDs();
    }

    /**
     * These are all the force IDs that have been matched up to a template Note: since there's a default Reinforcements
     * template, this is all forces that have been assigned to this scenario
     */
    public List<Integer> getPlayerTemplateForceIDs() {
        return backingScenario.getPlayerTemplateForceIDs();
    }

    /**
     * These are all the "primary" force IDs, meaning forces that have been used by the scenario to drive the generation
     * of the OpFor.
     */
    @XmlElementWrapper(name = "primaryForceIDs")
    @XmlElement(name = "primaryForceID")
    public ArrayList<Integer> getPrimaryForceIDs() {
        return primaryForceIDs;
    }

    public void setPrimaryForceIDs(ArrayList<Integer> primaryForceIDs) {
        this.primaryForceIDs = primaryForceIDs;
    }

    /**
     * This convenience method sets the scenario's current state to PRIMARY_FORCES_COMMITTED and fixes the forces that
     * were assigned to this scenario prior as "primary".
     */
    public void commitPrimaryForces() {
        currentState = ScenarioState.PRIMARY_FORCES_COMMITTED;
        getPrimaryForceIDs().clear();
        for (int forceID : backingScenario.getPlayerTemplateForceIDs()) {
            getPrimaryForceIDs().add(forceID);
        }
    }

    public ScenarioState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ScenarioState state) {
        currentState = state;
    }

    @Override
    public String getInfo() {
        return getInfo(null);
    }

    public String getInfo(@Nullable Campaign campaign) {
        StringBuilder stateBuilder = new StringBuilder();

        if (isStrategicObjective()) {
            stateBuilder.append("<span color='")
                  .append(ReportingUtilities.getNegativeColor())
                  .append("'>Contract objective located</span><br/>");
        }

        if (backingScenario != null) {
            stateBuilder.append("<b>Scenario:</b> ").append(backingScenario.getName()).append("<br/>");

            if (backingScenario.getTemplate() != null) {
                stateBuilder.append("<i>")
                      .append(backingScenario.getTemplate().shortBriefing)
                      .append("</i>")
                      .append("<br/>");
            }

            if (isTurningPoint()) {
                stateBuilder.append("<span color='")
                      .append(MekHQ.getMHQOptions().getFontColorWarning())
                      .append("'>Turning Point</span><br/>");
            }

            stateBuilder.append("<b>Status:</b> ").append(currentState.getScenarioStateName()).append("<br/>");


            stateBuilder.append("<b>Terrain:</b> ").append(backingScenario.getMap()).append("<br/>");

            if (deploymentDate != null) {
                stateBuilder.append("<b>Deployment Date:</b> ").append(deploymentDate).append("<br/>");
            }

            if (actionDate != null) {
                stateBuilder.append("<b>Battle Date:</b> ").append(actionDate).append("<br/>");
            }

            if (returnDate != null) {
                stateBuilder.append("<b>Return Date:</b> ").append(returnDate).append("<br/>");
            }

            int hostileBV = backingScenario.getTeamTotalBattleValue(campaign, false);
            int alliedBV = backingScenario.getTeamTotalBattleValue(campaign, true);

            if (campaign != null) {
                stateBuilder.append(String.format("<b>Hostile BV:</b> %s<br>",
                      hostileBV == 0 && alliedBV == 0 ? "UNKNOWN" : hostileBV));
                stateBuilder.append(String.format("<b>Allied BV:</b> %s",
                      hostileBV == 0 && alliedBV == 0 ? "UNKNOWN" : alliedBV));
            }
        }

        stateBuilder.append("</html>");
        return stateBuilder.toString();
    }

    public void updateMinefieldCount(int minefieldType, int number) {
        backingScenario.setNumPlayerMinefields(minefieldType, number);
    }

    public String getName() {
        return backingScenario.getName();
    }

    /**
     * Returns the name of this object as an HTML hyperlink.
     *
     * <p>The hyperlink is formatted with a "SCENARIO:" protocol prefix followed by the object's ID. This allows UI
     * components that support HTML to render the name as a clickable link, which can be used to navigate to or focus on
     * this specific object when clicked.</p>
     *
     * @return An HTML formatted string containing the object's name as a hyperlink with its ID
     *
     * @author Illiani
     * @since 0.50.05
     */
    public String getHyperlinkedName() {
        return String.format("<a href='SCENARIO:%s'>%s</a>",
              backingScenario != null ? backingScenario.getId() : -1,
              getName());
    }

    public int getRequiredPlayerLances() {
        return requiredPlayerLances;
    }


    public void setRequiredPlayerLances(int requiredPlayerLances) {
        this.requiredPlayerLances = requiredPlayerLances;
    }

    public void incrementRequiredPlayerLances() {
        requiredPlayerLances++;
    }

    public boolean isTurningPoint() {
        return turningPoint;
    }

    /**
     * Determines if the current scenario is considered "special."
     *
     * <p>This method checks whether the backing scenario exists and if it qualifies as a special scenario
     * by invoking {@link AtBDynamicScenario#isSpecialScenario()}. A "special" scenario typically indicates unique
     * conditions or behavior within the current context.</p>
     *
     * @return {@code true} if there is a backing scenario, and it is marked as special; {@code false} otherwise.
     */
    public boolean isSpecial() {
        return backingScenario != null && backingScenario.isSpecialScenario();
    }

    public void setTurningPoint(boolean turningPoint) {
        this.turningPoint = turningPoint;
    }

    @XmlTransient
    public AtBDynamicScenario getBackingScenario() {
        return backingScenario;
    }

    /**
     * Retrieves the {@link AtBContract} associated with the backing scenario.
     *
     * <p>If the backing scenario is null, this method will return {@code null}. Otherwise, it
     * retrieves the associated contract through the provided campaign instance.
     *
     * @param campaign The {@code Campaign} instance used to obtain the contract.
     *
     * @return The {@code AtBContract} associated with the current backing scenario, or {@code null} if no backing
     *       scenario exists.
     */
    public @Nullable AtBContract getBackingContract(Campaign campaign) {
        if (backingScenario == null) {
            return null;
        }

        return backingScenario.getContract(campaign);
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getDeploymentDate() {
        return deploymentDate;
    }

    public void setDeploymentDate(LocalDate deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDate actionDate) {
        this.actionDate = actionDate;
        backingScenario.setDate(actionDate);
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public StratConCoords getCoords() {
        return coords;
    }

    public void setCoords(StratConCoords coords) {
        this.coords = coords;
    }

    public boolean isStrategicObjective() {
        return isStrategicObjective;
    }

    public void setStrategicObjective(boolean value) {
        isStrategicObjective = value;
    }

    public ScenarioTemplate getScenarioTemplate() {
        return backingScenario.getTemplate();
    }

    public int getBackingScenarioID() {
        return backingScenarioID;
    }

    public void setBackingScenario(AtBDynamicScenario backingScenario) {
        this.backingScenario = backingScenario;

        setBackingScenarioID(backingScenario.getId());
    }

    public void setBackingScenarioID(int backingScenarioID) {
        this.backingScenarioID = backingScenarioID;
    }

    public int getNumDefensivePoints() {
        return numDefensivePoints;
    }

    public void setNumDefensivePoints(int numDefensivePoints) {
        this.numDefensivePoints = numDefensivePoints;
    }

    public void useDefensivePoint() {
        numDefensivePoints--;
    }

    public Set<Integer> getFailedReinforcements() {
        return failedReinforcements;
    }

    public void setFailedReinforcements(Set<Integer> failedReinforcements) {
        this.failedReinforcements = failedReinforcements;
    }

    public void addFailedReinforcements(int forceID) {
        failedReinforcements.add(forceID);
    }

    public void removeFailedReinforcements(int forceID) {
        failedReinforcements.remove(forceID);
    }

    public boolean ignoreForceAutoAssignment() {
        return ignoreForceAutoAssignment;
    }

    public void setIgnoreForceAutoAssignment(boolean ignoreForceAutoAssignment) {
        this.ignoreForceAutoAssignment = ignoreForceAutoAssignment;
    }

    public int getLeadershipPointsUsed() {
        return leadershipPointsUsed;
    }

    public void setAvailableLeadershipBudget(int leadershipPointsUsed) {
        this.leadershipPointsUsed = leadershipPointsUsed;
    }

    /**
     * Retrieves the {@link StratConTrackState} that contains this {@link StratConScenario} within the given
     * {@link StratConCampaignState} or derives the campaign state if not provided.
     *
     * <p>
     * If a {@link StratConCampaignState} is not provided, the method attempts to derive it using information from the
     * backing scenario associated with this {@link StratConScenario}. It uses the campaign and contract details to
     * fetch the {@link StratConCampaignState}. Once the campaign state is obtained (or provided as input), it searches
     * for the track that contains this scenario.
     * </p>
     *
     * <p>
     * If no matching track is found, or if the input or derived data is incomplete (such as missing tracks or
     * scenarios), the method returns {@code null}.
     * </p>
     *
     * <strong>Usage:</strong>
     * <p>
     * Use this method to locate the {@link StratConTrackState} that contains this scenario, either by directly
     * providing a {@link StratConCampaignState} or allowing the method to derive one using the campaign and available
     * scenario details.
     * </p>
     *
     * @param campaign      The {@link Campaign} containing the data needed to derive the campaign state if none is
     *                      provided.
     * @param campaignState The {@link StratConCampaignState} to search for the track containing this scenario. Can be
     *                      {@code null}, in which case the method attempts to determine the campaign state.
     *
     * @return The {@link StratConTrackState} that contains this scenario, or {@code null} if no matching track is found
     *       or if enough data to derive the campaign state is unavailable.
     */
    public @Nullable StratConTrackState getTrackForScenario(Campaign campaign,
          @Nullable StratConCampaignState campaignState) {
        // If a campaign state hasn't been provided, we try to derive it from the available
        // scenario information.
        if (campaignState == null) {
            backingScenario = getBackingScenario();

            if (backingScenario == null) {
                return null;
            }

            AtBContract contract = backingScenario.getContract(campaign);

            campaignState = contract.getStratconCampaignState();

            if (campaignState == null) {
                return null;
            }
        }

        // If we have been provided a campaign state, or have derived one, we can start tracking
        // down the associated track.
        List<StratConTrackState> tracks = campaignState.getTracks();

        if (tracks == null) {
            return null;
        }

        for (StratConTrackState track : tracks) {
            Map<StratConCoords, StratConScenario> scenarios = track.getScenarios();

            if (scenarios == null) {
                return null;
            }

            if (scenarios.containsValue(this)) {
                return track;
            }
        }

        return null;
    }

    /**
     * Resets the state of the current scenario for the given campaign. This includes updating the scenario state,
     * clearing associated forces and units, and detaching them from the scenario. It also ensures that the scenario's
     * backing contract and campaign state remain consistent.
     *
     * @param campaign The {@link Campaign} object for which the scenario needs to be reset.
     *                 <p>
     *                 The method performs the following:
     *                 <ul>
     *                     <li>Resets the scenario's state to {@code UNRESOLVED}.</li>
     *                     <li>Clears any leadership budget and failed reinforcements associated with the scenario.</li>
     *                     <li>Resets the list of primary forces linked to the scenario.</li>
     *                     <li>If the scenario has a backing {@link AtBDynamicScenario}, it fetches the corresponding contract and
     *                         {@link StratConCampaignState} to handle associated track and force assignments:</li>
     *                         <li>-- Clears all forces and units assigned to the scenario, detaching them appropriately.</li>
     *                         <li>-- Undeploys all units and clears scenario IDs for the forces and units associated with the scenario.</li>
     *                         <li>-- Unassigns the force from the {@link StratConTrackState} and triggers a
     *                             {@link DeploymentChangedEvent} for updates.</li>
     *                 </ul>
     *
     *                 <strong>Note:</strong> If the backing scenario ID is invalid or the contract is null, the
     *                                method exits early and performs no further actions.
     */
    public void resetScenario(Campaign campaign) {
        setCurrentState(UNRESOLVED);
        setAvailableLeadershipBudget(0);
        setFailedReinforcements(new HashSet<>());
        setPrimaryForceIDs(new ArrayList<>());

        int backingScenarioId = getBackingScenarioID();
        Scenario backingScenario = campaign.getScenario(backingScenarioId);

        if (backingScenarioId != -1 && backingScenario instanceof AtBDynamicScenario) {
            AtBContract contract = ((AtBDynamicScenario) backingScenario).getContract(campaign);
            if (contract == null) {
                return;
            }
            StratConCampaignState campaignState = contract.getStratconCampaignState();

            StratConTrackState track = getTrackForScenario(campaign, campaignState);
            for (Formation formation : campaign.getAllFormations()) {
                if (formation.getScenarioId() == backingScenarioId) {
                    formation.clearScenarioIds(campaign, true);
                    backingScenario.removeFormation(formation.getId());

                    for (UUID uid : formation.getAllUnits(false)) {
                        Unit unit = campaign.getUnit(uid);
                        if (unit != null) {
                            backingScenario.removeUnit(unit.getId());
                            unit.undeploy();
                        }
                    }

                    track.unassignFormation(formation.getId());
                    MekHQ.triggerEvent(new DeploymentChangedEvent(formation, backingScenario));
                }
            }

            for (Unit unit : campaign.getUnits()) {
                if (unit.getScenarioId() == backingScenarioId) {
                    backingScenario.removeUnit(unit.getId());
                    unit.undeploy();
                    MekHQ.triggerEvent(new DeploymentChangedEvent(unit, backingScenario));
                }
            }
        }
    }
}
