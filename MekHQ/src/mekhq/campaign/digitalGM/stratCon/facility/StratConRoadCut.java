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

import java.time.LocalDate;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState.LocalDateAdapter;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;

/**
 * A road hex where the player has cut the enemy's supply line, by winning a fight over the enemy's convoys there. While
 * the cut lasts, the enemy's supply cannot pass through the hex.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConRoadCut {
    private StratConCoords coords;
    private LocalDate endDate;

    public StratConRoadCut() {
    }

    /**
     * @param coords  the road hex
     * @param endDate the first day the enemy's supply passes through the hex again
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConRoadCut(StratConCoords coords, LocalDate endDate) {
        this.coords = coords;
        this.endDate = endDate;
    }

    public StratConCoords getCoords() {
        return coords;
    }

    public void setCoords(StratConCoords coords) {
        this.coords = coords;
    }

    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
