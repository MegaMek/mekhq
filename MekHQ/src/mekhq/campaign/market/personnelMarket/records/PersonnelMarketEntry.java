/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.personnelMarket.records;

import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Represents an entry in the personnel market configuration.
 *
 * <p>Each entry defines the parameters for generating personnel of a specific role/profession, along with its
 * frequency, availability period, and possible fallback category.</p>
 *
 * <p>Immutable data container for use in generating market applicants.</p>
 *
 * @param weight             The selection weight for random applicant generation.
 * @param profession         The personnel role this entry produces.
 * @param count              Maximum number of applicants generated per batch for this entry.
 * @param introductionYear   Year this profession becomes available.
 * @param extinctionYear     Last year this profession remains available.
 * @param fallbackProfession Alternate role to use if this one is unavailable.
 *
 * @author Illiani
 * @since 0.50.06
 */
public record PersonnelMarketEntry(int weight, PersonnelRole profession, int count, int introductionYear,
      int extinctionYear, PersonnelRole fallbackProfession) {
}

