package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

public class DamageHandlerChooser {

    public static DamageHandler<?> chooseHandler(Entity entity) {
        if (entity instanceof Infantry) {
            return new InfantryDamageHandler((Infantry) entity);
        } else if (entity instanceof Mek) {
            return new MekDamageHandler((Mek) entity);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementDamageHandler((GunEmplacement) entity);
        } else if (entity instanceof Aero) {
            return new AeroDamageHandler((Aero) entity);
        }
        return new SimpleDamageHandler(entity);
    }

}
