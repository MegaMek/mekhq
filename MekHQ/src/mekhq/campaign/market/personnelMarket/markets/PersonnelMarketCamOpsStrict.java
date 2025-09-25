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
package mekhq.campaign.market.personnelMarket.markets;

import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.CAMPAIGN_OPERATIONS_STRICT;

import mekhq.campaign.Campaign;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;

/**
 * Implements the personnel market using the Campaign Operations Strict ruleset.
 *
 * <p>This class specializes {@link PersonnelMarketCamOpsRevised} by supplying stricter data sets for applicant
 * generation according to the strict interpretation of Campaign Operations. It configures market entries with data
 * relevant to this ruleset.
 *
 * <ul>
 *     <li>Initializes clan and Inner Sphere entries for the "Strict" rules variant.</li>
 *     <li>Associates this market with the {@code CAMPAIGN_OPERATIONS_STRICT} style.</li>
 *     <li>Inherits applicant origin and recruitment logic from its parent.</li>
 * </ul>
 *
 * <p><b>Extends:</b> {@link PersonnelMarketCamOpsRevised}</p>
 * <p><b>Associated Market Style:</b> {@link mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle#CAMPAIGN_OPERATIONS_STRICT}</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelMarketCamOpsStrict extends PersonnelMarketCamOpsRevised {
    /**
     * Constructs a personnel market instance using Campaign Operations Strict rules.
     *
     * <p>Initializes and loads the appropriate market entry libraries.</p>
     *
     * @param campaign the parent campaign instance
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketCamOpsStrict() {
        super();

        setAssociatedPersonnelMarketStyle(CAMPAIGN_OPERATIONS_STRICT);

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        setClanMarketEntries(personnelMarketLibraries.getClanMarketCamOpsStrict());
        setInnerSphereMarketEntries(personnelMarketLibraries.getInnerSphereMarketCamOpsStrict());
    }
}
