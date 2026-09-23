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
