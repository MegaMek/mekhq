/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.other;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory.createEntityWithCrew;
import static mekhq.campaign.randomEvents.prisoners.prisonerEvents.MobType.HUGE;
import static mekhq.campaign.randomEvents.prisoners.prisonerEvents.MobType.LARGE;
import static mekhq.campaign.randomEvents.prisoners.prisonerEvents.MobType.MEDIUM;
import static mekhq.campaign.randomEvents.prisoners.prisonerEvents.MobType.SMALL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.BotForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * The pieces of a riot: generating the civilian mobs that make up a StratCon "Crowd Control" scenario's opposition, and
 * announcing the riot to the player in an immersive, faction-aware dialog.
 *
 * <p>Riots break out when a responding formation's Civil Disobedience or Scheduled Parade point of interest turns
 * violent (see {@code StratConCivilDisobedienceBehavior} and {@code StratConScheduledParadeBehavior}); this replaced
 * the old weekly riot event on Riot Duty contracts.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class RiotScenario {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RiotScenario";
    private static final MMLogger LOGGER = MMLogger.create(RiotScenario.class);

    private RiotScenario() {
    }

    /**
     * Generates a riot's civilian mobs and adds them to the scenario's "Civilians" {@link BotForce}, as a riot does.
     * Call it once the scenario has been finalized, since finalizing is what creates its bot forces.
     *
     * @param campaign        the current campaign
     * @param faction         the faction the mobs are created for
     * @param backingScenario the finalized "Crowd Control" scenario
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void addRiotingMobs(Campaign campaign, Faction faction, AtBScenario backingScenario) {
        addMobsToCivilians(backingScenario, findMobsForRiots(campaign, faction));
    }

    /**
     * Adds the given mobs to every "Civilians" {@link BotForce} of the scenario.
     */
    private static void addMobsToCivilians(AtBScenario backingScenario, List<Unit> mobUnits) {
        for (BotForce botForce : backingScenario.getBotForces()) {
            if (botForce.getName().contains("Civilians")) {
                for (Unit mobUnit : mobUnits) {
                    botForce.addEntity(mobUnit.getEntity());
                }
            }
        }
    }

    /**
     * Creates a list of civilian mob {@link Unit}s to participate in the riot. The number of mobs and their composition
     * are determined by random rolls; each created {@link Entity} is wrapped in a {@link Unit} bound to this campaign.
     *
     * <p>If a mob entity cannot be created at any point, the method logs the issue and returns whatever has been
     * built so far.</p>
     *
     * @param campaign the campaign the mob units belong to
     * @param faction  the faction used when creating crewed entities (skill, tags, and other factional details)
     *
     * @return a possibly empty, never {@code null} list of mob units
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<Unit> findMobsForRiots(Campaign campaign, Faction faction) {
        List<Unit> mobs = new ArrayList<>();

        int mobCount = d6(2);
        for (int i = 1; i <= mobCount; i++) {
            Entity mobEntity = createMobEntity(campaign, faction);

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
     * @param campaign the campaign the mob belongs to
     * @param faction  the faction context for the created entity
     *
     * @return the created mob entity, or {@code null} if creation failed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static @Nullable Entity createMobEntity(Campaign campaign, Faction faction) {
        int size = d6(5);
        if (size <= SMALL.getMaximum()) {
            return createMob(campaign, faction, SMALL.getName());
        }

        if (size <= MEDIUM.getMaximum()) {
            return createMob(campaign, faction, MEDIUM.getName());
        }

        if (size <= LARGE.getMaximum()) {
            return createMob(campaign, faction, LARGE.getName());
        }

        return createMob(campaign, faction, HUGE.getName());
    }

    /**
     * Looks up a civilian mob chassis by name and creates a crewed {@link Entity} at ultra-green skill for use as a
     * riot participant.
     *
     * <p>Logs and returns {@code null} if the requested summary cannot be found.</p>
     *
     * @param campaign the campaign the mob belongs to
     * @param faction  the faction used to initialize crew and tags
     * @param mobName  the MekSummary/variant name to spawn
     *
     * @return a crewed entity ready for scenario use, or {@code null} on lookup/creation failure
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static @Nullable Entity createMob(Campaign campaign, Faction faction, String mobName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(mobName);
        if (mekSummary == null) {
            LOGGER.error("Cannot find entry for {}", mobName);
            return null;
        }

        return createEntityWithCrew(faction, SkillLevel.ULTRA_GREEN, campaign, mekSummary, false);
    }

    /**
     * Tells the player, in an immersive, faction-aware dialog from the employer's liaison, that a riot has broken out
     * at the given hex.
     *
     * @param campaign the current campaign
     * @param contract the contract the riot belongs to
     * @param track    the sector the riot is in
     * @param coords   the hex the riot is at
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void reportRiot(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords) {
        // Trigger a dialog to inform the user that a riot has broken out
        String commanderAddress = campaign.getCommanderAddress();
        String key;
        if (campaign.getPlayerForce().isClanForce()) {
            key = "RiotScenario.report.clan";
        } else {
            if (campaign.getPlayerForce().getFaction().isComStarOrWoB()) {
                key = "RiotScenario.report.cs";
            } else if (campaign.isMercenaryCampaign()) {
                key = "RiotScenario.report.merc";
            } else {
                key = "RiotScenario.report.is";
            }
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
