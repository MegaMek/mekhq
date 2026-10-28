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
package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;

/**
 * The guides that slide over the right of a contract market window, one per page or section, as the Oracle Console's
 * guides do: a darkened scrim over the window and a {@link HudGuideDrawer} with its Steps, Example and Terms tabs.
 * Clicking the scrim or the drawer's close button dismisses the guide, and the checkbox at the drawer's foot turns off
 * guides opening by themselves.
 *
 * <p>Each guide's text comes from resource keys under its own prefix: {@code eyebrow}, {@code title} and
 * {@code exampleIntro}, plus numbered {@code step.N.title} and {@code step.N.text}, {@code example.N.who} and
 * {@code example.N.text}, and {@code term.N.name} and {@code term.N.text}. The numbered keys are read until the next
 * number is missing, so text can be added or reworded without code changes. The drawer's tab and close labels are
 * shared by every guide.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class MarketGuide {
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;
    private static final Color SCRIM = new Color(2, 7, 13, 160);

    private static final String SHARED_PREFIX = "guide.contractMarket.";

    private final JDialog window;
    private final HudGuideDrawer drawer;
    private final JPanel scrim;
    private final HudCheckBox autoOpen;

    /**
     * Adds a hidden guide drawer to a window's layered pane, ready to {@link #open(String)}.
     *
     * @param window the window the guides slide over
     */
    MarketGuide(JDialog window) {
        this.window = window;
        autoOpen = new HudCheckBox(text(SHARED_PREFIX, "autoOpen"));
        autoOpen.addActionListener(event -> ContractMarketGuidePreferences.getInstance()
                                                  .setGuideAutoOpen(autoOpen.isSelected()));
        drawer = new HudGuideDrawer(List.of(text(SHARED_PREFIX, "tab.steps"), text(SHARED_PREFIX, "tab.example"),
              text(SHARED_PREFIX, "tab.terms")), autoOpen, text(SHARED_PREFIX, "close"), this::close);
        scrim = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(SCRIM);
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        scrim.setOpaque(false);
        scrim.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                close();
            }
        });

        // The drawer and its scrim float over the window's content on the layered pane.
        JLayeredPane layers = window.getLayeredPane();
        layers.add(scrim, JLayeredPane.MODAL_LAYER);
        layers.add(drawer, Integer.valueOf(JLayeredPane.MODAL_LAYER + 1));
        scrim.setVisible(false);
        drawer.setVisible(false);
        layers.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                layout();
            }
        });
    }

    /**
     * Opens a guide on its Steps tab, with the keyboard focus on its close button.
     *
     * @param prefix the resource-key prefix the guide's text is read from, such as
     *               {@code "guide.contractMarket.offers."}
     */
    void open(String prefix) {
        autoOpen.setSelected(ContractMarketGuidePreferences.getInstance().isGuideAutoOpen());
        drawer.showGuide(build(prefix));
        scrim.setVisible(true);
        drawer.setVisible(true);
        layout();
        drawer.focusClose();
    }

    void close() {
        scrim.setVisible(false);
        drawer.setVisible(false);
    }

    boolean isOpen() {
        return drawer.isVisible();
    }

    private void layout() {
        JLayeredPane layers = window.getLayeredPane();
        int width = layers.getWidth();
        int height = layers.getHeight();
        scrim.setBounds(0, 0, width, height);
        int drawerWidth = Math.min(HudGuideDrawer.drawerWidth(), width);
        drawer.setBounds(width - drawerWidth, 0, drawerWidth, height);
        drawer.revalidate();
    }

    private static Guide build(String prefix) {
        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix, "step." + number + ".title")); number++) {
            steps.add(new Step(text(prefix, "step." + number + ".title"), text(prefix, "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix, "example." + number + ".who")); number++) {
            example.add(new ExampleLine(text(prefix, "example." + number + ".who"),
                  text(prefix, "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix, "term." + number + ".name")); number++) {
            terms.add(new Term(text(prefix, "term." + number + ".name"), text(prefix, "term." + number + ".text")));
        }
        return new Guide(text(prefix, "eyebrow"), text(prefix, "title"), steps, null, text(prefix, "exampleIntro"),
              example, terms, null);
    }

    private static String text(String prefix, String key) {
        return getTextAt(RESOURCE_BUNDLE, prefix + key);
    }
}
