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
package mekhq.gui.campaignOptions.contents;

import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import megamek.client.ui.settings.CollapsibleSectionPanel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkillsPagesTest {
    @BeforeEach
    void resetSkillTypes() {
        SkillType.initializeTypes();
    }

    @Test
    void gunneryPageLaysOutItsWideExpandedSection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel page = new SkillsPages(new CampaignOptions()).createSkillsPage(COMBAT_GUNNERY);
            page.setSize(page.getPreferredSize());

            layoutTree(page);

            CollapsibleSectionPanel section = findComponent(page, CollapsibleSectionPanel.class);
            JTable table = findComponent(page, JTable.class);
            assertTrue(section.isExpanded());
            assertTrue(section.getWidth() > 0);
            assertTrue(section.getHeight() > 0);
            assertTrue(table.getWidth() > 0);
            assertTrue(table.getHeight() > 0);
            assertFalse(table.getModel().getRowCount() == 0);
        });
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " found");
    }

    private static <T extends Component> T findComponentOrNull(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
