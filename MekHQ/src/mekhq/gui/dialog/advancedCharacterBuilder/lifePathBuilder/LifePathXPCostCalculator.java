package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTabStorage;

class LifePathXPCostCalculator {
    static int calculateXPCost(@Nullable Integer discount, LifePathTabStorage fixedXPStorage,
          Map<Integer, LifePathTabStorage> flexibleXPStorage) {
        // Basic Info
        int globalCost = discount != null ? discount : 0;

        // Fixed XP
        Collection<Integer> fixedAttributes = fixedXPStorage.attributes().values();
        globalCost += fixedAttributes.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedTraits = fixedXPStorage.traits().values();
        globalCost += fixedTraits.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedSkills = fixedXPStorage.skills().values();
        globalCost += fixedSkills.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedAbilities = fixedXPStorage.abilities().values();
        globalCost += fixedAbilities.stream().mapToInt(Integer::intValue).sum();

        // Flexible XP
        int runningCost = 0;
        int length = flexibleXPStorage.size();
        for (Map.Entry<Integer, LifePathTabStorage> entry : flexibleXPStorage.entrySet()) {
            LifePathTabStorage storage = entry.getValue();

            Collection<Integer> flexibleAttributes = storage.attributes().values();
            runningCost += flexibleAttributes.stream().mapToInt(Integer::intValue).sum();

            Collection<Integer> flexibleTraits = storage.traits().values();
            runningCost += flexibleTraits.stream().mapToInt(Integer::intValue).sum();

            Collection<Integer> flexibleSkills = storage.skills().values();
            runningCost += flexibleSkills.stream().mapToInt(Integer::intValue).sum();

            Collection<Integer> flexibleAbilities = storage.abilities().values();
            runningCost += flexibleAbilities.stream().mapToInt(Integer::intValue).sum();
        }
        int meanCost = runningCost / length;

        // Total and return
        globalCost += runningCost;

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }
}
