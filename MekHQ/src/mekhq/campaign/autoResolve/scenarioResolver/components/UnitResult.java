/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve.scenarioResolver.components;

/**
 * @author Luana Coppio
 */
public class UnitResult implements Comparable<UnitResult> {

    public final UnitStrength unitStrength;
    public final int diceResult;

    public UnitResult(UnitStrength unitStrength, int diceResult) {
        this.unitStrength = unitStrength;
        this.diceResult = diceResult;
    }

    public UnitStrength unitStrength() {
        return unitStrength;
    }

    public int diceResult() {
        return diceResult;
    }

    public String toString() {
        return unitStrength.toString() + " Dice result: " + diceResult;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UnitResult other) {
            return unitStrength.equals(other.unitStrength) && diceResult == other.diceResult;
        }
        return false;
    }

    public int hashCode() {
        return unitStrength.hashCode() + diceResult;
    }

    @Override
    public int compareTo(UnitResult other) {
        return Integer.compare(diceResult, other.diceResult);
    }

}
