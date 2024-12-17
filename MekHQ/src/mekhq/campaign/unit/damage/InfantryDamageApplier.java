/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit.damage;

import megamek.common.*;

/**
 * @author Luana Coppio
 */
public record InfantryDamageApplier(Infantry entity) implements DamageApplier<Infantry> {

    @Override
    public boolean crewMustSurvive() {
        return false;
    }

    @Override
    public boolean entityMustSurvive() {
        return false;
    }

    @Override
    public void damageArmor(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValueArmor = te.getArmor(BattleArmor.LOC_SQUAD);
                var newArmorValue = Math.max(currentValueArmor - 1, 0);
                if (te.getArmor(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setArmor(newArmorValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        }
        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
    }

    @Override
    public void damageInternals(HitDetails hitDetails) {
        if (entity() instanceof BattleArmor te) {
            for (int i = 0; i < te.getTroopers(); i++) {
                var currentValue = te.getInternal(BattleArmor.LOC_SQUAD);
                var newValue = Math.max(currentValue - 1, 0);
                if (te.getInternal(BattleArmor.LOC_TROOPER_1 + i) > 0) {
                    te.setInternal(newValue, BattleArmor.LOC_TROOPER_1 + i);
                }
            }
        } else {
            var currentValue = entity().getInternal(Infantry.LOC_INFANTRY);
            var newValue = Math.max(currentValue - 1, 0);
            entity().setInternal(newValue, Infantry.LOC_INFANTRY);
        }

        if (entity().isCrippled()) {
            entity().setDestroyed(true);
        }
    }
}
