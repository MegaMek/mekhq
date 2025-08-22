package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.Map;

import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTabStorage;

class LifePathXPCostCalculator {
    static int calculateXPCost(int discount, LifePathTabStorage fixedXPStorage,
          Map<Integer, LifePathTabStorage> flexibleXPStorage) {
        // Basic Info
        int globalCost = -discount;

        // Fixed XP
        globalCost = getCost(fixedXPStorage, globalCost);

        // Flexible XP
        int runningCost = 0;
        int length = flexibleXPStorage.size();
        for (Map.Entry<Integer, LifePathTabStorage> entry : flexibleXPStorage.entrySet()) {
            LifePathTabStorage storage = entry.getValue();

            runningCost = getCost(storage, runningCost);
        }
        int meanCost = runningCost / length;

        // Total and return
        globalCost += meanCost;

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }

    private static int getCost(LifePathTabStorage fixedXPStorage, int globalCost) {
        Collection<Integer> fixedAttributes = fixedXPStorage.attributes().values();
        globalCost += fixedAttributes.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedTraits = fixedXPStorage.traits().values();
        globalCost += fixedTraits.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedSkills = fixedXPStorage.skills().values();
        globalCost += fixedSkills.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedAbilities = fixedXPStorage.abilities().values();
        globalCost += fixedAbilities.stream().mapToInt(Integer::intValue).sum();
        return globalCost;
    }
}
