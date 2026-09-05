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
package mekhq.campaign.universe.commandGeneration;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.Unit;

/**
 * The single decision point for whether support carriers may deploy to a scenario.
 *
 * <p>Support carriers hold technicians, doctors and administrators for the TOE. They are unarmoured civilians carrying
 * knives, and every casualty is a real support character off the roster, so today they never deploy: not by the TOE
 * context menu, not when a formation containing them - HQ, typically - is assigned, and not by StratCon. Deploying a
 * formation that holds carriers deploys its fighting units and leaves the carriers, and their people, at home.</p>
 *
 * <p>That is a rule for now, not forever. The one situation in which support staff should ever be engaged is an attack
 * on the player's own base, and when that scenario type exists it opens this gate for itself - behind a campaign
 * option, so a player who does not want their technicians fighting for the motor pool never has to. Every site that
 * decides whether a carrier deploys asks here, so opening it means changing one method, not hunting for the call
 * sites. The reconciler is already deployment-aware and needs no change when that happens: it leaves a deployed
 * carrier alone and catches its profession up when the deployment ends.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportCarrierDeployment {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SupportPersonnelToTOE";

    private SupportCarrierDeployment() {
        // utility class
    }

    /**
     * Whether support carriers may deploy to this scenario.
     *
     * @param scenario the scenario being deployed to; {@code null} when the deployment has no scenario context, which
     *                 is treated like any other scenario
     *
     * @return {@code true} if carriers may deploy. Always {@code false} today; a future scenario type that pulls
     *       support staff into a fight returns {@code true} for itself here
     */
    public static boolean isAllowed(@Nullable Scenario scenario) {
        return false;
    }

    /**
     * Whether this one unit is a carrier that stays home for this scenario.
     *
     * @param unit     the unit; {@code null} is not a carrier
     * @param scenario the scenario, for the gate
     *
     * @return {@code true} if the unit is a carrier and the gate is closed
     */
    public static boolean staysHome(@Nullable Unit unit, @Nullable Scenario scenario) {
        return (unit != null) && unit.isCarrier() && !isAllowed(scenario);
    }

    /**
     * The carriers under a formation that will stay home if it is assigned to this scenario.
     *
     * @param campaign  the campaign
     * @param formation the formation being assigned; its whole subtree is inspected
     * @param scenario  the scenario, for the gate
     *
     * @return the carriers that would be left behind; empty when the gate is open or there are none
     */
    public static List<Unit> carriersStayingHome(Campaign campaign, Formation formation, @Nullable Scenario scenario) {
        List<Unit> stayingHome = new ArrayList<>();
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (staysHome(unit, scenario)) {
                stayingHome.add(unit);
            }
        }
        return stayingHome;
    }

    /**
     * Whether assigning this formation would send nothing at all, because every unit under it is a carrier that stays
     * home. Such a formation should not be offered for deployment, and an attempt to assign it should be refused with
     * an explanation rather than silently accepted.
     *
     * @param campaign  the campaign
     * @param formation the formation
     * @param scenario  the scenario, for the gate
     *
     * @return {@code true} if the formation has units and all of them would stay home
     */
    public static boolean deploysNothing(Campaign campaign, Formation formation, @Nullable Scenario scenario) {
        List<UUID> unitIds = formation.getAllUnits(false);
        if (unitIds.isEmpty()) {
            return false;
        }
        return carriersStayingHome(campaign, formation, scenario).size() == unitIds.size();
    }

    /**
     * The dialog text explaining which carriers stayed home when these formations were assigned, or {@code null} when
     * none did. Callers show it with a plain information dialog; the campaign report carries the same fact.
     *
     * @param campaign   the campaign
     * @param formations the formations that were assigned
     * @param scenario   the scenario they were assigned to
     *
     * @return the HTML body for a dialog, or {@code null} if nothing stayed home
     */
    public static @Nullable String stayingHomeMessage(Campaign campaign, Collection<Formation> formations,
          @Nullable Scenario scenario) {
        List<String> lines = new ArrayList<>();
        for (Formation formation : formations) {
            int count = carriersStayingHome(campaign, formation, scenario).size();
            if (count > 0) {
                lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.stayHome.line", count,
                      formation.getName()));
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.stayHome.body",
              String.join("<br>", lines));
    }

    /** @return the dialog title for {@link #stayingHomeMessage} */
    public static String stayingHomeTitle() {
        return getTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.stayHome.title");
    }

    /**
     * The dialog text for a formation that holds only carriers.
     *
     * @param formation the formation the player tried to assign
     *
     * @return the HTML body for an error dialog
     */
    public static String nothingToDeployMessage(Formation formation) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.nothingToDeploy.body",
              formation.getName());
    }

    /** @return the dialog title for {@link #nothingToDeployMessage} */
    public static String nothingToDeployTitle() {
        return getTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.nothingToDeploy.title");
    }
}
