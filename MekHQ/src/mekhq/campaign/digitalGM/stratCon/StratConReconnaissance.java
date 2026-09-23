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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;
import static java.lang.Math.min;

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;

/**
 * The Recon Raid special mechanic: in place of any points of interest, every sector has a strategic objective to scout
 * at least half of its land hexes - every hex that is not ocean - before the contract ends (see
 * {@link StrategicObjectiveType#Reconnaissance}). The contract keeps its Essential scenarios alongside.
 *
 * <p>Any hex the player can see counts as scouted, however it was revealed: by deploying or scouting, by a facility
 * that reveals the sector, or by a GM. The objective's counts are worked out from the sector's map whenever it is
 * checked, so no reveal can be missed; and they only ever rise, so an objective once met stays met even if the sector's
 * fog of war is later reset.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConReconnaissance {
    private StratConReconnaissance() {
    }

    /**
     * @param contract                       the contract
     * @param isContractsUseSpecialMechanics whether the "Contracts Use Special Mechanics" option is on
     *
     * @return {@code true} if the contract uses the reconnaissance mechanic: a Recon Raid, with the option on
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean usesReconnaissance(AbstractContract contract, boolean isContractsUseSpecialMechanics) {
        ContractObjectiveType objectiveType = contract.getObjectiveType();
        return isContractsUseSpecialMechanics && (objectiveType != null) && objectiveType.isReconRaid();
    }

    /**
     * Gives every sector of the contract its reconnaissance objective.
     *
     * @param campaignState the contract's StratCon state, with its sectors already set up
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void addReconnaissanceObjectives(StratConCampaignState campaignState) {
        for (StratConTrackState track : campaignState.getTracks()) {
            StratConStrategicObjective objective = new StratConStrategicObjective();
            objective.setObjectiveType(StrategicObjectiveType.Reconnaissance);
            objective.setDesiredObjectiveCount(getRequiredHexCount(track));
            track.addStrategicObjective(objective);
        }
    }

    /**
     * @param track a sector
     *
     * @return the sector's reconnaissance objective, or {@code null} if it has none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConStrategicObjective getObjective(StratConTrackState track) {
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if (objective.getObjectiveType() == StrategicObjectiveType.Reconnaissance) {
                return objective;
            }
        }
        return null;
    }

    /**
     * @param track a sector
     *
     * @return how many of the sector's hexes can be scouted for the objective: every hex that is not ocean
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getLandHexCount(StratConTrackState track) {
        int landHexes = 0;
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                if (!StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(new StratConCoords(x, y)))) {
                    landHexes++;
                }
            }
        }
        return landHexes;
    }

    /**
     * @param track a sector
     *
     * @return how many of the sector's land hexes the player can currently see: all of them while the whole sector is
     *       revealed (by a GM, or by a facility that reveals the sector), otherwise those revealed on the map
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getScoutedHexCount(StratConTrackState track) {
        if (track.isGmRevealed() || track.hasActiveTrackReveal()) {
            return getLandHexCount(track);
        }

        int scoutedHexes = 0;
        for (StratConCoords coords : track.getRevealedCoords()) {
            if (!track.isOutOfBounds(coords)
                      && !StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                scoutedHexes++;
            }
        }
        return scoutedHexes;
    }

    /**
     * @param track a sector
     *
     * @return how many land hexes must be scouted to meet the sector's objective: at least half of them
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getRequiredHexCount(StratConTrackState track) {
        return (getLandHexCount(track) + 1) / 2;
    }

    /**
     * Brings a reconnaissance objective up to date with its sector's map. Until the objective is met, its desired count
     * follows the sector's current size (a GM may resize a sector); its current count rises to the number of land hexes
     * scouted, capped at the desired count, and never falls.
     *
     * @param objective the reconnaissance objective
     * @param track     the sector it belongs to
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void updateObjective(StratConStrategicObjective objective, StratConTrackState track) {
        boolean isMet = (objective.getDesiredObjectiveCount() > 0)
                              && (objective.getCurrentObjectiveCount() >= objective.getDesiredObjectiveCount());
        if (isMet) {
            return;
        }

        int required = getRequiredHexCount(track);
        objective.setDesiredObjectiveCount(required);
        objective.setCurrentObjectiveCount(min(required,
              max(objective.getCurrentObjectiveCount(), getScoutedHexCount(track))));
    }
}
