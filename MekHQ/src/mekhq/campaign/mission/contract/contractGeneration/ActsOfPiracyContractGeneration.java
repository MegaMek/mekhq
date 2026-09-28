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
package mekhq.campaign.mission.contract.contractGeneration;

import jakarta.annotation.Nonnull;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractNature;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.mission.contract.contractData.NonNegotiableTermsData;
import mekhq.campaign.mission.contract.utilities.NegotiationStepMath.Term;

/**
 * Generates acts-of-piracy contracts ({@link ContractSearchType#PIRATE}): a pirate band's opportunistic raid, or the
 * rare raid a real power has secretly bankrolled. Overrides the {@link AbstractContractGeneration} hooks with the
 * pirate-specific handlers - a fixed pirate-raid objective, covert status only when secretly bankrolled, and the
 * dictated, locked terms a raid carries - while inheriting the shared generation skeleton.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ActsOfPiracyContractGeneration extends AbstractContractGeneration {
    @Override
    protected ContractSearchType getSearchType() {
        return ContractSearchType.PIRATE;
    }

    /**
     * An acts-of-piracy search is always a pirate raid, rather than a roll on the general objective table.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nonnull ContractObjectiveData pickObjective(int contractGenerationModifier, ChaosContract contract) {
        ContractObjectiveData objectiveData = ChaosContractDeterminationObjective.determinePirateContractObjectiveType();
        contract.setObjectiveData(objectiveData);
        return objectiveData;
    }

    /**
     * A pirate raid is overt opportunism, except when a real power has secretly bankrolled it - identified by the hidden
     * sponsor the pirate employer determination attaches (see {@link ChaosContractDeterminationEmployerPirate}) - in
     * which case it is always run covert, so the band never learns who really pointed them at the target.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void determineCovertStatus(ChaosObjectiveType chaosObjectiveType, ChaosContract contract) {
        if (contract.getNature() != ContractNature.NORMAL) {
            return;
        }
        if (contract.getEmployerData().sponsorFactionCode() != null) {
            contract.setNature(ContractNature.COVERT);
        }
    }

    /**
     * Applies the fixed, non-negotiable terms every pirate raid carries, on top of the rolled baseline. Command rights
     * and salvage are always maxed - a pirate band answers to no one and keeps everything it takes. A self-directed
     * (non-covert) band additionally gets no support and no transport, fending for itself; a secretly-bankrolled
     * (covert) raid leaves those to its hidden sponsor's own determination. Every fixed term is then locked so it can be
     * neither re-negotiated, lowered, nor raised, regardless of the non-negotiable-terms campaign option.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void applyTypeSpecificTerms(ChaosContract contract) {
        boolean secretlyBankrolled = contract.isCovertOperation();

        ContractTermsData terms = contract.getContractTerms()
                                        .withCommandRights(ChaosContractStepsTable.STEP_SEVENTEEN)
                                        .withSalvageRights(ChaosContractStepsTable.STEP_SEVENTEEN);
        if (!secretlyBankrolled) {
            terms = terms.withSupport(ChaosContractStepsTable.STEP_ONE)
                          .withTransport(ChaosContractStepsTable.STEP_ONE);
        }
        contract.setContractTerms(terms);

        NonNegotiableTermsData locked = contract.getNonNegotiableTermsData();
        if (locked == null) {
            locked = NonNegotiableTermsData.none();
        }
        locked = locked.withLocked(Term.COMMAND_RIGHTS).withLocked(Term.SALVAGE);
        if (!secretlyBankrolled) {
            locked = locked.withLocked(Term.SUPPORT).withLocked(Term.TRANSPORT);
        }
        contract.setNonNegotiableTermsData(locked);
    }
}
