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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testUtilities.parts.RefitKitPricing.componentPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.bays.CrewQuartersCargoBay;
import megamek.common.equipment.DockingCollar;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.kfs.KFChargingSystem;
import mekhq.campaign.parts.kfs.KFDriveCoil;
import mekhq.campaign.parts.kfs.KFDriveController;
import mekhq.campaign.parts.kfs.KFFieldInitiator;
import mekhq.campaign.parts.kfs.KFHeliumTank;
import mekhq.campaign.parts.missing.MissingAeroLifeSupport;
import mekhq.campaign.parts.missing.MissingFireControlSystem;
import mekhq.campaign.parts.missing.MissingJumpshipDockingCollar;
import mekhq.campaign.parts.missing.MissingLFBattery;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A spacecraft keeps its K-F drive, fire control and life support through a refit that only changes their size or
 * value: more docking collars, a lithium-fusion battery, guns in more arcs, a bigger crew. The refit pays the difference
 * in price instead of buying a new system and leaving the old one as a spare (issue #10204). These are custom jobs,
 * since refit kits are not available for spacecraft.
 */
class RefitSpacecraftSystemsTest {
    private static final List<Class<? extends Part>> DRIVE_PARTS = List.of(KFDriveCoil.class,
          KFDriveController.class, KFFieldInitiator.class, KFChargingSystem.class, KFHeliumTank.class);

    private final PartsScenario scenario = PartsScenario.create();

    private void complete(Refit refit) throws Exception {
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    private static List<Part> driveParts(Unit unit) {
        List<Part> driveParts = new ArrayList<>();
        for (Class<? extends Part> drivePartType : DRIVE_PARTS) {
            driveParts.addAll(PartsScenario.unitParts(unit, drivePartType));
        }
        return driveParts;
    }

    private static Money listPriceOf(List<Part> parts) {
        Money value = Money.zero();
        for (Part part : parts) {
            value = value.plus(part.getStickerPrice());
        }
        return value;
    }

    private static long countOf(Refit refit, Class<? extends Part> partType) {
        long count = 0;
        for (Part part : refit.getShoppingList()) {
            if (partType.isInstance(part)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSpareNamed(String prefix) {
        for (String spareName : PartsCensus.ofWarehouseStock(scenario.getWarehouse()).keySet()) {
            if (spareName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void anUnchangedJumpShipCostsNothing() throws Exception {
        Unit invader = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        Refit refit = new Refit(invader, UnitFixture.INVADER_JUMPSHIP.loadEntity(), true, false, false);

        assertEquals(Refit.NO_CHANGE, refit.getRefitClass());
        assertEquals(Money.zero(), refit.getCost());
    }

    @Test
    void anAddedDockingCollarKeepsTheDriveAndPaysForTheDifference() throws Exception {
        Unit invader = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        Money driveValueBefore = listPriceOf(driveParts(invader));
        Entity withFourCollars = UnitFixture.INVADER_JUMPSHIP.loadEntity();
        withFourCollars.addTransporter(new DockingCollar(3));
        Refit refit = new Refit(invader, withFourCollars, true, false, false);

        assertEquals(1, countOf(refit, MissingJumpshipDockingCollar.class));
        assertEquals(1, refit.getShoppingList().size(), "Only the collar is bought, not a new drive");
        assertEquals(Refit.CLASS_C, refit.getRefitClass(), "A collar is added where nothing was taken out");
        Money collarPrice = componentPrice(refit);
        Money priceBeforeCompletion = refit.getCost();

        complete(refit);

        Money driveValueAfter = listPriceOf(driveParts(invader));
        assertEquals(collarPrice.plus(driveValueAfter.minus(driveValueBefore)), priceBeforeCompletion,
              "The refit pays for the collar and the rise in the drive's value");
        assertFalse(hasSpareNamed("K-F"), "The old drive is not left behind as a spare");
        for (Part drivePart : driveParts(invader)) {
            assertEquals(4, docksOf(drivePart), drivePart.getName() + " now supports four collars");
        }
    }

    @Test
    void anAddedLithiumFusionBatteryPaysForTheDearerDrive() throws Exception {
        Unit invader = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        Money driveValueBefore = listPriceOf(driveParts(invader));
        Entity withBattery = UnitFixture.INVADER_JUMPSHIP.loadEntity();
        ((Jumpship) withBattery).setLF(true);
        Refit refit = new Refit(invader, withBattery, true, false, false);

        assertEquals(1, countOf(refit, MissingLFBattery.class));
        assertEquals(1, refit.getShoppingList().size());
        assertEquals(Refit.CLASS_C, refit.getRefitClass());
        Money priceBeforeCompletion = refit.getCost();

        complete(refit);

        Money driveValueAfter = listPriceOf(driveParts(invader));
        assertEquals(driveValueBefore.multipliedBy(3), driveValueAfter, "A battery triples the drive's value");
        assertEquals(driveValueAfter.minus(driveValueBefore), priceBeforeCompletion,
              "The refit pays for the rise in the drive's value");
    }

    @Test
    void aBiggerCrewKeepsTheLifeSupport() throws Exception {
        Unit invader = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        Entity withBiggerCrew = UnitFixture.INVADER_JUMPSHIP.loadEntity();
        withBiggerCrew.addTransporter(new CrewQuartersCargoBay(10));
        withBiggerCrew.setNCrew(withBiggerCrew.getNCrew() + 10);
        Refit refit = new Refit(invader, withBiggerCrew, true, false, false);

        assertEquals(0, countOf(refit, MissingAeroLifeSupport.class));
        assertEquals(Money.of(5000 * 10), refit.getCost(), "Life support costs 5,000 per crew member (SO)");

        complete(refit);

        assertFalse(hasSpareNamed("Spacecraft Life Support"));
        AeroLifeSupport lifeSupport = PartsScenario.unitParts(invader, AeroLifeSupport.class).getFirst();
        int peopleAboard = invader.getEntity().getNCrew() + invader.getEntity().getNPassenger();
        assertEquals(Money.of(5000.0 * peopleAboard), lifeSupport.getStickerPrice());
    }

    @Test
    void gunsInMoreArcsKeepTheFireControlSystem() throws Exception {
        // The Leopard carries guns in four arcs, the pirate Leopard PA in all six
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        Refit refit = new Refit(leopard, UnitFixture.LEOPARD_PA_DROPSHIP.loadEntity(), true, false, false);

        assertEquals(0, countOf(refit, MissingFireControlSystem.class));
        assertEquals(0, countOf(refit, MissingAeroLifeSupport.class));

        complete(refit);

        Map<String, Integer> spares = PartsCensus.ofWarehouseStock(scenario.getWarehouse());
        assertFalse(spares.containsKey("Fire Control System"), "The old fire control is not a spare: " + spares);
        assertFalse(spares.containsKey("Spacecraft Life Support"), "Nor is the old life support: " + spares);
        FireControlSystem fireControl = PartsScenario.unitParts(leopard, FireControlSystem.class).getFirst();
        assertEquals(Money.of(100000 + (10000 * 6)), fireControl.getStickerPrice(), "Fire control for six arcs");
    }

    private static int docksOf(Part drivePart) {
        return switch (drivePart) {
            case KFDriveCoil coil -> coil.getDocks();
            case KFDriveController controller -> controller.getDocks();
            case KFFieldInitiator initiator -> initiator.getDocks();
            case KFChargingSystem chargingSystem -> chargingSystem.getDocks();
            case KFHeliumTank heliumTank -> heliumTank.getDocks();
            default -> throw new IllegalArgumentException(drivePart.getName());
        };
    }
}
