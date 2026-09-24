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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.JumpPathItinerary.CircuitPlan;
import mekhq.campaign.JumpPathItinerary.Plan;
import mekhq.campaign.enums.LithiumFusionBatteryMode;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JumpDriveProfile} and how {@link JumpPath} and {@link JumpPathItinerary} apply it.
 *
 * @author Illiani
 * @since 0.51.01
 */
class JumpDriveProfileTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3067, 1, 1);
    private static final double TRANSIT_DAYS = 2.0;

    private static PlanetarySystem system(double rechargeHours) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getTimeToJumpPoint(anyDouble())).thenReturn(TRANSIT_DAYS);
        when(system.getRechargeTime(any(LocalDate.class), anyBoolean())).thenReturn(rechargeHours);
        return system;
    }

    /** Origin, three intermediate stops recharging 48, 96 and 144 hours, then the destination. */
    private static JumpPath fourJumpPath() {
        JumpPath path = new JumpPath();
        path.addSystem(system(1000.0));
        path.addSystem(system(48.0));
        path.addSystem(system(96.0));
        path.addSystem(system(144.0));
        path.addSystem(system(1000.0));
        return path;
    }

    @Nested
    class FromMode {
        @Test
        void disabled_isStandard() {
            assertSame(JumpDriveProfile.STANDARD, JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DISABLED, true));
        }

        @Test
        void halvedRecharge_halvesAndStoresNoCharges() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, true);
            assertEquals(0.5, profile.rechargeMultiplier());
            assertEquals(0, profile.storedJumpCharges());
        }

        @Test
        void doubleJump_storesChargeOnlyWhenBatteryCharged() {
            assertEquals(1, JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, true).storedJumpCharges());
            assertEquals(0, JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, false).storedJumpCharges());
            assertEquals(1.0, JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, true).rechargeMultiplier());
        }
    }

    @Nested
    class AdjustRechargeTime {
        @Test
        void standard_leavesTimeUnchanged() {
            assertEquals(176.0, JumpDriveProfile.STANDARD.adjustRechargeTime(176.0));
        }

        @Test
        void halved_halvesTime() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            assertEquals(88.0, profile.adjustRechargeTime(176.0));
        }

        @Test
        void infiniteRecharge_staysInfinite() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            assertTrue(Double.isInfinite(profile.adjustRechargeTime(Double.POSITIVE_INFINITY)));
        }
    }

    @Nested
    class JumpPathEstimates {
        @Test
        void standardProfile_matchesLegacyCalculation() {
            JumpPath path = fourJumpPath();
            assertEquals(path.getTotalRechargeTime(TEST_DATE, false),
                  path.getTotalRechargeTime(TEST_DATE, false, JumpDriveProfile.STANDARD));
            assertEquals(288.0 / 24.0, path.getTotalRechargeTime(TEST_DATE, false, JumpDriveProfile.STANDARD));
        }

        @Test
        void nullProfile_isTreatedAsStandard() {
            JumpPath path = fourJumpPath();
            assertEquals(288.0 / 24.0, path.getTotalRechargeTime(TEST_DATE, false, null));
        }

        @Test
        void halvedProfile_halvesEveryIntermediateRecharge() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            assertEquals(144.0 / 24.0, fourJumpPath().getTotalRechargeTime(TEST_DATE, false, profile));
        }

        @Test
        void storedCharge_skipsFirstIntermediateRecharge() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, true);
            // The 48-hour first stop is skipped: 96 + 144
            assertEquals(240.0 / 24.0, fourJumpPath().getTotalRechargeTime(TEST_DATE, false, profile));
        }

        @Test
        void totalTime_includesTransitAndAdjustedRecharge() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            double expected = (144.0 / 24.0) + TRANSIT_DAYS + TRANSIT_DAYS;
            assertEquals(expected, fourJumpPath().getTotalTime(TEST_DATE, 0.0, false, profile), 1e-9);
        }

        @Test
        void singleJump_hasNoRechargeToSkip() {
            JumpPath path = new JumpPath();
            path.addSystem(system(176.0));
            path.addSystem(system(176.0));
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, true);
            assertEquals(0.0, path.getTotalRechargeTime(TEST_DATE, false, profile));
        }
    }

    @Nested
    class ItineraryEstimates {
        @Test
        void standardProfile_matchesLegacyPlan() {
            JumpPath path = fourJumpPath();
            assertEquals(JumpPathItinerary.calculate(path, TEST_DATE, 1.0, null, 0.0, CircuitPlan.none()),
                  JumpPathItinerary.calculate(path, TEST_DATE, 1.0, null, 0.0, CircuitPlan.none(),
                        JumpDriveProfile.STANDARD));
        }

        @Test
        void halvedProfile_halvesPlanRecharge() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            Plan plan = JumpPathItinerary.calculate(fourJumpPath(), TEST_DATE, 1.0, null, 0.0, CircuitPlan.none(),
                  profile);

            assertEquals(144.0 / 24.0, plan.rechargeDays(), 1e-9);
            assertEquals(24, plan.entries().get(1).rechargeHours());
            assertEquals(48, plan.entries().get(2).rechargeHours());
            assertEquals(72, plan.entries().get(3).rechargeHours());
        }

        @Test
        void storedCharge_skipsFirstStopInPlan() {
            JumpDriveProfile profile = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.DOUBLE_JUMP, true);
            Plan plan = JumpPathItinerary.calculate(fourJumpPath(), TEST_DATE, 1.0, null, 0.0, CircuitPlan.none(),
                  profile);

            assertEquals(0, plan.entries().get(1).rechargeHours());
            assertEquals(96, plan.entries().get(2).rechargeHours());
            assertEquals(240.0 / 24.0, plan.rechargeDays(), 1e-9);
        }

        @Test
        void requiredAcceleration_accountsForShorterRecharge() {
            JumpPath path = fourJumpPath();
            JumpDriveProfile halved = JumpDriveProfile.fromMode(LithiumFusionBatteryMode.HALVED_RECHARGE, false);
            double desiredDays = 20.0;

            double standardAcceleration = JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, desiredDays,
                  null, 0.0, CircuitPlan.none(), JumpDriveProfile.STANDARD).accelerationG().orElseThrow();
            double halvedAcceleration = JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, desiredDays,
                  null, 0.0, CircuitPlan.none(), halved).accelerationG().orElseThrow();

            // Less time recharging leaves more of the schedule for transit, so less acceleration is needed
            assertTrue(halvedAcceleration < standardAcceleration);
        }
    }
}
