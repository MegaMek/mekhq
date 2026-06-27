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
    private static final String DETAIL_ROW_INDENT = "   ";
    private static final String HEADER_ROW_FORMAT = "%-59s%s\n\n";
    private static final String CAPACITY_ROW_FORMAT = "%-36s      %4d (%4d)      %-37s     %4d (%4d)\n";
    private static final String DETAIL_ROW_FORMAT = "%-36s      %4d\n";

    //region Constructors
    public TransportReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public String getTransportDetails() {
        HangarStatistics stats = getCampaign().getHangarStatistics();

        int totalMek = stats.getNumberOfUnitsByType(Entity.ETYPE_MEK);
        int totalDS = stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int totalSC = stats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int totalASF = stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER);
        int totalLV = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true);
        int totalHV = stats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int totalSH = stats.getNumberOfSuperHeavyVehicles();
        int totalInf = stats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
        int totalBA = stats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int totalProto = stats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK);

        int noMek = Math.max(totalMek - stats.getOccupiedBays(Entity.ETYPE_MEK), 0);
        int noDS = Math.max(totalDS - stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(totalSC - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int noASF = Math.max(totalASF - stats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER), 0);
        int nolv = Math.max(totalLV - stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(totalHV - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int nosh = Math.max(totalSH - stats.getOccupiedSuperHeavyVehicleBays(), 0);
        int noinf = Math.max(totalInf - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(totalBA - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(totalProto - stats.getOccupiedBays(Entity.ETYPE_PROTOMEK), 0);
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
        int occupiedSmallCraftBays = stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) + placedASF;

        final StringBuilder sb = new StringBuilder(String.format(HEADER_ROW_FORMAT,
              resources.getString("TransportReport.CapacityHeader.text"),
              resources.getString("TransportReport.UnitsHeader.text")));

        // Lets do Meks first.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.MekBays.text"),
              stats.getTotalMekBays(),
              stats.getOccupiedBays(Entity.ETYPE_MEK),
              resources.getString("TransportReport.Meks.text"),
              totalMek,
              noMek));

        // Let's do Fighters next (ASF, Conventional Fighters, Fixed-Wing Support all use Fighter bays).
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.FighterBays.text"),
              stats.getTotalASFBays(),
              stats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER),
              resources.getString("TransportReport.Fighters.text"),
              totalASF,
              newNoASF));

        // Let's do Light Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.LightVehicleBays.text"),
              stats.getTotalLightVehicleBays(),
              stats.getOccupiedBays(Entity.ETYPE_TANK, true),
              resources.getString("TransportReport.LightVehicles.text"),
              totalLV,
              newNolv));

        // Let's do Heavy Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.HeavyVehicleBays.text"),
              stats.getTotalHeavyVehicleBays(),
              occupiedHeavyVehicleBays,
              resources.getString("TransportReport.HeavyVehicles.text"),
              totalHV,
              newNohv));

        if ((nolv > 0) && (placedlv > 0)) {
            // Light Vehicles placed in free Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  DETAIL_ROW_INDENT + resources.getString("TransportReport.LightInHeavyVehicleBays.text"),
                  placedlv));
        }

        // Let's do Super Heavy Vehicles next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.SuperHeavyVehicleBays.text"),
              stats.getTotalSuperHeavyVehicleBays(),
              occupiedSuperHeavyVehicleBays,
              resources.getString("TransportReport.SuperHeavyVehicles.text"),
              totalSH,
              nosh));

        if (placedLvInSh > 0) {
            // Light Vehicles placed in free Super Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  DETAIL_ROW_INDENT + resources.getString("TransportReport.LightInSuperHeavyBays.text"),
                  placedLvInSh));
        }

        if ((nohv > 0) && (placedhv > 0)) {
            // Heavy Vehicles placed in free Super Heavy Vehicle bays.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  DETAIL_ROW_INDENT + resources.getString("TransportReport.HeavyInSuperHeavyBays.text"),
                  placedhv));
        }

        // Let's do Infantry next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.InfantryBays.text"),
              stats.getTotalInfantryBays(),
              stats.getOccupiedBays(Entity.ETYPE_INFANTRY),
              resources.getString("TransportReport.Infantry.text"),
              totalInf,
              noinf));

        // Let's do Battle Armor next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.BattleArmorBays.text"),
              stats.getTotalBattleArmorBays(),
              stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR),
              resources.getString("TransportReport.BattleArmor.text"),
              totalBA,
              noBA));

        // Let's do Small Craft next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.SmallCraftBays.text"),
              stats.getTotalSmallCraftBays(),
              occupiedSmallCraftBays,
              resources.getString("TransportReport.SmallCraft.text"),
              totalSC,
              noSC));

        if (placedASF > 0) {
            // Let's do ASF in Free Small Craft Bays next.
            sb.append(String.format(DETAIL_ROW_FORMAT,
                  DETAIL_ROW_INDENT + resources.getString("TransportReport.FightersInSmallCraftBays.text"),
                  placedASF));
        }

        // Let's do ProtoMeks next.
        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.ProtoMekBays.text"),
              stats.getTotalProtoMekBays(),
              stats.getOccupiedBays(Entity.ETYPE_PROTOMEK),
              resources.getString("TransportReport.ProtoMeks.text"),
              totalProto,
              noProto));

        sb.append("\n\n");

        sb.append(String.format(CAPACITY_ROW_FORMAT,
              resources.getString("TransportReport.DockingCollars.text"),
              stats.getTotalDockingCollars(),
              stats.getOccupiedBays(Entity.ETYPE_DROPSHIP),
              resources.getString("TransportReport.DropShips.text"),
              totalDS,
              noDS));

        sb.append("\n\n");

        sb.append(String.format(DETAIL_ROW_FORMAT,
              resources.getString("TransportReport.MothballedUnits.text"),
              mothballedAsCargo));

        return sb.toString();
    }
}
