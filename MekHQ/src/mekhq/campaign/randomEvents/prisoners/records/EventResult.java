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
 */
package mekhq.campaign.randomEvents.prisoners.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect;

import static mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect.NONE;

/**
 * Represents the result of an event response, including the effect type,
 * the guard flag, the magnitude, and an optional skill type.
 *
 * @param effect The type of effect this result describes
 * @param isGuard Whether this result applies to a guard
 * @param magnitude The intensity or magnitude of the effect
 * @param skillType An optional skill type associated with the effect
 */
public record EventResult(
    @JsonProperty(value = "effect") EventResultEffect effect,
    @JsonProperty(value = "isGuard") boolean isGuard,
    @JsonProperty(value = "magnitude") int magnitude,
    @JsonProperty(value = "skillType") String skillType
) {
    // Additional logic to provide defaults for missing properties
    public EventResult {
        effect = (effect != null) ? effect : NONE;
        skillType = (skillType != null) ? skillType : "";
    }
}
