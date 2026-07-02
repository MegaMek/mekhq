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
