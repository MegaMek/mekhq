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

import static megamek.common.compute.Compute.d6;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;

/**
 * Determines the employer for a government (non-mercenary) Chaos contract. It reuses the shared flavor/anchor
 * resolution of {@link AbstractContractDeterminationEmployer} but overrides the per-faction hooks so that every faction
 * the resolution needs is the player's own: {@link #resolveFlavorFaction} and {@link #resolveAnchorFaction} both return
 * the player faction, and {@link #checkForSpecialEmployer} is barred, so no landless themed flavor faction, borrowed
 * anchor, covert sponsor, or ComStar/Word of Blake special employer is ever produced. The rolled
 * {@link ChaosEmployerType} is kept only for its contract-terms modifiers.
 */
public class ChaosContractDeterminationEmployerGovernment extends AbstractContractDeterminationEmployer {
    /**
     * A government contract is paid by the player's own faction, so the flavor faction is always the player faction
     * regardless of the rolled type.
     */
    @Override
    protected @Nullable Faction resolveFlavorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction) {
        return playerFaction;
    }

    /**
     * A government contract's conflict is anchored on the player's own faction's territory, so the anchor is always the
     * player faction.
     */
    @Override
    protected Faction resolveAnchorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction, Faction flavor) {
        return playerFaction;
    }

    /**
     * A government contract is issued by the player's own faction, which never fronts, takes over, or covertly backs
     * its own work through ComStar or the Word of Blake, so no special employer is ever generated.
     */
    @Override
    protected @Nullable Faction checkForSpecialEmployer(int currentYear, boolean covertViable) {
        return null;
    }

    @Override
    protected ChaosEmployerType determineEmployerType() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> ChaosEmployerType.CIVILIAN_ORGANIZATION_MILITIA;
            case 3, 4, 12 -> ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT;
            case 5, 6, 9 -> ChaosEmployerType.LOCAL_SYSTEM_OWNER;
            case 7, 11 -> ChaosEmployerType.ANY_SYSTEM_OWNER;
            case 8 -> ChaosEmployerType.NOBLE;
            case 10 -> ChaosEmployerType.ANY_PLANETARY_GOVERNMENT;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
