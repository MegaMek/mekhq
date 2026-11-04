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
package mekhq.gui.dialog.glossary;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.DIVIDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.gui.dialog.glossary.GlossaryConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.dialog.glossary.GlossaryConsole.Mode;

/**
 * Builds the guide for each {@link GlossaryConsole} page, and for the {@link GuideViewer}, from the resource bundle.
 * Each guide's steps, example lines and terms are numbered keys ({@code GlossaryGuide.glossary.step.1.title},
 * {@code .step.2.title}, ...), read until the next number is missing, so text can be added or reworded without code
 * changes.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GlossaryGuides {
    /** The keyboard shortcuts the Glossary page's guide lists, by the name of their resource keys. */
    private static final List<String> SHORTCUTS = List.of("search", "results", "escape", "history", "export");
    /** The keyboard shortcuts the guide viewer's guide lists, by the name of their resource keys. */
    private static final List<String> VIEWER_SHORTCUTS = List.of("find", "pages", "zoom", "viewerEscape");

    private GlossaryGuides() {}

    /**
     * @param mode the console's page
     *
     * @return the page's guide
     */
    static Guide build(final Mode mode) {
        return build(guidePrefix(mode), getFormattedTextAt(RESOURCE_BUNDLE, "GlossaryGuide.eyebrow", mode.label()),
              mode == Mode.GLOSSARY ? shortcuts(SHORTCUTS) : null);
    }

    /**
     * @return the guide viewer's guide
     */
    static Guide buildViewer() {
        return build("GlossaryGuide.viewer.", text("GlossaryGuide.viewer.eyebrow"), shortcuts(VIEWER_SHORTCUTS));
    }

    private static Guide build(final String prefix, final String eyebrow, final @Nullable JComponent stepsExtra) {

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

        return new Guide(eyebrow, text(prefix + "title"), steps, stepsExtra, text(prefix + "exampleIntro"), example,
              terms, null);
    }

    /**
     * @param mode the console's page
     *
     * @return the label of the button that opens the page's guide
     */
    static String buttonLabel(final Mode mode) {
        return text(guidePrefix(mode) + "button");
    }

    private static String guidePrefix(final Mode mode) {
        return "GlossaryGuide." + mode.name().toLowerCase(Locale.ROOT) + ".";
    }

    private static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /** The keyboard shortcuts, each key beside what it does, set out like the Oracle's key to its answers. */
    private static JComponent shortcuts(final List<String> names) {
        // The toolkit cannot be asked for the menu key without a screen, as in tests; Ctrl is then the likeliest.
        int menuMask = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                             : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        String menuKey = InputEvent.getModifiersExText(menuMask);
        JPanel key = new JPanel();
        key.setOpaque(false);
        key.setLayout(new BoxLayout(key, BoxLayout.Y_AXIS));
        key.add(leftAligned(Hud.eyebrow(text("GlossaryGuide.shortcuts"))));
        key.add(Box.createVerticalStrut(scaleForGUI(6)));
        JPanel grid = new JPanel(new GridLayout(names.size(), 1, 0, scaleForGUI(1)));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (String shortcut : names) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(SURFACE);
            row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5),
                  scaleForGUI(8)));
            JLabel keys = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "GlossaryGuide.shortcut." + shortcut + ".keys", menuKey));
            keys.setForeground(ACCENT_BRIGHT);
            keys.setFont(hudFont(Font.BOLD, 0.8f, 0.04f));
            keys.setPreferredSize(new Dimension(scaleForGUI(130), keys.getPreferredSize().height));
            JLabel meaning = new JLabel(text("GlossaryGuide.shortcut." + shortcut + ".text"));
            meaning.setForeground(TEXT_MUTED);
            meaning.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
            row.add(keys);
            row.add(meaning);
            row.add(Box.createHorizontalGlue());
            grid.add(row);
        }
        key.add(leftAligned(grid));
        return key;
    }
}
