/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents;

import static java.io.File.separator;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.createEntityWithCrew;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.HUGE;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.LARGE;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.MEDIUM;
import static mekhq.campaign.randomEvents.prisoners.enums.MobType.SMALL;
import static mekhq.campaign.stratCon.StratConContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratCon.StratConRulesManager.generateExternalScenario;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConCoords;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Builds and schedules a StratCon "Crowd Control" scenario representing a civilian riot, then injects generated
 * civilian mobs into the scenario's AI-controlled force. On success, an in-character dialog is shown to the player
 * explaining the situation from the appropriate factional voice.
 *
 * <p>High-level flow:</p>
 * <ol>
 *   <li>Generate a list of civilian mob {@link Unit}s sized by random roll.</li>
 *   <li>Load the "Crowd Control" scenario template.</li>
 *   <li>Select a random StratCon track and a free coordinate on that track.</li>
 *   <li>Generate a playable scenario and attach the mobs to the "Civilians" {@link BotForce} if present.</li>
 *   <li>Present an immersive, faction-aware report to the player.</li>
 * </ol>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class RiotScenario {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RiotScenario";
    private final MMLogger LOGGER = MMLogger.create(RiotScenario.class);

    private final Campaign campaign;

    /**
     * Constructs a new {@code RiotScenario} and immediately attempts to generate a riot scenario for the supplied
     * contract.
     *
     * @param campaign the current campaign context; used for entity creation, localization, dialogs, and StratCon
     *                 integration
     * @param contract the active contract driving employer, faction, track selection, and scenario generation
     *
     * @author Illiani
     * @since 0.50.10
     */
    public RiotScenario(Campaign campaign, AtBContract contract) {
        this.campaign = campaign;

        List<Unit> allMobs = findMobsForRiots(contract.getEnemy());
        createRiotScenario(contract, allMobs);
    }

    /**
     * Creates a list of civilian mob {@link Unit}s to participate in the riot. The number of mobs and their composition
     * are determined by random rolls; each created {@link Entity} is wrapped in a {@link Unit} bound to this campaign.
     *
     * <p>If a mob entity cannot be created at any point, the method logs the issue and returns whatever has been
     * built so far.</p>
     *
     * @param faction the faction used when creating crewed entities (skill, tags, and other factional details)
     *
     * @return a possibly empty, never {@code null} list of mob units
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<Unit> findMobsForRiots(Faction faction) {
        List<Unit> mobs = new ArrayList<>();

        int mobCount = d6(2);
        for (int i = 1; i <= mobCount; i++) {
            Entity mobEntity = createMobEntity(faction);

            if (mobEntity == null) {
                LOGGER.info("Failed to create mob");
                return mobs;
            }

            mobs.add(new Unit(mobEntity, campaign));
        }

        return mobs;
    }

    /**
     * Chooses a mob size band by random roll and creates a corresponding civilian {@link Entity}.
     *
     * @param faction the faction context for the created entity
     *
     * @return the created mob entity, or {@code null} if creation failed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable Entity createMobEntity(Faction faction) {
        int size = d6(5);
        if (size <= SMALL.getMaximum()) {
            return createMob(faction, SMALL.getName());
        }

        if (size <= MEDIUM.getMaximum()) {
            return createMob(faction, MEDIUM.getName());
        }

        if (size <= LARGE.getMaximum()) {
            return createMob(faction, LARGE.getName());
        }

        return createMob(faction, HUGE.getName());
    }

    /**
     * Looks up a civilian mob chassis by name and creates a crewed {@link Entity} at ultra-green skill for use as a
     * riot participant.
     *
     * <p>Logs and returns {@code null} if the requested summary cannot be found.</p>
     *
     * @param faction the faction used to initialize crew and tags
     * @param mobName the MekSummary/variant name to spawn
     *
     * @return a crewed entity ready for scenario use, or {@code null} on lookup/creation failure
     *
     * @author Illiani
     * @since 0.50.10
     */
    public @Nullable Entity createMob(Faction faction, String mobName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(mobName);
        if (mekSummary == null) {
            LOGGER.error("Cannot find entry for {}", mobName);
            return null;
        }

        return createEntityWithCrew(faction, SkillLevel.ULTRA_GREEN, campaign, mekSummary, false);
    }

    /**
     * Assembles and schedules the riot scenario using the "Crowd Control" template, attaches the provided mobs to the
     * bot "Civilians" force if it exists, and notifies the player via an immersive, faction-aware dialog.
     *
     * <p>Failure points (template load, track/coords lookup, scenario generation) are logged and cause an early
     * return without throwing.</p>
     *
     * @param contract the contract providing StratCon campaign state and employer context
     * @param mobUnits the civilian mobs to inject into the scenario's "Civilians" {@link BotForce}; ignored if that
     *                 force is absent
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void createRiotScenario(AtBContract contract, List<Unit> mobUnits) {
        final String DIRECTORY = "data" + separator + "scenariotemplates" + separator;
        final String SCENARIO_FILE = DIRECTORY + "Crowd Control.xml";

        ScenarioTemplate template = ScenarioTemplate.Deserialize(SCENARIO_FILE);

        // If we've failed to deserialize the requested template, report the error and make the delivery.
        if (template == null) {
            LOGGER.info("Failed to deserialize {}", SCENARIO_FILE);
            return;
        }

        // Pick a random track where the interception will take place. If we fail to get a track, we log an error and
        // skip the scenario
        StratConTrackState track;
        try {
            final StratConCampaignState campaignState = contract.getStratconCampaignState();
            List<StratConTrackState> tracks = campaignState.getTracks();
            track = ObjectUtility.getRandomItem(tracks);
        } catch (NullPointerException e) {
            LOGGER.info("Failed to fetch a track: {}", e.getMessage());
            return;
        }

        StratConCoords coords = getUnoccupiedCoords(track);
        if (coords == null) {
            LOGGER.info("Failed to fetch a free set of coords");
            return;
        }

        StratConScenario scenario = generateExternalScenario(campaign,
              contract,
              track,
              coords,
              template,
              false,
              false,
              false,
              0);

        if (scenario == null) {
            LOGGER.info("Failed to generate a scenario");
            return;
        }

        // If we successfully generated a scenario, we need to make a couple of final adjustments and announce the
        // situation to the player
        AtBScenario backingScenario = scenario.getBackingScenario();
        for (BotForce botForce : backingScenario.getBotForces()) {
            if (botForce.getName().contains("Civilians")) {
                for (Unit mobUnit : mobUnits) {
                    botForce.addEntity(mobUnit.getEntity());
                }
            }
        }

        // Trigger a dialog to inform the user that an interception has taken place
        String commanderAddress = campaign.getCommanderAddress();
        String key;
        if (campaign.isClanCampaign()) {
            key = "RiotScenario.report.clan";
        } else if (campaign.getFaction().isComStarOrWoB()) {
            key = "RiotScenario.report.cs";
        } else if (campaign.isMercenaryCampaign()) {
            key = "RiotScenario.report.merc";
        } else {
            key = "RiotScenario.report.is";
        }
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              key,
              commanderAddress,
              track.getDisplayableName(),
              coords.toBTString());
        Person speaker = contract.getEmployerLiaison();

        new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              null,
              null,
              null,
              false);
    }
}
