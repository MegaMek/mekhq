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
 * A mutable accumulator of the negotiation modifiers an employer contributes during contract generation. Multiple
 * sources (faction, generosity, oversight, era, unit reputation, special objectives) each add their deltas via the
 * {@code modify*} methods, and the running totals are read back with the getters.
 */
public class EmployerModifierData {
    private double employmentMultiplier = 0.0;
    private double tempoMultiplier = 0.0;
    private int commandModifier = 0;
    private int salvageModifier = 0;
    private int supportModifier = 0;
    private int transportModifier = 0;

    /**
     * Creates an accumulator with all modifiers initialized to zero.
     */
    public EmployerModifierData() {}

    /**
     * @return the accumulated employment multiplier
     */
    public double getEmploymentMultiplier() {
        return employmentMultiplier;
    }

    /**
     * Adds the given delta to the accumulated employment multiplier.
     *
     * @param delta the amount to add
     */
    public void modifyEmploymentMultiplier(double delta) {
        employmentMultiplier += delta;
    }

    /**
     * @return the accumulated operations-tempo multiplier
     */
    public double getTempoMultiplier() {
        return tempoMultiplier;
    }

    /**
     * Adds the given delta to the accumulated operations-tempo multiplier.
     *
     * @param delta the amount to add
     */
    public void modifyTempoMultiplier(double delta) {
        tempoMultiplier += delta;
    }

    /**
     * @return the accumulated command-rights modifier
     */
    public int getCommandModifier() {
        return commandModifier;
    }

    /**
     * Adds the given delta to the accumulated command-rights modifier.
     *
     * @param delta the amount to add
     */
    public void modifyCommandModifier(int delta) {
        commandModifier += delta;
    }

    /**
     * @return the accumulated salvage-rights modifier
     */
    public int getSalvageModifier() {
        return salvageModifier;
    }

    /**
     * Adds the given delta to the accumulated salvage-rights modifier.
     *
     * @param delta the amount to add
     */
    public void modifySalvageModifier(int delta) {
        salvageModifier += delta;
    }

    /**
     * @return the accumulated support-rights modifier
     */
    public int getSupportModifier() {
        return supportModifier;
    }

    /**
     * Adds the given delta to the accumulated support-rights modifier.
     *
     * @param delta the amount to add
     */
    public void modifySupportModifier(int delta) {
        supportModifier += delta;
    }

    /**
     * @return the accumulated transport-rights modifier
     */
    public int getTransportModifier() {
        return transportModifier;
    }

    /**
     * Adds the given delta to the accumulated transport-rights modifier.
     *
     * @param delta the amount to add
     */
    public void modifyTransportModifier(int delta) {
        transportModifier += delta;
    }
}
