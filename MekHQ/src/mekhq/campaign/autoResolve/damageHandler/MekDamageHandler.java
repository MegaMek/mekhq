package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MekDamageHandler implements DamageHandler<Mek> {

    private final Mek entity;
    private final Random random = new Random();

    public MekDamageHandler(Mek entity) {
        this.entity = entity;
    }

    @Override
    public Mek getEntity() {
        return entity;
    }

    @Override
    public HitDetails setupHitDetails(HitData hit, int dmg) {
        int originalArmor = entity.getOArmor(hit);
        int damageToApply = Math.max((int) Math.floor((double) originalArmor / 10), dmg);
        int currentArmorValue = entity.getArmor(hit);
        int setArmorValueTo = currentArmorValue - damageToApply;
        boolean hitInternal = setArmorValueTo < 0;
        boolean isHeadHit = (entity.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED)
            && (hit.getLocation() == Mek.LOC_HEAD);
        int hitCrew = isHeadHit || hitInternal ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }

    @Override
    public HitData getHitData(int hitLocation) {
        boolean isRear = hitLocation == Mek.LOC_CT || hitLocation == Mek.LOC_LT || hitLocation == Mek.LOC_RT;
        boolean isRearHit = isRear && entity.getArmor(hitLocation, true) > 0 && random.nextInt(10) == 0;
        return new HitData(hitLocation, isRearHit, HitData.EFFECT_NONE);
    }
}
