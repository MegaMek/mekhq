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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.glossary;

import static mekhq.gui.dialog.glossary.NewGlossaryDialog.DOCUMENTATION_COMMAND_STRING;
import static mekhq.gui.dialog.glossary.NewGlossaryDialog.documentationEntries;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MHQPDFReaderPanel;

/**
 * Dialog for displaying {@link DocumentationEntry} details in a tabbed interface, along with a contents pane listing
 * all available entries. Each entry can display formatted HTML and supports clickable links to navigate between
 * documentation.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class NewDocumentationEntryDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(NewDocumentationEntryDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewGlossaryDialog";

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final Dimension DIALOG_SIZE = UIUtil.scaleForGUI(1500, 500);
    private static final int CENTER_PANEL_MINIMUM_WIDTH = UIUtil.scaleForGUI(900);
    private static final Dimension BUTTON_SIZE = UIUtil.scaleForGUI(100, 30);
    private static final int CONTENTS_INDENT = UIUtil.scaleForGUI(400);

    /**
     * The main tabbed pane showing documentation entries.
     */
    private final EnhancedTabbedPane tabbedPane;

    /**
     * Constructs a dialog for the specified parent and initial documentation entry.
     *
     * @param parent             the parent dialog for modal positioning
     * @param documentationEntry the initial documentation entry to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public NewDocumentationEntryDialog(JDialog parent, DocumentationEntry documentationEntry) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.title"));

        tabbedPane = new EnhancedTabbedPane(false, true);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.setMinimumSize(new Dimension(CENTER_PANEL_MINIMUM_WIDTH, Integer.MIN_VALUE));
        addDocumentationEntry(documentationEntry);

        JScrollPaneWithSpeed scrollFullContents = buildDocumentationPane();
        JPanel fullContentsWrapper = new JPanel(new BorderLayout());
        fullContentsWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        fullContentsWrapper.add(scrollFullContents, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, fullContentsWrapper);
        splitPane.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        splitPane.setDividerSize(PADDING);
        splitPane.setOneTouchExpandable(true);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(contentPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(DIALOG_SIZE));
        setLocationRelativeTo(parent.getParent());
        setMinimumSize(new Dimension(CENTER_PANEL_MINIMUM_WIDTH, DIALOG_SIZE.height));
        setPreferences(); // Must be before setVisible

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(getWidth() - CONTENTS_INDENT));

        setVisible(true);
    }

    /**
     * Adds a new documentation entry as a tab and displays it, or selects the tab if already present.
     *
     * @param documentationEntry the documentation entry to display in a new tab
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void addDocumentationEntry(DocumentationEntry documentationEntry) {
        String title = documentationEntry.getTitle();

        // Check if a tab with this title already exists
        int tabIndex = -1;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (title.equals(tabbedPane.getTitleAt(i))) {
                tabIndex = i;
                break;
            }
        }
        if (tabIndex != -1) {
            tabbedPane.setSelectedIndex(tabIndex);
            return;
        }

        // If it doesn't, create a new tab
        MHQPDFReaderPanel pnlReader = new MHQPDFReaderPanel(getOwner(), documentationEntry.getFileAddress());

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(pnlReader, BorderLayout.CENTER);
        outerPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JButton btnCloseAllTabs = new JButton(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.button.closeTab.all"));
        btnCloseAllTabs.setPreferredSize(BUTTON_SIZE);
        btnCloseAllTabs.addActionListener(e -> dispose());

        JButton btnCloseTab = new JButton(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.button.closeTab.single"));
        btnCloseTab.setPreferredSize(BUTTON_SIZE);
        btnCloseTab.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex != -1) {
                tabbedPane.removeTabAt(selectedIndex);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btnCloseAllTabs);
        buttonPanel.add(btnCloseTab);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(outerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        addTab(title, panel);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    /**
     * Adds a new tab to the dialog's tabbed pane.
     *
     * @param title     the tab title
     * @param component the content component for the tab
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void addTab(String title, Component component) {
        tabbedPane.addTab(title, component);
    }

    /**
     * Builds the scrollable contents pane listing all documentation entries with links for navigation.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void handleHyperlinkClick(HyperlinkEvent hyperlinkEvent) {
        if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String[] splitReference = hyperlinkEvent.getDescription().split(":", 2);
            if (splitReference.length != 2) {
                LOGGER.warn("Malformed hyperlink: {}", hyperlinkEvent.getDescription());
                return;
            }
            String command = splitReference[0];
            String entry = splitReference[1];

            if (command.equalsIgnoreCase(DOCUMENTATION_COMMAND_STRING)) {
                DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);

                if (documentationEntry == null) {
                    LOGGER.warn("Documentation entry not found: {}", entry);
                    return;
                }

                addDocumentationEntry(documentationEntry);
            }
        }
    }

    /**
     * Returns a randomly-selected faction logo scaled for display in a glossary tab.
     *
     * @return an {@link ImageIcon} representing the faction logo
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JScrollPaneWithSpeed buildDocumentationPane() {
        StringBuilder formatedDocumentationText = new StringBuilder();
        formatedDocumentationText.append(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.contentsPane.title"));

        for (String entry : documentationEntries) {
            DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);
            String title = documentationEntry != null ? documentationEntry.getTitle() : "-";

            formatedDocumentationText.append("<a href='DOCUMENTATION:")
                  .append(entry)
                  .append("'>")
                  .append(title)
                  .append("</a><br>");
        }

        JTextPane txtDocumentation = new JTextPane();
        txtDocumentation.setContentType("text/html");
        txtDocumentation.setText(formatedDocumentationText.toString());
        txtDocumentation.setEditable(false);
        txtDocumentation.setBorder(null);
        txtDocumentation.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        txtDocumentation.setCaretPosition(0);
        txtDocumentation.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                handleHyperlinkClick(e);
            }
        });

        JScrollPaneWithSpeed scrollDocumentation = new JScrollPaneWithSpeed(txtDocumentation);
        scrollDocumentation.setBorder(RoundedLineBorder.createRoundedLineBorder());

        return scrollDocumentation;
    }

    /**
     * Ensures user preferences for this dialog are tracked under the MekHQ system rather than MegaMek.
     *
     * <p>Automatically manages the dialog's preferences and logs any exceptions.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewDocumentationEntryDialog.class);
            this.setName("NewDocumentationEntryDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
