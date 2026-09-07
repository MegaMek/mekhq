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
package mekhq.gui.commandGeneration.contents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.gui.commandGeneration.contents.SparesAndFinancesTab.SparesCategory;
import org.junit.jupiter.api.Test;

/**
 * The Spares section of the Command Designer mirrors the Campaign Options autoLogistics grid. It drifted once
 * already: Armor Kit and Bomb were added to the campaign options and never reached the generator, which is what
 * issue #9955 reported. These tests hold the two lists together.
 */
class SparesCategoryTest {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CommandGenerationDialog";
    private static final String AUTO_LOGISTICS_PREFIX = "autoLogistics";

    @Test
    void everyAutoLogisticsCampaignOptionHasACategory() {
        Set<String> covered = new HashSet<>();
        for (SparesCategory category : SparesCategory.values()) {
            covered.add(category.getCampaignOption().xmlTag());
        }

        List<String> missing = new ArrayList<>();
        for (CampaignOption<?> option : autoLogisticsOptions()) {
            if (!covered.contains(option.xmlTag())) {
                missing.add(option.xmlTag());
            }
        }

        assertTrue(missing.isEmpty(),
              "autoLogistics options with no Spares category, so the Command Designer cannot set them: " + missing);
    }

    @Test
    void everyCategoryPointsAtADistinctOption() {
        Set<String> seen = new HashSet<>();
        for (SparesCategory category : SparesCategory.values()) {
            assertNotNull(category.getCampaignOption(), category + " has no campaign option");
            assertTrue(seen.add(category.getCampaignOption().xmlTag()),
                  category + " reuses an option another category already writes");
        }
    }

    @Test
    void everyCategoryHasADistinctResourceKey() {
        Set<String> seen = new HashSet<>();
        for (SparesCategory category : SparesCategory.values()) {
            assertTrue(seen.add(category.getResourceKey()), category + " reuses another category's resource key");
        }
    }

    @Test
    void everyCategoryHasALabelAndATooltip() {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
        for (SparesCategory category : SparesCategory.values()) {
            String labelKey = "lbl" + category.getResourceKey() + ".text";
            String tooltipKey = "lbl" + category.getResourceKey() + ".tooltip";
            assertTrue(bundle.containsKey(labelKey), "no label for " + category + " (" + labelKey + ")");
            assertTrue(bundle.containsKey(tooltipKey), "no tooltip for " + category + " (" + tooltipKey + ")");
            assertFalse(bundle.getString(labelKey).isBlank(), labelKey + " is blank");
        }
    }

    @Test
    void theTwoCategoriesTheIssueReportedAreThere() {
        Set<String> keys = new HashSet<>();
        for (SparesCategory category : SparesCategory.values()) {
            keys.add(category.getResourceKey());
        }
        assertTrue(keys.contains("SparesArmorKit"), "Armor Kit was the first of the two missing categories");
        assertTrue(keys.contains("SparesBomb"), "Bomb was the second");
    }

    /**
     * Every autoLogistics option, taken from {@link CampaignOption#values()} rather than listed here - a list would
     * be the very duplication these tests exist to catch.
     */
    private static List<CampaignOption<?>> autoLogisticsOptions() {
        List<CampaignOption<?>> options = new ArrayList<>();
        for (CampaignOption<?> option : CampaignOption.values()) {
            if (option.xmlTag().startsWith(AUTO_LOGISTICS_PREFIX)) {
                options.add(option);
            }
        }
        assertFalse(options.isEmpty(), "found no autoLogistics options at all, so this test proves nothing");
        return options;
    }

    @Test
    void theBundleItselfLoads() {
        try {
            assertNotNull(ResourceBundle.getBundle(RESOURCE_BUNDLE));
        } catch (MissingResourceException exception) {
            throw new AssertionError("the Command Designer bundle is missing", exception);
        }
    }
}
