package mekhq.campaign.mission.scenarios.salvage;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.unit.Unit;

/**
 * The {@link SalvageSystem#CAM_OPS_REVISED CamOps (Revised)} salvage system: Campaign Operations salvage with MekHQ
 * revisions.
 *
 * <p>On top of {@link CamOpsStrictSalvage CamOps (Strict)}:</p>
 * <ul>
 *   <li>Salvage formations may fight in a scenario and still salvage it.</li>
 *   <li>Salvaging 'Meks need working legs.</li>
 *   <li>'Meks without two working hands may still salvage using cargo space, such as lift hoists (but can't drag
 *   salvage).</li>
 *   <li>Carrying units may recover as many wrecks as fit in their cargo space (or, in space, suitable bays).</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CamOpsRevisedSalvage extends CamOpsStrictSalvage {
    @Override
    public boolean isSalvageFormationCombatAllowed() {
        return true;
    }

    @Override
    public boolean isMultipleSalvagePerUnitAllowed() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A 'Mek without two working hands can also salvage on the ground, using its cargo space. It still needs
     * working hands to drag salvage.</p>
     */
    @Override
    public boolean canSalvage(Unit unit, boolean isInSpace) {
        if (super.canSalvage(unit, isInSpace)) {
            return true;
        }

        return !isInSpace &&
                     (unit.getEntity() instanceof Mek) &&
                     unit.isFullyCrewed() &&
                     (unit.getCargoCapacityForSalvage() > 0.0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A 'Mek with a destroyed leg, or that is permanently immobilized, can't salvage.</p>
     */
    @Override
    protected boolean isImmobilized(Entity entity) {
        if (entity instanceof Mek mek) {
            return mek.atLeastOneBadLeg() || mek.isPermanentlyImmobilized(false);
        }

        return super.isImmobilized(entity);
    }
}
