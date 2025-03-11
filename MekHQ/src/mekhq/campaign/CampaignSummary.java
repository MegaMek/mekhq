/*
 * Copyright (c) 2020-2025 The MegaMek Team.
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
import mekhq.MekHQ;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getAdministrativeStrain;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getAdministrativeStrainModifier;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getCombinedSkillValues;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.calculatePrisonerCapacity;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.calculatePrisonerCapacityUsage;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * calculates and stores summary information on a campaign for use in reporting,
 * mostly for the command center
 */
public class CampaignSummary {

    Campaign campaign;

    // unit totals
    private int mekCount;
    private int veeCount;
    private int aeroCount;
    private int infantryCount;
    private int totalUnitCount;

    // unit damage status
    private int[] countDamageStatus;

    // personnel totals
    private int totalCombatPersonnel;
    private int totalSupportPersonnel;
    private int totalInjuries;

    // mission status
    private int[] countMissionByStatus;
    private int completedMissions;

    // cargo
    private double cargoCapacity;
    private double cargoTons;

    // transport capacity
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
     * This will update all of the values in CampaignSummary to the latest from the
     * campaign. It should
     * be run before pulling out any reports
     */
    public void updateInformation() {

        // personnel
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
            if (p.getPrimaryRole().isCombat()) {
                totalCombatPersonnel++;
            } else {
                totalSupportPersonnel++;
            }
        }

        // units
        countDamageStatus = new int[Entity.DMG_CRIPPLED + 1];
        mekCount = 0;
        veeCount = 0;
        aeroCount = 0;
        infantryCount = 0;
        int squadCount = 0;
        for (Unit u : campaign.getHangar().getUnits()) {
            Entity e = u.getEntity();
            if (u.isUnmanned() || u.isSalvage() || u.isMothballed() || u.isMothballing() || !u.isPresent()
                    || (null == e)) {
                continue;
            }
            countDamageStatus[u.getDamageState()]++;
            switch (e.getUnitType()) {
                case UnitType.MEK:
                case UnitType.PROTOMEK:
                    mekCount++;
                    break;
                case UnitType.VTOL:
                case UnitType.TANK:
                    veeCount++;
                    break;
                case UnitType.AEROSPACEFIGHTER:
                case UnitType.CONV_FIGHTER:
                    aeroCount++;
                    break;
                case UnitType.BATTLE_ARMOR:
                    infantryCount++;
                    break;
                case UnitType.INFANTRY:
                    Infantry i = (Infantry) e;
                    squadCount += i.getSquadCount();
                    break;
            }
        }
        // squad should count as 1/4 of a unit for force composition
        infantryCount += (int) Math.ceil(squadCount / 4.0);
        totalUnitCount = mekCount + veeCount + infantryCount + aeroCount;

        // missions
        countMissionByStatus = new int[MissionStatus.values().length];
        for (Mission m : campaign.getMissions()) {
            countMissionByStatus[m.getStatus().ordinal()]++;
        }

        completedMissions = 0;
        for (MissionStatus status : MissionStatus.values()) {
            if (status.isCompleted()) {
                completedMissions += countMissionByStatus[status.ordinal()];
            }
        }

        // cargo capacity
        CargoStatistics cargoStats = campaign.getCargoStatistics();
        cargoCapacity = cargoStats.getTotalCombinedCargoCapacity();
        cargoTons = cargoStats.getCargoTonnage(false);
        double mothballedTonnage = cargoStats.getCargoTonnage(false, true);
        cargoTons = (cargoTons + mothballedTonnage);

        // transport capacity
        HangarStatistics hangarStats = campaign.getHangarStatistics();
        int noMek = Math.max(
                hangarStats.getNumberOfUnitsByType(Entity.ETYPE_MEK) - hangarStats.getOccupiedBays(Entity.ETYPE_MEK),
                0);
        int noSC = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT)
                - hangarStats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);
        @SuppressWarnings("unused") // FIXME: What type of bays do ConvFighters use?
        int noCF = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER)
                - hangarStats.getOccupiedBays(Entity.ETYPE_CONV_FIGHTER), 0);
        int noASF = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACEFIGHTER)
                - hangarStats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER), 0);
        int nolv = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)
                - hangarStats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int nohv = Math.max(
                hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK) - hangarStats.getOccupiedBays(Entity.ETYPE_TANK),
                0);
        int noinf = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY)
                - hangarStats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR)
                - hangarStats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK)
                - hangarStats.getOccupiedBays(Entity.ETYPE_PROTOMEK), 0);
        int freehv = Math.max(hangarStats.getTotalHeavyVehicleBays() - hangarStats.getOccupiedBays(Entity.ETYPE_TANK),
                0);
        int freeSC = Math
                .max(hangarStats.getTotalSmallCraftBays() - hangarStats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);

        // check for free bays elsewhere
        noASF = Math.max(noASF - freeSC, 0);
        nolv = Math.max(nolv - freehv, 0);

        unitsOver = noMek + noASF + nolv + nohv + noinf + noBA + noProto;
        unitsTransported = hangarStats.getOccupiedBays(Entity.ETYPE_MEK) +
                hangarStats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) +
                hangarStats.getOccupiedBays(Entity.ETYPE_AEROSPACEFIGHTER) +
                hangarStats.getOccupiedBays(Entity.ETYPE_TANK, true) +
                hangarStats.getOccupiedBays(Entity.ETYPE_TANK) +
                hangarStats.getOccupiedBays(Entity.ETYPE_INFANTRY) +
                hangarStats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR) +
                hangarStats.getOccupiedBays(Entity.ETYPE_PROTOMEK);

        nDS = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
    }

    /**
     * A report that gives numbers of combat and support personnel as well as
     * injuries
     *
     * @return a <code>String</code> of the report
     */
    public String getPersonnelReport() {
        return totalCombatPersonnel + " combat, " + totalSupportPersonnel + " support ("
                + totalInjuries + " injured)";
    }

    /**
     * A report that gives the number of units in different damage states
     *
     * @return a <code>String</code> of the report
     */
    public String getForceRepairReport() {
        return countDamageStatus[Entity.DMG_LIGHT] + " light, " +
                countDamageStatus[Entity.DMG_MODERATE] + " moderate, " +
                countDamageStatus[Entity.DMG_HEAVY] + " heavy, " +
                countDamageStatus[Entity.DMG_CRIPPLED] + " crippled";
    }

    /**
     * A report that gives the percentage composition of the force in mek, armor,
     * infantry, and aero units.
     *
     * @return a <code>String</code> of the report
     */
    public String getForceCompositionReport() {
        List<String> composition = new ArrayList<>();
        if (mekCount > 0) {
            composition.add((int) Math.round(100 * mekCount / (double) totalUnitCount) + "% mek");
        }
        if (veeCount > 0) {
            composition.add((int) Math.round(100 * veeCount / (double) totalUnitCount) + "% armor");
        }
        if (infantryCount > 0) {
            composition.add((int) Math.round(100 * infantryCount / (double) totalUnitCount) + "% infantry");
        }
        if (aeroCount > 0) {
            composition.add((int) Math.round(100 * aeroCount / (double) totalUnitCount) + "% aero");
        }
        return String.join(", ", composition);
    }

    /**
     * A report that gives the percentage of successful missions
     *
     * @return a <code>String</code> of the report
     */
    public String getMissionSuccessReport() {
        int successRate = (int) Math.round((100 * countMissionByStatus[MissionStatus.SUCCESS.ordinal()])
                / (double) completedMissions);
        return successRate + "%";
    }

    /**
     * Generates an HTML report about the current and maximum cargo capacity.
     * The current cargo capacity (cargoTons) and maximum cargo capacity (cargoCapacity) are
     * rounded to 1 decimal place. The comparison between the current and maximum cargo capacity
     * determines the font's color in the report.
     * - If the current cargo exceeds the maximum capacity, the color is set to MHQ's defined negative color.
     * - If the current cargo equals the maximum capacity, the color is set to MHQ's defined warning color.
     * - In other cases, the regular color is used.
     *
     * @return A {@link StringBuilder} object containing the HTML formatted report of cargo usage
     * against capacity.
     */
    public StringBuilder getCargoCapacityReport() {
        BigDecimal roundedCargo = new BigDecimal(Double.toString(cargoTons));
        roundedCargo = roundedCargo.setScale(1, RoundingMode.HALF_UP);

        BigDecimal roundedCapacity = new BigDecimal(Double.toString(cargoCapacity));
        roundedCapacity = roundedCapacity.setScale(1, RoundingMode.HALF_UP);

        int comparison = roundedCargo.compareTo(roundedCapacity);

        StringBuilder report = new StringBuilder("<html>");

        if (comparison > 0) {
            report.append("<font color='")
                    .append(MekHQ.getMHQOptions().getFontColorWarningHexColor())
                    .append("'>");
        }

        report.append(roundedCargo)
                .append(" tons (")
                .append(roundedCapacity)
                .append(" tons capacity)");

        if (!report.toString().equals(roundedCargo + " tons (" + roundedCapacity + " tons capacity)")) {
            report.append("</font></html>");
        } else {
            report.append("</html>");
        }

        return report;
    }

    /**
     * A report that gives information about the transportation capacity
     *
     * @return a <code>String</code> of the report
     */
    public String getTransportCapacity() {
        int percentTransported = 0;
        if ((unitsOver + unitsTransported) > 0) {
            percentTransported = 100 - (int) Math.round(100 * unitsOver / (double) (unitsOver + unitsTransported));
        }
        String dropshipAppend = "";
        int dockingCollars = campaign.getHangarStatistics().getTotalDockingCollars();
        if (nDS > 0) {
            dropshipAppend = ", " + nDS + " dropships/" + dockingCollars + " docking collars";
        }

        return percentTransported + "% bay capacity" + dropshipAppend;
    }

    /**
     * Generates an administrative capacity report for the Command Center.
     *
     * @param campaign the campaign for which the administrative capacity report is
     *                 generated
     * @return the administrative capacity report in HTML format
     */
    public String getAdministrativeCapacityReport(Campaign campaign) {
        int combinedSkillValues = getCombinedSkillValues(campaign, SkillType.S_ADMIN);

        StringBuilder administrativeCapacityReport = new StringBuilder()
                .append(getAdministrativeStrain(campaign)).append(" / ")
                .append(campaign.getCampaignOptions().getAdministrativeCapacity() * combinedSkillValues)
                .append(" personnel");

        if (getAdministrativeStrainModifier(campaign) > 0) {
            administrativeCapacityReport
                    .append(" (+")
                    .append(getAdministrativeStrainModifier(campaign))
                    .append(')');
        }

        return administrativeCapacityReport.toString();
    }

    /**
     * Returns a summary of fatigue related facilities.
     *
     * @return A summary of fatigue related facilities.
     */
    public String getFacilityReport() {
        final String WARNING = " \u26A0";
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        boolean exceedsCapacity;
        String color;
        String closingSpan;
        String colorBlindWarning;

        StringBuilder report = new StringBuilder("<html>");

        // Field Kitchens
        if (campaignOptions.isUseFatigue()) {
            int personnelCount;
            if (campaignOptions.isUseFieldKitchenIgnoreNonCombatants()) {
                personnelCount = campaign.getActiveCombatPersonnel().size();
            } else {
                personnelCount = campaign.getActivePersonnel(true).size();
            }

            int fieldKitchenCapacity = checkFieldKitchenCapacity(campaign);

            exceedsCapacity = personnelCount > fieldKitchenCapacity;

            color = exceedsCapacity
                ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor())
                : "";
            closingSpan = exceedsCapacity ? CLOSING_SPAN_TAG : "";
            colorBlindWarning = exceedsCapacity ? WARNING : "";
            report.append(String.format("Kitchens %s(%s/%s)%s%s",
                color, personnelCount, checkFieldKitchenCapacity(campaign), closingSpan, colorBlindWarning));
        }

        // Hospital Beds
        if (campaignOptions.isUseAdvancedMedical()) {
            if (campaignOptions.isUseFatigue()) {
                report.append("<br>");
            }

            int patients = (int) campaign.getPatients().stream()
                    .filter(patient -> patient.getDoctorId() != null)
                    .count();

            int doctorCapacity = campaign.getActivePersonnel().stream()
                    .filter(person -> (person.getPrimaryRole().isDoctor()) || (person.getSecondaryRole().isDoctor()))
                    .mapToInt(person -> campaignOptions.getMaximumPatients())
                    .sum();

            exceedsCapacity = patients > doctorCapacity;

            color = exceedsCapacity
                ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
                : "";
            closingSpan = exceedsCapacity ? CLOSING_SPAN_TAG : "";
            colorBlindWarning = exceedsCapacity ? WARNING : "";

            report.append(String.format("Hospital Beds %s(%s/%s)%s%s",
                color, patients, doctorCapacity, closingSpan, colorBlindWarning));
        }

        // Prisoners
        if (!campaignOptions.getPrisonerCaptureStyle().isNone()) {
            if (campaignOptions.isUseFatigue() || campaignOptions.isUseAdvancedMedical()) {
                report.append("<br>");
            }
            int capacityUsage = calculatePrisonerCapacityUsage(campaign);
            int prisonerCapacity = calculatePrisonerCapacity(campaign);

            exceedsCapacity = capacityUsage > prisonerCapacity;

            color = capacityUsage > (prisonerCapacity * 0.75) // at risk of a minor event
                ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor())
                : "";
            color = exceedsCapacity // at risk of a major event
                ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
                : color;
            closingSpan = exceedsCapacity ? CLOSING_SPAN_TAG : "";
            colorBlindWarning = exceedsCapacity ? WARNING : "";

            report.append(String.format("Prisoners %s(%s/%s)%s%s",
                color, capacityUsage, prisonerCapacity, closingSpan, colorBlindWarning));
        }

        return report.append("</html>").toString();
    }
}
