/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.components;

import megamek.common.Compute;
import megamek.common.Entity;

/**
 * @author Luana Coppio
 */
public class UnitStrength {

    public final int BV;
    public final int modifier;
    public final Entity entity;
    public final int numberOfDices;

    public UnitStrength(int BV, int modifier, Entity entity) {
        this.BV = BV;
        this.modifier = modifier;
        this.entity = entity;
        this.numberOfDices = numberOfDices();
    }

    private int numberOfDices() {
        return (int) Math.max(Math.floor((double) BV / 1000), 1);
    }

    public int diceRoll() {
        var dices = numberOfDices();
        if (dices < 2) {
            return Math.min(Math.max(Compute.d6() + modifier, 1), 6);
        }
        return Math.min(Math.max(Compute.d6(numberOfDices, 1) + modifier, 1), 6);
    }

    public Entity entity() {
        return entity;
    }

    public int getBV() {
        return BV;
    }

    public int getModifier() {
        return modifier;
    }

    public String toString() {
        return entity.getDisplayName() + " BV: " + BV + " Modifier: " + modifier + " Dices: " + numberOfDices;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UnitStrength) {
            var other = (UnitStrength) obj;
            return BV == other.BV && modifier == other.modifier && entity.equals(other.entity);
        }
        return false;
    }
}
