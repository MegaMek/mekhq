/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.ui.swing.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.Glossary;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.lang.Math.round;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.baseComponents.MHQDialogImmersive.GLOSSARY_COMMAND_STRING;

/**
 * The {@code GlossaryDialog} class represents a dialog window for displaying glossary entries.
 * It displays detailed information about a glossary term, including its title and description,
 * in a styled HTML format.
 *
 * <p>
 * This class uses a {@link JEditorPane} to render glossary entry content and supports hyperlink
 * interactions for related glossary entries. If a related term is clicked, a new {@code GlossaryDialog}
 * is opened to show its details.
 * </p>
 */
public class GlossaryDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(GlossaryDialog.class);

    private final JDialog parent;
    private final Campaign campaign;
    private Glossary entry;

    private int CENTER_WIDTH = UIUtil.scaleForGUI(400);
    private int CENTER_HEIGHT = UIUtil.scaleForGUI(300);
    private int PADDING = UIUtil.scaleForGUI(10);

    /**
     * Constructs a new {@code GlossaryDialog} to display a glossary entry.
     *
     * <p>
     * If the glossary key does not correspond to an existing entry, an error is logged,
     * and the dialog is not displayed.
     * </p>
     *
     * @param parent The parent {@code JDialog} to temporarily hide while this dialog is open.
     * @param campaign The {@code Campaign} instance containing the glossary data.
     * @param key The key for the glossary entry to display.
     */
    public GlossaryDialog(JDialog parent, Campaign campaign, String key) {
        this.parent = parent;
        this.campaign = campaign;

        try {
            this.entry = Glossary.valueOf(key);
            parent.setVisible(false);
            displayDialog(entry.getTitle(), entry.getDescription());
        } catch (IllegalArgumentException e) {
            logger.error("No entry available for key {}", key);
        }
    }

    /**
     * Displays the glossary dialog with the given title and description.
     *
     * <p>
     * The content is rendered using HTML in a {@link JEditorPane}, allowing for styled text
     * and clickable hyperlinks. The dialog also provides a scrollable view for long content.
     * </p>
     *
     * @param title The title of the glossary entry.
     * @param description The detailed description of the glossary entry.
     */
    private void displayDialog(String title, String description) {
        setTitle(title);

        // Create a JEditorPane for the message
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        // Use inline CSS to set font family, size, and other style properties
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format(
            "<div style='width: %s; %s'>"
                + "<h1 style='text-align: center;'>%s</h1>"
                + "<p>%s</p>"
                + "</div>",
            CENTER_WIDTH, fontStyle, title, description
        ));
        setFontScaling(editorPane, false, 1.1);

        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(evt -> {
            if (evt.getEventType() == EventType.ACTIVATED) {
                handleHyperlinkClick(campaign, evt.getDescription());
            }
        });

        // Wrap the JEditorPane in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setMinimumSize(new Dimension(CENTER_WIDTH, scrollPane.getHeight()));

        // Create a JPanel with padding
        JPanel paddedPanel = new JPanel(new BorderLayout());
        paddedPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
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
     * This method ensures the parent dialog is made visible again after the glossary
     * dialog is closed.
     * </p>
     */
    private void onCloseAction() {
        dispose();
        parent.setVisible(true);
    }

    /**
     * Handles hyperlink clicks from the HTML content displayed in the glossary dialog.
     *
     * <p>
     * If the hyperlink points to another glossary term (via the {@code GLOSSARY} command),
     * a new {@code GlossaryDialog} is opened for the referenced term.
     * </p>
     *
     * @param campaign The {@link Campaign} instance containing glossary data.
     * @param reference The hyperlink reference string. Expected to be in the format
     *                  {@code GL_COMMAND:termKey}.
     */
    private void handleHyperlinkClick(Campaign campaign, String reference) {
        String[] splitReference = reference.split(":");

        String commandKey = splitReference[0];
        String entryKey = splitReference[1];

        if (commandKey.equals(GLOSSARY_COMMAND_STRING)) {
            new GlossaryDialog(this, campaign, entryKey);
        }
    }
}
