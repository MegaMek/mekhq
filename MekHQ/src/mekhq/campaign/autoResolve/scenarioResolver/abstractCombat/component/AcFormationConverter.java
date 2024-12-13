/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component;

import megamek.common.IGame;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.force.Force;
import megamek.common.strategicBattleSystems.BaseFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

/**
 * @author Luana Coppio
 */
public final class AcFormationConverter extends BaseFormationConverter<AcFormation> {

    public AcFormationConverter(Force force, IGame game) {
        super(force, game, new AcFormation());
    }

    @Override
    public AcFormation unsafeConvert() {
        var formation = super.unsafeConvert();
        var asDamageVector = setStdDamageForFormation(formation);
        formation.setStdDamage(asDamageVector);
        return formation;
    }

    private ASDamageVector setStdDamageForFormation(AcFormation formation) {
        var damages = formation.getUnits().stream().map(SBFUnit::getDamage).toList();
        var size = damages.size();
        var l = 0;
        var m = 0;
        var s = 0;
        for (var damage : damages) {
            l += damage.L.damage;
            m += damage.M.damage;
            s += damage.S.damage;
        }
        return new ASDamageVector(
            new ASDamage((double) s/size),
            new ASDamage((double) m/size),
            new ASDamage((double) l/size),
            null, 3, true);
    }

}
