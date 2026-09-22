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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * Places points of interest in StratCon sectors, obeying each type's placement rules. This is the way for contract
 * set-up and code outside StratCon to put a point of interest on the map; the GM's right-click tools place directly.
 *
 * <p>A hex is eligible for a point of interest of a given type (see {@link #canPlace}) when all of these hold:</p>
 * <ul>
 *     <li>it lies inside the sector;</li>
 *     <li>it is not ocean, if the type is land-only;</li>
 *     <li>its terrain is one the type allows, if the type limits its terrain;</li>
 *     <li>it holds no city, if the type avoids cities;</li>
 *     <li>it is not occupied (see {@link StratConTrackState#isHexOccupied}), if the type occupies its hex.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPointOfInterestPlacer {
    private static final MMLogger LOGGER = MMLogger.create(StratConPointOfInterestPlacer.class);

    private StratConPointOfInterestPlacer() {
    }

    /**
     * Places a point of interest of the given type.
     *
     * @param track  the sector to place it in
     * @param typeId the type ID of its definition
     * @param coords the hex to place it on, or {@code null} to choose an eligible hex at random
     * @param today  the current campaign date, from which its lifespan is counted
     *
     * @return the placed point of interest, or {@code null} if the type is unknown or no eligible hex could be found
     *       (or the chosen hex is not eligible)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterest place(StratConTrackState track, String typeId,
          @Nullable StratConCoords coords, LocalDate today) {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(typeId);
        if (definition == null) {
            LOGGER.warn("Cannot place a point of interest of unknown type {} on track {}.",
                  typeId,
                  track.getDisplayableName());
            return null;
        }

        StratConCoords destination = (coords == null) ? findPlacementCoords(track, definition) : coords;
        if ((destination == null) || !canPlace(track, definition, destination)) {
            LOGGER.info("No eligible hex for a point of interest of type {} on track {}.",
                  typeId,
                  track.getDisplayableName());
            return null;
        }

        StratConPointOfInterest pointOfInterest = StratConPointOfInterest.fromDefinition(definition,
              destination,
              today);
        return track.addPointOfInterest(pointOfInterest) ? pointOfInterest : null;
    }

    /**
     * Places a point of interest of the given type and makes it a strategic objective of its sector (see
     * {@link StrategicObjectiveType#PointOfInterest}).
     *
     * @param track  the sector to place it in
     * @param typeId the type ID of its definition
     * @param coords the hex to place it on, or {@code null} to choose an eligible hex at random
     * @param today  the current campaign date, from which its lifespan is counted
     *
     * @return the placed point of interest, or {@code null} if it could not be placed, in which case no objective is
     *       added either
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterest placeAsStrategicObjective(StratConTrackState track,
          String typeId, @Nullable StratConCoords coords, LocalDate today) {
        StratConPointOfInterest pointOfInterest = place(track, typeId, coords, today);
        if (pointOfInterest != null) {
            addStrategicObjective(track, pointOfInterest);
        }
        return pointOfInterest;
    }

    /**
     * Makes a point of interest already in a sector a strategic objective of that sector.
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest
     *
     * @return the new objective
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static StratConStrategicObjective addStrategicObjective(StratConTrackState track,
          StratConPointOfInterest pointOfInterest) {
        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveType(StrategicObjectiveType.PointOfInterest);
        objective.setPointOfInterestId(pointOfInterest.getId());
        objective.setDesiredObjectiveCount(1);
        track.addStrategicObjective(objective);
        return objective;
    }

    /**
     * Picks a random eligible hex for a point of interest of the given type. A type that occupies its hex also avoids
     * hexes where player forces are deployed, as facilities do.
     *
     * @param track      the sector to search
     * @param definition the point of interest's definition
     *
     * @return an eligible hex, or {@code null} if there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConCoords findPlacementCoords(StratConTrackState track,
          StratConPointOfInterestDefinition definition) {
        Collection<StratConCoords> forceCoords = track.getAssignedForceCoords().values();
        List<StratConCoords> candidates = new ArrayList<>();

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!canPlace(track, definition, coords)) {
                    continue;
                }

                if (definition.isOccupiesHex() && forceCoords.contains(coords)) {
                    continue;
                }

                candidates.add(coords);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(Compute.randomInt(candidates.size()));
    }

    /**
     * @param track      the sector
     * @param definition the point of interest's definition
     * @param coords     the hex to test
     *
     * @return {@code true} if a point of interest of that type may be placed on the hex (see the class description)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean canPlace(StratConTrackState track, StratConPointOfInterestDefinition definition,
          StratConCoords coords) {
        if (track.isOutOfBounds(coords)) {
            return false;
        }

        String terrain = track.getTerrainTile(coords);
        if (definition.isLandOnly() && StratConBiomeManifest.isOceanTerrain(terrain)) {
            return false;
        }

        if (!isAllowedTerrain(definition, terrain)) {
            return false;
        }

        if (definition.isAvoidCities() && track.isCity(coords)) {
            return false;
        }

        return !definition.isOccupiesHex() || !track.isHexOccupied(coords);
    }

    /**
     * A definition's allowed terrain entries may each name a terrain type (such as {@code Plains}) or a terrain
     * category (such as {@code VEGETATION}), matched ignoring case. No entries means any terrain.
     */
    private static boolean isAllowedTerrain(StratConPointOfInterestDefinition definition, String terrain) {
        List<String> allowedTerrain = definition.getAllowedTerrainCategories();
        if (allowedTerrain.isEmpty()) {
            return true;
        }

        String category = StratConBiomeManifest.getInstance().getTerrainCategory(terrain).name();
        for (String allowed : allowedTerrain) {
            if (allowed.equalsIgnoreCase(terrain) || allowed.equalsIgnoreCase(category)) {
                return true;
            }
        }

        return false;
    }
}
