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
package mekhq.campaign.universe.WarriorsAlmanac;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.TechBase;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.UnitType;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.parts.Part;

/**
 * A single Warrior's Almanac development event: one part or unit reaching a particular
 * {@link AlmanacTechAdvancementPhase} (prototype, production, common, or extinct) in a given year.
 *
 * <p>Entries are grouped by the year the event occurred (see {@link #buildAlmanacPartsData} and
 * {@link #buildAlmanacUnitsData}), then presented per-category (Warehouse part category / unit type) in the
 * {@code WarriorsAlmanacDialog}. {@code categoryOrder} preserves the canonical ordering used to lay out the tabs, while
 * {@code categoryLabel} is the localized tab title.</p>
 *
 * @param name          the display name of the part or unit
 * @param techBase      the tech base of the part or unit, used for faction gating and the tech-base column
 * @param categoryOrder the canonical ordinal of the category, used only to order the tabs
 * @param categoryLabel the localized category name, used as the tab title
 * @param categoryIntro the localized introductory blurb shown above the category's table
 * @param phase         the development phase that occurred this year
 *
 * @author Illiani
 * @since 0.50.07
 */
public record WarriorsAlmanacEntry(String name, TechBase techBase, int categoryOrder, String categoryLabel,
      String categoryIntro, AlmanacTechAdvancementPhase phase) {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarriorsAlmanacDialog";

    /**
     * Builds the parts almanac, keyed by the year each development event occurred.
     *
     * <p>Omni-podded parts are skipped. The supplied {@code isClan} flag selects the Inner Sphere or Clan tech-date
     * perspective for the part, and should reflect the player force (see {@code Force#isClanForce}).</p>
     *
     * @param partsStore the parts store to enumerate
     * @param isClan     whether to use Clan tech dates ({@code true}) or Inner Sphere tech dates ({@code false})
     *
     * @return a map of year to the development events that occurred that year
     */
    public static Map<Integer, List<WarriorsAlmanacEntry>> buildAlmanacPartsData(PartsStore partsStore,
          boolean isClan) {
        final Map<Integer, List<WarriorsAlmanacEntry>> byYear = new HashMap<>();

        for (Part part : partsStore.getInventory()) {
            if (part.isOmniPodded()) {
                continue;
            }

            final String name = part.getName();
            final TechBase techBase = part.getTechBase();
            final AlmanacPartCategory category = AlmanacPartCategory.categorize(part);
            final int order = category.ordinal();
            final String label = category.getLabel();
            final String intro = category.getIntro();

            addEntry(byYear, part.getPrototypeDate(isClan), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.PROTOTYPE);
            addEntry(byYear, part.getProductionDate(isClan), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.PRODUCTION);
            addEntry(byYear, part.getCommonDate(isClan), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.COMMON);
            addEntry(byYear, part.getExtinctionDate(isClan), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.EXTINCT);
        }

        return byYear;
    }

    /**
     * Builds the units almanac, keyed by the year each development event occurred.
     *
     * <p>Unit tech dates are tech-base agnostic (they are precomputed on the {@link MekSummary}), so no perspective
     * flag is required.</p>
     *
     * @return a map of year to the development events that occurred that year
     */
    public static Map<Integer, List<WarriorsAlmanacEntry>> buildAlmanacUnitsData() {
        final Map<Integer, List<WarriorsAlmanacEntry>> byYear = new HashMap<>();

        for (MekSummary summary : MekSummaryCache.getInstance().getAllMeks()) {
            final String name = summary.getName();
            final TechBase techBase = resolveTechBase(summary);
            final int typeCode = UnitType.determineUnitTypeCode(summary.getUnitType());
            // A negative code means the unit type is unrecognized; sort those after every known type.
            final int order = (typeCode < 0) ? UnitType.SIZE : typeCode;
            final String label = (typeCode < 0) ? summary.getUnitType() : UnitType.getTypeDisplayableName(typeCode);
            final String intro = resolveUnitTypeIntro(typeCode);

            addEntry(byYear, summary.getPrototypeDate(), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.PROTOTYPE);
            addEntry(byYear, summary.getProductionDate(), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.PRODUCTION);
            addEntry(byYear, summary.getCommonDate(), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.COMMON);
            addEntry(byYear, summary.getExtinctionDate(), name, techBase, order, label, intro,
                  AlmanacTechAdvancementPhase.EXTINCT);
        }

        return byYear;
    }

    private static void addEntry(Map<Integer, List<WarriorsAlmanacEntry>> byYear, int year, String name,
          TechBase techBase, int categoryOrder, String categoryLabel, String categoryIntro,
          AlmanacTechAdvancementPhase phase) {
        if (year == ITechnology.DATE_NONE) {
            return;
        }
        byYear.computeIfAbsent(year, ignored -> new ArrayList<>())
              .add(new WarriorsAlmanacEntry(name, techBase, categoryOrder, categoryLabel, categoryIntro, phase));
    }

    /**
     * Resolves the introductory blurb for a unit type, falling back to a generic blurb for types without a bespoke one
     * (or unrecognized types).
     *
     * @param typeCode the {@link UnitType} code, or a negative value if unrecognized
     *
     * @return the localized intro text
     */
    private static String resolveUnitTypeIntro(int typeCode) {
        if (typeCode >= 0) {
            // UnitType names may contain spaces (e.g. "Small Craft"); strip them to form a stable resource key.
            final String key = "WarriorsAlmanacDialog.unitType." + UnitType.getTypeName(typeCode).replace(" ", "")
                                     + ".intro";
            final String text = getTextAt(RESOURCE_BUNDLE, key);
            if (isResourceKeyValid(text)) {
                return text;
            }
        }
        return getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.unitType.default.intro");
    }

    private static TechBase resolveTechBase(MekSummary summary) {
        if (summary.isMixedTech()) {
            return TechBase.ALL;
        }
        return summary.isClan() ? TechBase.CLAN : TechBase.IS;
    }
}
