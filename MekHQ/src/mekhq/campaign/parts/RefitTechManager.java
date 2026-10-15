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
package mekhq.campaign.parts;

import java.util.List;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.Faction;
import megamek.common.interfaces.ITechManager;
import mekhq.campaign.Campaign;

/**
 * The campaign's tech rules for choosing a refit design. They are the campaign's own, except that designs mixing
 * Inner Sphere and Clan equipment are always allowed: the purchasing options limit what can be bought, not what can be
 * fitted from parts already owned.
 */
public final class RefitTechManager implements ITechManager {
    private final Campaign campaign;

    /**
     * @param campaign the campaign whose tech rules apply
     */
    public RefitTechManager(Campaign campaign) {
        this.campaign = campaign;
    }

    @Override
    public boolean useMixedTech() {
        return true;
    }

    @Override
    public int getTechIntroYear() {
        return campaign.getTechIntroYear();
    }

    @Override
    public int getGameYear() {
        return campaign.getGameYear();
    }

    @Override
    public List<Integer> getTechAvailabilityYears() {
        return campaign.getTechAvailabilityYears();
    }

    @Override
    public Faction getTechFaction() {
        return campaign.getTechFaction();
    }

    @Override
    public boolean useClanTechBase() {
        return campaign.useClanTechBase();
    }

    @Override
    public SimpleTechLevel getTechLevel() {
        return campaign.getTechLevel();
    }

    @Override
    public boolean unofficialNoYear() {
        return campaign.unofficialNoYear();
    }

    @Override
    public boolean useVariableTechLevel() {
        return campaign.useVariableTechLevel();
    }

    @Override
    public boolean showExtinct() {
        return campaign.showExtinct();
    }
}
