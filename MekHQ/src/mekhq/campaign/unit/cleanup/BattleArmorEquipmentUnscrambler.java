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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;

public class BattleArmorEquipmentUnscrambler extends EquipmentUnscrambler {
    // region Constructors
    public BattleArmorEquipmentUnscrambler(final Unit unit) {
        super(unit);

        if (!(unit.getEntity() instanceof BattleArmor)) {
            throw new IllegalArgumentException("Attempting to assign trooper values to parts for non-BA unit");
        }
    }
    // endregion Constructors

    @Override
    public EquipmentUnscramblerResult unscramble() {
        assignTroopersAndEquipmentNums(unit);

        EquipmentUnscramblerResult result = new EquipmentUnscramblerResult(unit);
        result.setSucceeded(true);
        return result;
    }

    public static void assignTroopersAndEquipmentNums(final Unit unit) {
        if (!(unit.getEntity() instanceof BattleArmor)) {
            throw new IllegalArgumentException("Attempting to assign trooper values to parts for non-BA unit");
        }

        // Create a list that we can remove parts from as we match them
        final List<EquipmentPart> tempParts = unit.getParts().stream()
                                                    .filter(p -> p instanceof EquipmentPart)
                                                    .map(p -> (EquipmentPart) p)
                                                    .collect(Collectors.toList());

        for (final Mounted<?> m : unit.getEntity().getEquipment()) {
            final int eqNum = unit.getEntity().getEquipmentNum(m);
            // Look for parts of the same type with the equipment number already set
            // correctly
            List<EquipmentPart> parts = tempParts.stream()
                                              .filter(part -> part.getType()
                                                                    .getInternalName()
                                                                    .equals(m.getType().getInternalName())
                                                                    && part.getEquipmentNum() == eqNum)
                                              .collect(Collectors.toList());

            // If we don't find any, just match the internal name and set the equipment
            // number.
            if (parts.isEmpty()) {
                parts = tempParts.stream()
                              .filter(part -> part.getType().getInternalName().equals(m.getType().getInternalName()))
                              .collect(Collectors.toList());

                parts.forEach(part -> part.setEquipmentNum(eqNum));
            }

            if (parts.stream().allMatch(part -> part instanceof BattleArmorEquipmentPart)) {
                // Try to find one for each trooper; if the Entity has multiple pieces of
                // equipment
                // of this type this will make sure we're only setting one group to this eq
                // number.
                Part[] perTrooper = new Part[unit.getEntity().locations() - 1];
                for (EquipmentPart p : parts) {
                    int trooper = ((BattleArmorEquipmentPart) p).getTrooper();
                    if (trooper > 0) {
                        perTrooper[trooper - 1] = p;
                    }
                }

                // Assign a part to any empty position and set the trooper field
                for (int t = 0; t < perTrooper.length; t++) {
                    if (perTrooper[t] == null) {
                        for (final Part part : parts) {
                            if (((BattleArmorEquipmentPart) part).getTrooper() < 1) {
                                ((BattleArmorEquipmentPart) part).setTrooper(t + 1);
                                perTrooper[t] = part;
                                break;
                            }
                        }
                    }
                }

                // Normally there should be a part in each position, but we will leave open the
                // possibility of equipment missing equipment for some troopers in the case of
                // modular/AP mounts or DWPs
                for (final Part part : perTrooper) {
                    if (part != null) {
                        tempParts.remove(part);
                    }
                }
            } else {
                // Ammo Bin
                tempParts.removeAll(parts);
            }
        }

        // TODO: Is it necessary to update armor?
    }

    @Override
    protected EquipmentProposal createProposal() {
        final EquipmentProposal proposal = new BattleArmorEquipmentProposal(unit);
        for (final Part part : unit.getParts()) {
            proposal.consider(part);
        }

        for (final Mounted<?> m : unit.getEntity().getEquipment()) {
            proposal.includeEquipment(unit.getEntity().getEquipmentNum(m), m);
        }

        return proposal;
    }

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
