/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.prisoners;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerEscapeeScenarioDialog;

import java.util.*;

import static java.io.File.separator;
import static megamek.common.Board.START_SW;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.personnel.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.HUGE;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.LARGE;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.MEDIUM;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.SMALL;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratcon.StratconRulesManager.generateExternalScenario;
import static mekhq.campaign.stratcon.StratconRulesManager.getAvailableForceIDs;
import static mekhq.campaign.stratcon.StratconRulesManager.sortForcesByMapType;

/**
 * Handles the generation and setup of a scenario involving escaped prisoners attempting to flee.
 *
 * <p>This class is responsible for creating scenarios where captured prisoners have been tracked
 * down and are attempting to regroup with allied forces. It dynamically builds the necessary mobs
 * (representing groups of escapees), generates the associated game scenario, assigns escapees to
 * these mobs, and sets up their behavior within the scenario.</p>
 *
 * <p>Once the scenario is prepared, the player is notified via a dialog that a special scenario
 * has been spawned.</p>
 */
public class PrisonEscapeScenario {
    private final Campaign campaign;
    private final AtBContract contract;
    private Set<Person> escapees;

    private final MMLogger logger = MMLogger.create(PrisonEscapeScenario.class);

    /**
     * Constructs the escape scenario for escaped prisoners.
     *
     * <p>This constructor initializes the creation of mobs based on the escaped prisoners, assigns
     * prisoners to these mobs, and generates the associated game scenario where their interception
     * will occur.</p>
     *
     * @param campaign  The current campaign instance, which provides contextual information and
     *                 game state.
     * @param contract  The AtB contract related to the campaign, used to manage scenario
     *                 generation and details.
     * @param escapees  A set of {@link Person} objects representing the escaped prisoners to be
     *                 included in the scenario.
     */
    public PrisonEscapeScenario(Campaign campaign, AtBContract contract, Set<Person> escapees) {
        this.campaign = campaign;
        this.contract = contract;
        this.escapees = escapees;

        List<Unit> allMobs = findMobsForEscapees();
        createEscapeeScenario(allMobs);
    }

    /**
     * Finds and creates game units (mobs) representing groups of escapees.
     *
     * <p>This method generates a list of mobs based on the size of the escapee group. Each mob is
     * created as a unit, and the prisoners are assigned roles within these units. If a prisoner
     * lacks the required skills to serve in the mob, the necessary skills are added automatically.</p>
     *
     * @return A list of {@link Unit} objects containing the generated mobs with assigned escapees.
     */
    private List<Unit> findMobsForEscapees() {
        List<Unit> mobs = new ArrayList<>();

        while (!escapees.isEmpty()) {
            int escapeeCount = escapees.size();

            Entity mobEntity = createMobEntity(escapeeCount);

            if (mobEntity == null) {
                logger.info("Failed to create mob");
                return mobs;
            }

            Unit mobUnit = new Unit(mobEntity, campaign);
            mobUnit.clearCrew();

            Set<Person> assignedEscapees = new HashSet<>();
            int maximumCrewSize = mobUnit.getFullCrewSize();
            for (Person escapee : escapees) {
                // If they don't have small arms, they're about to learn fast
                // We need to give them the skill, as it's required for the SOLDIER role, which is
                // required for serving in a CI unit, which mobs are.
                if (!escapee.hasSkill(S_SMALL_ARMS)) {
                    escapee.addSkill(S_SMALL_ARMS, 0, 0);
                }

                escapee.setPrimaryRole(campaign, SOLDIER);
                assignedEscapees.add(escapee);

                if (assignedEscapees.size() == maximumCrewSize) {
                    break;
                }
            }

            Crew crew = mobUnit.getEntity().getCrew();
            crew.setSize(assignedEscapees.size());

            for (Person escapee : assignedEscapees) {
                mobUnit.addPilotOrSoldier(escapee, null, false);
            }

            mobs.add(mobUnit);
            escapees.removeAll(assignedEscapees);
        }

        return mobs;
    }

    /**
     * Creates a mob entity based on the number of escapees.
     *
     * <p>The size of the mob determines what type of entity is created (e.g., small, medium,
     * large, or huge). The corresponding entity is created using predefined mob types.</p>
     *
     * @param escapeeCount The number of escapees to consider for the mob entity creation.
     * @return The created mob {@link Entity}, or {@code null} if the creation failed.
     */
    private Entity createMobEntity(int escapeeCount) {
        if (escapeeCount <= SMALL.getMaximum()) {
            return createMob(SMALL.getName());
        }

        if (escapeeCount <= MEDIUM.getMaximum()) {
            return createMob(MEDIUM.getName());
        }

        if (escapeeCount <= LARGE.getMaximum()) {
            return createMob(LARGE.getName());
        }

        return createMob(HUGE.getName());
    }

    /**
     * Creates and returns an {@link Entity} representing a mob with the specified name.
     *
     * <p>This method attempts to retrieve the map entry corresponding to the specified mob name
     * for creation. If the mob entry cannot be found or is invalid, an error is logged, and
     * {@code null} is returned.</p>
     *
     * @param mobName The name of the mob to be created.
     * @return The created mob {@link Entity}, or {@code null} if the creation failed.
     */
    public @Nullable Entity createMob(String mobName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(mobName);
        if (mekSummary == null) {
            logger.error("Cannot find entry for {}", mobName);
            return null;
        }

        MekFileParser mekFileParser;

        try {
            mekFileParser = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName());
        } catch (Exception ex) {
            logger.error("Unable to load unit: {}", mekSummary.getEntryName(), ex);
            return null;
        }

        return mekFileParser.getEntity();
    }

    /**
     * Creates and sets up the escapee interception scenario.
     *
     * <p>This method generates a scenario using predefined templates and inserts the generated mob
     * units as part of the escaping forces. It determines the track and coordinates for the
     * interception and assigns necessary behavior settings to the mob forces.</p>
     *
     * <p>If the scenario generation is successful, a dialog is triggered to inform the player
     * about the event.</p>
     *
     * @param mobUnits A list of {@link Unit} objects representing the escapee mobs to be included in the scenario.
     */
    private void createEscapeeScenario(List<Unit> mobUnits) {
        final String DIRECTORY = "data" + separator + "scenariotemplates" + separator;
        final String GENERIC = DIRECTORY + "Intercept the Escapees.xml";

        ScenarioTemplate template = ScenarioTemplate.Deserialize(GENERIC);

        // If we've failed to deserialize the requested template, report the error and make the delivery.
        if (template == null) {
            logger.info(String.format("Failed to deserialize %s", GENERIC));
            return;
        }

        // Pick a random track where the interception will take place. If we fail to get a track,
        // we log an error and make the delivery, in the same manner as above.
        StratconTrackState track;
        try {
            final StratconCampaignState campaignState = contract.getStratconCampaignState();
            List<StratconTrackState> tracks = campaignState.getTracks();
            track = ObjectUtility.getRandomItem(tracks);
        } catch (NullPointerException e) {
            logger.info(String.format("Failed to fetch a track: %s", e.getMessage()));
            return;
        }

        StratconCoords coords = getUnoccupiedCoords(track, false);

        if (coords == null) {
            logger.info("Failed to fetch a free set of coords");
            return;
        }

        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);
        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

        int randomForceIndex = randomInt(availableForceIDs.size());
        int randomForceID = availableForceIDs.get(randomForceIndex);

        // remove the force from the available lists, so we don't designate it as primary twice
        boolean autoAssignLances = contract.getCommandRights().isIntegrated();
        if (autoAssignLances) {
            availableForceIDs.removeIf(id -> id.equals(randomForceIndex));

            // we want to remove the actual int with the value, not the value at the index
            sortedAvailableForceIDs.get(AllGroundTerrain).removeIf(id -> id.equals(randomForceID));
        }

        StratconScenario scenario = generateExternalScenario(campaign, contract, track, coords,
            template, false, 0);

        if (scenario == null) {
            logger.info("Failed to generate a scenario");
            return;
        }

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, and announce the situation to the player
        AtBScenario backingScenario = scenario.getBackingScenario();

        for (BotForce botForce : backingScenario.getBotForces()) {
            if (botForce.getName().contains("Escapees")) {
                for (Unit mobUnit : mobUnits) {
                    botForce.addEntity(mobUnit.getEntity());
                }

                botForce.setRetreatEdge(START_SW);
                botForce.getBehaviorSettings().setAutoFlee(true);
            }
        }

        // Trigger a dialog to inform the user an interception has taken place
        new PrisonerEscapeeScenarioDialog(campaign, track, coords);
    }
}
