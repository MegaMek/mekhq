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
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.JOptionPane;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;

/**
 * Handles the new-day processing for a single {@link IPlace}: maintenance, parts arrival, and unit state transitions
 * (refitting, mothballing, delivery).
 *
 * <p>Each {@link IPlace} implementation ({@link mekhq.campaign.Campaign},
 * {@link mekhq.campaign.base.AbstractBase}, {@link AcademyCampusLocation}) owns one instance.
 * {@link mekhq.campaign.CampaignNewDayManager} iterates all places and calls {@link #processNewDay(Campaign)} on
 * each.</p>
 *
 * @since 0.50.10
 */
public class LocationNewDayManager {
    private static final MMLogger LOGGER = MMLogger.create(LocationNewDayManager.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private final IPlace place;

    public LocationNewDayManager(IPlace place) {
        this.place = place;
    }

    /**
     * Runs the per-place unit new-day cycle for the owned {@link IPlace}: maintenance on all units in the hangar, parts
     * transit and arrival in the warehouse, and unit state transitions (refit, mothball, delivery). Places that own
     * neither a hangar nor a warehouse return immediately.
     */
    public void processNewDayUnits(Campaign campaign) {
        Hangar hangar = place.getHangar();
        Warehouse warehouse = place.getWarehouse();

        if (hangar != null) {
            for (Unit unit : hangar.getUnits()) {
                try {
                    unit.resetEngineer();
                    if (unit.getEngineer() != null) {
                        unit.getEngineer().resetMinutesLeft(
                              campaign.getCampaignOptions().isTechsUseAdministration());
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
            List<Part> assignedParts = new ArrayList<>();
            List<Part> arrivedParts = new ArrayList<>();
            warehouse.forEachPart(part -> {
                if (part instanceof Refit) {
                    return;
                }

                if (part.getTech() != null) {
                    assignedParts.add(part);
                }

                if (!part.isPresent()) {
                    int newDaysToArrival = part.getDaysToArrival() - 1;
                    if (campaign.getCampaignOptions().isNoDeliveriesInTransit() &&
                              !place.isOnPlanet() &&
                              newDaysToArrival <= 0) {
                        return;
                    }
                    part.setDaysToArrival(newDaysToArrival);
                    if (part.isPresent()) {
                        arrivedParts.add(part);
                    }
                }
            });

            for (Part part : arrivedParts) {
                campaign.getQuartermaster().arrivePart(part);
            }

            for (Part part : assignedParts) {
                Person tech;
                if ((part.getUnit() != null) && (part.getUnit().getEngineer() != null)) {
                    tech = part.getUnit().getEngineer();
                } else {
                    tech = part.getTech();
                }

                if (tech != null) {
                    ILocation repairTarget = (part.getUnit() != null) ? part.getUnit() : part;
                    if (!LocationUtils.areSameEffectiveLocation(tech, repairTarget)) {
                        campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                              "CampaignNewDayManager.techAtDifferentLocation",
                              tech.getHyperlinkedFullTitle(),
                              part.getName()));
                        part.cancelAssignment(true);
                        continue;
                    }
                    if (tech.getSkillForWorkingOn(part) != null) {
                        try {
                            campaign.fixPart(part, tech);
                        } catch (Exception ex) {
                            LOGGER.error(ex,
                                  "Could not perform overnight maintenance on {} ({}) due to an error",
                                  part.getName(),
                                  part.getId());
                            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                                  "CampaignNewDayManager.maintenanceError.report",
                                  part.getName()));
                        }
                    } else {
                        campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                              "CampaignNewDayManager.techAbort.report",
                              tech.getHyperlinkedFullTitle(),
                              part.getName()));
                        part.cancelAssignment(false);
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                          "Could not find tech for part: " +
                                part.getName() +
                                " on unit: " +
                                part.getUnit().getHyperlinkedName(),
                          "Invalid Auto-continue",
                          JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                if (part.isSpare() && (part.getQuantity() > 0)) {
                    campaign.getQuartermaster().addPart(part, 0, false);
                }
            }
        }

        if (hangar != null) {
            int defaultRepairSite = AtBContract.getBestRepairLocation(campaign.getActiveAtBContracts());
            List<UUID> unitsToRemove = new ArrayList<>();
            for (Unit unit : hangar.getUnits()) {
                if (unit.isRefitting()) {
                    campaign.refit(unit.getRefit());
                }
                if (unit.isMothballing()) {
                    campaign.workOnMothballingOrActivation(unit);
                }
                if (!unit.isPresent()) {
                    unit.checkArrival(!place.isOnPlanet() &&
                                            campaign.getCampaignOptions().isNoDeliveriesInTransit());
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
            unitsToRemove.forEach(campaign::removeUnit);
        }

        // Recursively process any child IPlaces (e.g., AcademyCampusLocations under this place,
        // including bases that host their own on-campus academies).
        if (place.hasLocationNode()) {
            for (LocationNode child : place.getLocationNode().getChildren()) {
                if (child.getLocatable() instanceof AcademyCampusLocation campus) {
                    campus.getLocationNewDayManager().processNewDayUnits(campaign);
                }
            }
        }
    }

    /**
     * Runs {@link #processNewDayUnits(Campaign)} for every top-level {@link IPlace} in the
     * campaign. Each call propagates recursively to child places (e.g., academy campuses hosted
     * under a base or under a fixed planet location).
     */
    public static void processAllLocationUnits(Campaign campaign) {
        // Main force — also picks up any local campus locations parented to Campaign's LocationNode
        campaign.getLocationNewDayManager().processNewDayUnits(campaign);

        // Player bases — each propagates to its own child campuses via processNewDayUnits
        for (PlayerBase base : campaign.getPlayerBases()) {
            base.getLocationNewDayManager().processNewDayUnits(campaign);
        }

        // Fixed planet locations — seed campus children; each campus propagates further
        for (AbstractLocation loc : campaign.getLocations()) {
            for (LocationNode child : loc.getLocationNode().getChildren()) {
                if (child.getLocatable() instanceof AcademyCampusLocation campus) {
                    campus.getLocationNewDayManager().processNewDayUnits(campaign);
                }
            }
        }
    }
}
