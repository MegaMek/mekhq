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
package mekhq.campaign.digitalGM.stratCon.facility;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;

/**
 * One step of a supply route: from a hex to the next hex along the way to its side's source of supply. The two are
 * adjacent, except where a Supply Depot reaches a facility nearby without a road (see {@link StratConFacilitySupply}).
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSupplyLink {
    private StratConCoords from;
    private StratConCoords to;

    public StratConSupplyLink() {
    }

    /**
     * @param from the hex the step leaves
     * @param to   the next hex toward the source
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConSupplyLink(StratConCoords from, StratConCoords to) {
        this.from = from;
        this.to = to;
    }

    public StratConCoords getFrom() {
        return from;
    }

    public void setFrom(StratConCoords from) {
        this.from = from;
    }

    public StratConCoords getTo() {
        return to;
    }

    public void setTo(StratConCoords to) {
        this.to = to;
    }
}
