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
package mekhq.campaign.universe.factionStanding;

import java.util.List;

/**
 * A wrapper class for managing a list of {@link FactionStandingUltimatumData}.
 *
 * <p>This class provides methods to retrieve and update the list of ultimatums.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
class FactionStandingUltimatumsWrapper {
    private List<FactionStandingUltimatumData> ultimatums;

    /**
     * @return a {@link List} of {@link FactionStandingUltimatumData} representing the ultimatums.
     *
     * @author Illiani
     * @since 0.50.07
     */
    List<FactionStandingUltimatumData> getUltimatums() {
        return ultimatums;
    }

    /**
     * Sets the list of {@link FactionStandingUltimatumData} for this wrapper.
     *
     * @param ultimatums a {@link List} of {@link FactionStandingUltimatumData} to associate.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void setUltimatums(List<FactionStandingUltimatumData> ultimatums) {
        this.ultimatums = ultimatums;
    }
}
