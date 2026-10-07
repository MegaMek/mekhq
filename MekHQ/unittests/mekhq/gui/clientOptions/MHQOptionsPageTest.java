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
package mekhq.gui.clientOptions;

import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.settings.CollapsibleSectionPanel;
import megamek.client.ui.settings.SettingsCheckBox;
import mekhq.MHQOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MHQOptionsPageTest {
    private boolean originalExpansionPreference;

    @BeforeEach
    void setExpansionPreference() {
        originalExpansionPreference = GUIPreferences.getInstance().getExpandOptionSections();
        GUIPreferences.getInstance().setExpandOptionSections(true);
    }

    @AfterEach
    void restoreExpansionPreference() {
        GUIPreferences.getInstance().setExpandOptionSections(originalExpansionPreference);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void displayPageFollowsPreferenceAndStagesCheckboxChanges(boolean expanded) throws Exception {
        GUIPreferences.getInstance().setExpandOptionSections(expanded);
        SwingUtilities.invokeAndWait(() -> {
            MHQOptionsModel model = new MHQOptionsModel(new MHQOptions());
            MHQDisplayPage displayPage = new MHQDisplayPage(model);
            Component page = displayPage.createPage();
            List<CollapsibleSectionPanel> sections = findComponents(page, CollapsibleSectionPanel.class);

            assertEquals(4, sections.size());
            for (CollapsibleSectionPanel section : sections) {
                assertEquals(expanded, section.isExpanded());
            }
            SettingsCheckBox checkbox = findComponents(page, SettingsCheckBox.class).stream()
                  .filter(control -> "chkchkExpandOptionSections".equals(control.getName()))
                  .findFirst().orElseThrow();
            assertEquals(expanded, checkbox.isSelected());

            checkbox.setSelected(!expanded);
            displayPage.writeToModel();

            assertEquals(!expanded, model.expandOptionSections);
            assertEquals(expanded, GUIPreferences.getInstance().getExpandOptionSections());
        });
    }

    @Test
    void singleSectionClientPageStaysExpandedWithCollapsedPreference() throws Exception {
        GUIPreferences.getInstance().setExpandOptionSections(false);
        SwingUtilities.invokeAndWait(() -> {
            Component page = MHQOptionsPage.buildMHQPage("MHQFontsPage", "lblMHQFontsSection.text",
                  "lblMHQFontsSection.summary", new JPanel());
            List<CollapsibleSectionPanel> sections = findComponents(page, CollapsibleSectionPanel.class);

            assertEquals(1, sections.size());
            assertTrue(sections.getFirst().isExpanded());
        });
    }

    @Test
    void clientOptionsProviderUsesDefaultBundleLookups() {
        assertEquals(getText("displayPage.title"), MHQOptionsPage.TEXT_PROVIDER.getText("displayPage.title"));
        assertEquals(getFormattedText("editLog.dialog.title", "Test"),
              MHQOptionsPage.TEXT_PROVIDER.getFormattedText("editLog.dialog.title", "Test"));
    }

    private static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> components = new ArrayList<>();
        if (type.isInstance(root)) {
            components.add(type.cast(root));
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                components.addAll(findComponents(child, type));
            }
        }
        return components;
    }
}
