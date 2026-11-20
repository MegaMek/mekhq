/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

import mekhq.MekHQ;
import mekhq.campaign.NewPlayerQuickstart;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.utilities.WindowUtilities;

/**
 * Welcomes a new player to the New Player Quickstart before their campaign is built, drawn as a heads-up display in
 * the style of the Oracle Console. The command bar shows where and when the campaign starts; the body carries an
 * in-character welcome, what the player's company will hold, and how the quickstart works. The campaign itself is
 * built by {@link NewPlayerQuickstart} once the player confirms.
 *
 * <p>The dialog is modal and blocks until the player starts the campaign or cancels.</p>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class NewPlayerQuickstartDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewPlayerQuickstartDialog";
    private static final String KEY = "NewPlayerQuickstartDialog.";

    private boolean wasCanceled = true;

    /**
     * Shows the dialog and waits for the player's answer.
     *
     * @param parent the frame the dialog is centered on
     */
    public NewPlayerQuickstartDialog(Frame parent) {
        super(parent, text("windowTitle"), true);
        buildUI();
        installKeyBindings();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(720, 560));
        getContentPane().setPreferredSize(scaleForGUI(860, 680));
        pack();
        // Wrapped text only knows how tall it is once the first pack has given it a width, so measure again
        invalidateTree(getContentPane());
        pack();
        WindowUtilities.fitToScreen(this, parent);
        setVisible(true);
    }

    /**
     * @return {@code true} unless the player chose to start the campaign
     */
    public boolean wasCanceled() {
        return wasCanceled;
    }

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = Hud.commandBar(new BorderLayout(scaleForGUI(24), 0));
        commandBar.add(Hud.identity(text("eyebrow"), text("title"), text("subtitle")), BorderLayout.CENTER);

        HudStatTile dateTile = new HudStatTile(text("tile.date"), text("tile.date.sub"));
        dateTile.setValue(MekHQ.getMHQOptions().getDisplayFormattedDate(NewPlayerQuickstart.START_DATE), TEXT);
        HudStatTile baseTile = new HudStatTile(text("tile.base"), text("tile.base.sub"));
        baseTile.setValue(NewPlayerQuickstart.STARTING_SYSTEM_ID, ACCENT_BRIGHT);
        HudStatTile commandTile = new HudStatTile(text("tile.command"), text("tile.command.sub"));
        commandTile.setValue(text("tile.command.value"), TEXT);
        commandBar.add(Hud.topAligned(Hud.tileRow(dateTile, baseTile, commandTile)), BorderLayout.EAST);
        return commandBar;
    }

    private JComponent buildBody() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(true);
        column.setBackground(SURFACE_DEEP);
        column.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(18), scaleForGUI(20), scaleForGUI(20),
              scaleForGUI(20)));

        column.add(Hud.sectionHeading(text("section.transmission")));
        column.add(Box.createVerticalStrut(scaleForGUI(10)));
        column.add(Hud.wrappingHtml(text("inCharacter"), TEXT, hudFont(Font.PLAIN, 1.0f, 0.0f)));
        column.add(Box.createVerticalStrut(scaleForGUI(22)));

        column.add(Hud.sectionHeading(text("section.included")));
        column.add(Box.createVerticalStrut(scaleForGUI(10)));
        column.add(leftAligned(Hud.chipRow(List.of(text("included.lances"), text("included.support"),
              text("included.vehicles"), text("included.spares"), text("included.funds")), READY)));
        column.add(Box.createVerticalStrut(scaleForGUI(22)));

        column.add(Hud.sectionHeading(text("section.outOfCharacter")));
        column.add(Box.createVerticalStrut(scaleForGUI(10)));
        column.add(Hud.wrappingHtml(text("outOfCharacter"), TEXT_MUTED, hudFont(Font.PLAIN, 0.92f, 0.0f)));

        // Takes the viewport's width, so the text wraps instead of running off to the side
        JPanel framed = new JScrollablePanel();
        framed.setLayout(new BorderLayout());
        framed.setOpaque(true);
        framed.setBackground(SURFACE_DEEP);
        framed.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        framed.add(column, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(framed);
        Hud.styleScroll(scroll, GROUND, false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel body = Hud.windowBody(new BorderLayout());
        body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildFooter() {
        HudButton cancelButton = new HudButton(text("button.cancel").toUpperCase(Locale.ROOT), false);
        cancelButton.addActionListener(event -> close(true));
        HudButton confirmButton = new HudButton(text("button.confirm").toUpperCase(Locale.ROOT), true);
        confirmButton.addActionListener(event -> close(false));
        return Hud.footer(new JLabel(text("footer.summary")), cancelButton, confirmButton);
    }

    /** Enter starts the campaign and Escape cancels, as the buttons of a standard dialog would. */
    private void installKeyBindings() {
        JRootPane root = getRootPane();
        bindKey(root, KeyEvent.VK_ENTER, "confirm", false);
        bindKey(root, KeyEvent.VK_ESCAPE, "cancel", true);
    }

    private void bindKey(JRootPane root, int keyCode, String name, boolean cancel) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                close(cancel);
            }
        });
    }

    // endregion Layout

    private static void invalidateTree(Component component) {
        component.invalidate();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                invalidateTree(child);
            }
        }
    }

    private void close(boolean canceled) {
        wasCanceled = canceled;
        dispose();
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }
}
