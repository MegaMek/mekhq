package mekhq.campaign.mission.mission.contractGeneration;

public enum SpecialNegotiationsModifier {
    OBJECTIVE_HIGH_RISK(0.5, -1, -2, 1, 0),
    OBJECTIVE_COVERT(0.3, 1, 1, -1, -1);

    private final double tempoMultiplier;
    private final int commandModifier;
    private final int salvageModifier;
    private final int supportModifier;
    private final int transportModifier;

    SpecialNegotiationsModifier(final double tempoMultiplier, final int commandModifier,
          final int salvageModifier, final int supportModifier, final int transportModifier) {
        this.tempoMultiplier = tempoMultiplier;
        this.commandModifier = commandModifier;
        this.salvageModifier = salvageModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
    }

    public double getTempoMultiplier() {
        return tempoMultiplier;
    }

    public int getCommandModifier() {
        return commandModifier;
    }

    public int getSalvageModifier() {
        return salvageModifier;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public static void getNegotiationsModifier(boolean isHighRisk, boolean isCovert,
          NegotiationsModifierData modifierData) {
        if (isHighRisk) {
            applyNegotiationModifiers(OBJECTIVE_HIGH_RISK, modifierData);
        }

        if (isCovert) {
            applyNegotiationModifiers(OBJECTIVE_COVERT, modifierData);
        }
    }

    private static void applyNegotiationModifiers(SpecialNegotiationsModifier modifierEntry,
          NegotiationsModifierData modifierData) {
        modifierData.modifyTempoMultiplier(modifierEntry.getTempoMultiplier());
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
