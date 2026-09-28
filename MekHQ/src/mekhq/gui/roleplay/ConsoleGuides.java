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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.roleplay.FateChartAnswer;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.dialog.glossary.GlossaryEntryDialog;

/**
 * Builds the guide for each {@link OracleConsole} page from the resource bundle. Each page's steps, example lines and
 * terms are numbered keys ({@code OracleGuide.ask.step.1.title}, {@code .step.2.title}, ...), read until the next
 * number is missing, so text can be added or reworded without code changes.
 */
final class ConsoleGuides {
    private ConsoleGuides() {}

    /**
     * @param page    the page
     * @param console the console, as the parent of any glossary window the guide opens
     *
     * @return the page's guide
     */
    static Guide build(final ConsolePage page, final OracleConsole console) {
        String prefix = "OracleGuide." + page.guideKey() + ".";

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

        return new Guide(getFormattedTextAt(RESOURCE_BUNDLE, "OracleGuide.eyebrow", page.getLabel()),
              text(prefix + "title"), steps, page == ConsolePage.ASK ? answerKey() : null,
              text("OracleGuide.exampleIntro"), example, terms, glossaryLinks(page, console));
    }

    private static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * The key to reading a Fate Chart answer, with each answer in its banner colour.
     */
    private static JComponent answerKey() {
        JPanel key = new JPanel();
        key.setOpaque(false);
        key.setLayout(new BoxLayout(key, BoxLayout.Y_AXIS));
        key.add(leftAligned(Hud.eyebrow(text("OracleGuide.ask.answerKey"))));
        key.add(Box.createVerticalStrut(scaleForGUI(6)));
        JPanel grid = new JPanel(new GridLayout(FateChartAnswer.values().length, 1, 0, scaleForGUI(1)));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (FateChartAnswer answer : FateChartAnswer.values()) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(SURFACE);
            row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5),
                  scaleForGUI(8)));
            JLabel name = new JLabel(answer.getLabel().toUpperCase(Locale.ROOT));
            name.setForeground(AskPage.colorFor(answer));
            name.setFont(hudFont(Font.BOLD, 0.8f, 0.1f));
            name.setPreferredSize(new Dimension(scaleForGUI(130), name.getPreferredSize().height));
            JLabel meaning = new JLabel(text("OracleGuide.ask.answerKey." + answer.name()));
            meaning.setForeground(TEXT_MUTED);
            meaning.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            row.add(name);
            row.add(meaning);
            row.add(Box.createHorizontalGlue());
            grid.add(row);
        }
        key.add(leftAligned(grid));
        return key;
    }

    private static JComponent glossaryLinks(final ConsolePage page, final OracleConsole console) {
        JPanel links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        links.add(leftAligned(Hud.eyebrow(text("OracleGuide.glossary"))));
        links.add(Box.createVerticalStrut(scaleForGUI(4)));
        for (GlossaryEntry entry : glossaryEntriesFor(page)) {
            links.add(leftAligned(Hud.link(entry.getTitle(), () -> new GlossaryEntryDialog(console, entry))));
            links.add(Box.createVerticalStrut(scaleForGUI(3)));
        }
        return links;
    }

    private static List<GlossaryEntry> glossaryEntriesFor(final ConsolePage page) {
        return switch (page) {
            case ASK -> List.of(GlossaryEntry.ORACLE, GlossaryEntry.CHAOS_FACTOR, GlossaryEntry.SCENES,
                  GlossaryEntry.ORACLE_RANDOM_EVENTS, GlossaryEntry.ORACLE_CONCEPTS);
            case CHECKS -> List.of(GlossaryEntry.ORACLE_CHECKS, GlossaryEntry.ORACLE_CAST);
            case THREADS -> List.of(GlossaryEntry.PLOT_THREADS, GlossaryEntry.ORACLE_RANDOM_EVENTS);
            case CAST -> List.of(GlossaryEntry.ORACLE_CAST, GlossaryEntry.ORACLE_RANDOM_EVENTS);
            case JOURNAL -> List.of(GlossaryEntry.ORACLE, GlossaryEntry.PLOT_THREADS, GlossaryEntry.ORACLE_CAST);
        };
    }
}
