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

/**
 * The negotiation modifiers contributed by special contract objective characteristics &mdash; high-risk and covert
 * operations. Each constant bundles a tempo multiplier with command, salvage, support, and transport modifiers, applied
 * to an {@link EmployerModifierData} accumulator via {@link #applyHighRisk(EmployerModifierData)} and
 * {@link #applyCovert(EmployerModifierData)}.
 */
public enum SpecialNegotiationsModifier {
    /** Modifiers applied when the contract's objectives make it high-risk. */
    OBJECTIVE_HIGH_RISK(0.5, -1, -2, 1, 0),
    /** Modifiers applied when the contract's objectives are covert. */
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

    /**
     * @return the operations-tempo multiplier contributed by this modifier
     */
    public double getTempoMultiplier() {
        return tempoMultiplier;
    }

    /**
     * @return the command-rights modifier contributed by this modifier
     */
    public int getCommandModifier() {
        return commandModifier;
    }

    /**
     * @return the salvage-rights modifier contributed by this modifier
     */
    public int getSalvageModifier() {
        return salvageModifier;
    }

    /**
     * @return the support-rights modifier contributed by this modifier
     */
    public int getSupportModifier() {
        return supportModifier;
    }

    /**
     * @return the transport-rights modifier contributed by this modifier
     */
    public int getTransportModifier() {
        return transportModifier;
    }

    /**
     * Applies the high-risk objective modifiers to the given accumulator.
     *
     * @param modifierData the accumulator to apply the modifiers to
     */
    public static void applyHighRisk(EmployerModifierData modifierData) {
        applyNegotiationModifiers(OBJECTIVE_HIGH_RISK, modifierData);
    }

    /**
     * Applies the covert objective modifiers to the given accumulator.
     *
     * @param modifierData the accumulator to apply the modifiers to
     */
    public static void applyCovert(EmployerModifierData modifierData) {
        applyNegotiationModifiers(OBJECTIVE_COVERT, modifierData);
    }

    private static void applyNegotiationModifiers(SpecialNegotiationsModifier modifierEntry,
          EmployerModifierData modifierData) {
        modifierData.modifyTempoMultiplier(modifierEntry.getTempoMultiplier());
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
