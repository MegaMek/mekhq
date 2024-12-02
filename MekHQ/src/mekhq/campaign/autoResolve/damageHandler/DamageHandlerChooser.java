package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

public class DamageHandlerChooser {


    public static DamageHandler<?> chooseHandler(Entity entity) {
        return chooseHandler(entity, CrewMustSurvive.NO, EntityMustSurvive.NO);
    }

    public static DamageHandler<?> chooseHandler(Entity entity, CrewMustSurvive crewMustSurvive, EntityMustSurvive entityMustSurvive) {
        if (entity instanceof Infantry) {
            return new InfantryDamageHandler((Infantry) entity);
        } else if (entity instanceof Mek) {
            return new MekDamageHandler((Mek) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementDamageHandler((GunEmplacement) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof Aero) {
            return new AeroDamageHandler((Aero) entity, crewMustSurvive, entityMustSurvive);
        }
        return new SimpleDamageHandler(entity, crewMustSurvive, entityMustSurvive);
    }

}
