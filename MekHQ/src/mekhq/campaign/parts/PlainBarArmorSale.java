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

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.ArmorType;
import megamek.common.equipment.MiscType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.finances.Money;

/**
 * Sells spare support vehicle armor that older versions kept as plain armor, as a campaign loads.
 *
 * <p>Support vehicle armor is now kept as {@link SVArmor}, which is weighed and priced by its BAR and armor tech
 * rating. Older versions kept it as plain {@link Armor} at 16 points per ton (issue #5571). Armor on units is rebuilt
 * when a campaign loads, but spare armor in a warehouse is not: no repair or refit can use it, and parts in use counts
 * none in stock, so resupply orders more (issue #11171).</p>
 *
 * <p>The spares are sold rather than converted. A plain armor part records its BAR but not its armor tech rating, so
 * a campaign with BAR 7 armor at both C and D cannot tell which the spares were. Selling pays the same price the
 * player would get from the Sell button, and resupply then buys the right armor.</p>
 */
public final class PlainBarArmorSale {
    private static final MMLogger LOGGER = MMLogger.create(PlainBarArmorSale.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignXmlParser";

    private PlainBarArmorSale() {}

    /**
     * Sells every spare plain armor part carrying support vehicle BAR armor, in the main warehouse and each base's.
     * Parts on a unit, in transit or set aside for a refit or repair are left alone.
     *
     * @param campaign the campaign being loaded
     *
     * @return how many armor parts were sold
     */
    public static int sellPlainBarArmor(Campaign campaign) {
        List<LocalWarehouse> warehouses = new ArrayList<>();
        warehouses.add(campaign.getPlayerForce().getWarehouse());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            warehouses.add(base.getBaseWarehouse());
        }

        List<Armor> plainBarArmor = new ArrayList<>();
        for (LocalWarehouse warehouse : warehouses) {
            for (Part part : warehouse.getParts()) {
                if (isPlainBarArmorSpare(part)) {
                    plainBarArmor.add((Armor) part);
                }
            }
        }

        int soldPoints = 0;
        for (Armor armor : plainBarArmor) {
            int points = armor.getAmount();
            String armorName = armor.getName();
            Money salePrice = armor.getSalePrice();
            campaign.getQuartermaster().sellArmor(armor);
            soldPoints += points;
            campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CampaignXmlParser.compatibility.plainBarArmor", points, armorName,
                  salePrice.toAmountAndSymbolString()));
        }
        if (!plainBarArmor.isEmpty()) {
            LOGGER.info("[PlainBarArmor] Sold {} spare plain BAR armor parts, {} points in all", plainBarArmor.size(),
                  soldPoints);
        }
        return plainBarArmor.size();
    }

    /**
     * @return {@code true} when a part is a spare, present plain armor part whose armor type is support vehicle BAR
     *       armor
     */
    static boolean isPlainBarArmorSpare(Part part) {
        if (!(part instanceof Armor armor) || (part instanceof SVArmor)) {
            return false;
        }
        if (!part.isSpare() || !part.isPresent()) {
            return false;
        }
        ArmorType armorType = ArmorType.of(armor.getType(), armor.isClanTechBase());
        return (armorType != null) && armorType.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR);
    }
}
