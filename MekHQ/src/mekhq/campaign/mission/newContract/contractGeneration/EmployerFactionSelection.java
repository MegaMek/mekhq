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
package mekhq.campaign.mission.newContract.contractGeneration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.universe.Faction;

/**
 * The outcome of employer selection: the chosen employer faction together with the roll-table classifications used to
 * derive it. The global tier applies to every employer; the independent type is only present when the global tier is
 * {@link GlobalEmployerTableValue#INDEPENDENT}.
 *
 * @param employerFaction               the selected employer faction, or {@code null} if none could be generated
 * @param globalEmployerTableValue      the employer's global tier classification
 * @param independentEmployerTableValue the independent-employer sub-type, or {@code null} if not an independent
 *                                      employer
 */
public record EmployerFactionSelection(
      @Nullable Faction employerFaction,
      @Nonnull GlobalEmployerTableValue globalEmployerTableValue,
      @Nullable IndependentEmployerTableValue independentEmployerTableValue
) {
}
