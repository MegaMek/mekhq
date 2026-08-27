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

/**
 * Determines the employer for a mercenary-search Chaos contract. This is the default determination, so it keeps the
 * shared behavior of {@link AbstractContractDeterminationEmployer} unchanged: the flavor (paying) faction matches the
 * rolled {@link ChaosEmployerType} theme &mdash; a corporation for a corporation, a mercenary command for a
 * subcontract, and so on &mdash; and may be landless; the anchor faction always holds ground near the player so the
 * downstream enemy and target-system selection have real geography to work with; and a ComStar or Word of Blake patron
 * may step in to front, take over, or covertly bankroll the work.
 */
public class ChaosContractDeterminationEmployerMercenary extends AbstractContractDeterminationEmployer {
}
