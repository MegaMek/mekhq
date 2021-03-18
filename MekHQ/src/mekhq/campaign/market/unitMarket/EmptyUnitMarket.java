/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market.unitMarket;

import megamek.common.annotations.Nullable;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.universe.Faction;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a completely empty unit market, which is used when the market is disabled.
 */
public class EmptyUnitMarket extends AbstractUnitMarket {
    //region Variable Declarations
    private static final long serialVersionUID = 7526665298322946291L;
    //endregion Variable Declarations

    //region Constructors
    public EmptyUnitMarket() {
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
    protected void addOffers(final Campaign campaign, final int number, final UnitMarketType market,
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
