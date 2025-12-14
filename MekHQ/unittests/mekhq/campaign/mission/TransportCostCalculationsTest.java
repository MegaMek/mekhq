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
package mekhq.campaign.mission;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.units.Jumpship.DRIVE_CORE_NONE;
import static mekhq.campaign.mission.TransportCostCalculations.*;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.units.Entity;
import megamek.common.units.SpaceStation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TransportCostCalculationsTest {
    private static final LocalDate today = LocalDate.of(3151, 1, 1);
    private static final CargoStatistics mockCargoStatistics = mock(CargoStatistics.class);
    private static final HangarStatistics mockHangarStatistics = mock(HangarStatistics.class);

    private TransportCostCalculations transportCostCalculations;

    @BeforeEach
    public void setup() {
        transportCostCalculations = new TransportCostCalculations(new ArrayList<>(),
              new ArrayList<>(),
              mockCargoStatistics,
              mockHangarStatistics,
              EXP_REGULAR);
        transportCostCalculations.setTotalCost(Money.zero()); // Initialize total cost
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void testCalculateAdditionalJumpCollarsRequirements_notEnoughCollars(int additionalDropShips) {
        when(mockHangarStatistics.getTotalDockingCollars()).thenReturn(0);
        transportCostCalculations.setDropShipCount(0);
        transportCostCalculations.setAdditionalDropShipsRequired(additionalDropShips);

        transportCostCalculations.calculateAdditionalJumpCollarsRequirements();

        int actualCollarNeeds = transportCostCalculations.getAdditionalCollarsRequired();
        assertEquals(additionalDropShips, actualCollarNeeds,
              "Expected " + additionalDropShips + " additional collars required but was " + actualCollarNeeds);

        double predictedDockingCollarCost = round(additionalDropShips * JUMP_SHIP_COLLAR_COST);
        double actualDockingCollarCost = transportCostCalculations.getDockingCollarCost();

        assertEquals(predictedDockingCollarCost, actualDockingCollarCost,
              "Expected docking collar cost of " + predictedDockingCollarCost + " but was " + actualDockingCollarCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void testCalculateAdditionalJumpCollarsRequirements_enoughCollars(int additionalDropShips) {
        when(mockHangarStatistics.getTotalDockingCollars()).thenReturn(additionalDropShips);
        transportCostCalculations.setDropShipCount(0);
        transportCostCalculations.setAdditionalDropShipsRequired(0);

        transportCostCalculations.calculateAdditionalJumpCollarsRequirements();

        int predictedCollarNeeds = 0;
        int actualCollarNeeds = transportCostCalculations.getAdditionalCollarsRequired();

        assertEquals(predictedCollarNeeds, actualCollarNeeds,
              "Expected " + predictedCollarNeeds + " additional collars required but was " + actualCollarNeeds);

        double predictedDockingCollarCost = 0;
        double actualDockingCollarCost = transportCostCalculations.getDockingCollarCost();

        assertEquals(predictedDockingCollarCost, actualDockingCollarCost,
              "Expected docking collar cost of " + predictedDockingCollarCost + " but was " + actualDockingCollarCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_mothballedCargo_insufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(0.0);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(cargoSize);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(cargoSize);
        double totalCargoSize = cargoSize * 2;

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = (int) ceil(totalCargoSize / CARGO_PER_DROPSHIP);
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = round(totalCargoSize * CARGO_PER_TON_COST);
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_mothballedCargo_sufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(cargoSize * 3);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(cargoSize);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(cargoSize);

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = 0;
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = 0;
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_noMothballedCargo_insufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(0.0);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(cargoSize);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(0.0);

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = (int) ceil(cargoSize / CARGO_PER_DROPSHIP);
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = round(cargoSize * CARGO_PER_TON_COST);
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_noMothballedCargo_sufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(cargoSize * 3);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(cargoSize);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(0.0);

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = 0;
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = 0;
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_onlyMothballedCargo_insufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(0.0);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(0.0);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(cargoSize);

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = (int) ceil(cargoSize / CARGO_PER_DROPSHIP);
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = round(cargoSize * CARGO_PER_TON_COST);
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3000, 5000, 10000 })
    public void testCalculateCargoRequirements_onlyMothballedCargo_sufficientCapacity(double cargoSize) {
        when(mockCargoStatistics.getTotalCargoCapacity()).thenReturn(cargoSize * 3);

        when(mockCargoStatistics.getCargoTonnage(false, false)).thenReturn(0.0);
        when(mockCargoStatistics.getCargoTonnage(false, true)).thenReturn(cargoSize);

        transportCostCalculations.calculateCargoRequirements();
        int additionalDropShipsRequired = transportCostCalculations.getAdditionalDropShipsRequired();
        int predictedAdditionalDropShipsRequired = 0;
        assertEquals(predictedAdditionalDropShipsRequired, additionalDropShipsRequired,
              "Expected " +
                    predictedAdditionalDropShipsRequired +
                    " additional drop ships required but was " +
                    additionalDropShipsRequired);

        double actualCost = transportCostCalculations.getCargoBayCost();
        double expectedCost = 0;
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " cargo bay cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_smallCraft_noSpareBays(int bayRequirementCount) {
        transportCostCalculations.setSmallCraftCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSmallCraftBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * SMALL_CRAFT_COST);
        double actualCost = transportCostCalculations.getAdditionalSmallCraftBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_smallCraft_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setSmallCraftCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSmallCraftBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * SMALL_CRAFT_COST);
        double actualCost = transportCostCalculations.getAdditionalSmallCraftBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_smallCraft_tooManyExistingBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setSmallCraftCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSmallCraftBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * SMALL_CRAFT_COST);
        double actualCost = transportCostCalculations.getAdditionalSmallCraftBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_aerospaceOrConventionalFighter_noSpareBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalASFBays()).thenReturn(0);

        transportCostCalculations.setSmallCraftCount(0);
        transportCostCalculations.setASFCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalASFBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * ASF_COST);
        double actualCost = transportCostCalculations.getAdditionalASFBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_aerospaceOrConventionalFighter_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalASFBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setSmallCraftCount(0);
        transportCostCalculations.setASFCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalASFBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * ASF_COST);
        double actualCost = transportCostCalculations.getAdditionalASFBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_aerospaceOrConventionalFighter_tooManyExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalASFBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setSmallCraftCount(0);
        transportCostCalculations.setASFCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalASFBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * ASF_COST);
        double actualCost = transportCostCalculations.getAdditionalASFBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_aerospaceOrConventionalFighter_surplusCompatibleBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSmallCraftBays()).thenReturn(3);
        when(mockHangarStatistics.getTotalASFBays()).thenReturn(0);

        transportCostCalculations.setSmallCraftCount(0);
        transportCostCalculations.setASFCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalASFBaysRequired();
        int expectedBaysRequired = max(0, bayRequirementCount - 3);
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * ASF_COST);
        double actualCost = transportCostCalculations.getAdditionalASFBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_mek_noSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalMekBays()).thenReturn(0);

        transportCostCalculations.setMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalMekBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * MEK_COST);
        double actualCost = transportCostCalculations.getAdditionalMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_mek_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalMekBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalMekBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * MEK_COST);
        double actualCost = transportCostCalculations.getAdditionalMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_mek_tooManyExistingBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalMekBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalMekBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * MEK_COST);
        double actualCost = transportCostCalculations.getAdditionalMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_superHeavyVehicle_noSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * SUPER_HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_superHeavyVehicle_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setSuperHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * SUPER_HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_superHeavyVehicle_tooManyExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setSuperHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * SUPER_HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalSuperHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_heavyVehicle_noSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalHeavyVehicleBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_heavyVehicle_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalHeavyVehicleBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_heavyVehicle_tooManyExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalHeavyVehicleBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_heavyVehicle_surplusCompatibleBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalHeavyVehicleBaysRequired();
        int expectedBaysRequired = max(0, bayRequirementCount - 3);
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * HEAVY_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalHeavyVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_noSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(bayRequirementCount * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_tooFewExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(bayRequirementCount - 1);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        int expectedBaysRequired = 1;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_tooManyExistingBays(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(bayRequirementCount + 1);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        int expectedBaysRequired = 0;
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_surplusCompatibleBays_superHeavy(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        int expectedBaysRequired = max(0, bayRequirementCount - 3);
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_surplusCompatibleBays_heavy(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(0);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(3);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        int expectedBaysRequired = max(0, bayRequirementCount - 3);
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_lightVehicle_surplusCompatibleBays_superHeavyAndHeavy(
          int bayRequirementCount) {
        when(mockHangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(2);
        when(mockHangarStatistics.getTotalHeavyVehicleBays()).thenReturn(1);
        when(mockHangarStatistics.getTotalLightVehicleBays()).thenReturn(0);

        transportCostCalculations.setSuperHeavyVehicleCount(0);
        transportCostCalculations.setHeavyVehicleCount(0);
        transportCostCalculations.setLightVehicleCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalLightVehicleBaysRequired();
        int expectedBaysRequired = max(0, bayRequirementCount - 3);
        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * LIGHT_VEHICLE_COST);
        double actualCost = transportCostCalculations.getAdditionalLightVehicleBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_protoMek_noSpareBays(int bayRequirementCount) {
        double protoMekBayUsage = (double) bayRequirementCount / PROTOMEKS_PER_BAY;
        int expectedBaysRequired = max(0, MathUtility.roundAwayFromZero(protoMekBayUsage));

        when(mockHangarStatistics.getTotalProtoMekBays()).thenReturn(0);

        transportCostCalculations.setProtoMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalProtoMekBaysRequired();

        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * PROTOMEK_COST);
        double actualCost = transportCostCalculations.getAdditionalProtoMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_protoMek_tooFewSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalProtoMekBays()).thenReturn(1);

        double protoMekBayUsage = 1 - bayRequirementCount;
        protoMekBayUsage = protoMekBayUsage / PROTOMEKS_PER_BAY;
        int adjustedProtoMekBayUsage = MathUtility.roundAwayFromZero(protoMekBayUsage);
        int expectedProtoMekBaysRequired = -min(0, adjustedProtoMekBayUsage);

        transportCostCalculations.setProtoMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalProtoMekBaysRequired();

        assertEquals(expectedProtoMekBaysRequired, additionalBaysRequired,
              "Expected " +
                    expectedProtoMekBaysRequired +
                    " additional bays required but was " +
                    additionalBaysRequired);

        double expectedCost = round(expectedProtoMekBaysRequired * PROTOMEK_COST);
        double actualCost = transportCostCalculations.getAdditionalProtoMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_protoMek_tooManySpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalProtoMekBays()).thenReturn(bayRequirementCount);

        transportCostCalculations.setProtoMekCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalProtoMekBaysRequired();

        assertEquals(0, additionalBaysRequired,
              "Expected " +
                    0 +
                    " additional bays required but was " +
                    additionalBaysRequired);

        double expectedCost = 0;
        double actualCost = transportCostCalculations.getAdditionalProtoMekBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_battleArmor_noSpareBays(int bayRequirementCount) {
        double battleArmorBayUsage = (double) bayRequirementCount / BATTLE_ARMOR_SQUADS_PER_BAY;
        int expectedBaysRequired = max(0, MathUtility.roundAwayFromZero(battleArmorBayUsage));

        when(mockHangarStatistics.getTotalBattleArmorBays()).thenReturn(0);

        transportCostCalculations.setBattleArmorCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalBattleArmorBaysRequired();

        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * BATTLE_ARMOR_COST);
        double actualCost = transportCostCalculations.getAdditionalBattleArmorBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 8, 11, 24 })
    public void calculateAdditionalBayRequirementsFromUnits_battleArmor_tooFewSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalBattleArmorBays()).thenReturn(1);

        double battleArmorBayUsage = 1 - bayRequirementCount;
        battleArmorBayUsage = battleArmorBayUsage / BATTLE_ARMOR_SQUADS_PER_BAY;
        int adjustedBattleArmorBayUsage = MathUtility.roundAwayFromZero(battleArmorBayUsage);
        int expectedBattleArmorBaysRequired = -min(0, adjustedBattleArmorBayUsage);

        transportCostCalculations.setBattleArmorCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalBattleArmorBaysRequired();
        assertEquals(expectedBattleArmorBaysRequired, additionalBaysRequired,
              "Expected " +
                    expectedBattleArmorBaysRequired +
                    " additional bays required but was " +
                    additionalBaysRequired);

        double expectedCost = round(expectedBattleArmorBaysRequired * BATTLE_ARMOR_COST);
        double actualCost = transportCostCalculations.getAdditionalBattleArmorBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 8, 11, 24 })
    public void calculateAdditionalBayRequirementsFromUnits_battleArmor_tooManySpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalBattleArmorBays()).thenReturn(bayRequirementCount);

        transportCostCalculations.setBattleArmorCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalBattleArmorBaysRequired();
        assertEquals(0, additionalBaysRequired,
              "Expected " + 0 + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = 0;
        double actualCost = transportCostCalculations.getAdditionalBattleArmorBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_infantry_noSpareBays(int bayRequirementCount) {
        double infantryBayUsage = (double) bayRequirementCount / PLATOONS_PER_BAY;
        int expectedBaysRequired = max(0, MathUtility.roundAwayFromZero(infantryBayUsage));

        when(mockHangarStatistics.getTotalInfantryBays()).thenReturn(0);

        transportCostCalculations.setInfantryCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalInfantryBaysRequired();

        assertEquals(expectedBaysRequired, additionalBaysRequired,
              "Expected " + expectedBaysRequired + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(expectedBaysRequired * INFANTRY_COST);
        double actualCost = transportCostCalculations.getAdditionalInfantryBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 8, 11, 24 })
    public void calculateAdditionalBayRequirementsFromUnits_infantry_tooFewSpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalInfantryBays()).thenReturn(1);

        double infantryBayUsage = 1 - bayRequirementCount;
        infantryBayUsage = infantryBayUsage / PLATOONS_PER_BAY;
        int adjustedInfantryBayUsage = MathUtility.roundAwayFromZero(infantryBayUsage);
        int expectedInfantryBaysRequired = -min(0, adjustedInfantryBayUsage);

        transportCostCalculations.setInfantryCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalInfantryBaysRequired();
        assertEquals(expectedInfantryBaysRequired, additionalBaysRequired,
              "Expected " +
                    expectedInfantryBaysRequired +
                    " additional bays required but was " +
                    additionalBaysRequired);

        double expectedCost = round(expectedInfantryBaysRequired * INFANTRY_COST);
        double actualCost = transportCostCalculations.getAdditionalInfantryBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 8, 11, 24 })
    public void calculateAdditionalBayRequirementsFromUnits_infantry_tooManySpareBays(int bayRequirementCount) {
        when(mockHangarStatistics.getTotalInfantryBays()).thenReturn(bayRequirementCount);

        transportCostCalculations.setInfantryCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getAdditionalInfantryBaysRequired();
        assertEquals(0, additionalBaysRequired,
              "Expected " + 0 + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = 0;
        double actualCost = transportCostCalculations.getAdditionalInfantryBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3, 5, 10 })
    public void calculateAdditionalBayRequirementsFromUnits_otherUnit_noSpareBays(int bayRequirementCount) {
        transportCostCalculations.setOtherUnitCount(bayRequirementCount);
        transportCostCalculations.calculateAdditionalBayRequirementsFromUnits();

        int additionalBaysRequired = transportCostCalculations.getOtherUnitCount();
        assertEquals(bayRequirementCount, additionalBaysRequired,
              "Expected " + bayRequirementCount + " additional bays required but was " + additionalBaysRequired);

        double expectedCost = round(additionalBaysRequired * OTHER_UNIT_COST);
        double actualCost = transportCostCalculations.getAdditionalOtherUnitBaysCost();
        assertEquals(expectedCost, actualCost,
              "Expected " + expectedCost + " additional bays cost but was " + actualCost);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_dropShips(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getDropShipCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_smallCraft(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getSmallCraftCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_mek(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getMekCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_aerospaceFighter(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getAsfCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_conventionalFighter(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getAsfCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_protoMek(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getProtoMekCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_battleArmor(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getBattleArmorCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_infantry(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(false);
        when(mockEntity.isInfantry()).thenReturn(true);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getInfantryCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_otherUnit(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(false);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(mockEntity.isSmallCraft()).thenReturn(false);
        when(mockEntity.isMek()).thenReturn(false);
        when(mockEntity.isAerospaceFighter()).thenReturn(false);
        when(mockEntity.isConventionalFighter()).thenReturn(false);
        when(mockEntity.isProtoMek()).thenReturn(false);
        when(mockEntity.isBattleArmor()).thenReturn(false);
        when(mockEntity.isInfantry()).thenReturn(false);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedUnits = transportCostCalculations.getOtherUnitCount();
        assertEquals(unitCount, countedUnits, "Expected " + unitCount + " units but was " + countedUnits);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_superHeavyVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(10000.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(unitCount, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(0, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(0, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_heavyVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(75.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(0, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(unitCount, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(0, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    public void testCountUnitsByType_lightVehicles(int unitCount) {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockEntity.isVehicle()).thenReturn(true);
        when(mockEntity.getWeight()).thenReturn(25.0);

        Collection<Unit> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            units.add(mockUnit);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(units, new ArrayList<>(),
              mockCargoStatistics, mockHangarStatistics, EXP_REGULAR);
        transportCostCalculations.countUnitsByType();

        int countedSuperHeavyVehicles = transportCostCalculations.getSuperHeavyVehicleCount();
        assertEquals(0, countedSuperHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedSuperHeavyVehicles);

        int countedHeavyVehicles = transportCostCalculations.getHeavyVehicleCount();
        assertEquals(0, countedHeavyVehicles,
              "Expected " + unitCount + " units but was " + countedHeavyVehicles);

        int countedLightVehicles = transportCostCalculations.getLightVehicleCount();
        assertEquals(unitCount, countedLightVehicles,
              "Expected " + unitCount + " units but was " + countedLightVehicles);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 10, 30, 40, 50 })
    public void testCalculateAdditionalBayRequirementsFromPassengers(int passengerCount) {
        Person person = new Person(UUID.randomUUID());
        person.setStatus(PersonnelStatus.ACTIVE);
        person.setOptions(new PersonnelOptions());
        Collection<Person> passengers = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            passengers.add(person);
        }

        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(new ArrayList<>(),
              passengers,
              mockCargoStatistics,
              mockHangarStatistics,
              EXP_REGULAR);

        double additionalPassengerBaysCost = transportCostCalculations.getAdditionalPassengerBaysCost();
        double expectedCost = round(additionalPassengerBaysCost * PASSENGERS_COST);
        assertEquals(additionalPassengerBaysCost, expectedCost,
              "Expected additional passenger bays cost to be " +
                    expectedCost +
                    " but was " +
                    additionalPassengerBaysCost);

        double totalAdditionalBaysRequired = transportCostCalculations.getAdditionalPassengerBaysRequired();
        int expectedAdditionalBays = (int) ceil(totalAdditionalBaysRequired / BAYS_PER_DROPSHIP);
        assertEquals(expectedAdditionalBays, totalAdditionalBaysRequired,
              "Expected total additional bays required to be " +
                    expectedAdditionalBays +
                    " but was " +
                    totalAdditionalBaysRequired);

    }

    @Test
    public void testPerformJumpTransaction_unableToAffordJourney() {
        Finances finances = new Finances();

        PlanetarySystem mockCurrentPlanetarySystem = mock(PlanetarySystem.class);
        when(mockCurrentPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        PlanetarySystem mockDestinationPlanetarySystem = mock(PlanetarySystem.class);
        when(mockDestinationPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getLastSystem()).thenReturn(mockDestinationPlanetarySystem);

        String report = TransportCostCalculations.performJumpTransaction(finances,
              mockJumpPath,
              any(LocalDate.class),
              Money.of(100),
              mockCurrentPlanetarySystem);

        assertFalse(report.isBlank(), "Journey cost doesn't exceeds available funds");
    }

    @Test
    public void testPerformJumpTransaction_ableToAffordJourney() {
        Finances finances = new Finances();
        finances.credit(TransactionType.BONUS_EXCHANGE, today, Money.of(100), "");

        PlanetarySystem mockCurrentPlanetarySystem = mock(PlanetarySystem.class);
        when(mockCurrentPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        PlanetarySystem mockDestinationPlanetarySystem = mock(PlanetarySystem.class);
        when(mockDestinationPlanetarySystem.getName(any(LocalDate.class))).thenReturn("Test");

        JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getLastSystem()).thenReturn(mockDestinationPlanetarySystem);

        String report = TransportCostCalculations.performJumpTransaction(finances,
              mockJumpPath,
              today,
              Money.of(1),
              mockCurrentPlanetarySystem);

        assertTrue(report.isBlank(), "Journey cost exceeds available funds");
    }

    @Test
    public void testGetTotalCost_whenTotalCostIsSet() {
        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(
              new ArrayList<>(),
              new ArrayList<>(),
              mockCargoStatistics,
              mockHangarStatistics,
              EXP_REGULAR
        );
        Money expectedCost = Money.of(1000);
        transportCostCalculations.setTotalCost(expectedCost);

        Money actualCost = transportCostCalculations.getTotalCost();
        assertEquals(expectedCost, actualCost, "Expected total cost to be " + expectedCost + " but was " + actualCost);
    }

    @Test
    public void testGetTotalCost_whenTotalCostIsNull() {
        TransportCostCalculations transportCostCalculations = new TransportCostCalculations(
              new ArrayList<>(),
              new ArrayList<>(),
              mockCargoStatistics,
              mockHangarStatistics,
              EXP_REGULAR
        );

        Money actualCost = transportCostCalculations.getTotalCost();
        assertNull(actualCost, "Expected total cost to be null but was " + actualCost);
    }

    @Test
    void countsPersonAsPassenger_whenUnitIsNull() throws Exception {
        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(null);

        setAllPersonnel(List.of(person));

        assertEquals(1, invokeGetPassengerCount());
    }

    @Test
    void countsPersonAsPassenger_whenEntityIsNull() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(null);

        setAllPersonnel(List.of(person));

        assertEquals(1, invokeGetPassengerCount());
    }

    @Test
    void doesNotCountPersonAsPassenger_whenEntityIsSmallCraft() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(entity);

        when(entity.isSmallCraft()).thenReturn(true);
        when(entity.isWarShip()).thenReturn(false);
        when(entity.isJumpShip()).thenReturn(false);
        when(entity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(person));

        assertEquals(0, invokeGetPassengerCount());
    }

    @Test
    void doesNotCountPersonAsPassenger_whenEntityIsWarShip() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(entity);

        when(entity.isSmallCraft()).thenReturn(false);
        when(entity.isWarShip()).thenReturn(true);
        when(entity.isJumpShip()).thenReturn(false);
        when(entity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(person));

        assertEquals(0, invokeGetPassengerCount());
    }

    @Test
    void doesNotCountPersonAsPassenger_whenEntityIsJumpShip() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(entity);

        when(entity.isSmallCraft()).thenReturn(false);
        when(entity.isWarShip()).thenReturn(false);
        when(entity.isJumpShip()).thenReturn(true);
        when(entity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(person));

        assertEquals(0, invokeGetPassengerCount());
    }

    @Test
    void doesNotCountPersonAsPassenger_whenEntityIsDropShip() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(entity);

        when(entity.isSmallCraft()).thenReturn(false);
        when(entity.isWarShip()).thenReturn(false);
        when(entity.isJumpShip()).thenReturn(false);
        when(entity.isDropShip()).thenReturn(true);

        setAllPersonnel(List.of(person));

        assertEquals(0, invokeGetPassengerCount());
    }

    @Test
    void countsPersonAsPassenger_whenEntityIsNoneOfExcludedTypes() throws Exception {
        Person person = mock(Person.class);
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);

        when(person.getUnit()).thenReturn(unit);
        when(unit.getEntity()).thenReturn(entity);

        when(entity.isSmallCraft()).thenReturn(false);
        when(entity.isWarShip()).thenReturn(false);
        when(entity.isJumpShip()).thenReturn(false);
        when(entity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(person));

        assertEquals(1, invokeGetPassengerCount());
    }

    @Test
    void countsMixedCasesCorrectly() throws Exception {
        Person nullUnit = mock(Person.class);
        when(nullUnit.getUnit()).thenReturn(null);

        Person personWithNullUnitEntity = mock(Person.class);
        Unit unitWithNullEntity = mock(Unit.class);
        when(personWithNullUnitEntity.getUnit()).thenReturn(unitWithNullEntity);
        when(unitWithNullEntity.getEntity()).thenReturn(null);

        Person personWithSmallCraftUnit = mock(Person.class);
        Unit smallCraftUnit = mock(Unit.class);
        Entity smallCraftEntity = mock(Entity.class);
        when(personWithSmallCraftUnit.getUnit()).thenReturn(smallCraftUnit);
        when(smallCraftUnit.getEntity()).thenReturn(smallCraftEntity);
        when(smallCraftEntity.isSmallCraft()).thenReturn(true);
        when(smallCraftEntity.isWarShip()).thenReturn(false);
        when(smallCraftEntity.isJumpShip()).thenReturn(false);
        when(smallCraftEntity.isDropShip()).thenReturn(false);

        Person personWithWarShipUnit = mock(Person.class);
        Unit warShipUnit = mock(Unit.class);
        Entity warShipEntity = mock(Entity.class);
        when(personWithWarShipUnit.getUnit()).thenReturn(warShipUnit);
        when(warShipUnit.getEntity()).thenReturn(warShipEntity);
        when(warShipEntity.isSmallCraft()).thenReturn(false);
        when(warShipEntity.isWarShip()).thenReturn(true);
        when(warShipEntity.isJumpShip()).thenReturn(false);
        when(warShipEntity.isDropShip()).thenReturn(false);

        Person personWithJumpShipUnit = mock(Person.class);
        Unit jumpShipUnit = mock(Unit.class);
        Entity jumpShipEntity = mock(Entity.class);
        when(personWithJumpShipUnit.getUnit()).thenReturn(jumpShipUnit);
        when(jumpShipUnit.getEntity()).thenReturn(jumpShipEntity);
        when(jumpShipEntity.isSmallCraft()).thenReturn(false);
        when(jumpShipEntity.isWarShip()).thenReturn(false);
        when(jumpShipEntity.isJumpShip()).thenReturn(true);
        when(jumpShipEntity.isDropShip()).thenReturn(false);

        Person personWithDropShipUnit = mock(Person.class);
        Unit dropShipUnit = mock(Unit.class);
        Entity dropShipEntity = mock(Entity.class);
        when(personWithDropShipUnit.getUnit()).thenReturn(dropShipUnit);
        when(dropShipUnit.getEntity()).thenReturn(dropShipEntity);
        when(dropShipEntity.isSmallCraft()).thenReturn(false);
        when(dropShipEntity.isWarShip()).thenReturn(false);
        when(dropShipEntity.isJumpShip()).thenReturn(false);
        when(dropShipEntity.isDropShip()).thenReturn(true);

        Person normalPerson = mock(Person.class);
        Unit normalUnit = mock(Unit.class);
        Entity normalUnitEntity = mock(Entity.class);
        when(normalPerson.getUnit()).thenReturn(normalUnit);
        when(normalUnit.getEntity()).thenReturn(normalUnitEntity);
        when(normalUnitEntity.isSmallCraft()).thenReturn(false);
        when(normalUnitEntity.isWarShip()).thenReturn(false);
        when(normalUnitEntity.isJumpShip()).thenReturn(false);
        when(normalUnitEntity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(nullUnit, personWithNullUnitEntity, personWithSmallCraftUnit,
              personWithWarShipUnit, personWithJumpShipUnit, personWithDropShipUnit, normalPerson));

        // person with null unit, person with unit with null entity, person with normal unit
        assertEquals(3, invokeGetPassengerCount());
    }

    @Test
    void countsDropShipCorrectly() throws Exception {
        Person nullUnit = mock(Person.class);
        when(nullUnit.getUnit()).thenReturn(null);

        Person personWithNullUnitEntity = mock(Person.class);
        Unit unitWithNullEntity = mock(Unit.class);
        when(personWithNullUnitEntity.getUnit()).thenReturn(unitWithNullEntity);
        when(unitWithNullEntity.getEntity()).thenReturn(null);

        Person personWithDropShipUnit = mock(Person.class);
        Unit dropShipUnit = mock(Unit.class);
        Entity dropShipEntity = mock(Entity.class);
        when(personWithDropShipUnit.getUnit()).thenReturn(dropShipUnit);
        when(dropShipUnit.getEntity()).thenReturn(dropShipEntity);
        when(dropShipEntity.isSmallCraft()).thenReturn(false);
        when(dropShipEntity.isWarShip()).thenReturn(false);
        when(dropShipEntity.isJumpShip()).thenReturn(false);
        when(dropShipEntity.isDropShip()).thenReturn(true);

        Person normalPerson = mock(Person.class);
        Unit normalUnit = mock(Unit.class);
        Entity normalUnitEntity = mock(Entity.class);
        when(normalPerson.getUnit()).thenReturn(normalUnit);
        when(normalUnit.getEntity()).thenReturn(normalUnitEntity);
        when(normalUnitEntity.isSmallCraft()).thenReturn(false);
        when(normalUnitEntity.isWarShip()).thenReturn(false);
        when(normalUnitEntity.isJumpShip()).thenReturn(false);
        when(normalUnitEntity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(nullUnit, personWithNullUnitEntity, personWithDropShipUnit, normalPerson));

        // person with null unit, person with unit with null entity, person with normal unit
        assertEquals(3, invokeGetPassengerCount());
    }

    @Test
    void countsJumpShipCorrectly() throws Exception {
        Person nullUnit = mock(Person.class);
        when(nullUnit.getUnit()).thenReturn(null);

        Person personWithNullUnitEntity = mock(Person.class);
        Unit unitWithNullEntity = mock(Unit.class);
        when(personWithNullUnitEntity.getUnit()).thenReturn(unitWithNullEntity);
        when(unitWithNullEntity.getEntity()).thenReturn(null);

        Person personWithJumpShipUnit = mock(Person.class);
        Unit jumpShipUnit = mock(Unit.class);
        Entity jumpShipEntity = mock(Entity.class);
        when(personWithJumpShipUnit.getUnit()).thenReturn(jumpShipUnit);
        when(jumpShipUnit.getEntity()).thenReturn(jumpShipEntity);
        when(jumpShipEntity.isSmallCraft()).thenReturn(false);
        when(jumpShipEntity.isWarShip()).thenReturn(false);
        when(jumpShipEntity.isJumpShip()).thenReturn(true);
        when(jumpShipEntity.isDropShip()).thenReturn(false);

        Person normalPerson = mock(Person.class);
        Unit normalUnit = mock(Unit.class);
        Entity normalUnitEntity = mock(Entity.class);
        when(normalPerson.getUnit()).thenReturn(normalUnit);
        when(normalUnit.getEntity()).thenReturn(normalUnitEntity);
        when(normalUnitEntity.isSmallCraft()).thenReturn(false);
        when(normalUnitEntity.isWarShip()).thenReturn(false);
        when(normalUnitEntity.isJumpShip()).thenReturn(false);
        when(normalUnitEntity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(nullUnit, personWithNullUnitEntity, personWithJumpShipUnit, normalPerson));

        // person with null unit, person with unit with null entity, person with normal unit
        assertEquals(3, invokeGetPassengerCount());
    }

    @Test
    void countsWarShipCorrectly() throws Exception {
        Person nullUnit = mock(Person.class);
        when(nullUnit.getUnit()).thenReturn(null);

        Person personWithNullUnitEntity = mock(Person.class);
        Unit unitWithNullEntity = mock(Unit.class);
        when(personWithNullUnitEntity.getUnit()).thenReturn(unitWithNullEntity);
        when(unitWithNullEntity.getEntity()).thenReturn(null);

        Person personWithWarShipUnit = mock(Person.class);
        Unit warShipUnit = mock(Unit.class);
        Entity warShipEntity = mock(Entity.class);
        when(personWithWarShipUnit.getUnit()).thenReturn(warShipUnit);
        when(warShipUnit.getEntity()).thenReturn(warShipEntity);
        when(warShipEntity.isSmallCraft()).thenReturn(false);
        when(warShipEntity.isWarShip()).thenReturn(true);
        when(warShipEntity.isJumpShip()).thenReturn(false);
        when(warShipEntity.isDropShip()).thenReturn(false);

        Person normalPerson = mock(Person.class);
        Unit normalUnit = mock(Unit.class);
        Entity normalUnitEntity = mock(Entity.class);
        when(normalPerson.getUnit()).thenReturn(normalUnit);
        when(normalUnit.getEntity()).thenReturn(normalUnitEntity);
        when(normalUnitEntity.isSmallCraft()).thenReturn(false);
        when(normalUnitEntity.isWarShip()).thenReturn(false);
        when(normalUnitEntity.isJumpShip()).thenReturn(false);
        when(normalUnitEntity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(nullUnit, personWithNullUnitEntity, personWithWarShipUnit, normalPerson));

        // person with null unit, person with unit with null entity, person with normal unit
        assertEquals(3, invokeGetPassengerCount());
    }

    @Test
    void countsSmallCraftCorrectly() throws Exception {
        Person nullUnit = mock(Person.class);
        when(nullUnit.getUnit()).thenReturn(null);

        Person personWithNullUnitEntity = mock(Person.class);
        Unit unitWithNullEntity = mock(Unit.class);
        when(personWithNullUnitEntity.getUnit()).thenReturn(unitWithNullEntity);
        when(unitWithNullEntity.getEntity()).thenReturn(null);

        Person personWithSmallCraftUnit = mock(Person.class);
        Unit smallCraftUnit = mock(Unit.class);
        Entity smallCraftEntity = mock(Entity.class);
        when(personWithSmallCraftUnit.getUnit()).thenReturn(smallCraftUnit);
        when(smallCraftUnit.getEntity()).thenReturn(smallCraftEntity);
        when(smallCraftEntity.isSmallCraft()).thenReturn(true);
        when(smallCraftEntity.isWarShip()).thenReturn(false);
        when(smallCraftEntity.isJumpShip()).thenReturn(false);
        when(smallCraftEntity.isDropShip()).thenReturn(false);

        Person normalPerson = mock(Person.class);
        Unit normalUnit = mock(Unit.class);
        Entity normalUnitEntity = mock(Entity.class);
        when(normalPerson.getUnit()).thenReturn(normalUnit);
        when(normalUnit.getEntity()).thenReturn(normalUnitEntity);
        when(normalUnitEntity.isSmallCraft()).thenReturn(false);
        when(normalUnitEntity.isWarShip()).thenReturn(false);
        when(normalUnitEntity.isJumpShip()).thenReturn(false);
        when(normalUnitEntity.isDropShip()).thenReturn(false);

        setAllPersonnel(List.of(nullUnit, personWithNullUnitEntity, personWithSmallCraftUnit, normalPerson));

        // person with null unit, person with unit with null entity, person with normal unit
        assertEquals(3, invokeGetPassengerCount());
    }

    private int invokeGetPassengerCount() throws Exception {
        Method getPassengerCount = transportCostCalculations.getClass().getDeclaredMethod("getPassengerCount");
        getPassengerCount.setAccessible(true);
        return (int) getPassengerCount.invoke(transportCostCalculations);
    }

    private void setAllPersonnel(List<Person> people) throws Exception {
        Field allPersonnel = transportCostCalculations.getClass().getDeclaredField("allPersonnel"); // rename if needed
        allPersonnel.setAccessible(true);
        allPersonnel.set(transportCostCalculations, people);
    }

    @Test
    void returnsZero_whenDriveCoreIsNone() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(DRIVE_CORE_NONE);
        when(station.canJump()).thenReturn(true); // irrelevant due to drive core none

        assertEquals(0, invokeGetAdditionalCollarNeeds(station));
        verify(station, never()).getTonnage();
    }

    @Test
    void returnsZero_whenCannotJump() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(123); // anything but DRIVE_CORE_NONE
        when(station.canJump()).thenReturn(false);

        assertEquals(0, invokeGetAdditionalCollarNeeds(station));
        verify(station, never()).getTonnage();
    }

    @Test
    void returnsCeilTonnageOverAdaptorDivider_whenHasKfAdapter() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(123);
        when(station.canJump()).thenReturn(true);
        when(station.getTonnage()).thenReturn(1000.0);
        when(station.hasKFAdapter()).thenReturn(true);
        when(station.isModular()).thenReturn(true); // adapter branch should win

        int expected = (int) Math.ceil(1000.0 / getAdaptorDivider());
        assertEquals(expected, invokeGetAdditionalCollarNeeds(station));
    }

    @Test
    void returnsCeilTonnageOverModularDivider_whenModularAndNoAdapter() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(123);
        when(station.canJump()).thenReturn(true);
        when(station.getTonnage()).thenReturn(1000.0);
        when(station.hasKFAdapter()).thenReturn(false);
        when(station.isModular()).thenReturn(true);

        int expected = (int) Math.ceil(1000.0 / getModularDivider());
        assertEquals(expected, invokeGetAdditionalCollarNeeds(station));
    }

    @Test
    void returnsZero_whenNeitherAdapterNorModular() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(123);
        when(station.canJump()).thenReturn(true);
        when(station.getTonnage()).thenReturn(1000.0);
        when(station.hasKFAdapter()).thenReturn(false);
        when(station.isModular()).thenReturn(false);

        assertEquals(0, invokeGetAdditionalCollarNeeds(station));
    }

    @Test
    void adaptorBranch_roundingUsesCeil() throws Exception {
        SpaceStation station = mock(SpaceStation.class);
        when(station.getDriveCoreType()).thenReturn(123);
        when(station.canJump()).thenReturn(true);
        when(station.hasKFAdapter()).thenReturn(true);
        when(station.isModular()).thenReturn(false);

        double divider = getAdaptorDivider();

        when(station.getTonnage()).thenReturn(divider); // exact multiple => 1
        assertEquals(1, invokeGetAdditionalCollarNeeds(station));

        when(station.getTonnage()).thenReturn(divider + 0.0001); // just over => 2
        assertEquals(2, invokeGetAdditionalCollarNeeds(station));
    }

    private static int invokeGetAdditionalCollarNeeds(SpaceStation station) throws Exception {
        Method getAdditionalCollarNeeds = TransportCostCalculations.class.getDeclaredMethod("getAdditionalCollarNeeds",
              SpaceStation.class);
        getAdditionalCollarNeeds.setAccessible(true);
        return (int) getAdditionalCollarNeeds.invoke(null, station);
    }

    private static double getAdaptorDivider() throws Exception {
        return (double) TransportCostCalculations.class.getDeclaredField("SPACE_STATION_ADAPTOR_COLLAR_NEED_DIVIDER")
                              .get(null);
    }

    private static double getModularDivider() throws Exception {
        return (double) TransportCostCalculations.class.getDeclaredField("SPACE_STATION_MODULAR_COLLAR_NEED_DIVIDER")
                              .get(null);
    }
}
