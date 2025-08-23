/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.List;
import java.util.Objects;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Mounted;
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

    protected abstract @Nullable String createReport(EquipmentProposal proposal);

    public static EquipmentUnscrambler create(final Unit unit) {
        Objects.requireNonNull(unit, "Unit must not be null");
        if (unit.getEntity() instanceof BattleArmor) {
            return new BattleArmorEquipmentUnscrambler(unit);
        } else {
            return new DefaultEquipmentUnscrambler(unit);
        }
    }
}
