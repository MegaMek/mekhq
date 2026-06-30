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
package mekhq.campaign.randomEvents.randomEventsSystem;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of an event response, including the effect type, the guard flag, the magnitude, and an optional
 * skill type.
 *
 * @param effect                 The type of effect this result describes
 * @param affectedPersonnelTypes Whether this result applies to a guard
 * @param magnitude              The intensity or magnitude of the effect
 * @param affectedSkill          An optional skill type associated with the effect
 */
public record RandomEventResult(
      @JsonProperty(value = "effect") RandomEventResultEffect effect,
      @JsonProperty(value = "affectedPersonnelTypes") List<RandomEventEffectedPersonnelType> affectedPersonnelTypes,
      @JsonProperty(value = "magnitude") int magnitude,
      @JsonProperty(value = "affectedSkill") String affectedSkill
) {
    // Additional logic to provide defaults for missing properties
    public RandomEventResult {
        effect = (effect != null) ? effect : RandomEventResultEffect.NONE;
        affectedPersonnelTypes = (affectedPersonnelTypes != null) ? affectedPersonnelTypes : new ArrayList<>();
        affectedSkill = (affectedSkill != null) ? affectedSkill : "";
    }
}
