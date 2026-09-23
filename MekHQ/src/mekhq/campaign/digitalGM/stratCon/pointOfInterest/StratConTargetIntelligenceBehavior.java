package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of target intelligence: a lead on the enemy's installations, placed in place of every point of interest
 * on an Objective Raid contract, which gets no Essential scenarios. Target intelligence is not a strategic objective
 * itself; but some intelligence leads to a facility, and destroying that facility is.
 *
 * <p>Which intelligence leads to a facility is settled when the contract is accepted: one per point of the contract's
 * scale, at most (see {@code StratConContractInitializer#markFacilityLeads}), so the facilities it turns up never
 * overwhelm the contract. When any formation deploys onto a lead's hex, there is no roll: a random hostile facility is
 * placed at an eligible hex elsewhere in the sector and revealed, and destroying it becomes a strategic objective of the
 * sector (see {@link StratConContractInitializer#spawnObjectiveFacility}).</p>
 *
 * <p>Any other intelligence is followed up with the usual scenario roll. With no scenario, it proves worthless. With
 * one, the enemy was waiting, and the formation is ambushed in a template suited to its unit type; the intelligence is
 * spent whatever the ambush's result. Neither pays a combat bonus. (See {@link StratConAmbushPointOfInterestBehavior}
 * for the rules it shares.)</p>
 *
 * <p>Target intelligence is hidden until scouted, and never expires.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConTargetIntelligenceBehavior extends StratConAmbushPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(StratConTargetIntelligenceBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /** The behavior ID the target intelligence definition names. */
    public static final String BEHAVIOR_ID = "targetIntelligence";

    /** The type ID of the target intelligence definition. */
    public static final String TYPE_ID = "TargetIntelligence";

    /** The state key marking target intelligence that leads to a facility; its value is {@code true}. */
    public static final String FACILITY_LEAD_STATE_KEY = "facilityLead";

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConTargetIntelligenceBehavior";
    }

    /**
     * Target intelligence pays no combat bonus of its own: a fight won at an objective facility it turns up pays it, as
     * any strategic-objective facility's scenario does.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * @param pointOfInterest a piece of target intelligence
     *
     * @return {@code true} if it was marked, when its contract was accepted, as leading to a facility
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFacilityLead(StratConPointOfInterest pointOfInterest) {
        return Boolean.parseBoolean(pointOfInterest.getStateValue(FACILITY_LEAD_STATE_KEY));
    }

    /**
     * Intelligence leading to a facility turns it up at once, with no roll; any other intelligence is followed up as
     * the shared ambush rules decide.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        if (!isFacilityLead(pointOfInterest) || pointOfInterest.hasLinkedScenario()) {
            return super.onFormationDeployed(pointOfInterest, track, formationId, campaign);
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of target intelligence {}.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        StratConCoords facilityCoords = StratConContractInitializer.spawnObjectiveFacility(track, contract, campaign);
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());

        if (facilityCoords == null) {
            // The lead was good, but the sector has no room left for another base: nothing comes of it.
            addReport(GENERAL, "noRoom.report", pointOfInterest, track, campaign);
        } else {
            campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
                  getResourceKeyPrefix() + ".facility.report",
                  pointOfInterest.getDisplayableName(),
                  track.getDisplayableName(),
                  facilityCoords.toBTString()));
        }
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }
}
