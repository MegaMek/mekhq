/*
 * Copyright (c) 2013 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.report;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;

/**
 * @author Jay Lawson
 */
public class TransportReport extends AbstractReport {
    private static final String CAPACITY_ROW_FORMAT = "%-36s      %4d (%4d)      %-37s     %4d\n";
    private static final String DETAIL_ROW_FORMAT = "%-36s      %4d\n";

    //region Constructors
    public TransportReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public String getTransportDetails() {
        HangarStatistics stats = getCampaign().getHangarStatistics();

        int noMek = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_MEK) - stats.getOccupiedBays(Entity.ETYPE_MEK),
              0);
        int noDS = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP) -
                                  stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT) -
                                  stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noASF = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER) -
                                   stats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER), 0);
        int nolv = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true) -
                                  stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_TANK) - stats.getOccupiedBays(Entity.ETYPE_TANK),
              0);
        int nosh = Math.max(stats.getNumberOfSuperHeavyVehicles() - stats.getOccupiedSuperHeavyVehicleBays(), 0);
        int noinf = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) -
                                   stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) -
                                  stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK) -
                                     stats.getOccupiedBays(Entity.ETYPE_PROTOMEK),
              0);
        int freehv = Math.max(stats.getTotalHeavyVehicleBays() - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freesh = Math.max(stats.getTotalSuperHeavyVehicleBays() - stats.getOccupiedSuperHeavyVehicleBays(), 0);
        int freeSC = Math.max(stats.getTotalSmallCraftBays() - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int mothballedAsCargo = Math.max(stats.getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED), 0);

        int newNoASF = Math.max(noASF - freeSC, 0);
        int placedASF = Math.max(noASF - newNoASF, 0);
        if ((noASF > 0) && (freeSC > 0)) {
            freeSC -= placedASF;
        }

        int newNolv = Math.max(nolv - freehv, 0);
        int placedlv = Math.max(nolv - newNolv, 0);
        if ((nolv > 0) && (freehv > 0)) {
            freehv -= placedlv;
        }
            int occupiedHeavyVehicleBays = stats.getOccupiedBays(Entity.ETYPE_TANK) + placedlv;

        // Heavy Vehicles overflow into free Super Heavy Vehicle bays (SH bays accommodate any vehicle weight class).
        int newNohv = Math.max(nohv - freesh, 0);
        int placedhv = Math.max(nohv - newNohv, 0);
        if ((nohv > 0) && (freesh > 0)) {
            freesh -= placedhv;
        }

        // Light Vehicles still un-transported can also overflow into any remaining Super Heavy bays.
        int placedLvInSh = 0;
        if ((newNolv > 0) && (freesh > 0)) {
            placedLvInSh = Math.min(newNolv, freesh);
            newNolv -= placedLvInSh;
            freesh -= placedLvInSh;
        }
        int occupiedSuperHeavyVehicleBays = stats.getOccupiedSuperHeavyVehicleBays()
              + placedhv + placedLvInSh;

        final StringBuilder sb = new StringBuilder("Transports\n\n");

        // Lets do Meks first.
        sb.append(String.format(CAPACITY_ROW_FORMAT, "Mek Bays (Occupied):",
              stats.getTotalMekBays(), stats.getOccupiedBays(Entity.ETYPE_MEK), "Meks Not Transported:", noMek));

        // Let's do Fighters next (ASF, Conventional Fighters, Fixed-Wing Support all use Fighter bays).
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Fighter Bays (Occupied):",
              stats.getTotalASFBays(),
              stats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER),
              "Fighters Not Transported:",
              newNoASF));

        // Let's do Light Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Light Vehicle Bays (Occupied):",
              stats.getTotalLightVehicleBays(),
              stats.getOccupiedBays(Entity.ETYPE_TANK, true),
              "Light Vehicles Not Transported:",
              newNolv));

        // Let's do Heavy Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Heavy Vehicle Bays (Occupied):",
              stats.getTotalHeavyVehicleBays(),
              occupiedHeavyVehicleBays,
              "Heavy Vehicles Not Transported:",
              newNohv));

        if ((nolv > 0) && (placedlv > 0)) {
            // Light Vehicles placed in free Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  "   Light in Heavy Vehicle Bays:",
                  placedlv));
        }

        // Let's do Super Heavy Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Super Heavy Vehicle Bays (Occupied):",
              stats.getTotalSuperHeavyVehicleBays(),
              occupiedSuperHeavyVehicleBays,
              "Super Heavy Vehicles Not Transported:",
              nosh));

        if ((nohv > 0) && (placedhv > 0)) {
            // Heavy Vehicles placed in free Super Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  "   Heavy in Super Heavy Bays:",
                  placedhv));
        }

        if (placedLvInSh > 0) {
            // Light Vehicles placed in free Super Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  "   Light in Super Heavy Bays:",
                  placedLvInSh));
        }

        // Let's do Infantry next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Infantry Bays (Occupied):",
              stats.getTotalInfantryBays(),
              stats.getOccupiedBays(Entity.ETYPE_INFANTRY),
              "Infantry Not Transported:",
              noinf));

        // Let's do Battle Armor next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Battle Armor Bays (Occupied):",
              stats.getTotalBattleArmorBays(),
              stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR),
              "Battle Armor Not Transported:",
              noBA));

        // Let's do Small Craft next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Small Craft Bays (Occupied):",
              stats.getTotalSmallCraftBays(),
              stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT),
              "Small Craft Not Transported:",
              noSC));

        if ((noASF > 0) && (freeSC > 0)) {
            // Let's do ASF in Free Small Craft Bays next.
            sb.append(String.format(CAPACITY_ROW_FORMAT, "   Fighters in Small Craft Bays (Occupied):",
                  stats.getTotalSmallCraftBays(), stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) + placedASF,
                  "Fighters Not Transported:", newNoASF));
        }

        // Let's do ProtoMeks next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "ProtoMek Bays (Occupied):",
              stats.getTotalProtoMekBays(),
              stats.getOccupiedBays(Entity.ETYPE_PROTOMEK),
              "ProtoMeks Not Transported:",
              noProto));

        sb.append("\n\n");

        sb.append(String.format(CAPACITY_ROW_FORMAT,
              "Docking Collars (Occupied):",
              stats.getTotalDockingCollars(),
              stats.getOccupiedBays(Entity.ETYPE_DROPSHIP),
              "DropShips Not Transported:",
              noDS));

        sb.append("\n\n");

        sb.append(String.format(DETAIL_ROW_FORMAT, "Mothballed Units (see Cargo report)", mothballedAsCargo));

        return sb.toString();
    }
}
