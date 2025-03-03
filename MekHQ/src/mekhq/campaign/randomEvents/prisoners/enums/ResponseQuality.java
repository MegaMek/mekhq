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
package mekhq.campaign.randomEvents.prisoners.enums;

/**
 * Represents the quality of a response in prisoner-related random events.
 *
 * <p>This enumeration defines three levels of response quality, which influence how various
 * prisoner events and interactions are processed in the campaign. It is used to categorize
 * reactions or results in situations involving prisoners.</p>
 */
public enum ResponseQuality {
    /**
     * Represents a neutral response quality.
     *
     * <p>Indicates that the response neither has a positive nor a negative influence, but
     * reflects a balanced or indifferent outcome from the associated prisoner interaction.</p>
     */
    RESPONSE_NEUTRAL,

    /**
     * Represents a positive response quality.
     *
     * <p>Indicates a favorable interaction or result, often leading to improved outcomes
     * during prisoner-related events, such as successful negotiations or rescues.</p>
     */
    RESPONSE_POSITIVE,

    /**
     * Represents a negative response quality.
     *
     * <p>Indicates an unfavorable interaction or result, often leading to deteriorated
     * outcomes during prisoner-related events, such as failed negotiations or missed
     * rescues.</p>
     */
    RESPONSE_NEGATIVE
}
