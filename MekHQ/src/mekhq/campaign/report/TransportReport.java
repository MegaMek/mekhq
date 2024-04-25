/*
 * TransportReport.java
 *
 * Copyright (c) 2013 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.report;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;

/**
 * @author Jay Lawson
 */
public class TransportReport extends AbstractReport {
    //region Constructors
    public TransportReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public String getTransportDetails() {
        HangarStatistics stats = getCampaign().getHangarStatistics();

        int noMech = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_MECH) - stats.getOccupiedBays(Entity.ETYPE_MECH), 0);
        int noDS = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP) - stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), 0);
        int noSC = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT) - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        @SuppressWarnings("unused") // FIXME: What type of bays do ConvFighters use?
        int noCF = Math
                .max(stats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER) - stats.getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACEFIGHTER) - stats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER), 0);
        int nolv = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true) - stats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_TANK) - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noinf = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        @SuppressWarnings("unused") // FIXME: This should be used somewhere...
        int noProto = Math.max(stats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH) - stats.getOccupiedBays(Entity.ETYPE_PROTOMECH),
                0);
        int freehv = Math.max(stats.getTotalHeavyVehicleBays() - stats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeinf = Math.max(stats.getTotalInfantryBays() - stats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int freeba = Math.max(stats.getTotalBattleArmorBays() - stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int freeSC = Math.max(stats.getTotalSmallCraftBays() - stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        int mothballedAsCargo = Math.max(stats.getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED), 0);

        String asfAppend = "";
        int newNoASF = Math.max(noASF - freeSC, 0);
        int placedASF = Math.max(noASF - newNoASF, 0);
        if ((noASF > 0) && (freeSC > 0)) {
            asfAppend = " [" + placedASF + " ASF will be placed in Small Craft bays]";
            freeSC -= placedASF;
        }

        String lvAppend = "";
        int newNolv = Math.max(nolv - freehv, 0);
        int placedlv = Math.max(nolv - newNolv, 0);
        if ((nolv > 0) && (freehv > 0)) {
            lvAppend = " [" + placedlv + " Light Vehicles will be placed in Heavy Vehicle bays]";
            freehv -= placedlv;
        }

        if ((noBA > 0) && (freeinf > 0)) {

        }

        if ((noinf > 0) && (freeba > 0)) {

        }

        final StringBuilder sb = new StringBuilder("Transports\n\n");

        // Lets do Mechs first.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Mech Bays (Occupied):",
                stats.getTotalMechBays(), stats.getOccupiedBays(Entity.ETYPE_MECH), "Mechs Not Transported:", noMech));

        // Lets do ASF next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d%s\n", "ASF Bays (Occupied):",
                stats.getTotalASFBays(), stats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER), "ASF Not Transported:", noASF, asfAppend));

        // Lets do Light Vehicles next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d%s\n", "Light Vehicle Bays (Occupied):",
                stats.getTotalLightVehicleBays(), stats.getOccupiedBays(Entity.ETYPE_TANK, true), "Light Vehicles Not Transported:",
                nolv, lvAppend));

        // Lets do Heavy Vehicles next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Heavy Vehicle Bays (Occupied):",
                stats.getTotalHeavyVehicleBays(), stats.getOccupiedBays(Entity.ETYPE_TANK), "Heavy Vehicles Not Transported:",
                nohv));

        if (noASF > 0 && freeSC > 0) {
            // Lets do ASF in Free Small Craft Bays next.
            sb.append(String.format("%-35s   %4d (%4d)      %-35s     %4d\n", "   Light Vehicles in Heavy Vehicle Bays (Occupied):",
                    stats.getTotalHeavyVehicleBays(), stats.getOccupiedBays(Entity.ETYPE_TANK) + placedlv,
                    "Light Vehicles Not Transported:", newNolv));
        }

        if ((nolv > 0) && (freehv > 0)) {

        }

        // Lets do Infantry next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Infantry Bays (Occupied):",
                stats.getTotalInfantryBays(), stats.getOccupiedBays(Entity.ETYPE_INFANTRY), "Infantry Not Transported:", noinf));

        if ((noBA > 0) && (freeinf > 0)) {

        }

        // Lets do Battle Armor next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Battle Armor Bays (Occupied):",
                stats.getTotalBattleArmorBays(), stats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), "Battle Armor Not Transported:",
                noBA));

        if ((noinf > 0) && (freeba > 0)) {

        }

        // Lets do Small Craft next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Small Craft Bays (Occupied):",
                stats.getTotalSmallCraftBays(), stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), "Small Craft Not Transported:",
                noSC));

        if ((noASF > 0) && (freeSC > 0)) {
            // Lets do ASF in Free Small Craft Bays next.
            sb.append(String.format("%-35s   %4d (%4d)      %-35s     %4d\n", "   ASF in Small Craft Bays (Occupied):",
                    stats.getTotalSmallCraftBays(), stats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) + placedASF,
                    "ASF Not Transported:", newNoASF));
        }

        // Lets do ProtoMechs next.
        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "ProtoMech Bays (Occupied):",
                stats.getTotalProtomechBays(), stats.getOccupiedBays(Entity.ETYPE_PROTOMECH), "ProtoMechs Not Transported:", noSC));

        sb.append("\n\n");

        sb.append(String.format("%-35s      %4d (%4d)      %-35s     %4d\n", "Docking Collars (Occupied):",
                stats.getTotalDockingCollars(), stats.getOccupiedBays(Entity.ETYPE_DROPSHIP), "DropShips Not Transported:", noDS));

        sb.append("\n\n");

        sb.append(String.format("%-35s      %4d\n", "Mothballed Units (see Cargo report)", mothballedAsCargo));

        return sb.toString();
    }
}
