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

import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderTabBasicInformation;
import mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathTab;

public class LifePathProgressTextBuilder {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";
    private static final int TEXT_PANEL_WIDTH = LifePathBuilderDialog.getTextPanelWidth();

    public static String getProgressText(LifePathBuilderTabBasicInformation basicInfoTab, LifePathTab requirementsTab,
          LifePathTab exclusionsTab, LifePathTab fixedXPTab, LifePathTab flexibleXPTab) {
        StringBuilder newProgressText = new StringBuilder();

        int calculatedCost = LifePathXPCostCalculator.calculateXPCost(basicInfoTab.getDiscount(),
              fixedXPTab.getAttributes(), fixedXPTab.getTraits(), fixedXPTab.getSkills(),
              fixedXPTab.getAbilities(), flexibleXPTab.getTabCount(), flexibleXPTab.getPickCount(),
              flexibleXPTab.getAttributes(), flexibleXPTab.getTraits(), flexibleXPTab.getSkills(),
              flexibleXPTab.getAbilities());

        String newBasicText = getNewBasicText(basicInfoTab, calculatedCost);
        newProgressText.append(newBasicText);

        String newFixedXPText = getFixedXPText(fixedXPTab);
        newProgressText.append(newFixedXPText);

        String newFlexibleXPText = getFlexibleXPText(flexibleXPTab);
        newProgressText.append(newFlexibleXPText);

        String newRequirementsText = getNewRequirementsText(requirementsTab);
        newProgressText.append(newRequirementsText);

        String newExclusionsText = getNewExclusionsText(exclusionsTab);
        newProgressText.append(newExclusionsText);

        return newProgressText.toString();
    }

    private static String getNewBasicText(LifePathBuilderTabBasicInformation basicInfoTab, int calculatedCost) {
        StringBuilder newText = new StringBuilder();

        String name = basicInfoTab.getName();
        newText.append("<h1 style='text-align:center; margin:0'>").append(name).append("</h1>");

        String flavorText = basicInfoTab.getFlavorText();
        if (!flavorText.isBlank()) {
            newText.append("<blockquote><i>").append(flavorText).append("</i></blockquote>");
        }

        int age = basicInfoTab.getAge();
        if (!flavorText.isBlank()) {
            newText.append("<br>");
        }
        newText.append(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic.age", age));

        newText.append("<h2>");
        newText.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab.progress.basic.cost",
              calculatedCost));
        newText.append("</h2>");

        List<ATOWLifeStage> lifeStages = basicInfoTab.getLifeStages();
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
        newText.append(lifeStageText);

        List<LifePathCategory> categories = basicInfoTab.getCategories();
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

        return String.format("<div style='width:%dpx;'>%s</div>", TEXT_PANEL_WIDTH, newText);
    }

    private static String getNewRequirementsText(LifePathTab lifePathTab) {
        StringBuilder newText = new StringBuilder();

        List<String> progress = lifePathTab.buildProgressText();
        if (isEmpty(progress)) {
            return "";
        }

        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        newText.append("<h2 style='text-align:center; margin:0;'>").append(title).append(
              "</h2>");

        for (int i = 0; i < progress.size(); i++) {
            String group = progress.get(i);
            if (group.isBlank()) {
                continue;
            }

            if (i != 0) {
                newText.append("<br>");
            }

            newText.append("&#9654; ");
            newText.append(group);
        }

        return newText.toString();
    }

    private static boolean isEmpty(List<String> progress) {
        for (String group : progress) {
            if (!group.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String getNewExclusionsText(LifePathTab lifePathTab) {
        StringBuilder newText = new StringBuilder();

        List<String> progress = lifePathTab.buildProgressText();
        if (isEmpty(progress)) {
            return "";
        }

        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.exclusions.tab.title");
        newText.append("<h2 style='text-align:center; margin:0;'>").append(title).append(
              "</h2>");

        for (int i = 0; i < progress.size(); i++) {
            if (i != 0) {
                newText.append(", ");
            }

            newText.append(progress.get(i));
        }

        return newText.toString();
    }

    private static String getFixedXPText(LifePathTab lifePathTab) {
        StringBuilder newText = new StringBuilder();

        List<String> progress = lifePathTab.buildProgressText();
        if (isEmpty(progress)) {
            return "";
        }

        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.fixed_xp.tab.title");
        newText.append("<h2 style='text-align:center; margin:0;'>").append(title).append(
              "</h2>");

        for (int i = 0; i < progress.size(); i++) {
            if (i != 0) {
                newText.append(", ");
            }

            newText.append(progress.get(i));
        }

        return newText.toString();
    }

    private static String getFlexibleXPText(LifePathTab lifePathTab) {
        StringBuilder newText = new StringBuilder();

        List<String> progress = lifePathTab.buildProgressText();
        if (isEmpty(progress)) {
            return "";
        }

        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.flexible_xp.tab.title");
        newText.append("<h2 style='text-align:center; margin:0;'>").append(title).append(
              "</h2>");

        for (int i = 0; i < progress.size(); i++) {
            String group = progress.get(i);
            if (group.isBlank()) {
                continue;
            }

            if (i != 0) {
                newText.append("<br>");
            }

            newText.append("&#9654; ");
            newText.append(group);
        }

        return newText.toString();
    }
}
