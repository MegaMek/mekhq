package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.GunEmplacement;
import megamek.common.HitData;
import megamek.common.Mek;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static megamek.common.Compute.randomInt;

public class GunEmplacementDamageHandler implements DamageHandler<GunEmplacement> {

    private final GunEmplacement entity;
    private final Random random = new Random();

    public GunEmplacementDamageHandler(GunEmplacement entity) {
        this.entity = entity;
    }

    @Override
    public GunEmplacement getEntity() {
        return entity;
    }

    @Override
    public int getHitLocation() {
        if (entity.isDestroyed()) {
            return -1;
        }
        return 0;
    }

}
