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
package mekhq.gui.campaignOptions.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

import mekhq.gui.baseComponents.MHQCollapsiblePanel;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CampaignOptionsPagePanel#getSectionSearchText()}, which
 * feeds the navigation filter so a search can
 * match a collapsible section's heading or summary, not only the page title.
 * Literal sections are used so the assertions
 * do not depend on the campaign options resource bundle.
 */
class CampaignOptionsPagePanelTest {

    @Test
    void sectionSearchTextConcatenatesLiteralTitlesAndSummaries() {
        CampaignOptionsPagePanel page = CampaignOptionsPagePanel.builder("Test", "Test", "")
                .literalSection("Alpha Section", "alpha summary", new JLabel())
                .literalSection("Beta Section", null, new JLabel())
                .build();

        String searchText = page.getSectionSearchText();

        assertTrue(searchText.contains("Alpha Section"), searchText);
        assertTrue(searchText.contains("alpha summary"), searchText);
        assertTrue(searchText.contains("Beta Section"), searchText);
        // A null summary must not leak the literal "null" into the searchable text.
        assertFalse(searchText.contains("null"), searchText);
    }

    @Test
    void sectionSearchTextIsEmptyWhenThereAreNoSections() {
        CampaignOptionsPagePanel page = CampaignOptionsPagePanel.builder("Test", "Test", "")
                .build();

        assertEquals("", page.getSectionSearchText());
    }

    @Test
    void expandSectionsMatchingRevealsOnlyMatchingCollapsedSection() {
        CampaignOptionsPagePanel page = CampaignOptionsPagePanel.builder("Test", "Test", "")
                .sectionsExpandedByDefault(false)
                .literalSection("Alpha Section", "alpha summary", new JLabel())
                .literalSection("Beta Section", "beta summary", new JLabel())
                .build();
        List<MHQCollapsiblePanel> sections = findSections(page);

        boolean matched = page.expandSectionsMatching(text -> text.contains("Beta Section"));

        assertTrue(matched);
        assertEquals(2, sections.size());
        assertFalse(sections.get(0).isExpanded());
        assertTrue(sections.get(1).isExpanded());
    }

    private static List<MHQCollapsiblePanel> findSections(Container root) {
        List<MHQCollapsiblePanel> sections = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof MHQCollapsiblePanel section) {
                sections.add(section);
            }
            if (child instanceof Container container) {
                sections.addAll(findSections(container));
            }
        }
        return sections;
    }
}
