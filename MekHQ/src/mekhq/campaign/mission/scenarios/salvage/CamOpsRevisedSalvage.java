/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
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
