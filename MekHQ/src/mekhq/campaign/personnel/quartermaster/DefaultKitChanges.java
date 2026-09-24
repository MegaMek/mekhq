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
package mekhq.campaign.personnel.quartermaster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.quartermaster.AbstractKitIssuer.KitIssueTotals;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KitProfession;

/**
 * Detects changes to the per-profession and per-group default kit campaign options, so the player can be offered to
 * move everyone carrying the old default onto the new one.
 *
 * <p>Only a switch from one real kit to another counts. Turning a default on (from "none" or coveralls) or off is not
 * a switch: there is no old kit to replace, or no new kit to replace it with.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class DefaultKitChanges {
    /** The armor-kit groups that carry a default kit option. Soldiers have none. */
    private static final List<Category> ARMOR_GROUPS = List.of(Category.MEKWARRIOR, Category.INFANTRY,
          Category.AIRCRAFT);

    /**
     * One changed default: an equipment-kit profession ({@code profession} set) or an armor-kit group
     * ({@code category} set), with the kit it used to be and the kit it is now.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Change(@Nullable KitProfession profession, @Nullable Category category, String oldKitName,
          EquipmentType newKit) {
        /**
         * @return the old default kit's display name
         */
        public String oldKitDisplayName() {
            EquipmentType oldKit = EquipmentType.get(oldKitName);
            return (oldKit != null) ? oldKit.getName() : oldKitName;
        }
    }

    private DefaultKitChanges() {
    }

    /**
     * Records every default kit option's current value, to compare against once the options have been applied.
     *
     * @param options the campaign options before the change
     *
     * @return option to value
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Map<CampaignOption<String>, String> snapshot(CampaignOptions options) {
        Map<CampaignOption<String>, String> values = new LinkedHashMap<>();
        for (KitProfession profession : KitProfession.values()) {
            CampaignOption<String> option = EquipmentKitIssuer.defaultKitOption(profession);
            values.put(option, options.get(option));
        }
        for (Category category : ARMOR_GROUPS) {
            CampaignOption<String> option = ArmorKitIssuer.defaultKitOption(category);
            values.put(option, options.get(option));
        }
        return values;
    }

    /**
     * The defaults that switched from one real kit to another between a snapshot and the current options.
     *
     * @param before  the snapshot taken before the change
     * @param options the campaign options after the change
     *
     * @return the switched defaults (empty if none)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Change> detect(Map<CampaignOption<String>, String> before, CampaignOptions options) {
        List<Change> changes = new ArrayList<>();
        for (KitProfession profession : KitProfession.values()) {
            CampaignOption<String> option = EquipmentKitIssuer.defaultKitOption(profession);
            String oldKitName = before.get(option);
            EquipmentType newKit = switchedTo(oldKitName, options.get(option), EquipmentKitCatalog.NO_DEFAULT_KIT);
            if (newKit != null) {
                changes.add(new Change(profession, null, oldKitName, newKit));
            }
        }
        for (Category category : ARMOR_GROUPS) {
            CampaignOption<String> option = ArmorKitIssuer.defaultKitOption(category);
            String oldKitName = before.get(option);
            EquipmentType newKit = switchedTo(oldKitName, options.get(option), ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
            if (newKit != null) {
                changes.add(new Change(null, category, oldKitName, newKit));
            }
        }
        return changes;
    }

    /**
     * Moves everyone carrying each changed default's old kit onto the new one. Kits come from stores; any shortfall is
     * ordered (and paid for) and swapped in when it arrives.
     *
     * @param campaign the campaign
     * @param changes  the switched defaults to apply
     *
     * @return how many kits were issued and ordered
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static KitIssueTotals apply(Campaign campaign, List<Change> changes) {
        KitIssueTotals totals = new KitIssueTotals();
        for (Change change : changes) {
            if (change.profession() != null) {
                EquipmentKitIssuer.switchDefaultKit(campaign, change.profession(), change.oldKitName(),
                      change.newKit(), totals);
            } else if (change.category() != null) {
                ArmorKitIssuer.switchDefaultKit(campaign, change.category(), change.oldKitName(), change.newKit(),
                      totals);
            }
        }
        return totals;
    }

    /** The new kit if the option moved from one real kit to a different real kit, otherwise {@code null}. */
    private static @Nullable EquipmentType switchedTo(@Nullable String oldKitName, @Nullable String newKitName,
          String disabledValue) {
        if (!isRealKit(oldKitName, disabledValue) || !isRealKit(newKitName, disabledValue)
                  || oldKitName.equals(newKitName)) {
            return null;
        }
        return EquipmentType.get(newKitName);
    }

    private static boolean isRealKit(@Nullable String kitName, String disabledValue) {
        return (kitName != null)
                     && !kitName.isBlank()
                     && !kitName.equals(disabledValue)
                     && (EquipmentType.get(kitName) != null);
    }
}
