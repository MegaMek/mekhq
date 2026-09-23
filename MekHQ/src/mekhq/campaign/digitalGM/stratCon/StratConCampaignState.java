/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import org.w3c.dom.Node;

/**
 * Contract-level state object for a StratCon campaign.
 *
 * @author NickAragua
 */
@XmlRootElement(name = "StratConCampaignState")
public class StratConCampaignState {
    private static final MMLogger LOGGER = MMLogger.create(StratConCampaignState.class);

    public static final String ROOT_XML_ELEMENT_NAME = "StratConCampaignState";

    @XmlTransient
    private AbstractContract contract;

    // these are all state variables that affect the current Stratcon Campaign
    private int supportPoints;
    private int victoryPoints;
    // how far the contract's hostilities have escalated; see StratConEscalation
    private int escalation;
    // whether "Contracts Use Special Mechanics" was on when the contract was accepted; see
    // isContractsUseSpecialMechanics
    private boolean contractsUseSpecialMechanics;
    private String briefingText;
    @XmlElement(required = true, defaultValue = "false")
    private boolean allowEarlyVictory;

    // these are applied to any scenario generated in the campaign; use sparingly
    private List<String> globalScenarioModifiers = new ArrayList<>();

    @XmlElementWrapper(name = "campaignTracks")
    @XmlElement(name = "campaignTrack")
    private final List<StratConTrackState> tracks;

    private List<LocalDate> weeklyScenarios;
    private final List<LocalDate> strategicScenarioSpawnDates;
    private final List<StratConScheduledPointOfInterest> scheduledPointsOfInterest;

    @XmlTransient
    public AbstractContract getContract() {
        return contract;
    }

    public void setContract(AbstractContract contract) {
        this.contract = contract;
    }

    public StratConCampaignState() {
        tracks = new ArrayList<>();
        weeklyScenarios = new ArrayList<>();
        strategicScenarioSpawnDates = new ArrayList<>();
        scheduledPointsOfInterest = new ArrayList<>();
    }

    public StratConCampaignState(AbstractContract contract) {
        tracks = new ArrayList<>();
        weeklyScenarios = new ArrayList<>();
        strategicScenarioSpawnDates = new ArrayList<>();
        scheduledPointsOfInterest = new ArrayList<>();
        setContract(contract);
    }

    public StratConTrackState getTrack(int index) {
        return tracks.get(index);
    }

    public List<StratConTrackState> getTracks() {
        return tracks;
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public void addTrack(StratConTrackState track) {
        tracks.add(track);
    }

    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    @XmlElementWrapper(name = "weeklyScenarios")
    @XmlElement(name = "weeklyScenario")
    public List<LocalDate> getWeeklyScenarios() {
        return weeklyScenarios;
    }

    public void addWeeklyScenario(LocalDate weeklyScenario) {
        weeklyScenarios.add(weeklyScenario);
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setWeeklyScenarios(final List<LocalDate> weeklyScenarios) {
        this.weeklyScenarios = weeklyScenarios;
    }

    /** @return the still-to-come days on which strategic-objective scenarios appear (mutable; drained as they spawn) */
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    @XmlElementWrapper(name = "strategicScenarioSpawnDates")
    @XmlElement(name = "strategicScenarioSpawnDate")
    public List<LocalDate> getStrategicScenarioSpawnDates() {
        return strategicScenarioSpawnDates;
    }

    public void addStrategicScenarioSpawnDate(LocalDate spawnDate) {
        strategicScenarioSpawnDates.add(spawnDate);
    }

    /**
     * @return the points of interest still to appear over the contract's run (mutable; drained as they are placed)
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlElementWrapper(name = "scheduledPointsOfInterest")
    @XmlElement(name = "scheduledPointOfInterest")
    public List<StratConScheduledPointOfInterest> getScheduledPointsOfInterest() {
        return scheduledPointsOfInterest;
    }

    /**
     * @param scheduledPointOfInterest a point of interest to place on its scheduled day
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void addScheduledPointOfInterest(StratConScheduledPointOfInterest scheduledPointOfInterest) {
        scheduledPointsOfInterest.add(scheduledPointOfInterest);
    }

    /**
     * Moves every date still to come in the contract's pre-rolled schedule - its strategic-objective scenarios and its
     * points of interest - by the given number of days. Used when the contract's start date moves, so the schedule
     * keeps its place within the contract.
     *
     * @param days how many days to move them; negative moves them earlier
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void shiftScheduledDates(long days) {
        if (days == 0) {
            return;
        }

        strategicScenarioSpawnDates.replaceAll(spawnDate -> spawnDate.plusDays(days));
        for (StratConScheduledPointOfInterest scheduledPointOfInterest : scheduledPointsOfInterest) {
            LocalDate spawnDate = scheduledPointOfInterest.getSpawnDate();
            if (spawnDate != null) {
                scheduledPointOfInterest.setSpawnDate(spawnDate.plusDays(days));
            }
        }
    }

    public int getSupportPoints() {
        return supportPoints;
    }

    /**
     * Modifies the current support points by the specified amount.
     *
     * <p>
     * This method increases or decreases the support points by the given number. It adds the value of {@code change} to
     * the existing support points total. This can be used to reflect changes due to various gameplay events or
     * actions.
     * </p>
     *
     * @param change The amount to adjust the support points by. Positive values will increase the support points, while
     *               negative values will decrease them.
     */
    public void changeSupportPoints(int change) {
        supportPoints += change;
    }

    public void setSupportPoints(int supportPoints) {
        this.supportPoints = supportPoints;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.victoryPoints = victoryPoints;
    }

    public void changeVictoryPoints(int delta) {
        victoryPoints += delta;
    }

    /**
     * @return how far the contract's hostilities have escalated, from 0 up to the contract's maximum (see
     *       {@link StratConEscalation}); 0 for contracts that do not track Escalation
     */
    public int getEscalation() {
        return escalation;
    }

    /**
     * Sets the contract's Escalation directly. Play should raise it through {@link StratConEscalation}, which caps it
     * and keeps any Escalation objectives up to date.
     *
     * @param escalation the new Escalation
     */
    public void setEscalation(int escalation) {
        this.escalation = escalation;
    }

    /**
     * Whether the contract runs its type's special mechanics (see {@link StratConContractMechanics}). This is the
     * "Contracts Use Special Mechanics" option as it stood when the contract was accepted, and is always {@code false}
     * in mapless play, where those mechanics are not set up. Rules read this rather than the option itself: what a
     * contract was set up with (its Essential scenarios, its points of interest, its Escalation) cannot change with the
     * option mid-contract, so neither can the rules that act on them.
     *
     * @return {@code true} if the contract uses its type's special mechanics
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isContractsUseSpecialMechanics() {
        return contractsUseSpecialMechanics;
    }

    /**
     * Records whether the contract uses its type's special mechanics. Set once, when the contract is accepted (see
     * {@link #isContractsUseSpecialMechanics()}).
     *
     * @param contractsUseSpecialMechanics {@code true} if the contract uses its type's special mechanics
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setContractsUseSpecialMechanics(boolean contractsUseSpecialMechanics) {
        this.contractsUseSpecialMechanics = contractsUseSpecialMechanics;
    }

    public String getBriefingText() {
        return briefingText;
    }

    public void setBriefingText(String briefingText) {
        this.briefingText = briefingText;
    }

    public boolean allowEarlyVictory() {
        return allowEarlyVictory;
    }

    public void setAllowEarlyVictory(boolean allowEarlyVictory) {
        this.allowEarlyVictory = allowEarlyVictory;
    }

    public List<String> getGlobalScenarioModifiers() {
        return globalScenarioModifiers;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setGlobalScenarioModifiers(List<String> globalScenarioModifiers) {
        this.globalScenarioModifiers = globalScenarioModifiers;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void useSupportPoint() {
        supportPoints--;
    }

    /**
     * Decreases the number of support points by the specified decrement.
     *
     * @param decrement The number of support points to use/decrease.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public void useSupportPoints(int decrement) {
        supportPoints -= decrement;
    }

    /**
     * Convenience/speed method of determining whether a force with the given ID has been deployed to a track in this
     * campaign.
     *
     * @param forceID the force ID to check
     *
     * @return Deployed or not.
     */
    public boolean isForceDeployedHere(int forceID) {
        for (StratConTrackState trackState : tracks) {
            if (trackState.getAssignedForceCoords().containsKey(forceID)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the scenario with the given campaign scenario ID from any tracks where it's present
     */
    public void removeStratConScenario(int scenarioID) {
        for (StratConTrackState trackState : tracks) {
            trackState.removeScenario(scenarioID);
        }
    }

    /**
     * Retrieves the {@link StratConScenario} associated with a given {@link AtBScenario}.
     *
     * <p>
     * This method searches through all {@link StratConTrackState} objects in the {@link StratConCampaignState} to find
     * the first {@link StratConScenario} whose backing scenario matches the specified {@link AtBScenario}. If no such
     * scenario is found, it returns {@code null}.
     * </p>
     *
     * <strong>Usage:</strong>
     * <p>
     * Use this method to easily fetch the {@link StratConScenario} associated with the provided {@link AtBScenario}.
     * </p>
     *
     * @param campaign The {@link Campaign} containing the data to search through.
     * @param scenario The {@link AtBScenario} to find the corresponding {@link StratConScenario} for.
     *
     * @return The matching {@link StratConScenario}, or {@code null} if no corresponding scenario is found.
     */
    public static @Nullable StratConScenario getStratConScenarioFromAtBScenario(Campaign campaign,
          AtBScenario scenario) {
        AbstractContract contract = scenario.getContract(campaign);
        if (contract == null) {
            return null;
        }

        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null) {
            return null;
        }

        for (StratConTrackState track : campaignState.getTracks()) {
            for (StratConScenario stratConScenario : track.getScenarios().values()) {
                if (scenario.equals(stratConScenario.getBackingScenario())) {
                    return stratConScenario; // Return the first matching scenario if found
                }
            }
        }

        return null;
    }

    /**
     * Determines whether the contract can be ended early based on strategic objective completion.
     *
     * <p>A contract can be ended early only if:</p>
     * <ul>
     *   <li>The base contract allows early termination</li>
     *   <li>There is at least one strategic objective defined</li>
     *   <li>All strategic objectives across all tracks have been resolved (completed or failed)</li>
     *   <li>No strategic-objective point of interest is still waiting to appear</li>
     * </ul>
     *
     * <p>Strategic-objective scenarios still waiting to appear do not hold the contract open.</p>
     *
     * @return {@code true} if the contract can be ended early, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean canEndContractEarly() {
        if (!allowEarlyVictory()) {
            return false;
        }

        // Objectives still to come are not on the map yet, so they would otherwise not count against ending early.
        if (hasScheduledStrategicObjectivePointsOfInterest()) {
            return false;
        }

        boolean hasObjectives = false;
        for (StratConTrackState track : tracks) {
            List<StratConStrategicObjective> objectives = track.getStrategicObjectives();
            for (StratConStrategicObjective objective : objectives) {

                hasObjectives = true;
                if (!objective.isObjectiveResolved(track)) {
                    return false;
                }
            }
        }

        return hasObjectives;
    }

    /**
     * @return {@code true} if a point of interest that will be a strategic objective is still scheduled to appear
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean hasScheduledStrategicObjectivePointsOfInterest() {
        for (StratConScheduledPointOfInterest scheduledPointOfInterest : scheduledPointsOfInterest) {
            if (scheduledPointOfInterest.isStrategicObjective()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Serialize this instance of a campaign state to a PrintWriter Omits initial xml declaration
     *
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(StratConCampaignState.class);
            JAXBElement<StratConCampaignState> stateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME),
                  StratConCampaignState.class,
                  this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(stateElement, pw);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    /**
     * Attempt to deserialize an instance of a Campaign State from the passed-in XML Node
     *
     * @param xmlNode The node with the campaign state
     *
     * @return Possibly an instance of a StratConCampaignState
     */
    public static StratConCampaignState Deserialize(Node xmlNode) {
        StratConCampaignState resultingCampaignState = null;

        try {
            JAXBContext context = JAXBContext.newInstance(StratConCampaignState.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<StratConCampaignState> templateElement = um.unmarshal(xmlNode, StratConCampaignState.class);
            resultingCampaignState = templateElement.getValue();
        } catch (Exception e) {
            LOGGER.error("Error Deserializing Campaign State", e);
        }

        // Hack: LocalDate doesn't serialize/deserialize nicely within a map, so we
        // store it as an int-string map instead
        // while we're here, manually restore the coordinate-force lookup
        if (resultingCampaignState != null) {
            for (StratConTrackState track : resultingCampaignState.getTracks()) {
                track.restoreReturnDates();
                track.restoreAssignedCoordForces();
            }
        }

        return resultingCampaignState;
    }

    /**
     * This adapter provides a way to convert between a LocalDate and the ISO-8601 string representation of the date
     * that is used for XML marshaling and unmarshalling in JAXB.
     */
    public static class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
        @Override
        public String marshal(LocalDate date) {
            return date.toString();
        }

        @Override
        public LocalDate unmarshal(String date) throws Exception {
            return LocalDate.parse(date);
        }
    }
}
