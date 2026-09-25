package mekhq.campaign.mission.scenarios.salvage;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How a single recovery unit brings in a wreck on the ground, where it could do either.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum RecoveryMethod {
    /** Carried in the unit's cargo space, which can be shared with other wrecks. */
    CARRY,
    /** Dragged, which commits the unit to this wreck alone. */
    DRAG;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.recoveryMethod." + name());
    }
}
