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
import static mekhq.gui.dialog.glossary.NewGlossaryDialog.GLOSSARY_COMMAND_STRING;
import static mekhq.gui.dialog.glossary.NewGlossaryDialog.glossaryEntries;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * Dialog for displaying {@link GlossaryEntry} details in a tabbed interface, along with a contents pane listing all
 * available entries. Each entry can display formatted HTML and supports clickable links to navigate between glossary
 * terms. Faction logos are randomly selected and displayed alongside glossary entries.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class NewGlossaryEntryDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(NewGlossaryEntryDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewGlossaryDialog";

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final Dimension DIALOG_SIZE = UIUtil.scaleForGUI(1200, 500);
    private static final int CENTER_PANEL_MINIMUM_WIDTH = UIUtil.scaleForGUI(900);
    private static final int TEXT_WIDTH = UIUtil.scaleForGUI(600);
    private static final Dimension BUTTON_SIZE = UIUtil.scaleForGUI(100, 30);
    private static final int CONTENTS_INDENT = UIUtil.scaleForGUI(300);

    /**
     * The list of faction codes eligible for random selection as glossary tab images.
     *
     * @see Factions#getFactionLogo(int, String)
     */
    private final List<String> FACTION_CODES_FOR_IMAGE = List.of("ARC",
          "ARD",
          "CDP",
          "CC",
          "CIR",
          "CBS",
          "CB",
          "CCC",
          "CCO",
          "CFM",
          "CGB",
          "CGS",
          "CHH",
          "CIH",
          "CJF",
          "CMG",
          "CNC",
          "CDS",
          "CSJ",
          "CSR",
          "CSA",
          "CSV",
          "CSL",
          "CW",
          "CWE",
          "CWIE",
          "CWOV",
          "CS",
          "DC",
          "DA",
          "DTA",
          "CEI",
          "FC",
          "FS",
          "FOR",
          "FVC",
          "FRR",
          "FWL",
          "FR",
          "HL",
          "IP",
          "LL",
          "LA",
          "MOC",
          "MH",
          "MERC",
          "MV",
          "NC",
          "OC",
          "OA",
          "PIR",
          "RD",
          "RF",
          "ROS",
          "RWR",
          "IND",
          "SIC",
          "SL",
          "TC",
          "TD",
          "UC",
          "WOB",
          "TH",
          "CI",
          "SOC",
          "CWI",
          "EF",
          "GV",
          "JF",
          "MSC",
          "OP",
          "RA",
          "RCM",
          "NIOPS",
          "AXP",
          "NDC",
          "REB");

    /**
     * The main tabbed pane showing glossary entries.
     */
    private final EnhancedTabbedPane tabbedPane;

    /**
     * Constructs a dialog for the specified parent and initial glossary entry.
     *
     * @param parent        the parent dialog for modal positioning
     * @param glossaryEntry the initial glossary entry to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public NewGlossaryEntryDialog(JDialog parent, GlossaryEntry glossaryEntry) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.title"));

        tabbedPane = new EnhancedTabbedPane(false, true);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.setMinimumSize(new Dimension(CENTER_PANEL_MINIMUM_WIDTH, Integer.MIN_VALUE));
        addGlossaryEntry(glossaryEntry);

        JScrollPaneWithSpeed scrollFullGlossary = buildGlossaryPane();
        JPanel fullContentsWrapper = new JPanel(new BorderLayout());
        fullContentsWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        fullContentsWrapper.add(scrollFullGlossary, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, fullContentsWrapper);
        splitPane.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        splitPane.setDividerSize(PADDING);
        splitPane.setOneTouchExpandable(true);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(contentPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(DIALOG_SIZE));
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(CENTER_PANEL_MINIMUM_WIDTH, DIALOG_SIZE.height));
        setPreferences(); // Must be before setVisible

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(getWidth() - CONTENTS_INDENT));

        setVisible(true);
    }

    /**
     * Adds a new glossary entry as a tab and displays it, or selects the tab if already present.
     *
     * @param glossaryEntry the glossary entry to display in a new tab
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void addGlossaryEntry(GlossaryEntry glossaryEntry) {
        String title = glossaryEntry.getTitle();

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
        String definition = glossaryEntry.getDefinition();
        String formatedGlossaryText = "<h2 style='text-align:center;'>" + title + "</h2>" + definition;

        JTextPane txtDefinition = new JTextPane();
        txtDefinition.setContentType("text/html");
        String fontStyle = "font-family: Noto Sans;";
        txtDefinition.setText(String.format(
              "<div style='width: %s; %s'>%s</div>",
              TEXT_WIDTH, fontStyle, formatedGlossaryText));
        FlatLafStyleBuilder.setFontScaling(txtDefinition, false, 1.1);
        txtDefinition.setEditable(false);
        txtDefinition.setBorder(null);
        txtDefinition.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        txtDefinition.setCaretPosition(0);
        txtDefinition.setMaximumSize(new Dimension(TEXT_WIDTH, Integer.MAX_VALUE));
        txtDefinition.setPreferredSize(new Dimension(TEXT_WIDTH, txtDefinition.getPreferredSize().height));
        txtDefinition.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtDefinition.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                handleHyperlinkClick(e);
            }
        });

        // Create a panel to hold both image and text, then put that in the scroll pane
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);

        JLabel lblImage = new JLabel(getImage());
        lblImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImage.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
        scrollContent.add(lblImage);
        scrollContent.add(Box.createVerticalStrut(PADDING));
        scrollContent.add(txtDefinition);

        JScrollPaneWithSpeed scrollGlossaryEntry = new JScrollPaneWithSpeed(scrollContent);
        scrollGlossaryEntry.setBorder(null);
        scrollGlossaryEntry.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollGlossaryEntry.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(scrollGlossaryEntry, BorderLayout.CENTER);
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
     * Handles hyperlink events within glossary HTML panes. Hyperlinks allow navigation between glossary terms and,
     * where implemented, documentation.
     *
     * @param hyperlinkEvent the hyperlink event to process
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

            if (command.equalsIgnoreCase(GLOSSARY_COMMAND_STRING)) {
                GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName(entry);

                if (glossaryEntry == null) {
                    LOGGER.warn("Glossary entry not found: {}", entry);
                    return;
                }

                addGlossaryEntry(glossaryEntry);
            } else if (command.equalsIgnoreCase(DOCUMENTATION_COMMAND_STRING)) {
                DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);

                if (documentationEntry == null) {
                    LOGGER.warn("Documentation entry not found: {}", entry);
                    return;
                }

                new NewDocumentationEntryDialog(this, documentationEntry);
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
    private ImageIcon getImage() {
        String randomFactionCode = ObjectUtility.getRandomItem(FACTION_CODES_FOR_IMAGE);

        // game year is largely irrelevant for the glossary dialog
        ImageIcon baseImage = Factions.getFactionLogo(3025, randomFactionCode);

        return ImageUtilities.scaleImageIcon(baseImage, 100, false);
    }

    /**
     * Builds the scrollable contents pane listing all glossary entries with links for navigation.
     *
     * @return a {@link JScrollPaneWithSpeed} containing the formatted glossary contents
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JScrollPaneWithSpeed buildGlossaryPane() {
        StringBuilder formatedGlossaryText = new StringBuilder();
        formatedGlossaryText.append(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.contentsPane.title"));

        for (String entry : glossaryEntries) {
            GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName(entry);
            String title = glossaryEntry != null ? glossaryEntry.getTitle() : "-";

            formatedGlossaryText.append("<a href='GLOSSARY:")
                  .append(entry)
                  .append("'>")
                  .append(title)
                  .append("</a><br>");
        }

        JTextPane txtGlossary = new JTextPane();
        txtGlossary.setContentType("text/html");
        txtGlossary.setText(formatedGlossaryText.toString());
        txtGlossary.setEditable(false);
        txtGlossary.setBorder(null);
        txtGlossary.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        txtGlossary.setCaretPosition(0);
        txtGlossary.addHyperlinkListener(this::handleHyperlinkClick);

        JScrollPaneWithSpeed scrollGlossary = new JScrollPaneWithSpeed(txtGlossary);
        scrollGlossary.setBorder(RoundedLineBorder.createRoundedLineBorder());

        return scrollGlossary;
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
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewGlossaryEntryDialog.class);
            this.setName("NewGlossaryEntryDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
