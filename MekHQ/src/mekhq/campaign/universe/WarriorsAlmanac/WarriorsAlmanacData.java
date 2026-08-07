package mekhq.campaign.universe.WarriorsAlmanac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public static Map<Integer, WarriorsAlmanacData> buildAlmanacUnitsData(String techBase) {
        final Map<Integer, WarriorsAlmanacData> unitTechDatesByYear = new HashMap<>();

        for (MekSummary summary : MekSummaryCache.getInstance().getAllMeks()) {
            if (Objects.equals(summary.getTechBase(), techBase)) {
                // The tech dates are precomputed on the MekSummary, so there's no need to load the full Entity.
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
}
