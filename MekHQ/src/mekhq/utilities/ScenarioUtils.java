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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.utilities;

import static mekhq.MHQConstants.MAP_GEN_PATH;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.sentry.Sentry;
import megamek.common.board.Board;
import megamek.common.loaders.MapSettings;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.server.ServerBoardHelper;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;

/**
 * @author Luana Coppio
 */
public class ScenarioUtils {

    private static final MMLogger LOGGER = MMLogger.create(ScenarioUtils.class);

    private ScenarioUtils() {}

    /**
     * Creates a game board based on the settings in the provided Scenario. This method extracts map configuration from
     * the scenario and delegates to the board creation logic.
     *
     * @param scenario The Scenario containing board configuration parameters
     *
     * @return A Board object configured according to the scenario settings, or a default board if invalid parameters
     */
    public static Board getBoardFor(Scenario scenario) {
        // Check for valid dimensions and map
        MapSettings mapSettings = getMapSettings(scenario);
        return ServerBoardHelper.getPossibleGameBoard(mapSettings, false);
    }

    public static MapSettings getMapSettings(Scenario scenario) {
        if (scenario instanceof AtBScenario atBScenario) {
            return getStratconMapSettings(atBScenario);
        } else {
            return getNonStratconMapSettings(scenario);
        }
    }

    private static MapSettings getNonStratconMapSettings(Scenario scenario) {
        return getMapSettings(scenario.getMapSizeX(), scenario.getMapSizeY(), scenario.getMap(),
              scenario.isUsingFixedMap(), scenario.getBoardType() == Scenario.T_SPACE,
              scenario.getBoardType() == Scenario.T_ATMOSPHERE);
    }

    private static MapSettings getStratconMapSettings(AtBScenario scenario) {
        return getMapSettings(scenario.getMapX(),
              scenario.getMapY(),
              scenario.getMap(),
              scenario.isUsingFixedMap(),
              scenario.getBoardType() == Scenario.T_SPACE || "Space".equals(scenario.getTerrainType()),
              scenario.getBoardType() == Scenario.T_ATMOSPHERE);
    }

    public static MapSettings getMapSettings(int mapSizeX, int mapSizeY, String mapName, boolean isUsingFixedMap,
          boolean isSpace, boolean isAtmosphere) {
        MapSettings mapSettings = MapSettings.getInstance();

        if ((mapName == null) || (mapSizeX <= 1) || (mapSizeY <= 1)) {
            LOGGER.error("Invalid map settings provided for scenario {}", mapName);
            return mapSettings;
        }

        mapSettings.setBoardSize(mapSizeX, mapSizeY);
        mapSettings.setMapSize(1, 1);
        mapSettings.getBoardsSelectedVector().clear();

        if (isSpace) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        } else if (isUsingFixedMap) {
            String board = mapName.replace(".board", "").replace("\\", "/");
            mapSettings.getBoardsSelectedVector().add(board);

            if (isAtmosphere) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }
        } else {
            File mapgenFile = new MegaMekFile(new File(MAP_GEN_PATH), mapName + ".xml").getFile();
            try (InputStream is = new FileInputStream(mapgenFile)) {
                mapSettings = MapSettings.getInstance(is);
            } catch (IOException ex) {
                Sentry.captureException(ex);
                LOGGER.error(ex, "Could not load map file data/mapgen/{}.xml", mapName);
            }

            if (isAtmosphere) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }

            // Reset size parameters after getting new instance
            // Note that the side effect of "setMapSize" is to fill the boardsSelectedVector
            // with null entries!
            // We know we are only using a single map so replace the null 0th entry
            mapSettings.setBoardSize(mapSizeX, mapSizeY);
            mapSettings.setMapSize(1, 1);
            mapSettings.getBoardsSelectedVector().set(0, MapSettings.BOARD_GENERATED);
        }

        return mapSettings;
    }
}
