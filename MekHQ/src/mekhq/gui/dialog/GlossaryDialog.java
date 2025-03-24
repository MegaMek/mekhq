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
 */
package mekhq.gui.dialog;

import static java.lang.Math.round;
import static javax.swing.BorderFactory.createEmptyBorder;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.handleImmersiveHyperlinkClick;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent.EventType;

import megamek.client.ui.swing.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * The {@code GlossaryDialog} class represents a dialog window for displaying glossary entries. It displays detailed
 * information about a glossary term, including its title and description, in a styled HTML format.
 *
 * <p>
 * This class uses a {@link JEditorPane} to render glossary entry content and supports hyperlink interactions for
 * related glossary entries. If a related term is clicked, a new {@code GlossaryDialog} is opened to show its details.
 * </p>
 */
public class GlossaryDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(GlossaryDialog.class);

    private final JDialog parent;
    private final Campaign campaign;

    private int CENTER_WIDTH = UIUtil.scaleForGUI(800);
    private int CENTER_HEIGHT = UIUtil.scaleForGUI(400);
    private int PADDING = UIUtil.scaleForGUI(10);

    private final String GLOSSARY_BUNDLE = "mekhq.resources.Glossary";

    /**
     * Creates a new {@code GlossaryDialog} instance to display information about a glossary term.
     *
     * <p>
     * The dialog retrieves the glossary term's title and description using the provided key and displays the content in
     * a styled format. During its construction, the parent dialog is hidden to ensure that only this dialog is visible
     * to the user.
     * </p>
     *
     * @param parent   The parent {@link JDialog} that is temporarily hidden while this dialog is displayed.
     * @param campaign The {@link Campaign} object containing resources and glossary entries.
     * @param key      The unique identifier for the glossary term to be displayed.
     */
    public GlossaryDialog(JDialog parent, Campaign campaign, String key) {
        this.parent = parent;
        this.campaign = campaign;

        parent.setVisible(false);
        buildDialog(key);
    }

    /**
     * Builds the Glossary Dialog by setting its title and definition based on the key provided.
     *
     * <p>
     * This method fetches the title and definition strings for the glossary term from the resource bundle. If the title
     * is invalid (i.e., the resource key is not found), it logs an error and terminates the dialog building process.
     * </p>
     *
     * @param key The resource key used to retrieve the glossary term's title and definition.
     */
    private void buildDialog(String key) {
        String title = getFormattedTextAt(GLOSSARY_BUNDLE, key + ".title");
        if (!isResourceKeyValid(title)) {
            logger.error("No valid title for {}", key);
            return;
        }

        String description = getFormattedTextAt(GLOSSARY_BUNDLE, key + ".definition");
        if (!isResourceKeyValid(description)) {
            logger.error("No valid definition for {}", key);
            return;
        }

        setTitle(title);

        // Create a JEditorPane for the message
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        // Use inline CSS to set font family, size, and other style properties
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>" +
                                               "<h1 style='text-align: center;'>%s</h1>" +
                                               "%s</div>", CENTER_WIDTH, fontStyle, title, description));
        setFontScaling(editorPane, false, 1.1);

        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(evt -> {
            if (evt.getEventType() == EventType.ACTIVATED) {
                handleImmersiveHyperlinkClick(this, campaign, evt.getDescription());
            }
        });

        // Wrap the JEditorPane in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setMinimumSize(new Dimension(CENTER_WIDTH, scrollPane.getHeight()));
        // This line ensures the scroll pane starts scrolled to the top, not bottom.
        SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));

        // Create a JPanel with padding
        JPanel paddedPanel = new JPanel(new BorderLayout());
        paddedPanel.setBorder(createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        paddedPanel.add(scrollPane, BorderLayout.CENTER);
        add(paddedPanel);

        // Assign close action
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseAction();
            }
        });

        // Set dialog properties
        setSize((int) round((CENTER_WIDTH + (PADDING * 2)) * 1.1), CENTER_HEIGHT);
        setLocationRelativeTo(null);
        setModal(true);
        setVisible(true);
    }

    /**
     * Handles user interactions when the dialog is closed.
     *
     * <p>
     * This method ensures the parent dialog is made visible again after the glossary dialog is closed.
     * </p>
     */
    private void onCloseAction() {
        dispose();
        parent.setVisible(true);
    }
}
