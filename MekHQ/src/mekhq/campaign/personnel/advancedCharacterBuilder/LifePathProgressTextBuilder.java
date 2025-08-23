/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import java.util.Map;

import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabBasicInformation;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabExclusions;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabFixedXP;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabFlexibleXP;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathTab;

public class LifePathProgressTextBuilder {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";
    private static final int TEXT_PANEL_WIDTH = LifePathBuilderDialog.getTextPanelWidth();

    public static String getProgressText(int gameYear, LifePathBuilderTabBasicInformation basicInfoTab,
          LifePathTab requirementsTab, LifePathBuilderTabExclusions exclusionsTab,
          LifePathBuilderTabFixedXP fixedXPTab, LifePathBuilderTabFlexibleXP flexibleXPTab) {
        StringBuilder newProgressText = new StringBuilder();

        int calculatedCost = LifePathXPCostCalculator.calculateXPCost(basicInfoTab.getDiscount(),
              fixedXPTab.getFixedXPTabStorage(), flexibleXPTab.getFlexibleXPTabStorageMap());

        String newBasicText = getNewBasicText(basicInfoTab, calculatedCost);
        newProgressText.append(newBasicText);

        String newFixedXPText = getFixedXPText(fixedXPTab);
        newProgressText.append(newFixedXPText);

        String newFlexibleXPText = getFlexibleXPText(flexibleXPTab);
        newProgressText.append(newFlexibleXPText);

        String newRequirementsText = getNewRequirementsText(requirementsTab, gameYear);
        newProgressText.append(newRequirementsText);

        String newExclusionsText = getNewExclusionsText(exclusionsTab);
        newProgressText.append(newExclusionsText);

        return newProgressText.toString();
    }

    private static String getNewBasicText(LifePathBuilderTabBasicInformation basicInfoTab, int calculatedCost) {
        StringBuilder newText = new StringBuilder();

        String name = basicInfoTab.getName();
        if (!name.isBlank()) {
            newText.append("<h1 style='text-align:center; margin:0'>").append(name).append("</h1>");
        }

        String flavorText = basicInfoTab.getFlavorText();
        if (!flavorText.isBlank()) {
            newText.append("<i>").append(flavorText).append("</i>");
        }

        int age = basicInfoTab.getAge();
        if (age > 0) {
            if (!newText.isEmpty()) {
                newText.append("<br>");
            }
            newText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.age", age));
        }

        if (calculatedCost > 0) {
            newText.append("<h2>");
            newText.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathBuilderDialog.tab.progress.basic.cost",
                  calculatedCost));
            newText.append("</h2>");
        }

        List<ATOWLifeStage> lifeStages = basicInfoTab.getLifeStages();
        if (!lifeStages.isEmpty()) {
            StringBuilder lifeStageText = new StringBuilder();
            lifeStageText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.stages"));

            List<ATOWLifeStage> orderedLifeStages = lifeStages.stream()
                                                          .sorted(ATOWLifeStage::compareTo)
                                                          .toList();

            for (int i = 0; i < orderedLifeStages.size(); i++) {
                ATOWLifeStage lifeStage = orderedLifeStages.get(i);
                if (i == 0) {
                    lifeStageText.append(lifeStage.getDisplayName());
                } else {
                    lifeStageText.append(", ").append(lifeStage.getDisplayName());
                }
            }
            newText.append("<br>").append(lifeStageText);
        }

        List<LifePathCategory> categories = basicInfoTab.getCategories();
        if (!categories.isEmpty()) {
            StringBuilder categoriesText = new StringBuilder();
            categoriesText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.categories"));

            List<LifePathCategory> orderedCategories = categories.stream()
                                                             .sorted(LifePathCategory::compareTo)
                                                             .toList();

            for (int i = 0; i < orderedCategories.size(); i++) {
                LifePathCategory category = orderedCategories.get(i);
                if (i == 0) {
                    categoriesText.append(category.getDisplayName());
                } else {
                    categoriesText.append(", ").append(category.getDisplayName());
                }
            }
            newText.append("<br>").append(categoriesText);
        }

        return String.format("<div style='width:%dpx;'>%s</div>", TEXT_PANEL_WIDTH, newText);
    }

    private static String getNewRequirementsText(LifePathTab requirementsTab, int gameYear) {
        StringBuilder newRequirementsText = new StringBuilder();

        boolean isEmpty = true;
        List<String> progress = requirementsTab.getTabProgress();
        if (progress.isEmpty()) {
            return "";
        }

        String requirementsTitle = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        newRequirementsText.append("<h2 style='text-align:center; margin:0;'>").append(requirementsTitle).append(
              "</h2>");

        boolean firstRequirement = true;
        for (int i = 0; i < progress.size(); i++) {
            String requirements = progress.get(i);
            if (requirements.isBlank()) {
                continue;
            }

            if (!firstRequirement) {
                newRequirementsText.append("<br>");
            }
            firstRequirement = false;

            String requirementTitle = getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathBuilderDialog.tab." + (i == 0 ? "compulsory" : "optional") + ".label");
            newRequirementsText.append("&#9654; <b>").append(requirementTitle).append(": </b>");
            newRequirementsText.append(requirements);
        }

        return newRequirementsText.toString();
    }

    private static String getNewExclusionsText(LifePathBuilderTabExclusions exclusionsTab) {
        StringBuilder newExclusionsText = new StringBuilder();

        String exclusions = exclusionsTab.getExclusionsTabTextStorage();
        if (exclusions.isBlank()) {
            return "";
        }

        String exclusionsTitle = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.exclusions.tab.title");
        newExclusionsText.append("<h2 style='text-align:center; margin:0;'>").append(exclusionsTitle).append(
              "</h2>");

        newExclusionsText.append(exclusions);

        return newExclusionsText.toString();
    }

    private static String getFixedXPText(LifePathBuilderTabFixedXP fixedXPTab) {
        StringBuilder newFixedXPText = new StringBuilder();

        String awards = fixedXPTab.getFixedXPTabTextStorage();
        if (awards.isBlank()) {
            return "";
        }

        String exclusionsTitle = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.fixedXP.tab.title");
        newFixedXPText.append("<h2 style='text-align:center; margin:0;'>").append(exclusionsTitle).append(
              "</h2>");

        newFixedXPText.append(awards);

        return newFixedXPText.toString();
    }

    private static String getFlexibleXPText(LifePathBuilderTabFlexibleXP flexibleXPTab) {
        StringBuilder newFlexibleXPText = new StringBuilder();

        boolean isEmpty = true;
        Map<Integer, String> unorderedFlexibleAwards = flexibleXPTab.getFlexibleXPTabTextMap();
        for (int i = 0; i < unorderedFlexibleAwards.size(); i++) {
            String awards = unorderedFlexibleAwards.get(i);
            if (!awards.isBlank()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) {
            return "";
        }

        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.flexibleXP.tab.title");
        newFlexibleXPText.append("<h2 style='text-align:center; margin:0;'>").append(title).append(
              "</h2>");

        List<String> orderedAwards = unorderedFlexibleAwards.entrySet().stream()
                                           .sorted(Map.Entry.comparingByKey())
                                           .map(Map.Entry::getValue)
                                           .toList();

        boolean firstAwardSet = true;
        for (String awards : orderedAwards) {
            if (awards.isBlank()) {
                continue;
            }

            if (!firstAwardSet) {
                newFlexibleXPText.append("<br>");
            }
            firstAwardSet = false;

            newFlexibleXPText.append("&#9654; ");
            newFlexibleXPText.append(awards);
        }

        return newFlexibleXPText.toString();
    }
}
