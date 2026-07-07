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
package mekhq.campaign.mission.newContract.contractGeneration;

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

    public static void getNegotiationsModifier(Faction employer, int year, EmployerModifierData modifierData) {
        getFactionModifiers(employer, modifierData);
        getGenerosityModifiers(employer, modifierData);
        getOversightModifiers(employer, modifierData);
        getEraModifiers(year, modifierData);
    }

    private static void getFactionModifiers(Faction employer, EmployerModifierData modifierData) {
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

    private static void getGenerosityModifiers(Faction employer, EmployerModifierData modifierData) {
        if (employer.isGenerous()) {
            applyNegotiationModifiers(EMPLOYER_GENEROSITY_GENEROUS, modifierData);
        } else if (employer.isStingy()) {
            applyNegotiationModifiers(EMPLOYER_GENEROSITY_STINGY, modifierData);
        }
    }

    private static void getOversightModifiers(Faction employer, EmployerModifierData modifierData) {
        if (employer.isControlling()) {
            applyNegotiationModifiers(EMPLOYER_OVERSIGHT_CONTROLLING, modifierData);
        } else if (employer.isLenient()) {
            applyNegotiationModifiers(EMPLOYER_OVERSIGHT_LENIENT, modifierData);
        }
    }

    private static void getEraModifiers(int year, EmployerModifierData modifierData) {
        if (year <= 2780) {
            applyNegotiationModifiers(ERA_PRE_SUCCESSION_WARS, modifierData);
        } else if (year > 3061) {
            applyNegotiationModifiers(ERA_POST_CLAN_INVASION, modifierData);
        }
    }

    private static void applyNegotiationModifiers(EmployerNegotiationsModifier modifierEntry,
          EmployerModifierData modifierData) {
        modifierData.modifyTempoMultiplier(modifierEntry.getEmploymentMultiplier());
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
