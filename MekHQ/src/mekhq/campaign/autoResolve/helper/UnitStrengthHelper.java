package mekhq.campaign.autoResolve.helper;

import megamek.common.*;
import mekhq.campaign.autoResolve.scenarioResolver.components.UnitStrength;

public class UnitStrengthHelper {

    static public UnitStrength getUnitStrength(Entity entity) {
        // The values here are completely arbitrary and should be adjusted as needed, probably should be in a config file.
        if (entity instanceof Mek) {
            return new UnitStrength(entity.calculateBattleValue(), +1, entity);
        } else if (entity instanceof Infantry) {
            return new UnitStrength(entity.calculateBattleValue(), -2, entity);
        } else if (entity instanceof GunEmplacement) {
            return new UnitStrength(entity.calculateBattleValue(), -2, entity);
        } else if (entity instanceof Tank) {
            return new UnitStrength(entity.calculateBattleValue(), 0, entity);
        } else if (entity instanceof Aero) {
            return new UnitStrength(entity.calculateBattleValue(), -1, entity);
        }
        return new UnitStrength(entity.calculateBattleValue(), -1, entity);
    }
}
