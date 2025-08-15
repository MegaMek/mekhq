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
package mekhq.campaign.randomEvents.prisoners.enums;

/**
 * Represents the quality of a response in prisoner-related random events.
 *
 * <p>This enumeration defines three levels of response quality, which influence how various
 * prisoner events and interactions are processed in the campaign. It is used to categorize reactions or results in
 * situations involving prisoners.</p>
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
     * outcomes during prisoner-related events, such as failed negotiations or missed rescues.</p>
     */
    RESPONSE_NEGATIVE
}
