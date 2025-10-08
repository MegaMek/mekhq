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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A dialog window for displaying both glossary and documentation entries in MekHQ.
 *
 * <p>This dialog presents the user with an about pane, a searchable glossary of terms, and a set of documentation
 * links, all styled and organized for optimal navigation.</p>
 *
 * <p>Clicking on glossary or documentation entries will trigger additional dialogs or logic.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class NewGlossaryDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(NewGlossaryDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewGlossaryDialog";

    /**
     * The command string prefix used to identify glossary entry hyperlinks.
     *
     * <p>Hyperlink URLs in the glossary pane use this prefix to indicate that an entry should be handled as a
     * glossary term, e.g. {@code "GLOSSARY:entryKey"}.</p>
     *
     * @see #handleGlossaryHyperlinkClick(JDialog, HyperlinkEvent)
     * @since 0.50.07
     */
    public static final String GLOSSARY_COMMAND_STRING = "GLOSSARY";

    /**
     * The command string prefix used to identify documentation entry hyperlinks.
     *
     * <p>Hyperlink URLs in the documentation pane use this prefix to indicate that an entry should be handled as a
     * documentation term, e.g. {@code "DOCUMENTATION:entryKey"}.</p>
     *
     * @see #handleGlossaryHyperlinkClick(JDialog, HyperlinkEvent)
     * @since 0.50.07
     */
    public static final String DOCUMENTATION_COMMAND_STRING = "DOCUMENTATION";

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int ABOUT_WIDTH = UIUtil.scaleForGUI(400) - (PADDING * 2);
    private static final int DEFAULT_DIALOG_HEIGHT = UIUtil.scaleForGUI(400);
    private static final Dimension BUTTON_SIZE = UIUtil.scaleForGUI(100, 30);

    /**
     * Glossary term keys sorted by localized title.
     */
    final static List<String> glossaryEntries = GlossaryEntry.getLookUpNamesSortedByTitle();

    /**
     * Documentation keys sorted by localized title.
     */
    final static List<String> documentationEntries = DocumentationEntry.getLookUpNamesSortedByTitle();
    private int minimumWidth = 0;

    /**
     * Creates and displays a new glossary and documentation dialog. Automatically sizes and positions the dialog based
     * on UI scaling.
     *
     * @param parent the parent {@link JFrame} for this dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    public NewGlossaryDialog(Frame parent) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.title"));

        JPanel pnlContents = buildContentsPanel();

        setLayout(new BorderLayout());
        add(pnlContents, BorderLayout.EAST);

        setMinimumSize(new Dimension(minimumWidth, DEFAULT_DIALOG_HEIGHT));
        setPreferences(); // Must be before setVisible
        setLocationRelativeTo(parent);

        setVisible(true);
    }

    /**
     * Builds the complete contents panel, including about, glossary, and documentation sections, with appropriate
     * padding and layout.
     *
     * @return the contents {@code JPanel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel buildContentsPanel() {
        JPanel pnlAbout = buildAboutPanel();
        JPanel aboutWrapper = new JPanel(new BorderLayout());
        aboutWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        aboutWrapper.add(pnlAbout, BorderLayout.CENTER);

        minimumWidth += aboutWrapper.getPreferredSize().width;

        FastJScrollPane scrollGlossary = buildGlossaryPane();
        JPanel contentsWrapper = new JPanel(new BorderLayout());
        contentsWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        contentsWrapper.add(scrollGlossary, BorderLayout.CENTER);

        minimumWidth += contentsWrapper.getPreferredSize().width;

        FastJScrollPane scrollDocumentation = buildDocumentationPane();
        JPanel documentationWrapper = new JPanel(new BorderLayout());
        documentationWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        documentationWrapper.add(scrollDocumentation, BorderLayout.CENTER);

        minimumWidth += documentationWrapper.getPreferredSize().width;

        JPanel contentDocsPanel = new JPanel();
        contentDocsPanel.setLayout(new BoxLayout(contentDocsPanel, BoxLayout.X_AXIS));
        contentDocsPanel.add(aboutWrapper);
        contentDocsPanel.add(contentsWrapper);
        contentDocsPanel.add(documentationWrapper);
        return contentDocsPanel;
    }

    /**
     * Builds the about panel, which includes the ComStar faction log, descriptive text, and a close button.
     *
     * @return the about {@code JPanel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel buildAboutPanel() {
        JLabel lblImage = new JLabel(getImage());

        JPanel imageWrapper = new JPanel();
        imageWrapper.setLayout(new BorderLayout());
        imageWrapper.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        imageWrapper.add(lblImage, BorderLayout.CENTER);
        imageWrapper.setAlignmentY(Component.TOP_ALIGNMENT);

        JTextPane txtAbout = new JTextPane();
        txtAbout.setContentType("text/html");
        String fontStyle = "font-family: Noto Sans;";
        txtAbout.setText(String.format("<div style='width: %s; %s'>%s</div>",
              ABOUT_WIDTH, fontStyle, getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.aboutPane")));
        FlatLafStyleBuilder.setFontScaling(txtAbout, false, 1.1);
        txtAbout.setBorder(null);
        txtAbout.setEditable(false);
        txtAbout.setCaretPosition(0);
        txtAbout.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                dispose();
                handleGlossaryHyperlinkClick(this, e);
            }
        });
        txtAbout.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel aboutPanel = new JPanel();
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        aboutPanel.add(imageWrapper);
        aboutPanel.add(txtAbout);

        FastJScrollPane scrollAbout = new FastJScrollPane(aboutPanel);
        scrollAbout.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollAbout.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollAbout.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JButton btnClose = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.button.close"));
        btnClose.setPreferredSize(BUTTON_SIZE);
        btnClose.setMaximumSize(BUTTON_SIZE);
        btnClose.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        buttonPanel.add(btnClose);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollAbout, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;

    }

    /**
     * Retrieves and returns the logo image/icon to show in the dialog.
     *
     * <p>Uses a default year, as the glossary display is not year-specific.</p>
     *
     * @return a scaled {@link ImageIcon} for the dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static ImageIcon getImage() {
        // game year is largely irrelevant for the glossary dialog
        ImageIcon baseImage = Factions.getFactionLogo(3025, "CS");

        return ImageUtilities.scaleImageIcon(baseImage, 100, false);
    }

    /**
     * Builds the scrollable glossary section, populated with clickable entry links.
     *
     * @return a {@link FastJScrollPane} containing the glossary links
     *
     * @author Illiani
     * @since 0.50.07
     */
    private FastJScrollPane buildGlossaryPane() {
        StringBuilder formatedGlossaryText = new StringBuilder();
        formatedGlossaryText.append(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.contentsPane.title"));

        String lastFirstLetter = "";
        for (String entry : glossaryEntries) {
            GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName(entry);
            String title = glossaryEntry != null ? glossaryEntry.getTitle() : "-";

            if (!lastFirstLetter.equals(title.substring(0, 1))) {
                lastFirstLetter = title.substring(0, 1);
                formatedGlossaryText.append("<h2>")
                      .append(lastFirstLetter)
                      .append("</h2>");
            }

            formatedGlossaryText.append("<a href='GLOSSARY:")
                  .append(entry)
                  .append("'>")
                  .append(title)
                  .append("</a><br>");
        }

        JTextPane txtGlossary = createGlossaryDialogTextPane(formatedGlossaryText);

        FastJScrollPane scrollGlossary = new FastJScrollPane(txtGlossary);
        scrollGlossary.setBorder(RoundedLineBorder.createRoundedLineBorder());

        return scrollGlossary;
    }

    /**
     * Configures a {@link JTextPane} for glossary or documentation display with HTML content and hyperlinks.
     *
     * @param formatedGlossaryText the text (HTML-format) to display
     *
     * @return a configured {@link JTextPane}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JTextPane createGlossaryDialogTextPane(StringBuilder formatedGlossaryText) {
        JTextPane txtGlossary = new JTextPane();
        txtGlossary.setContentType("text/html");
        txtGlossary.setText(formatedGlossaryText.toString());
        txtGlossary.setEditable(false);
        txtGlossary.setBorder(null);
        txtGlossary.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        txtGlossary.setCaretPosition(0);
        txtGlossary.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                dispose();
                handleGlossaryHyperlinkClick(this, e);
            }
        });

        return txtGlossary;
    }

    /**
     * Builds the scrollable documentation section, each entry as a clickable link.
     *
     * @return a {@link FastJScrollPane} containing the documentation links
     *
     * @author Illiani
     * @since 0.50.07
     */
    private FastJScrollPane buildDocumentationPane() {
        StringBuilder formatedDocumentationText = new StringBuilder();
        formatedDocumentationText.append(getTextAt(RESOURCE_BUNDLE, "GlossaryDialog.documentationPane.title"));

        String lastFirstLetter = "";
        for (String entry : documentationEntries) {
            DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);
            String title = documentationEntry != null ? documentationEntry.getTitle() : "-";

            if (!lastFirstLetter.equals(title.substring(0, 1))) {
                lastFirstLetter = title.substring(0, 1);
                formatedDocumentationText.append("<h2>")
                      .append(lastFirstLetter)
                      .append("</h2>");
            }

            formatedDocumentationText.append("<a href='DOCUMENTATION:")
                  .append(entry)
                  .append("'>")
                  .append(title)
                  .append("</a><br>");
        }

        JTextPane txtDocumentation = createGlossaryDialogTextPane(formatedDocumentationText);

        FastJScrollPane scrollDocumentation = new FastJScrollPane(txtDocumentation);
        scrollDocumentation.setBorder(RoundedLineBorder.createRoundedLineBorder());

        return scrollDocumentation;
    }

    /**
     * Handles hyperlink events from glossary and documentation panes.
     *
     * <p>Triggers display of new dialogs for the glossary or documentation.</p>
     *
     * @param hyperlinkEvent the event generated by the user's click
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void handleGlossaryHyperlinkClick(JDialog parent, HyperlinkEvent hyperlinkEvent) {
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

                new NewGlossaryEntryDialog(parent, glossaryEntry);
            } else if (command.equalsIgnoreCase(DOCUMENTATION_COMMAND_STRING)) {
                DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entry);

                if (documentationEntry == null) {
                    LOGGER.warn("Documentation entry not found: {}", entry);
                    return;
                }

                try {
                    new NewDocumentationEntryDialog(parent, documentationEntry);
                } catch (Exception ex) {
                    LOGGER.error("Failed to open PDF", ex);
                }
            }
        }
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
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewGlossaryDialog.class);
            this.setName("NewGlossaryDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
