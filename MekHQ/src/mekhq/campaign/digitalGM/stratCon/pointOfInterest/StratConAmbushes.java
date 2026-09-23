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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.MEK;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * Sets up the ambushes that points of interest can spring on a deploying formation.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class StratConAmbushes {
    private StratConAmbushes() {
    }

    /**
     * Places an ambush on a point of interest's hex and links the point of interest to it. The ambush is drawn from the
     * named template, if there is one, or else from the templates suited to ambushing the deploying formation's unit
     * type; and - like any ambush - is a Crisis and never a Turning Point. It is not Essential. The deploying formation
     * is then assigned to it like any scenario found on the hex.
     *
     * @param pointOfInterest the point of interest the ambush is at
     * @param track           the sector it sits in
     * @param formationId     the ID of the deploying formation
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     * @param templateName    the file name of the template to draw the ambush from, or {@code null} to draw it from the
     *                        templates suited to ambushing the formation's unit type
     *
     * @return the ambush, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          int formationId, AbstractContract contract, Campaign campaign, @Nullable String templateName) {
        // With no usable template, a random scenario springs the ambush instead; a missing named one is logged.
        ScenarioTemplate template;
        if (templateName != null) {
            template = StratConScenarioFactory.getSpecificScenario(templateName);
        } else {
            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            int unitType = (formation == null) ? MEK : formation.getPrimaryUnitType(campaign);
            template = StratConScenarioFactory.getRandomScenario(unitType, true, false);
        }

        // Facilities are ignored: these points of interest occupy their hex, so none can share it.
        StratConScenario ambush = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              null,
              campaign,
              contract,
              track,
              template,
              true,
              null);
        if (ambush == null) {
            return null;
        }

        ambush.getBackingScenario().setIsCrisis(true);
        ambush.setTurningPoint(false);
        track.addScenario(ambush);
        StratConPointOfInterestRules.linkScenario(pointOfInterest, ambush);
        return ambush;
    }
}
