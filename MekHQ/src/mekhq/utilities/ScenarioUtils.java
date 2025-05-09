/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package mekhq.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.sentry.Sentry;
import megamek.common.Board;
import megamek.common.MapSettings;
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

    public static Board getBoardFor(AtBScenario scenario) {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
        mapSettings.setMapSize(1, 1);
        mapSettings.getBoardsSelectedVector().clear();

        // if the scenario is taking place in space, do space settings instead
        if (scenario.getBoardType() == Scenario.T_SPACE || scenario.getTerrainType().equals("Space")) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        } else if (scenario.isUsingFixedMap()) {
            // TODO : remove inline file type
            String board = scenario.getMap().replace(".board", "");
            board = board.replace("\\", "/");
            mapSettings.getBoardsSelectedVector().add(board);

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }
        } else {
            File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml");
            try (InputStream is = new FileInputStream(mapgenFile)) {
                mapSettings = MapSettings.getInstance(is);
            } catch (IOException ex) {
                Sentry.captureException(ex);
                // TODO: Remove inline file path
                LOGGER.error(ex, "Could not load map file data/mapgen/{}.xml", scenario.getMap());
            }

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }

            // duplicate code, but getting a new instance of map settings resets the size
            // parameters
            mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
            mapSettings.setMapSize(1, 1);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        }
        return ServerBoardHelper.getPossibleGameBoard(mapSettings, false);
    }
}
