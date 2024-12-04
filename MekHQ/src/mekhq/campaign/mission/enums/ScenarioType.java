/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.enums;

import megamek.logging.MMLogger;

public enum ScenarioType {
    NONE,
    SPECIAL_LOSTECH,
    SPECIAL_RESUPPLY;

    /**
     * @return {@code true} if the scenario is considered a LosTech scenario, {@code false} otherwise.
     */
    public boolean isLosTech() {
        return this == SPECIAL_LOSTECH;
    }

    /**
     * @return {@code true} if the scenario is considered a Resupply scenario, {@code false} otherwise.
     */
    public boolean isResupply() {
        return this == SPECIAL_RESUPPLY;
    }

    public static ScenarioType fromOrdinal(int ordinal) {
        for (ScenarioType scenarioType : values()) {
            if (scenarioType.ordinal() == ordinal) {
                return scenarioType;
            }
        }

        final MMLogger logger = MMLogger.create(ScenarioType.class);
        logger.error(String.format("Unknown Scenario Type ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }
}
