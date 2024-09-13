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

import java.util.List;
import java.util.Objects;

import megamek.common.BattleArmor;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public abstract class EquipmentUnscrambler {
    // region Variable Declarations
    protected final Unit unit;
    // endregion Variable Declarations

    // region Constructors
    protected EquipmentUnscrambler(final Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }
    // endregion Constructors

    public EquipmentUnscramblerResult unscramble() {
        final EquipmentProposal proposal = createProposal();
        for (final UnscrambleStep step : createSteps()) {
            if (proposal.isReduced()) {
                break;
            }

            for (final Part part : proposal.getParts()) {
                if (proposal.hasProposal(part)) {
                    continue;
                }

                if (part instanceof EquipmentPart) {
                    step.visit(proposal, (EquipmentPart) part);
                } else if (part instanceof MissingEquipmentPart) {
                    step.visit(proposal, (MissingEquipmentPart) part);
                }
            }
        }

        // Apply any changes to the equipment numbers
        proposal.apply();

        final EquipmentUnscramblerResult result = new EquipmentUnscramblerResult(unit);
        result.setSucceeded(proposal.isReduced());
        result.setMessage(createReport(proposal));

        return result;
    }

    protected EquipmentProposal createProposal() {
        final EquipmentProposal proposal = new EquipmentProposal(unit);
        for (final Part part : unit.getParts()) {
            proposal.consider(part);
        }

        for (final Mounted<?> m : unit.getEntity().getEquipment()) {
            proposal.includeEquipment(unit.getEntity().getEquipmentNum(m), m);
        }

        return proposal;
    }

    protected abstract List<UnscrambleStep> createSteps();

    protected abstract @Nullable String createReport(final EquipmentProposal proposal);

    public static EquipmentUnscrambler create(final Unit unit) {
        Objects.requireNonNull(unit, "Unit must not be null");
        if (unit.getEntity() instanceof BattleArmor) {
            return new BattleArmorEquipmentUnscrambler(unit);
        } else {
            return new DefaultEquipmentUnscrambler(unit);
        }
    }
}
