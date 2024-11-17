package mekhq.campaign.autoResolve.scenarioResolver.unitsMatter;

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
