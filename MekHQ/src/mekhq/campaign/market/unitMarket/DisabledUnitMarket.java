/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.unitMarket;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.universe.Faction;
import org.w3c.dom.Node;

/**
 * This is a completely empty unit market, which is used when the market is disabled.
 */
public class DisabledUnitMarket extends AbstractUnitMarket {
    //region Constructors
    public DisabledUnitMarket() {
        super(UnitMarketMethod.NONE);
        super.setOffers(new ArrayList<>());
    }
    //endregion Constructors

    //region Getters/Setters
    @Override
    public void setOffers(final List<UnitMarketOffer> offers) {

    }
    //endregion Getters/Setters

    //region Process New Day
    //region Generate Offers
    @Override
    public void processNewDay(final Campaign campaign) {

    }

    @Override
    public void generateUnitOffers(final Campaign campaign) {

    }

    @Override
    public @Nullable String addSingleUnit(final Campaign campaign, final UnitMarketType market,
          final int unitType, final Faction faction,
          final int quality, final int percent) {
        return null;
    }

    @Override
    public void addOffers(final Campaign campaign, final int number, final UnitMarketType market,
          final int unitType, final Faction faction, final int quality,
          final int priceTarget) {

    }

    @Override
    public int generateWeight(final Campaign campaign, final int unitType, final Faction faction) {
        return 0;
    }
    //endregion Generate Offers

    //region Offer Removal
    @Override
    public void removeUnitOffers(final Campaign campaign) {

    }
    //endregion Offer Removal
    //endregion Process New Day

    //region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, int indent) {

    }

    @Override
    public void fillFromXML(Node wn, Campaign campaign, Version version) {

    }
    //endregion File I/O
}
