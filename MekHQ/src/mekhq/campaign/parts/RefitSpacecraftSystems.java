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

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.kfs.KFChargingSystem;
import mekhq.campaign.parts.kfs.KFDriveCoil;
import mekhq.campaign.parts.kfs.KFDriveController;
import mekhq.campaign.parts.kfs.KFFieldInitiator;
import mekhq.campaign.parts.kfs.KFHeliumTank;
import mekhq.campaign.parts.missing.MissingAeroLifeSupport;
import mekhq.campaign.parts.missing.MissingDropshipDockingCollar;
import mekhq.campaign.parts.missing.MissingFireControlSystem;
import mekhq.campaign.parts.missing.MissingJumpshipDockingCollar;
import mekhq.campaign.parts.missing.MissingKFBoom;
import mekhq.campaign.parts.missing.MissingKFChargingSystem;
import mekhq.campaign.parts.missing.MissingKFDriveCoil;
import mekhq.campaign.parts.missing.MissingKFDriveController;
import mekhq.campaign.parts.missing.MissingKFFieldInitiator;
import mekhq.campaign.parts.missing.MissingKFHeliumTank;
import mekhq.campaign.parts.missing.MissingLFBattery;
import mekhq.campaign.unit.Unit;

/**
 * The ship-wide systems a spacecraft keeps through a refit: the K-F drive, the fire control system, the combat
 * information center and life support. Their prices follow the ship's design (docking collars, a lithium-fusion
 * battery, the arcs that carry guns, the crew), and the parts are otherwise matched by that price, so any such change
 * used to throw the old system out as a spare and buy a new one. A refit keeps them instead and charges only the
 * difference in price. A change of K-F drive core type is a new drive.
 */
final class RefitSpacecraftSystems {
    /** The missing parts that stand for a ship component a refit fits new. */
    private static final List<Class<? extends Part>> NEW_SHIP_COMPONENT_TYPES = List.of(
          MissingDropshipDockingCollar.class,
          MissingJumpshipDockingCollar.class,
          MissingKFBoom.class,
          MissingLFBattery.class,
          MissingKFDriveCoil.class,
          MissingKFDriveController.class,
          MissingKFFieldInitiator.class,
          MissingKFChargingSystem.class,
          MissingKFHeliumTank.class,
          MissingFireControlSystem.class,
          MissingAeroLifeSupport.class);

    private RefitSpacecraftSystems() {}

    /**
     * @param oldPart a part of the ship as it is
     * @param newPart a part of the design it is being refitted to
     *
     * @return {@code true} if both are the same ship-wide system, which the ship keeps through the refit
     */
    static boolean isSameSystem(Part oldPart, Part newPart) {
        if (oldPart instanceof FireControlSystem) {
            return newPart instanceof FireControlSystem;
        }
        if (oldPart instanceof CombatInformationCenter) {
            return newPart instanceof CombatInformationCenter;
        }
        return switch (oldPart) {
            case AeroLifeSupport oldLifeSupport -> (newPart instanceof AeroLifeSupport newLifeSupport)
                  && (oldLifeSupport.isForFighter() == newLifeSupport.isForFighter());
            case KFDriveCoil oldCoil -> (newPart instanceof KFDriveCoil newCoil)
                  && (oldCoil.getCoreType() == newCoil.getCoreType());
            case KFDriveController oldController -> (newPart instanceof KFDriveController newController)
                  && (oldController.getCoreType() == newController.getCoreType());
            case KFFieldInitiator oldInitiator -> (newPart instanceof KFFieldInitiator newInitiator)
                  && (oldInitiator.getCoreType() == newInitiator.getCoreType());
            case KFChargingSystem oldChargingSystem -> (newPart instanceof KFChargingSystem newChargingSystem)
                  && (oldChargingSystem.getCoreType() == newChargingSystem.getCoreType());
            case KFHeliumTank oldHeliumTank -> (newPart instanceof KFHeliumTank newHeliumTank)
                  && (oldHeliumTank.getCoreType() == newHeliumTank.getCoreType());
            case LFBattery oldBattery -> (newPart instanceof LFBattery newBattery)
                  && (oldBattery.getCoreType() == newBattery.getCoreType());
            default -> false;
        };
    }

    /**
     * A ship component the refit fits where nothing was taken out is a Class C refit (CO p. 211): a docking collar or
     * K-F boom, a lithium-fusion battery, or a whole new drive, fire control or life support system.
     *
     * @param newPart a part the new design needs and the ship does not have
     *
     * @return {@code true} if it is a new ship component
     */
    static boolean isNewShipComponent(Part newPart) {
        for (Class<? extends Part> shipComponentType : NEW_SHIP_COMPONENT_TYPES) {
            if (shipComponentType.isInstance(newPart)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param keptPart the ship's system, priced for the ship as it is
     * @param newPart  the same system, priced for the new design
     *
     * @return what the refit pays to bring the kept system up to the new design, never less than zero. Both prices are
     *       those of a new part, so the kept system's wear does not lower the charge.
     */
    static Money upgradeCost(Part keptPart, Part newPart) {
        Money newDesignPrice = newPart.adjustCostsForCampaignOptions(newPart.getStickerPrice(), true);
        Money keptSystemPrice = newPart.adjustCostsForCampaignOptions(keptPart.getStickerPrice(), true);
        Money difference = newDesignPrice.minus(keptSystemPrice);
        return difference.isPositive() ? difference : Money.zero();
    }

    /**
     * Brings the ship-wide systems up to date with the unit's new design once a refit completes: the prices of fire
     * control, the combat information center and life support, and the docking collars the K-F drive supports.
     *
     * @param unit the refitted unit, already holding its new design
     */
    static void refreshKeptSystems(Unit unit) {
        Entity entity = unit.getEntity();
        int docks = entity.getDocks();
        for (Part part : unit.getParts()) {
            switch (part) {
                case FireControlSystem fireControlSystem -> fireControlSystem.calculateCost();
                case CombatInformationCenter combatInformationCenter -> combatInformationCenter.calculateCost();
                case AeroLifeSupport lifeSupport -> lifeSupport.calculateCost();
                case KFDriveCoil coil -> coil.setDocks(docks);
                case KFDriveController controller -> controller.setDocks(docks);
                case KFFieldInitiator initiator -> initiator.setDocks(docks);
                case KFChargingSystem chargingSystem -> chargingSystem.setDocks(docks);
                case KFHeliumTank heliumTank -> heliumTank.setDocks(docks);
                case LFBattery battery -> battery.setDocks(docks);
                default -> {
                }
            }
        }
    }
}
