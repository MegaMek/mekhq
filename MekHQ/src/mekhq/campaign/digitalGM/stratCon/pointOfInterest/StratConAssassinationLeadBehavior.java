package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

/**
 * The behavior of a lead on an assassination target, placed in place of every point of interest on an Assassination
 * contract. It works just as a Mole Hunting contract's potential lead does (see {@link StratConPotentialLeadBehavior}),
 * but a lead that pans out is fought as a {@value #SCENARIO_TEMPLATE} scenario against the target, and its reports
 * speak of the target rather than a mole.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConAssassinationLeadBehavior extends StratConPotentialLeadBehavior {
    /** The behavior ID the assassination lead definition names. */
    public static final String BEHAVIOR_ID = "assassinationLead";

    /** The type ID of the assassination lead definition. */
    public static final String TYPE_ID = "AssassinationLead";

    /** The scenario template fought over a lead that pans out. */
    static final String SCENARIO_TEMPLATE = "Assassination.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConAssassinationLeadBehavior";
    }
}
