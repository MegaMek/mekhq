package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.logging.MMLogger;

public record AcsDetectionHelper(AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsDetectionHelper.class);

    /**
     * Performs sensor detection for all formations of all players and updates the
     * visibility status in the
     * game accordingly. Does not send anything.
     */
    void performSensorDetection() {
        // They are already at point blank range!!!
    }
}
