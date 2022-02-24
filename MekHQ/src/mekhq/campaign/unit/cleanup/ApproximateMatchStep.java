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

import megamek.common.AmmoType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;

public class ApproximateMatchStep extends UnscrambleStep {
    @Override
    public void visit(final EquipmentProposal proposal, final EquipmentPart part) {
        if (part instanceof AmmoBin) {
            visit(proposal, (AmmoBin) part);
        }
    }

    public void visit(final EquipmentProposal proposal, final AmmoBin ammoBin) {
        final Mounted mount = proposal.getEquipment(ammoBin.getEquipmentNum());
        if ((mount != null) && (mount.getType() instanceof AmmoType)
                && ammoBin.canChangeMunitions((AmmoType) mount.getType())) {
            proposal.proposeMapping(ammoBin, ammoBin.getEquipmentNum());
        }
    }
}
