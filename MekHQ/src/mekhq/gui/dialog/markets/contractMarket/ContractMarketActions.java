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
package mekhq.gui.dialog.markets.contractMarket;

import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The per-offer actions the {@link ContractDossierPanel} can invoke, implemented by the owning
 * {@link ChaosContractMarketDialog}. Keeping these behind an interface lets the dossier stay a pure view that forwards
 * button presses without knowing how the market fulfils them.
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface ContractMarketActions {
    /** Accept the offer and close the market. */
    void accept(AbstractContract contract);

    /** Open negotiations on the offer's terms. */
    void negotiate(AbstractContract contract);

    /** Remove the offer from the board. */
    void delete(AbstractContract contract);

    /** Open the GM editor for the offer. */
    void edit(AbstractContract contract);
}
