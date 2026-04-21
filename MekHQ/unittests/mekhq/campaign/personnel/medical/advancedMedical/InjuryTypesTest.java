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
package mekhq.campaign.personnel.medical.advancedMedical;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;

/**
 * Test class for {@link InjuryTypes}
 */
class InjuryTypesTest {

    @Test
    void testRegisterAllDoesNotCrash() {
        // Test that registerAll() completes without throwing an exception
        assertDoesNotThrow(() -> InjuryTypes.registerAll(),
              "InjuryTypes.registerAll() should not throw an exception");
    }

    @Test
    void testRegisterAllCanBeCalledMultipleTimes() {
        // Test that registerAll() can be called multiple times without issue
        // (it has internal protection against re-registration)
        assertDoesNotThrow(() -> {
            InjuryTypes.registerAll();
            InjuryTypes.registerAll();
            InjuryTypes.registerAll();
        }, "InjuryTypes.registerAll() should be safe to call multiple times");
    }

    /**
     * Regression test for <a href="https://github.com/MegaMek/mekhq/issues/7565">#7565</a>. Permanent injuries that can
     * only reset their recovery timer (or would be replaced by a different injury type) must not produce any stress
     * effects.
     */
    @ParameterizedTest(name = "{0} permanent injury produces no stress effects")
    @MethodSource(value = "permanentInjuryNoStressEffectData")
    void testGenStressEffect_permanentInjuryProducesNoEffects(String injuryName, InjuryType type,
          BodyLocation location, int severity) {
        // Setup
        Campaign campaign = mock(Campaign.class);
        Person person = mock(Person.class);
        Injury permanentInjury = new Injury(30, injuryName, location, type, severity,
              LocalDate.now(), true);

        // Act
        List<GameEffect> effects = type.genStressEffect(campaign, person, permanentInjury, 3);

        // Assert
        assertTrue(effects.isEmpty(),
              injuryName + " is permanent and should produce no stress effects, but got: " + effects);
    }

    static Stream<Arguments> permanentInjuryNoStressEffectData() {
        return Stream.of(
              // Severe concussion can only become cerebral contusion (non-permanent type) — blocked
              Arguments.of("Concussion (severe)", InjuryTypes.CONCUSSION, BodyLocation.HEAD, 2),
              // These only reset recovery timer — blocked for permanent
              Arguments.of("Broken limb", InjuryTypes.BROKEN_LIMB, BodyLocation.LEFT_ARM, 1),
              Arguments.of("Broken collar bone", InjuryTypes.BROKEN_COLLAR_BONE, BodyLocation.CHEST, 1),
              Arguments.of("Punctured lung", InjuryTypes.PUNCTURED_LUNG, BodyLocation.CHEST, 1)
        );
    }

    /**
     * Regression test for <a href="https://github.com/MegaMek/mekhq/issues/7565">#7565</a>. Permanent injuries that can
     * worsen within the same type (severity increase) should still produce worsening effects, but must not reset the
     * recovery timer.
     */
    @ParameterizedTest(name = "{0} permanent injury can still worsen but skips timer reset")
    @MethodSource(value = "permanentInjuryCanWorsenData")
    void testGenStressEffect_permanentInjuryCanWorsenButSkipsTimerReset(String injuryName, InjuryType type,
          BodyLocation location, int severity) {
        // Setup
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        Person person = mock(Person.class);
        Injury permanentInjury = new Injury(30, injuryName, location, type, severity,
              LocalDate.of(3025, 1, 1), true);

        // Act
        List<GameEffect> effects = type.genStressEffect(campaign, person, permanentInjury, 3);

        // Assert
        assertFalse(effects.isEmpty(),
              injuryName + " is permanent but should still produce worsening effects");
        assertEquals(1, effects.size(),
              injuryName + " should produce exactly 1 effect (worsening only, no timer reset)");
        assertFalse(effects.getFirst().desc().contains("recovery timer"),
              injuryName + " permanent injury should not have a recovery timer reset effect");
    }

    static Stream<Arguments> permanentInjuryCanWorsenData() {
        return Stream.of(
              // Concussion sev 1 can worsen to sev 2 (same type)
              Arguments.of("Concussion", InjuryTypes.CONCUSSION, BodyLocation.HEAD, 1),
              // Cerebral contusion can worsen to CTE (inherently permanent type)
              Arguments.of("Cerebral contusion", InjuryTypes.CEREBRAL_CONTUSION, BodyLocation.HEAD, 1),
              // Internal bleeding can worsen in severity (same type)
              Arguments.of("Internal bleeding", InjuryTypes.INTERNAL_BLEEDING, BodyLocation.ABDOMEN, 1),
              Arguments.of("Internal bleeding (severe)", InjuryTypes.INTERNAL_BLEEDING, BodyLocation.ABDOMEN, 2)
        );
    }
}
