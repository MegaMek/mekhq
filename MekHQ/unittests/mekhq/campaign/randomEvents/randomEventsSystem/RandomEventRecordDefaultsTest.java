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
package mekhq.campaign.randomEvents.randomEventsSystem;

import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NO_ATTRIBUTE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseQuality.RESPONSE_NEGATIVE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseQuality.RESPONSE_NEUTRAL;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseQuality.RESPONSE_POSITIVE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResultEffect.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the compact-constructor default contracts of the GREG records and the response-quality target modifiers.
 *
 * <p>The YAML deserializer relies on these defaults to fill in optional keys that are absent from an event definition.
 * If a default changes, events that omit those keys would silently behave differently after loading.</p>
 */
class RandomEventRecordDefaultsTest {

    @Test
    void resultNullEffectDefaultsToNone() {
        RandomEventResult result = new RandomEventResult(null, null, 0, null);

        assertSame(NONE, result.effect(), "A null effect must default to NONE.");
    }

    @Test
    void resultNullAffectedPersonnelTypesDefaultsToEmptyList() {
        RandomEventResult result = new RandomEventResult(NONE, null, 0, null);

        assertTrue(result.affectedPersonnelTypes().isEmpty(),
              "A null affectedPersonnelTypes list must default to empty, never null.");
    }

    @Test
    void resultNullAffectedSkillDefaultsToEmptyString() {
        RandomEventResult result = new RandomEventResult(NONE, null, 0, null);

        assertEquals("", result.affectedSkill(), "A null affectedSkill must default to an empty string.");
    }

    @Test
    void responseEntryNullQualityDefaultsToNeutral() {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(null, "", "", null, null, null);

        assertSame(RESPONSE_NEUTRAL, entry.quality(), "A null response quality must default to RESPONSE_NEUTRAL.");
    }

    @Test
    void responseEntryNullAbilityCheckDefaultsToNoAttribute() {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(RESPONSE_NEUTRAL, "", "", null, null, null);

        assertSame(NO_ATTRIBUTE, entry.abilityCheckType(), "A null abilityCheckType must default to NO_ATTRIBUTE.");
    }

    @Test
    void responseEntryNullEffectListsDefaultToEmpty() {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(RESPONSE_NEUTRAL,
              "",
              "",
              NO_ATTRIBUTE,
              null,
              null);

        assertTrue(entry.effectsSuccess().isEmpty(), "A null success-effects list must default to empty.");
        assertTrue(entry.effectsFailure().isEmpty(), "A null failure-effects list must default to empty.");
    }

    @Test
    void responseEntryNullFollowOnEventDefaultToEmpty() {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(RESPONSE_NEUTRAL, null, "", NO_ATTRIBUTE, null,
              null);

        assertTrue(entry.followOnEvent().isEmpty(), "A null follow on entry must default to empty.");
    }

    @Test
    void responseQualityTargetModifiersMatchSpecification() {
        assertEquals(0, RESPONSE_NEUTRAL.getTargetNumberModifier(), "Neutral response modifier.");
        assertEquals(-3, RESPONSE_POSITIVE.getTargetNumberModifier(), "Positive response makes the check easier.");
        assertEquals(3, RESPONSE_NEGATIVE.getTargetNumberModifier(), "Negative response makes the check harder.");
    }
}
