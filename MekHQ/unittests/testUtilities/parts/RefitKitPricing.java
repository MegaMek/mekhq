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
package testUtilities.parts;

import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.missing.MissingPart;

/**
 * Works out what a refit kit should cost from its components: the price of every part it contains, plus 10 percent
 * (CO p.212). Each component is priced the way the campaign prices it when bought on its own.
 */
public final class RefitKitPricing {
    /** A refit kit costs 10 percent more than its components (CO p.212). */
    public static final double KIT_MARKUP = 1.1;

    private RefitKitPricing() {
    }

    /**
     * @return the kit price for this refit, rounded to whole C-bills, assuming the warehouse holds none of the parts
     */
    public static Money expectedKitPrice(Refit refit) {
        return componentPrice(refit).multipliedBy(KIT_MARKUP).round();
    }

    /**
     * Prices every component the refit must buy: a new part for each missing part, a bin's worth of ammunition for
     * each new ammo bin, and the armor the new design needs.
     */
    public static Money componentPrice(Refit refit) {
        Money price = Money.zero();
        for (Part part : refit.getShoppingList()) {
            if (part instanceof MissingPart missingPart) {
                price = price.plus(missingPart.getNewPart().getActualValue());
            } else if (part instanceof AmmoBin ammoBin) {
                price = price.plus(new AmmoStorage(0, ammoBin.getType(), ammoBin.getFullShots(), refit.getCampaign())
                                         .getActualValue());
            } else {
                price = price.plus(part.getActualValue());
            }
        }
        Armor armorSupplies = refit.getNewArmorSupplies();
        if (armorSupplies != null) {
            price = price.plus(armorSupplies.getValueNeeded());
        }
        return price;
    }
}
