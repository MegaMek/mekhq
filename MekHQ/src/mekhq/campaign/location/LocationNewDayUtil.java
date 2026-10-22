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

package mekhq.campaign.location;

import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.JOptionPane;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.contract.utilities.ContractRepairLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.RefitPartOrders;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.dialog.RefitPartsArrivedNotice;

/**
 * Utility class for new-day processing of a single {@link IPlace}: maintenance, parts arrival, and unit state
 * transitions (refitting, mothballing, delivery).
 */
public final class LocationNewDayUtil {
    private static final MMLogger LOGGER = MMLogger.create(LocationNewDayUtil.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private LocationNewDayUtil() {}

    /**
     * Runs the per-place unit new-day cycle for {@code place}: maintenance on all units in the hangar, parts transit
     * and arrival in the warehouse, and unit state transitions (refit, mothball, delivery). Places that own neither a
     * hangar nor a warehouse return immediately.
     */
    public static void processNewDayUnits(IPlace place, Campaign campaign) {
        LocalHangar hangar = place.getHangar();
        LocalWarehouse warehouse = place.getWarehouse();

        if (hangar != null) {
            // need to loop through units twice, the first time to do all maintenance and
            // the second time to do whatever else. Otherwise, maintenance minutes might
            // get sucked up by other stuff. This is also a good place to ensure that a
            // unit's engineer gets reset and updated.
            for (Unit unit : hangar.getUnits()) {
                // do maintenance checks
                try {
                    unit.resetEngineer();
                    if (unit.getEngineer() != null) {
                        unit.getEngineer().resetMinutesLeft(
                              campaign.getCampaignOptions().get(CampaignOption.TECHS_USE_ADMINISTRATION));
                    }
                    Maintenance.doMaintenance(campaign, unit);
                } catch (Exception ex) {
                    LOGGER.error(ex,
                          "Unable to perform maintenance on {} ({}) due to an error",
                          unit.getName(),
                          unit.getId().toString());
                    campaign.addReport(TECHNICAL,
                          String.format("ERROR: An error occurred performing maintenance on %s, check the log",
                                unit.getName()));
                }
            }
        }

        if (warehouse != null) {
            // need to check for assigned tasks in two steps to avoid concurrent modification problems
            List<Part> assignedParts = new ArrayList<>();
            List<Part> arrivedParts = new ArrayList<>();
            warehouse.forEachPart(part -> {
                if (part instanceof Refit) {
                    return;
                }

                if (part.getTech() != null) {
                    assignedParts.add(part);
                }

                // If the part is currently in-transit...
                if (!part.isPresent()) {
                    // ... decrement the number of days until it arrives...
                    int newDaysToArrival = part.getDaysToArrival() - 1;

                    // If we're in transit and we don't allow deliveries while in transit the part will remain fixed
                    // with a delivery time of 1 day until we arrive at our destination.
                    if (campaign.getCampaignOptions().get(CampaignOption.NO_DELIVERIES_IN_TRANSIT) &&
                              !place.isOnPlanet() &&
                              newDaysToArrival <= 0) {
                        return;
                    }
                    part.setDaysToArrival(newDaysToArrival);
                    if (part.isPresent()) {
                        // ... and mark the part as arrived if it is now here.
                        arrivedParts.add(part);
                    }
                }
            });

            // arrive parts before attempting refit or parts will not get reserved that day
            for (Part part : arrivedParts) {
                campaign.getQuartermaster().arrivePart(part);
            }
            for (Unit unit : RefitPartOrders.findRefitsReadyToBegin(campaign, arrivedParts)) {
                RefitPartsArrivedNotice.show(campaign, unit);
            }

            // finish up any overnight assigned tasks
            for (Part part : assignedParts) {
                Person tech = getOvernightTech(part);

                if (tech != null) {
                    ILocation repairTarget = (part.getUnit() != null) ? part.getUnit() : part;
                    continueOvernightWork(campaign, part, repairTarget, tech);
                } else {
                    JOptionPane.showMessageDialog(null,
                          getFormattedTextAt(RESOURCE_BUNDLE, "LocationNewDayUtil.noTechForPart.message",
                                part.getName(), part.getUnit().getHyperlinkedName()),
                          getTextAt(RESOURCE_BUNDLE, "LocationNewDayUtil.noTechForPart.title"),
                          JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // check to see if this part can now be combined with other spare parts
                if (part.isSpare() && (part.getQuantity() > 0)) {
                    campaign.getQuartermaster().addPart(part, 0, false);
                }
            }
        }

        if (hangar != null) {
            // ok now we can check for other stuff we might need to do to units
            int defaultRepairSite = ContractRepairLocation.getBestRepairLocation(campaign.getActiveContracts());
            List<UUID> unitsToRemove = new ArrayList<>();
            int podSwapsCarriedOver = 0;
            for (Unit unit : hangar.getUnits()) {
                podSwapsCarriedOver += continueOvernightPodSwaps(unit, campaign);
                if (unit.isRefitting()) {
                    campaign.refit(unit.getRefit());
                }
                if (unit.isMothballing()) {
                    campaign.workOnMothballingOrActivation(unit);
                }
                if (!unit.isPresent()) {
                    unit.checkArrival(!place.isOnPlanet() &&
                                            campaign.getCampaignOptions().get(CampaignOption.NO_DELIVERIES_IN_TRANSIT));
                    // Has unit just been delivered?
                    if (unit.isPresent()) {
                        campaign.addReport(ACQUISITIONS, getFormattedTextAt(RESOURCE_BUNDLE,
                              "unitArrived.text",
                              unit.getHyperlinkedName(),
                              spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                              CLOSING_SPAN_TAG));
                        unit.setSite(defaultRepairSite);
                    }
                }
                if (!unit.isRepairable() && !unit.hasSalvageableParts()) {
                    unitsToRemove.add(unit.getId());
                }
            }
            if (podSwapsCarriedOver > 0) {
                LOGGER.debug("[PodSpace] {} pod swaps carried over to the new day", podSwapsCarriedOver);
            }
            // Remove any unrepairable, unsalvageable units
            unitsToRemove.forEach(campaign::removeUnit);
        }

        // Recursively process any child IPlaces (e.g., AcademyCampusLocations under this place,
        // including bases that host their own on-campus academies).
        if (place.hasLocationNode()) {
            for (LocationNode child : place.getLocationNode().getChildren()) {
                if (child.getLocatable() instanceof AcademyCampusLocation campus) {
                    processNewDayUnits(campus, campaign);
                }
            }
        }
    }

    /**
     * Pod swaps are not parts, so the warehouse pass above never sees them. Any on this unit that ran out of time
     * yesterday are carried on here, after the parts, in the same way.
     *
     * @return how many pod swaps were carried over
     */
    private static int continueOvernightPodSwaps(Unit unit, Campaign campaign) {
        List<PodSpace> assignedPodSpaces = new ArrayList<>();
        for (PodSpace podSpace : unit.getPodSpace()) {
            if (podSpace.isBeingWorkedOn()) {
                assignedPodSpaces.add(podSpace);
            }
        }
        for (PodSpace podSpace : assignedPodSpaces) {
            Person tech = getOvernightTech(podSpace);
            if (tech != null) {
                continueOvernightWork(campaign, podSpace, unit, tech);
            }
        }
        return assignedPodSpaces.size();
    }

    /**
     * @return who carries on an overnight job: the unit's own engineer for a self-crewed unit, otherwise the tech who
     *       started it
     */
    private static @Nullable Person getOvernightTech(IPartWork partWork) {
        Unit unit = partWork.getUnit();
        if ((unit != null) && (unit.getEngineer() != null)) {
            return unit.getEngineer();
        }
        return partWork.getTech();
    }

    /**
     * Carries on a job that ran out of time yesterday. The job is dropped, with a report, if the tech has moved away
     * from it or can no longer work on it.
     */
    private static void continueOvernightWork(Campaign campaign, IPartWork partWork, ILocation repairTarget,
          Person tech) {
        // If the tech has moved to a different location since the assignment was made,
        // cancel it and notify the player rather than silently failing.
        if (!LocationUtils.areSameEffectiveLocation(tech, repairTarget)) {
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CampaignNewDayManager.techAtDifferentLocation",
                  tech.getHyperlinkedFullTitle(),
                  partWork.getPartName()));
            partWork.cancelAssignment(true);
            return;
        }
        if (tech.getSkillForWorkingOn(partWork) != null) {
            try {
                campaign.fixPart(partWork, tech);
            } catch (Exception exception) {
                LOGGER.error(exception,
                      "Could not perform overnight maintenance on {} due to an error",
                      partWork.getPartName());
                campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                      "CampaignNewDayManager.maintenanceError.report",
                      partWork.getPartName()));
            }
        } else {
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CampaignNewDayManager.techAbort.report",
                  tech.getHyperlinkedFullTitle(),
                  partWork.getPartName()));
            partWork.cancelAssignment(false);
        }
    }

    /**
     * Runs {@link #processNewDayUnits(IPlace, Campaign)} for every top-level {@link IPlace} in the campaign. Each call
     * propagates recursively to child places (e.g., academy campuses hosted under a base or under a fixed planet
     * location).
     */
    public static void processAllLocationUnits(Campaign campaign) {
        for (AbstractLocation location : campaign.getCampaignLocationManager().getLocations()) {
            for (ILocation child : location.getChildLocations()) {
                if (child instanceof IPlace place) {
                    processNewDayUnits(place, campaign);
                }
            }
        }
    }
}
