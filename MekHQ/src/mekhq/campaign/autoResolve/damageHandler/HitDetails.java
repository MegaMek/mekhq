package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.HitData;

public record HitDetails(HitData hit, int damageToApply, int setArmorValueTo, boolean hitInternal, int hitCrew) {
    public HitDetails withIncrementedHitCrew() {
        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew + 1);
    }
}
