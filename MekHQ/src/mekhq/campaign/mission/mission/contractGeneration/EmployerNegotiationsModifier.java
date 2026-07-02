package mekhq.campaign.mission.mission.contractGeneration;

import mekhq.campaign.universe.Faction;

public enum EmployerNegotiationsModifier {
    EMPLOYER_SUPER_POWER(1.3, 0, 0, 1, 2),
    EMPLOYER_MAJOR_POWER(1.2, 0, -1, 0, 1),
    EMPLOYER_MINOR_POWER(1.1, 0, -2, 0, 0),
    EMPLOYER_INDEPENDENT_WORLD(1.0, 0, -1, -1, 0),
    EMPLOYER_MERCENARY_OR_CORPORATION(1.1, -1, 2, 1, 1),
    TYPICAL_NO_MODIFIERS(1.0, 0, 0, 0, 0),
    EMPLOYER_GENEROSITY_STINGY(-0.2, 0, -1, -1, -1),
    EMPLOYER_GENEROSITY_GENEROUS(0.2, 0, 1, 2, 1),
    EMPLOYER_OVERSIGHT_CONTROLLING(0, -2, -1, 0, 0),
    EMPLOYER_OVERSIGHT_LENIENT(0, 1, 1, 0, 0),
    ERA_PRE_SUCCESSION_WARS(0, 0, -2, 0, 0),
    ERA_POST_CLAN_INVASION(0, 0, -2, 0, 0);

    private final double employmentMultiplier;
    private final int commandModifier;
    private final int salvageModifier;
    private final int supportModifier;
    private final int transportModifier;

    EmployerNegotiationsModifier(final double employmentMultiplier, final int commandModifier,
          final int salvageModifier, final int supportModifier, final int transportModifier) {
        this.employmentMultiplier = employmentMultiplier;
        this.commandModifier = commandModifier;
        this.salvageModifier = salvageModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
    }

    public double getEmploymentMultiplier() {
        return employmentMultiplier;
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

    public static void getNegotiationsModifier(Faction employer, int year, NegotiationsModifierData modifierData) {
        getFactionModifiers(employer, modifierData);
        getGenerosityModifiers(employer, modifierData);
        getOversightModifiers(employer, modifierData);
        getEraModifiers(year, modifierData);
    }

    private static void getFactionModifiers(Faction employer, NegotiationsModifierData modifierData) {
        if (employer.isSuperPower()) {
            applyNegotiationModifiers(EMPLOYER_SUPER_POWER, modifierData);
        } else if (employer.isMajorPower()) {
            applyNegotiationModifiers(EMPLOYER_MAJOR_POWER, modifierData);
        } else if (employer.isMinorPower()) {
            applyNegotiationModifiers(EMPLOYER_MINOR_POWER, modifierData);
        } else if (employer.isIndependent()) {
            applyNegotiationModifiers(EMPLOYER_INDEPENDENT_WORLD, modifierData);
        } else if (employer.isMercenary() || employer.isCorporation()) {
            applyNegotiationModifiers(EMPLOYER_MERCENARY_OR_CORPORATION, modifierData);
        } else {
            applyNegotiationModifiers(TYPICAL_NO_MODIFIERS, modifierData);
        }
    }

    private static void getGenerosityModifiers(Faction employer, NegotiationsModifierData modifierData) {
        if (employer.isGenerous()) {
            applyNegotiationModifiers(EMPLOYER_GENEROSITY_GENEROUS, modifierData);
        } else if (employer.isStingy()) {
            applyNegotiationModifiers(EMPLOYER_GENEROSITY_STINGY, modifierData);
        }
    }

    private static void getOversightModifiers(Faction employer, NegotiationsModifierData modifierData) {
        if (employer.isControlling()) {
            applyNegotiationModifiers(EMPLOYER_OVERSIGHT_CONTROLLING, modifierData);
        } else if (employer.isLenient()) {
            applyNegotiationModifiers(EMPLOYER_OVERSIGHT_LENIENT, modifierData);
        }
    }

    private static void getEraModifiers(int year, NegotiationsModifierData modifierData) {
        if (year <= 2780) {
            applyNegotiationModifiers(ERA_PRE_SUCCESSION_WARS, modifierData);
        } else if (year > 3061) {
            applyNegotiationModifiers(ERA_POST_CLAN_INVASION, modifierData);
        }
    }

    private static void applyNegotiationModifiers(EmployerNegotiationsModifier modifierEntry,
          NegotiationsModifierData modifierData) {
        modifierData.modifyTempoMultiplier(modifierEntry.getEmploymentMultiplier());
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
