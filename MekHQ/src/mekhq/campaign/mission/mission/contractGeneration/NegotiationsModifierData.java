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

public class NegotiationsModifierData {
    private double employmentMultiplier = 0.0;
    private double tempoMultiplier = 0.0;
    private int commandModifier = 0;
    private int salvageModifier = 0;
    private int supportModifier = 0;
    private int transportModifier = 0;

    public NegotiationsModifierData() {}

    public double getEmploymentMultiplier() {
        return employmentMultiplier;
    }

    public void modifyEmploymentMultiplier(double delta) {
        employmentMultiplier += delta;
    }

    public double getTempoMultiplier() {
        return tempoMultiplier;
    }

    public void modifyTempoMultiplier(double delta) {
        tempoMultiplier += delta;
    }

    public int getCommandModifier() {
        return commandModifier;
    }

    public void modifyCommandModifier(int delta) {
        commandModifier += delta;
    }

    public int getSalvageModifier() {
        return salvageModifier;
    }

    public void modifySalvageModifier(int delta) {
        salvageModifier += delta;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public void modifySupportModifier(int delta) {
        supportModifier += delta;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public void modifyTransportModifier(int delta) {
        transportModifier += delta;
    }
}
