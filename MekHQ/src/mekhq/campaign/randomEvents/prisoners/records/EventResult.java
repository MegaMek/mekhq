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

