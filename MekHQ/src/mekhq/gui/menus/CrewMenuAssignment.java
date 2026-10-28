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
package mekhq.gui.menus;

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Locale;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Problem;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService.CommitReport;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.campaign.unit.crewAssignment.SeatType;

/**
 * Applies a crew assignment chosen from a right-click menu, the same way the crew assignment dialog does: through an
 * {@link AssignmentPlan} and {@link CrewAssignmentService}. So a menu assignment that cannot go ahead leaves the person
 * in the seat they already hold, rather than taking them out of it first.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewMenuAssignment {
    private static final String UNIT_RESOURCE_BUNDLE = "mekhq.resources.Unit";

    private CrewMenuAssignment() {
    }

    /**
     * Puts each person, in order, into a seat on a unit, until the seat is full. People already crewing the unit are
     * left where they are. Anyone held back because they are not at the unit's location is named in the daily report.
     *
     * @param campaign the campaign
     * @param unit     the unit
     * @param seat     the seat
     * @param people   the people
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void assign(Campaign campaign, Unit unit, SeatType seat, Person... people) {
        CampaignOptions options = campaign.getCampaignOptions();
        AssignmentPlan plan = new AssignmentPlan(options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL),
              options.get(CampaignOption.USE_IMPLANTS));
        for (Person person : people) {
            if (plan.canStage(person, unit, seat)) {
                plan.stage(person, unit, seat);
            } else if (!unit.equals(person.getUnit()) && !LocationUtils.areSameEffectiveLocation(unit, person)) {
                reportWrongLocation(campaign, person, unit, seat);
            }
        }

        CommitReport report = CrewAssignmentService.commit(plan, options.get(CampaignOption.USE_TRANSFERS),
              campaign.getLocalDate());
        for (Problem problem : report.skipped()) {
            if (problem.reason() == Reason.DIFFERENT_LOCATION) {
                reportWrongLocation(campaign, problem.change().person(), unit, seat);
            }
        }
    }

    /**
     * @return {@code true} if every person is qualified for the seat; whether it has room is not checked
     */
    static boolean allQualify(Campaign campaign, Unit unit, SeatType seat, Person... people) {
        CampaignOptions options = campaign.getCampaignOptions();
        boolean useAltAdvancedMedical = options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
        boolean useImplants = options.get(CampaignOption.USE_IMPLANTS);
        for (Person person : people) {
            if (!SeatEligibility.canTake(person, unit, seat, useAltAdvancedMedical, useImplants)) {
                return false;
            }
        }
        return true;
    }

    private static void reportWrongLocation(Campaign campaign, Person person, Unit unit, SeatType seat) {
        String seatName = seat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        campaign.addReport(TECHNICAL, getFormattedTextAt(UNIT_RESOURCE_BUNDLE,
              "Unit.assignmentBlockedByLocation.text", person.getHyperlinkedFullTitle(), seatName, unit.getName()));
    }
}
