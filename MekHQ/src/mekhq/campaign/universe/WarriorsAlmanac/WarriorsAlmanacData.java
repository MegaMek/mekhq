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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.TechBase;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.parts.Part;

public record WarriorsAlmanacData(List<String> prototypeDate, List<String> productionDate, List<String> commonDate,
      List<String> extinctionDate) {
    private WarriorsAlmanacData() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public static Map<Integer, WarriorsAlmanacData> buildAlmanacPartsData(PartsStore partsStore, boolean isClan) {
        final Map<Integer, WarriorsAlmanacData> partTechDatesByYear = new HashMap<>();

        for (Part part : partsStore.getInventory()) {
            if (part.isOmniPodded()) {
                continue;
            }

            final String name = part.getName();
            part.getTechBase();
            partTechDatesByYear.computeIfAbsent(part.getPrototypeDate(isClan), year -> new WarriorsAlmanacData())
                  .prototypeDate()
                  .add(name);
            partTechDatesByYear.computeIfAbsent(part.getProductionDate(isClan), year -> new WarriorsAlmanacData())
                  .productionDate()
                  .add(name);
            partTechDatesByYear.computeIfAbsent(part.getCommonDate(isClan), year -> new WarriorsAlmanacData())
                  .commonDate()
                  .add(name);
            partTechDatesByYear.computeIfAbsent(part.getExtinctionDate(isClan), year -> new WarriorsAlmanacData())
                  .extinctionDate()
                  .add(name);
        }

        return partTechDatesByYear;
    }

    public static Map<Integer, WarriorsAlmanacData> buildAlmanacUnitsData(TechBase techBase) {
        final Map<Integer, WarriorsAlmanacData> unitTechDatesByYear = new HashMap<>();

        for (MekSummary summary : MekSummaryCache.getInstance().getAllMeks()) {
            if (resolveTechBase(summary) == techBase) {
                final String name = summary.getName();
                unitTechDatesByYear.computeIfAbsent(summary.getPrototypeDate(), year -> new WarriorsAlmanacData())
                      .prototypeDate()
                      .add(name);
                unitTechDatesByYear.computeIfAbsent(summary.getProductionDate(), year -> new WarriorsAlmanacData())
                      .productionDate()
                      .add(name);
                unitTechDatesByYear.computeIfAbsent(summary.getCommonDate(), year -> new WarriorsAlmanacData())
                      .commonDate()
                      .add(name);
                unitTechDatesByYear.computeIfAbsent(summary.getExtinctionDate(), year -> new WarriorsAlmanacData())
                      .extinctionDate()
                      .add(name);
            }
        }

        return unitTechDatesByYear;
    }

    private static TechBase resolveTechBase(MekSummary summary) {
        if (summary.isMixedTech()) {
            return TechBase.ALL;
        }
        return summary.isClan() ? TechBase.CLAN : TechBase.IS;
    }
}
