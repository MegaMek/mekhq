package mekhq.campaign.mission.enums;

import megamek.logging.MMLogger;

public enum ScenarioType {
    NONE,
    SPECIAL_LOSTECH,
    SPECIAL_RESUPPLY;

    /**
     * @return {@code true} if the scenario is considered a LosTech scenario, {@code false} otherwise.
     */
    public boolean isLosTech() {
        return this == SPECIAL_LOSTECH;
    }

    /**
     * @return {@code true} if the scenario is considered a Resupply scenario, {@code false} otherwise.
     */
    public boolean isResupply() {
        return this == SPECIAL_RESUPPLY;
    }

    public static ScenarioType fromOrdinal(int ordinal) {
        for (ScenarioType scenarioType : values()) {
            if (scenarioType.ordinal() == ordinal) {
                return scenarioType;
            }
        }

        final MMLogger logger = MMLogger.create(ScenarioType.class);
        logger.error(String.format("Unknown Scenario Type ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }
}
