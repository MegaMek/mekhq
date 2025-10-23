/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_TETRIS_MASTER;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.areFieldKitchensWithinCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenCapacity;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.checkFieldKitchenUsage;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getCombinedSkillValues;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getHRStrain;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getHRStrainModifier;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.calculatePrisonerCapacity;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.calculatePrisonerCapacityUsage;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.UnitType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.MASHCapacity;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * calculates and stores summary information on a campaign for use in reporting, mostly for the command center
 */
public class CampaignSummary {

    private static final String WARNING_ICON = "\u26A0";

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
     * Create a CampaignSummary
     */
    public CampaignSummary() {
    }

    /**
     * Link this CampaignSummary to a Campaign instance and update state with information from it.
     *
     * @param campaign Campaign to link
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
        updateInformation();
    }

    /**
     * This will update all the values in CampaignSummary to the latest from the campaign. It should be run before
     * pulling out any reports
     */
    public void updateInformation() {
        // personnel
        totalCombatPersonnel = 0;
        totalSupportPersonnel = 0;
        totalInjuries = 0;
        for (Person person : campaign.getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isCombat()) {
                totalCombatPersonnel++;
            } else if (!person.isDependent()) {
                totalSupportPersonnel++;
            }

            if (person.needsFixing()) {
                totalInjuries++;
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
            if (u.isUnmanned() ||
                      u.isSalvage() ||
                      u.isMothballed() ||
                      u.isMothballing() ||
                      !u.isPresent() ||
                      (null == e)) {
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
                case UnitType.AEROSPACE_FIGHTER:
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

        double tetrisMasterMultiplier = 1.0;
        for (Person person : campaign.getActivePersonnel(false, false)) {
            PersonnelOptions options = person.getOptions();
            if (options.booleanOption(ADMIN_TETRIS_MASTER)) {
                tetrisMasterMultiplier += 0.05;
            }
        }

        cargoCapacity = cargoCapacity * tetrisMasterMultiplier;

        cargoTons = cargoStats.getCargoTonnage(false);
        double mothballedTonnage = cargoStats.getCargoTonnage(false, true);
        cargoTons = (cargoTons + mothballedTonnage);

        // transport capacity
        HangarStatistics hangarStats = campaign.getHangarStatistics();
        int noMek = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_MEK) -
                                   hangarStats.getOccupiedBays(Entity.ETYPE_MEK), 0);
        int noASF = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER) -
                                   hangarStats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER), 0);
        int noLV = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true) -
                                  hangarStats.getOccupiedBays(Entity.ETYPE_TANK, true), 0);
        int noHV = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK) -
                                  hangarStats.getOccupiedBays(Entity.ETYPE_TANK), 0);
        int noInf = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY) -
                                   hangarStats.getOccupiedBays(Entity.ETYPE_INFANTRY), 0);
        int noBA = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR) -
                                  hangarStats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR), 0);
        int noProto = Math.max(hangarStats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK) -
                                     hangarStats.getOccupiedBays(Entity.ETYPE_PROTOMEK), 0);
        int freeHV = Math.max(hangarStats.getTotalHeavyVehicleBays() - hangarStats.getOccupiedBays(Entity.ETYPE_TANK),
              0);
        int freeSC = Math.max(hangarStats.getTotalSmallCraftBays() -
                                    hangarStats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT), 0);

        // check for free bays elsewhere
        noASF = Math.max(noASF - freeSC, 0);
        noLV = Math.max(noLV - freeHV, 0);

        unitsOver = noMek + noASF + noLV + noHV + noInf + noBA + noProto;
        unitsTransported = hangarStats.getOccupiedBays(Entity.ETYPE_MEK) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_SMALL_CRAFT) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_AEROSPACE_FIGHTER) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_TANK, true) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_TANK) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_INFANTRY) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_BATTLEARMOR) +
                                 hangarStats.getOccupiedBays(Entity.ETYPE_PROTOMEK);

        nDS = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
    }

    /**
     * A report that gives numbers of combat and support personnel as well as injuries
     *
     * @return a <code>String</code> of the report
     */
    public String getPersonnelReport() {
        return totalCombatPersonnel + " combat, " + totalSupportPersonnel + " support (" + totalInjuries + " injured)";
    }

    /**
     * A report that gives the number of units in different damage states
     *
     * @return a <code>String</code> of the report
     */
    public String getForceRepairReport() {
        return countDamageStatus[Entity.DMG_LIGHT] +
                     " light, " +
                     countDamageStatus[Entity.DMG_MODERATE] +
                     " moderate, " +
                     countDamageStatus[Entity.DMG_HEAVY] +
                     " heavy, " +
                     countDamageStatus[Entity.DMG_CRIPPLED] +
                     " crippled";
    }

    /**
     * A report that gives the percentage composition of the force in mek, armor, infantry, and aero units.
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
        int successRate = (int) Math.round((100 * countMissionByStatus[MissionStatus.SUCCESS.ordinal()]) /
                                                 (double) completedMissions);
        return successRate + "%";
    }

    /**
     * Generates an HTML report about the current and maximum cargo capacity. The current cargo capacity (cargoTons) and
     * maximum cargo capacity (cargoCapacity) are rounded to 1 decimal place. The comparison between the current and
     * maximum cargo capacity determines the font's color in the report. - If the current cargo exceeds the maximum
     * capacity, the color is set to MHQ's defined negative color. - If the current cargo equals the maximum capacity,
     * the color is set to MHQ's defined warning color. - In other cases, the regular color is used.
     *
     * @return A {@link StringBuilder} object containing the HTML formatted report of cargo usage against capacity.
     */
    public StringBuilder getCargoCapacityReport() {
        BigDecimal roundedCargo = new BigDecimal(Double.toString(cargoTons));
        roundedCargo = roundedCargo.setScale(1, RoundingMode.HALF_UP);

        BigDecimal roundedCapacity = new BigDecimal(Double.toString(cargoCapacity));
        roundedCapacity = roundedCapacity.setScale(1, RoundingMode.HALF_UP);

        int comparison = roundedCargo.compareTo(roundedCapacity);

        StringBuilder report = new StringBuilder("<html>");

        if (comparison > 0) {
            report.append("<font color='").append(ReportingUtilities.getWarningColor()).append("'>");
        }

        report.append(roundedCargo).append(" tons (").append(roundedCapacity).append(" tons capacity)");

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
     * @param campaign the campaign for which the administrative capacity report is generated
     *
     * @return the administrative capacity report in HTML format
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String getAdministrativeCapacityReport(Campaign campaign) {
        return getHRCapacityReport(campaign);
    }

    /**
     * Generates an administrative capacity report for the Command Center.
     *
     * @param campaign the campaign for which the administrative capacity report is generated
     *
     * @return the administrative capacity report in HTML format
     */
    public String getHRCapacityReport(Campaign campaign) {
        int combinedSkillValues = getCombinedSkillValues(campaign, SkillType.S_ADMIN);

        StringBuilder hrCapacityReport = new StringBuilder().append("<html>")
                                               .append(getHRStrain(campaign))
                                               .append(" / ")
                                               .append(campaign.getCampaignOptions().getHRCapacity() *
                                                             combinedSkillValues)
                                               .append(" personnel");

        if (getHRStrainModifier(campaign) > 0) {
            hrCapacityReport.append(spanOpeningWithCustomColor(getNegativeColor()))
                  .append(" (<b>+")
                  .append(getHRStrainModifier(campaign))
                  .append("</b>)")
                  .append(CLOSING_SPAN_TAG)
                  .append(" ")
                  .append(WARNING_ICON);
        }

        hrCapacityReport.append("</html>");

        return hrCapacityReport.toString();
    }

    /**
     * Returns a summary of fatigue related facilities.
     *
     * @return A summary of fatigue related facilities.
     */
    public String getFacilityReport() {
        final String WARNING = " " + WARNING_ICON;
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        boolean exceedsCapacity;
        String color;
        String closingSpan;
        String colorBlindWarning;

        StringBuilder report = new StringBuilder("<html>");

        // Field Kitchens
        List<Unit> unitsInToe = campaign.getForce(FORCE_ORIGIN).getAllUnitsAsUnits(campaign.getHangar(), false);
        if (campaignOptions.isUseFatigue()) {
            int fieldKitchenCapacity = checkFieldKitchenCapacity(unitsInToe, campaignOptions.getFieldKitchenCapacity());
            fieldKitchenCapacity += FacilityRentals.getCapacityIncreaseFromRentals(campaign.getActiveContracts(),
                  ContractRentalType.KITCHENS);

            int fieldKitchenUsage = checkFieldKitchenUsage(campaign.getActivePersonnel(false, true),
                  campaignOptions.isUseFieldKitchenIgnoreNonCombatants());

            boolean isWithinCapacity = areFieldKitchensWithinCapacity(fieldKitchenCapacity, fieldKitchenUsage);
            color = isWithinCapacity ?
                          "" :
                          spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
            closingSpan = isWithinCapacity ? "" : CLOSING_SPAN_TAG;
            colorBlindWarning = isWithinCapacity ? "" : WARNING;

            report.append(String.format("Field Kitchens %s(%s/%s)%s%s",
                  color,
                  fieldKitchenUsage,
                  fieldKitchenCapacity,
                  closingSpan,
                  colorBlindWarning));
        }

        // Hospital Beds
        if (campaignOptions.isUseAdvancedMedical()) {
            if (campaignOptions.isUseFatigue()) {
                report.append("<br>");
            }

            int patients = campaign.getPatientsAssignedToDoctors().size();
            boolean useMASHTheatres = campaignOptions.isUseMASHTheatres();
            int mashTheatreCapacity = useMASHTheatres ? MASHCapacity.checkMASHCapacity(unitsInToe,
                  campaignOptions.getMASHTheatreCapacity()) : Integer.MAX_VALUE;

            final boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
            final int maximumPatients = campaignOptions.getMaximumPatients();
            int doctorCapacity = 0;
            for (Person person : campaign.getActivePersonnel(false, false)) {
                doctorCapacity += person.getDoctorMedicalCapacity(isDoctorsUseAdministration, maximumPatients);
            }


            exceedsCapacity = patients > doctorCapacity || patients > mashTheatreCapacity;

            color = exceedsCapacity ?
                          spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                          "";
            closingSpan = exceedsCapacity ? CLOSING_SPAN_TAG : "";
            colorBlindWarning = exceedsCapacity ? WARNING : "";

            if (useMASHTheatres) {
                report.append(String.format("Hospital Beds %s(%s/%s) (MASH Capacity %s)%s%s",
                      color,
                      patients,
                      doctorCapacity,
                      mashTheatreCapacity,
                      closingSpan,
                      colorBlindWarning));
            } else {
                report.append(String.format("Hospital Beds %s(%s/%s)%s%s",
                      color,
                      patients,
                      doctorCapacity,
                      closingSpan,
                      colorBlindWarning));
            }
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
                          ? spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()) : "";
            color = exceedsCapacity // at risk of a major event
                          ? spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) : color;
            closingSpan = exceedsCapacity ? CLOSING_SPAN_TAG : "";
            colorBlindWarning = exceedsCapacity ? WARNING : "";

            report.append(String.format("Prisoner Capacity %s(%s/%s)%s%s",
                  color,
                  capacityUsage,
                  prisonerCapacity,
                  closingSpan,
                  colorBlindWarning));
        }

        return report.append("</html>").toString();
    }
}
