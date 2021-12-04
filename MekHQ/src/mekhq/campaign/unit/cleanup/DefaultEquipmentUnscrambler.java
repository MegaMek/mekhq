/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.Arrays;
import java.util.List;

import megamek.common.BattleArmor;
import mekhq.campaign.unit.Unit;

public class DefaultEquipmentUnscrambler extends EquipmentUnscrambler {
    //region Constructors
    public DefaultEquipmentUnscrambler(final Unit unit) {
        super(unit);

        if (unit.getEntity() instanceof BattleArmor) {
            throw new IllegalArgumentException("DefaultEquipmentUnscrambler cannot unscramble BattleArmorEquipmentParts");
        }
    }
    //endregion Constructors

    @Override
    protected List<UnscrambleStep> createSteps() {
        return Arrays.asList(new ExactMatchStep(), new ApproximateMatchStep(),
                new MovedEquipmentStep(), new MovedAmmoBinStep());
    }

    @Override
    protected String createReport(final EquipmentProposal proposal) {
        return EquipmentProposalReport.createReport(proposal);
    }
}
