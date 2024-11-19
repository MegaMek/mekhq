package mekhq.campaign.autoResolve.scenarioResolver.components;

import megamek.common.Compute;
import megamek.common.Entity;

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
