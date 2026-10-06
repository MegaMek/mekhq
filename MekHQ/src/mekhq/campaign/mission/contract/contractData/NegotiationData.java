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
package mekhq.campaign.mission.contract.contractData;

import java.util.List;

import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.TermFunding;

/**
 * A record of how a contract's terms were negotiated, stored on the contract so the negotiation table can be reopened
 * and resumed rather than restarting from scratch.
 *
 * <p>The contract's live terms already hold the <em>current</em> (negotiated) step of each clause; this record adds
 * what the negotiation UI cannot otherwise recover: the pre-negotiation baseline steps, the budgets already spent, and
 * how each raised step was funded. Together they let the dialog restore the exact in-progress state and continue to
 * enforce the per-term cap and reputation/swap limits across sessions.</p>
 *
 * <p>The {@code funding} lists are in canonical clause order - pay, support, transport, salvage, command - matching
 * {@link ContractTermsData}; each inner list holds one {@link TermFunding} entry per step that clause was raised.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public record NegotiationData(int originalPayStep, int originalSupportStep, int originalTransportStep,
      int originalSalvageStep, int originalCommandStep, int reputationUsed, int swapsUsed,
      int sacrificeBank, List<List<TermFunding>> funding) {
}
