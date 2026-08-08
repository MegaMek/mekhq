package mekhq.campaign.personnel.familiarity;

import static java.lang.Math.round;
import static megamek.common.compute.Compute.d6;

public enum FamiliarityGainType {
    D6,
    D3,
    SINGLE;

    public int rollFamiliarity(int speed) {
        return switch (this) {
            case D6 -> d6(speed);
            case D3 -> (int) round(d6(speed) * 0.5);
            case SINGLE -> speed;
        };
    }
}
