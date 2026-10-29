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
package mekhq.gui.dialog.crewAssignment;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.DIVIDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Crewing;
import mekhq.gui.dialog.glossary.GlossaryEntryDialog;

/**
 * Builds the guide for each page of the {@link CrewAssignmentDialog}, from the {@code CrewGuide.<page>.} keys of its
 * resource bundle: numbered steps, a worked example, the page's terms, and links to the glossary entries behind them.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewAssignmentGuides {
    private static final String RESOURCE_BUNDLE = CrewAssignmentDialog.RESOURCE_BUNDLE;

    private CrewAssignmentGuides() {
    }

    /**
     * @param page   the page
     * @param parent the dialog, as the parent of any glossary window the guide opens
     *
     * @return the page's guide
     */
    static Guide build(final CrewAssignmentPage page, final JDialog parent) {
        String prefix = "CrewGuide." + page.name().toLowerCase(Locale.ROOT) + ".";

        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "step." + number + ".title")); number++) {
            steps.add(new Step(text(prefix + "step." + number + ".title"), text(prefix + "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "example." + number + ".who")); number++) {
            example.add(new ExampleLine(text(prefix + "example." + number + ".who"),
                  text(prefix + "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "term." + number + ".name")); number++) {
            terms.add(new Term(text(prefix + "term." + number + ".name"), text(prefix + "term." + number + ".text")));
        }

        return new Guide(getFormattedTextAt(RESOURCE_BUNDLE, "CrewGuide.eyebrow", page.getLabel()),
              text(prefix + "title"), steps, (page == CrewAssignmentPage.UNITS) ? ringKey() : null,
              text("CrewGuide.exampleIntro"), example, terms, glossaryLinks(page, parent));
    }

    private static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /** The key to a unit row's status ring, each state in its ring colour. */
    private static JComponent ringKey() {
        JPanel key = column();
        key.add(leftAligned(Hud.eyebrow(text("CrewGuide.units.ringKey"))));
        key.add(Box.createVerticalStrut(scaleForGUI(6)));
        List<Crewing> order = List.of(Crewing.FULL, Crewing.TEMP, Crewing.PARTIAL, Crewing.EMPTY);
        JPanel grid = new JPanel(new GridLayout(order.size(), 1, 0, scaleForGUI(1)));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (Crewing crewing : order) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(SURFACE);
            row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5),
                  scaleForGUI(8)));
            HudRing ring = new HudRing();
            ring.setColor(CrewRowRenderers.crewingColor(crewing), false);
            JLabel meaning = new JLabel(text("CrewGuide.units.ringKey." + crewing.name()));
            meaning.setForeground(TEXT_MUTED);
            meaning.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            row.add(ring);
            row.add(Box.createHorizontalStrut(scaleForGUI(10)));
            row.add(meaning);
            row.add(Box.createHorizontalGlue());
            grid.add(row);
        }
        key.add(leftAligned(grid));
        return key;
    }

    private static @Nullable JComponent glossaryLinks(final CrewAssignmentPage page, final JDialog parent) {
        List<GlossaryEntry> entries = glossaryEntriesFor(page);
        if (entries.isEmpty()) {
            return null;
        }
        JPanel links = column();
        links.add(leftAligned(Hud.eyebrow(text("CrewGuide.glossary"))));
        links.add(Box.createVerticalStrut(scaleForGUI(4)));
        for (GlossaryEntry entry : entries) {
            links.add(leftAligned(Hud.link(entry.getTitle(), () -> new GlossaryEntryDialog(parent, entry))));
            links.add(Box.createVerticalStrut(scaleForGUI(3)));
        }
        return links;
    }

    private static List<GlossaryEntry> glossaryEntriesFor(final CrewAssignmentPage page) {
        return switch (page) {
            case UNITS -> List.of(GlossaryEntry.VEHICLE_CREWS, GlossaryEntry.TEMP_CREWS, GlossaryEntry.TOE);
            case PERSONNEL -> List.of(GlossaryEntry.PROFESSIONS, GlossaryEntry.SKILL_TYPES, GlossaryEntry.FATIGUE,
                  GlossaryEntry.ADVANCED_MEDICAL);
            case REVIEW -> List.of(GlossaryEntry.TEMP_CREWS, GlossaryEntry.TEMP_PERSONNEL, GlossaryEntry.TOE);
        };
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
