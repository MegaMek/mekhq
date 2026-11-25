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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.Engine;
import megamek.common.units.Aero;
import megamek.common.units.Tank;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Peacetime operating costs (Campaign Operations): spare parts, training ammunition and fuel for a single unit.
 */
class UnitPeacetimeCostTest {
    private static final double TOLERANCE = 0.01;
    private static final double HYDROGEN_PER_TON = 15000.0;
    private static final double PETROCHEMICALS_PER_TON = 1000.0;

    private PartsScenario scenario;

    @BeforeEach
    void beforeEach() {
        scenario = PartsScenario.create();
        // Quirks are covered by SparePartsQuirkMultiplierTest
        scenario.getCampaign().getCampaignOptions().set(CampaignOption.USE_QUIRKS, false);
    }

    private static void assertMoney(double expected, Money actual) {
        assertEquals(expected, actual.getAmount().doubleValue(), TOLERANCE);
    }

    // Spare parts

    @Test
    void aWarShipPaysTheFullRateNotTheJumpShipRate() {
        Unit warShip = scenario.withUnit(UnitFixture.ESSEX_II_WARSHIP);

        assertMoney(warShip.getEntity().getWeight() * 0.001 * 15000, warShip.getSparePartsCost());
    }

    @Test
    void aJumpShipPaysATenthOfTheRate() {
        Unit jumpShip = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);

        assertMoney(jumpShip.getEntity().getWeight() * 0.0001 * 15000, jumpShip.getSparePartsCost());
    }

    @Test
    void aSpaceStationPaysATenthOfTheRate() {
        Unit spaceStation = scenario.withUnit(UnitFixture.OLYMPUS_SPACE_STATION);

        assertMoney(spaceStation.getEntity().getWeight() * 0.0001 * 15000, spaceStation.getSparePartsCost());
    }

    @Test
    void aProtoMekPaysLikeAMek() {
        Unit protoMek = scenario.withUnit(UnitFixture.MINOTAUR_PROTOMEK);

        assertMoney(protoMek.getEntity().getWeight() * 0.001 * 10000, protoMek.getSparePartsCost());
        assertTrue(protoMek.getSparePartsCost().isPositive());
    }

    @Test
    void conventionalInfantryPayForTheirTransportCompartment() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);

        // A platoon's weight is its transport weight; 0.2% of it at 10,000 a ton
        assertMoney(platoon.getEntity().getWeight() * 0.002 * 10000, platoon.getSparePartsCost());
    }

    @Test
    void sparePartsPayTheUnitsAcquisitionMultiplier() {
        scenario.getCampaign().setLocalDate(LocalDate.of(3060, 1, 1));
        scenario.getCampaign().getCampaignOptions().set(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER, 5.0);
        Unit clanMek = scenario.withUnit(UnitFixture.HOPLITE_C);

        // CO p.24: spare parts are multiplied by the Equipment Availability & Cost Table multipliers (CO p.14)
        assertMoney(clanMek.getEntity().getWeight() * 0.001 * 10000 * 5.0, clanMek.getSparePartsCost());
    }

    // Fuel

    @Test
    void aDropShipBurnsFifteenBurnDaysOfItsStrategicFuelUse() {
        Unit dropShip = scenario.withUnit(UnitFixture.UNION_3055_DROPSHIP);
        double fuelUse = ((Aero) dropShip.getEntity()).getStrategicFuelUse();

        assertMoney(fuelUse * 15 * HYDROGEN_PER_TON, dropShip.getFuelCost());
    }

    @Test
    void aWarShipBurnsFifteenBurnDays() {
        Unit warShip = scenario.withUnit(UnitFixture.ESSEX_II_WARSHIP);
        double fuelUse = ((Aero) warShip.getEntity()).getStrategicFuelUse();

        assertEquals(fuelUse * 15, warShip.getMonthlyHydrogenUsage(), TOLERANCE);
    }

    @Test
    void aJumpShipBurnsThreeBurnDaysForStationKeeping() {
        Unit jumpShip = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        double fuelUse = ((Aero) jumpShip.getEntity()).getStrategicFuelUse();

        assertEquals(fuelUse * 3, jumpShip.getMonthlyHydrogenUsage(), TOLERANCE);
    }

    @Test
    void anAerospaceFighterBurnsFourTimesItsFuel() {
        Unit fighter = scenario.withUnit(UnitFixture.SHILONE_SL_17);
        double fuelTons = ((Aero) fighter.getEntity()).getFuelTonnage();

        assertEquals(fuelTons * 4, fighter.getMonthlyHydrogenUsage(), TOLERANCE);
        assertMoney(fuelTons * 4 * HYDROGEN_PER_TON, fighter.getFuelCost());
    }

    @Test
    void hydrogenFromThePoolOnlyCoversWhatIsAvailable() {
        Unit fighter = scenario.withUnit(UnitFixture.SHILONE_SL_17);
        double usage = fighter.getMonthlyHydrogenUsage();

        assertMoney(0, fighter.getFuelCost(usage));
        assertMoney(0, fighter.getFuelCost(usage * 10));
        assertMoney((usage / 2) * HYDROGEN_PER_TON, fighter.getFuelCost(usage / 2));
    }

    @Test
    void aFusionMekNeedsNoFuel() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        assertMoney(0, locust.getFuelCost());
        assertEquals(0, locust.getMonthlyHydrogenUsage(), TOLERANCE);
    }

    @Test
    void aCombustionVehicleBurnsFourTimesItsFuelInPetrochemicals() {
        Unit vedette = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        Tank tank = (Tank) vedette.getEntity();
        assertEquals(Engine.COMBUSTION_ENGINE, tank.getEngine().getEngineType());

        assertMoney(tank.getFuelTonnage() * 4 * PETROCHEMICALS_PER_TON, vedette.getFuelCost());
        assertEquals(0, vedette.getMonthlyHydrogenUsage(), TOLERANCE,
              "Petrochemicals never draw on the force's hydrogen");
    }

    @Test
    void footInfantryNeedNoFuel() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);

        assertMoney(0, platoon.getFuelCost());
    }

    // Ammunition

    @Test
    void trainingAmmunitionIsAQuarterOfFullBinsEvenWhenEmpty() {
        Unit apc = scenario.withUnit(UnitFixture.APC_HOVER_LRM);
        AmmoBin bin = findAmmoBin(apc);
        Money fullCost = apc.getAmmoCost();
        assertTrue(fullCost.isPositive());

        bin.setShotsNeeded(bin.getFullShots());

        assertEquals(fullCost, apc.getAmmoCost(), "An empty bin still trains with a full load");
    }

    @Test
    void trainingAmmunitionIsPricedAsStandardRounds() {
        Unit apc = scenario.withUnit(UnitFixture.APC_HOVER_LRM);
        AmmoBin bin = findAmmoBin(apc);
        Money standardValue = bin.getStandardMunitionCapacityValue();

        AmmoType standardType = bin.getType();
        AmmoType thunderType = findThunderMunition(standardType);
        AmmoMounted mounted = (AmmoMounted) apc.getEntity().getEquipment(bin.getEquipmentNum());
        mounted.changeAmmoType(thunderType);
        assertTrue(mounted.getType().getMunitionType().contains(Munitions.M_THUNDER));

        assertEquals(standardValue, bin.getStandardMunitionCapacityValue());
    }

    private static AmmoBin findAmmoBin(Unit unit) {
        for (Part part : unit.getParts()) {
            if (part instanceof AmmoBin ammoBin) {
                return ammoBin;
            }
        }
        throw new AssertionError("No ammunition bin on " + unit.getName());
    }

    private static AmmoType findThunderMunition(AmmoType standardType) {
        for (AmmoType munition : AmmoType.getMunitionsFor(standardType.getAmmoType())) {
            AmmoType baseAmmo = munition.getBaseAmmo();
            boolean isThunder = munition.getMunitionType().contains(Munitions.M_THUNDER);
            if (isThunder && (baseAmmo != null)
                      && baseAmmo.getInternalName().equals(standardType.getInternalName())) {
                return munition;
            }
        }
        throw new AssertionError("No Thunder munition for " + standardType.getInternalName());
    }
}
