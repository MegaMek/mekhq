/*
 * Copyright (c) 2020 The MegaMek Team.
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
package mekhq.campaign;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.UnitType;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * calculates and stores summary information on a campaign for use in reporting, mostly for the command center
 */
public class CampaignSummary {

    Campaign campaign;

    //unit totals
    private int mechCount;
    private int veeCount;
    private int aeroCount;
    private int infantryCount;
    private int totalUnitCount;

    //unit damage status
    private int[] countDamageStatus;

    //personnel totals
    private int totalCombatPersonnel;
    private int totalSupportPersonnel;
    private int totalInjuries;

    //mission status
    private int[] countMissionByStatus;
    private int completedMissions;

    //cargo
    private double cargoCapacity;
    private double cargoTons;

    //transport capacity
    private int unitsOver;
    private int unitsTransported;
    private int nDS;

    /**
     *
     * @param c a {@link Campaign} for which a summary is desired
     */
    public CampaignSummary(Campaign c) {
        this.campaign = c;
        updateInformation();
    }

    /**
     * This will update all of the values in CampaignSummary to the latest from the campaign. It should
     * be run before pulling out any reports
     */
    public void updateInformation() {

        //personnel
        totalCombatPersonnel = 0;
        totalSupportPersonnel = 0;
        totalInjuries = 0;
        for (Person p : campaign.getActivePersonnel()) {
            // Add them to the total count
            if (!p.getPrisonerStatus().isFree()) {
                continue;
            }
            if (p.getHits() > 0) {
                totalInjuries++;
            }
            if (p.hasPrimaryCombatRole()) {
                totalCombatPersonnel++;
            } else if (p.hasPrimarySupportRole(false)) {
                totalSupportPersonnel++;
            }
        }

        //units
        countDamageStatus = new int[Entity.DMG_CRIPPLED + 1];
        mechCount = 0;
        veeCount = 0;
        aeroCount = 0;
        infantryCount = 0;
        int squadCount = 0;
        for (Unit u : campaign.getUnits()) {
            Entity e = u.getEntity();
            if (u.isUnmanned() || u.isSalvage() || u.isMothballed() || u.isMothballing() || !u.isPresent() || (null == e)) {
                continue;
            }
            countDamageStatus[u.getDamageState()]++;
            switch (e.getUnitType()) {
                case UnitType.MEK:
                case UnitType.PROTOMEK:
                    mechCount++;
                    break;
                case UnitType.VTOL:
                case UnitType.TANK:
                    veeCount++;
                    break;
                case UnitType.AERO:
                case UnitType.CONV_FIGHTER:
                    aeroCount++;
                    break;
                case UnitType.BATTLE_ARMOR:
                    infantryCount++;
                    break;
                case UnitType.INFANTRY:
                    Infantry i = (Infantry) e;
                    squadCount += i.getSquadN();
                    break;
            }
        }
        //squad should count as 1/4 of a unit for force composition
        infantryCount += (int) Math.ceil(squadCount / 4.0);
        totalUnitCount = mechCount + veeCount + infantryCount + aeroCount;

        //missions
        countMissionByStatus = new int[Mission.S_NUM];
        for (Mission m : campaign.getMissions()) {
            countMissionByStatus[m.getStatus()]++;
        }
        completedMissions = countMissionByStatus[Mission.S_SUCCESS] +
                countMissionByStatus[Mission.S_FAILED] +
                countMissionByStatus[Mission.S_BREACH];

        //cargo capacity
        cargoCapacity = campaign.getTotalCombinedCargoCapacity();
        cargoTons = campaign.getCargoTonnage(false);
        double mothballedTonnage = campaign.getCargoTonnage(false, true);
        cargoTons = (cargoTons + mothballedTonnage);

        //transport capacity
        int noMech = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_MECH) - campaign.getOccupiedBays(Entity.ETYPE_MECH), 0);
        int noSC = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT) - campaign.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        @SuppressWarnings("unused") // FIXME: What type of bays do ConvFighters use?
                int noCF = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER) - campaign.getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_AERO) - campaign.getOccupiedBays(Entity.ETYPE_AERO), 0);
        int nolv = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true) - campaign.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_TANK) - campaign.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noinf = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) - campaign.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) - campaign.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        @SuppressWarnings("unused") // FIXME: This should be used somewhere...
                int noProto = Math.max(campaign.getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH) - campaign.getOccupiedBays(Entity.ETYPE_PROTOMECH),  0);
        int freehv = Math.max(campaign.getTotalHeavyVehicleBays() - campaign.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int freeSC = Math.max(campaign.getTotalSmallCraftBays() - campaign.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);

        //check for free bays elsewhere
        noASF = Math.max(noASF - freeSC, 0);
        nolv = Math.max(nolv - freehv, 0);

        unitsOver = noMech + noASF + nolv + nohv + noinf + noBA + noProto;
        unitsTransported = campaign.getOccupiedBays(Entity.ETYPE_MECH) +
                campaign.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) +
                campaign.getOccupiedBays(Entity.ETYPE_AERO) +
                campaign.getOccupiedBays(Entity.ETYPE_TANK, true) +
                campaign.getOccupiedBays(Entity.ETYPE_TANK) +
                campaign.getOccupiedBays(Entity.ETYPE_INFANTRY) +
                campaign.getOccupiedBays(Entity.ETYPE_BATTLEARMOR) +
                campaign.getOccupiedBays(Entity.ETYPE_PROTOMECH);

        nDS = campaign.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
    }

    /**
     * A report that gives numbers of combat and support personnel as well as injuries
     * @return a <code>String</code> of the report
     */
    public String getPersonnelReport() {
        return Integer.toString(totalCombatPersonnel) + " combat, " +
                Integer.toString(totalSupportPersonnel) + " support (" +
                Integer.toString(totalInjuries) + " injured)";
    }

    /**
     * A report that gives the number of units in different damage states
     * @return a <code>String</code> of the report
     */
    public String getForceRepairReport() {
        return countDamageStatus[Entity.DMG_LIGHT] + " light, " +
                countDamageStatus[Entity.DMG_MODERATE] + " moderate, " +
                countDamageStatus[Entity.DMG_HEAVY] + " heavy, " +
                countDamageStatus[Entity.DMG_CRIPPLED] + " crippled";
    }

    /**
     * A report that gives the percentage composition of the force in mech, armor, infantry, and aero units.
     * @return a <code>String</code> of the report
     */
    public String getForceCompositionReport() {
        List<String> composition = new ArrayList<String>();
        if (mechCount > 0) {
            composition.add(Integer.toString((int) Math.round(100 * mechCount / (double) totalUnitCount)) + "% mech");
        }
        if (veeCount > 0) {
            composition.add(Integer.toString((int) Math.round(100 * veeCount / (double) totalUnitCount)) + "% armor");
        }
        if (infantryCount > 0) {
            composition.add(Integer.toString((int) Math.round(100 * infantryCount / (double) totalUnitCount)) + "% infantry");
        }
        if (aeroCount > 0) {
            composition.add(Integer.toString((int) Math.round(100 * aeroCount / (double) totalUnitCount)) + "% aero");
        }
        return String.join(", ", composition);
    }

    /**
     * A report that gives the percentage of successful missions
     * @return a <code>String</code> of the report
     */
    public String getMissionSuccessReport() {
        int successRate = (int) Math.round((100 * countMissionByStatus[Mission.S_SUCCESS]) / (double) completedMissions);
        return Integer.toString(successRate) + "%";
    }

    /**
     * A report that gives capacity and existing tonnage of all cargo
     * @return a <code>String</code> of the report
     */
    public String getCargoCapacityReport() {
        return Integer.toString((int) Math.round(cargoTons)) + " tons (" +
                Integer.toString((int) Math.round(cargoCapacity)) + " tons capacity)";
    }

    /**
     * A report that gives information about the transportation capacity
     * @return a <code>String</code> of the report
     */
    public String getTransportCapacity() {
        int percentTransported = 0;
        if ((unitsOver + unitsTransported) > 0) {
            percentTransported = 100 - (int) Math.round(100 * unitsOver / (double) (unitsOver + unitsTransported));
        }
        String dropshipAppend = "";
        int dockingCollars = campaign.getTotalDockingCollars();
        if (nDS > 0) {
            dropshipAppend = ", " + Integer.toString(nDS) + " dropships/" + Integer.toString(dockingCollars) + " docking collars";
        }

        return Integer.toString(percentTransported) + "% bay capacity" + dropshipAppend;
    }
}
