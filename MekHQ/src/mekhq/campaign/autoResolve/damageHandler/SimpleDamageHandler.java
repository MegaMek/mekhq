package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.Entity;

public record SimpleDamageHandler(Entity entity, CrewMustSurvive crewMustSurvive, EntityMustSurvive entityMustSurvive)
    implements DamageHandler<Entity> {}
