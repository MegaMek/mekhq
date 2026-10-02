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

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.TechBase;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;

/**
 * Which parts the campaign may buy, judged by their tech base. The "allow purchasing of Clan parts" and "allow
 * purchasing of Inner Sphere parts" options limit buying only: parts already owned can still be fitted. A refit that
 * needs a part it may not buy, and has none in stock, cannot finish, so the refit confirmation names those parts.
 */
public final class RefitPurchaseCheck {
    private static final MMLogger LOGGER = MMLogger.create(RefitPurchaseCheck.class);

    private RefitPurchaseCheck() {}

    /**
     * The rule acquisitions follow. Automatic acquisitions ignore the tech base options.
     *
     * @param campaignOptions the campaign options
     * @param techBase        the tech base of the item to buy, or {@code null} when it has none, which is never limited
     *
     * @return {@code true} if the campaign may buy an item of this tech base
     */
    public static boolean canBuyTechBase(CampaignOptions campaignOptions, @Nullable TechBase techBase) {
        if (campaignOptions.get(CampaignOption.ACQUISITIONS_TYPE) == AcquisitionsType.AUTOMATIC) {
            return true;
        }
        return switch (techBase) {
            case CLAN -> campaignOptions.get(CampaignOption.ALLOW_CLAN_PURCHASES);
            case IS -> campaignOptions.get(CampaignOption.ALLOW_IS_PURCHASES);
            case null, default -> true;
        };
    }

    /**
     * @param campaign the campaign
     * @param refit    the planned refit
     *
     * @return the names of the parts the refit still has to buy but may not, each named once; empty when the refit can
     *       get everything it needs
     */
    public static List<String> findPartsThatCannotBeBought(Campaign campaign, Refit refit) {
        List<String> partNames = new ArrayList<>();
        for (Part part : refit.getShoppingList()) {
            boolean canBuy = canBuyTechBase(campaign.getCampaignOptions(), part.getTechBase());
            if (!canBuy && !partNames.contains(part.getName())) {
                partNames.add(part.getName());
            }
        }
        if (!partNames.isEmpty()) {
            LOGGER.debug("[RefitPurchase] {}: cannot buy {}", refit.getUnit().getName(), partNames);
        }
        return partNames;
    }
}
