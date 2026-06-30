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

import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NO_ATTRIBUTE;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * Represents an individual response entry for a random event. Each response entry defines a response quality and its
 * associated effects.
 *
 * @param quality            the quality of the response, as defined by the {@link RandomEventResponseQuality} enum
 * @param followOnEvent      the internal {@code randomEventType} reference to the follow-on event
 * @param followOnEventDelay how many days to wait for the follow-on event
 * @param skillCheckSkill    the skill used in skill checks related to this response
 * @param abilityCheckType   the ability used in ability checks related to this response
 * @param effectsSuccess     a list of effects resulting from successful resolution of the event, as defined by the
 *                           {@link RandomEventResult} record
 * @param effectsFailure     a list of effects resulting from failing to resolve the event, as defined by the
 *                           {@link RandomEventResult} record
 */
public record RandomEventResponseEntry(
      RandomEventResponseQuality quality,
      String followOnEvent,
      int followOnEventDelay,
      String skillCheckSkill,
      SkillAttribute abilityCheckType,
      List<RandomEventResult> effectsSuccess,
      List<RandomEventResult> effectsFailure
) {
    private static final MMLogger LOGGER = MMLogger.create(RandomEventResponseEntry.class);

    // Additional logic to provide defaults for missing properties
    public RandomEventResponseEntry {
        quality = (quality != null) ? quality : RandomEventResponseQuality.RESPONSE_NEUTRAL;
        followOnEvent = (followOnEvent != null) ? followOnEvent : "";
        skillCheckSkill = (skillCheckSkill != null) ? skillCheckSkill : "";
        abilityCheckType = (abilityCheckType != null) ? abilityCheckType : NO_ATTRIBUTE;
        effectsSuccess = (effectsSuccess != null) ? effectsSuccess : new ArrayList<>();
        effectsFailure = (effectsFailure != null) ? effectsFailure : new ArrayList<>();

        // Validate event delays
        if (StringUtility.isNullOrBlank(followOnEvent) && followOnEventDelay > 0) {
            LOGGER.warn("Follow-on event delay specified without a follow-on event");
        }

        if (followOnEventDelay < 0) {
            LOGGER.warn("Follow-on event delay specified as negative");
        }
    }
}
