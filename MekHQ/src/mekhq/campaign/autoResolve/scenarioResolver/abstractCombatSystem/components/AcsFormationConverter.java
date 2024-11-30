/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.force.Force;
import megamek.common.strategicBattleSystems.BaseFormationConverter;

import java.util.Objects;

/**
 * @author Luana Coppio
 */
public final class AcsFormationConverter extends BaseFormationConverter<AcsFormation> {

    public AcsFormationConverter(Force force, IGame game) {
        super(force, game, new AcsFormation());
    }

    @Override
    public AcsFormation convert() {
        var formation = super.convert();
        return formation;
    }
}
