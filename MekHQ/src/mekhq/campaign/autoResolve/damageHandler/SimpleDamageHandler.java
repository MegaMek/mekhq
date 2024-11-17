package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

import java.util.Random;

public class SimpleDamageHandler implements DamageHandler<Entity> {

    private final Entity entity;
    private final Random random = new Random();

    public SimpleDamageHandler(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

}
