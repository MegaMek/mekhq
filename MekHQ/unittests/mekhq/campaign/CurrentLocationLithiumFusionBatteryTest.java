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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.enums.LithiumFusionBatteryMode;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.w3c.dom.Node;

/**
 * Tests for the Lithium-Fusion battery handling in {@link CurrentLocation}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CurrentLocationLithiumFusionBatteryTest {
    private static final LocalDate TODAY = LocalDate.of(3067, 1, 1);
    private static final double RECHARGE_HOURS = 176.0;
    private static final double TIME_TO_JUMP_POINT = 2.5;

    private PlanetarySystem originSystem;
    private Campaign campaign;
    private CurrentLocation currentLocation;

    @BeforeEach
    void setUp() {
        originSystem = mockSystem(RECHARGE_HOURS);

        campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.isUseCommandCircuit()).thenReturn(false);
        when(campaign.getFutureContracts()).thenReturn(Collections.emptyList());

        currentLocation = new CurrentLocation(originSystem, 0.0);
    }

    private static PlanetarySystem mockSystem(double rechargeHours) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getRechargeTime(TODAY, false)).thenReturn(rechargeHours);
        when(system.getTimeToJumpPoint(1.0)).thenReturn(TIME_TO_JUMP_POINT);
        return system;
    }

    private void useBatteryMode(LithiumFusionBatteryMode mode) {
        when(campaign.getEffectiveLithiumFusionBatteryMode(any(AbstractLocation.class))).thenReturn(mode);
    }

    private JumpPath jumpPathThrough(PlanetarySystem... systems) {
        return new JumpPath(new ArrayList<>(List.of(systems)));
    }

    @Nested
    class BatteryCharge {
        @Test
        void newLocation_batteryIsEmpty() {
            assertEquals(0.0, currentLocation.getLithiumFusionBatteryCharge());
            assertFalse(currentLocation.isLithiumFusionBatteryCharged());
        }

        @Test
        void setLithiumFusionBatteryCharge_clampsToValidRange() {
            currentLocation.setLithiumFusionBatteryCharge(1.5);
            assertEquals(1.0, currentLocation.getLithiumFusionBatteryCharge());
            assertTrue(currentLocation.isLithiumFusionBatteryCharged());

            currentLocation.setLithiumFusionBatteryCharge(-0.5);
            assertEquals(0.0, currentLocation.getLithiumFusionBatteryCharge());
        }

        @Test
        void chargeFully_alsoChargesBattery() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.chargeFully(campaign);
            }

            assertEquals(RECHARGE_HOURS, currentLocation.getRechargeTime());
            assertTrue(currentLocation.isLithiumFusionBatteryCharged());
        }

        @Test
        void unknownMode_isTreatedAsDisabled() {
            // An unstubbed mock campaign returns null for the mode
            assertEquals(RECHARGE_HOURS, currentLocation.getNeededRechargeTime(campaign));
        }
    }

    @Nested
    class HalvedRecharge {
        @Test
        void neededRechargeTime_isHalved() {
            useBatteryMode(LithiumFusionBatteryMode.HALVED_RECHARGE);
            assertEquals(RECHARGE_HOURS / 2, currentLocation.getNeededRechargeTime(campaign));
        }

        @Test
        void newDay_rechargeStopsAtHalvedTime() {
            useBatteryMode(LithiumFusionBatteryMode.HALVED_RECHARGE);
            for (int day = 0; day < 5; day++) {
                currentLocation.newDay(campaign, true);
            }

            assertEquals(RECHARGE_HOURS / 2, currentLocation.getRechargeTime());
        }

        @Test
        void chargeFully_usesHalvedTime() {
            useBatteryMode(LithiumFusionBatteryMode.HALVED_RECHARGE);
            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.chargeFully(campaign);
            }

            assertEquals(RECHARGE_HOURS / 2, currentLocation.getRechargeTime());
        }

        @Test
        void halvedRecharge_doesNotChargeBattery() {
            useBatteryMode(LithiumFusionBatteryMode.HALVED_RECHARGE);
            for (int day = 0; day < 10; day++) {
                currentLocation.newDay(campaign, true);
            }

            assertEquals(0.0, currentLocation.getLithiumFusionBatteryCharge());
        }
    }

    @Nested
    class BatteryRecharging {
        @Test
        void batteryChargesOnlyOnceDriveIsFull() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem quickRechargeSystem = mockSystem(20.0);
            CurrentLocation location = new CurrentLocation(quickRechargeSystem, 0.0);

            location.newDay(campaign, false);

            // 20 hours to fill the drive, leaving 4 of the 20 needed for the battery
            assertEquals(20.0, location.getRechargeTime());
            assertEquals(0.2, location.getLithiumFusionBatteryCharge(), 1e-9);
            verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("Lithium-Fusion batteries"));
        }

        @Test
        void batteryFillsAndReportsCompletion() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem quickRechargeSystem = mockSystem(20.0);
            CurrentLocation location = new CurrentLocation(quickRechargeSystem, 0.0);

            location.newDay(campaign, false);
            location.newDay(campaign, false);

            assertTrue(location.isLithiumFusionBatteryCharged());
            assertEquals(1.0, location.getLithiumFusionBatteryCharge());
            verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("batteries fully charged"));
        }

        @Test
        void batteryDoesNotChargeWhenDoubleJumpIsNotInEffect() {
            useBatteryMode(LithiumFusionBatteryMode.DISABLED);
            PlanetarySystem quickRechargeSystem = mockSystem(20.0);
            CurrentLocation location = new CurrentLocation(quickRechargeSystem, 0.0);

            location.newDay(campaign, false);
            location.newDay(campaign, false);

            assertEquals(0.0, location.getLithiumFusionBatteryCharge());
        }

        @Test
        void batteryDoesNotChargeWhereRechargingIsImpossible() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem deadSystem = mockSystem(Double.POSITIVE_INFINITY);
            CurrentLocation location = new CurrentLocation(deadSystem, 0.0);

            location.newDay(campaign, true);

            assertEquals(0.0, location.getLithiumFusionBatteryCharge());
        }

        @Test
        void instantRecharge_fillsBatteryImmediately() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem instantSystem = mockSystem(0.0);
            CurrentLocation location = new CurrentLocation(instantSystem, 0.0);

            location.newDay(campaign, true);

            assertTrue(location.isLithiumFusionBatteryCharged());
        }
    }

    @Nested
    class DoubleJump {
        @Test
        void chargedDriveAndBattery_jumpTwiceInOneDay() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem firstStop = mockSystem(RECHARGE_HOURS);
            PlanetarySystem secondStop = mockSystem(RECHARGE_HOURS);
            PlanetarySystem destination = mockSystem(RECHARGE_HOURS);
            JumpPath jumpPath = jumpPathThrough(originSystem, firstStop, secondStop, destination);

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                currentLocation.chargeFully(campaign);
                currentLocation.setTransitTime(TIME_TO_JUMP_POINT);

                currentLocation.newDay(campaign, false);
            }

            assertSame(secondStop, currentLocation.getCurrentSystem());
            assertEquals(2, jumpPath.size());
            assertEquals(0.0, currentLocation.getLithiumFusionBatteryCharge());
            // The drive was spent on the first jump and has the whole day to recharge
            assertEquals(24.0, currentLocation.getRechargeTime());
            verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("follow-on jump"));
        }

        @Test
        void emptyDriveButChargedBattery_jumpsOnBattery() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem firstStop = mockSystem(RECHARGE_HOURS);
            PlanetarySystem destination = mockSystem(RECHARGE_HOURS);
            JumpPath jumpPath = jumpPathThrough(originSystem, firstStop, destination);

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                currentLocation.setTransitTime(TIME_TO_JUMP_POINT);
                currentLocation.setLithiumFusionBatteryCharge(1.0);

                currentLocation.newDay(campaign, false);
            }

            assertSame(firstStop, currentLocation.getCurrentSystem());
            assertEquals(0.0, currentLocation.getLithiumFusionBatteryCharge());
            // The drive's own partial charge is kept, as the battery powered the jump
            assertEquals(24.0, currentLocation.getRechargeTime());
            verify(campaign, never()).addReport(eq(DailyReportType.GENERAL), contains("follow-on jump"));
        }

        @Test
        void finalJump_doesNotSpendBattery() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem destination = mockSystem(RECHARGE_HOURS);
            JumpPath jumpPath = jumpPathThrough(originSystem, destination);

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                currentLocation.chargeFully(campaign);
                currentLocation.setTransitTime(TIME_TO_JUMP_POINT);

                currentLocation.newDay(campaign, false);
            }

            assertSame(destination, currentLocation.getCurrentSystem());
            assertTrue(currentLocation.isLithiumFusionBatteryCharged());
        }

        @Test
        void disabledMode_ignoresChargedBattery() {
            useBatteryMode(LithiumFusionBatteryMode.DISABLED);
            PlanetarySystem firstStop = mockSystem(RECHARGE_HOURS);
            PlanetarySystem secondStop = mockSystem(RECHARGE_HOURS);
            PlanetarySystem destination = mockSystem(RECHARGE_HOURS);
            JumpPath jumpPath = jumpPathThrough(originSystem, firstStop, secondStop, destination);

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                currentLocation.chargeFully(campaign);
                currentLocation.setTransitTime(TIME_TO_JUMP_POINT);

                currentLocation.newDay(campaign, false);
            }

            assertSame(firstStop, currentLocation.getCurrentSystem());
            assertEquals(3, jumpPath.size());
        }

        @Test
        void emptyDriveAndBattery_waitsToRecharge() {
            useBatteryMode(LithiumFusionBatteryMode.DOUBLE_JUMP);
            PlanetarySystem destination = mockSystem(RECHARGE_HOURS);
            JumpPath jumpPath = jumpPathThrough(originSystem, destination);

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                currentLocation.setTransitTime(TIME_TO_JUMP_POINT);

                currentLocation.newDay(campaign, false);
            }

            assertSame(originSystem, currentLocation.getCurrentSystem());
            assertEquals(24.0, currentLocation.getRechargeTime());
        }
    }

    @Nested
    class Serialization {
        @Test
        void batteryChargeIsWrittenWhenPresent() {
            when(originSystem.getId()).thenReturn("Outreach");
            currentLocation.setLithiumFusionBatteryCharge(0.25);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            currentLocation.writeToXML(new PrintWriter(outputStream, true), 0);

            assertTrue(outputStream.toString().contains("<lithiumFusionBatteryCharge>0.25</lithiumFusionBatteryCharge>"));
        }

        @Test
        void emptyBatteryIsNotWritten() {
            when(originSystem.getId()).thenReturn("Outreach");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            currentLocation.writeToXML(new PrintWriter(outputStream, true), 0);

            assertFalse(outputStream.toString().contains("lithiumFusionBatteryCharge"));
        }

        @Test
        void batteryChargeIsRead() throws Exception {
            String xml = "<location><currentSystemId>Outreach</currentSystemId><transitTime>0.0</transitTime>"
                               + "<lithiumFusionBatteryCharge>0.75</lithiumFusionBatteryCharge></location>";
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Node node = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
            Campaign xmlCampaign = mockCampaign();
            when(xmlCampaign.getSystemById("Outreach")).thenReturn(originSystem);

            CurrentLocation location = CurrentLocation.generateInstanceFromXML(node, xmlCampaign);

            assertNotNull(location);
            assertEquals(0.75, location.getLithiumFusionBatteryCharge());
        }
    }
}
