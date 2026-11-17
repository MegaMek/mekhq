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

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;

/**
 * The enemy's morale on a contract.
 *
 * @param moraleLevel  the enemy's morale level
 * @param peaceful     whether a Garrison Duty or Retainer contract is between raids, with no enemy activity in the
 *                     Area of Operations. Only Garrison Duty and Retainer contracts are ever peaceful (see
 *                     {@link mekhq.campaign.mission.contract.AbstractContract#isPeaceful()}).
 * @param finalPayout  the lump sum paid on a successful completion in place of the remaining escrow, or {@code null}
 *                     when none has been set and the remaining escrow is paid as normal. A zero payout is deliberate
 *                     and renders no outstanding pay.
 */
public record MoraleData(ContractMoraleLevel moraleLevel, boolean peaceful, @Nullable Money finalPayout) {
    public MoraleData(ContractMoraleLevel moraleLevel) {
        this(moraleLevel, false, null);
    }

    /**
     * @param newMoraleLevel the new morale level
     *
     * @return a copy of this data at the new morale level
     */
    public MoraleData withMoraleLevel(ContractMoraleLevel newMoraleLevel) {
        return new MoraleData(newMoraleLevel, peaceful, finalPayout);
    }

    /**
     * @param isPeaceful whether the contract is now between raids
     *
     * @return a copy of this data with the new peaceful state
     */
    public MoraleData withPeaceful(boolean isPeaceful) {
        return new MoraleData(moraleLevel, isPeaceful, finalPayout);
    }

    /**
     * @param newFinalPayout the new final payout, or {@code null} for none
     *
     * @return a copy of this data with the new final payout
     */
    public MoraleData withFinalPayout(@Nullable Money newFinalPayout) {
        return new MoraleData(moraleLevel, peaceful, newFinalPayout);
    }
}
