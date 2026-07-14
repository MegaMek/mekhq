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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.gui.campaignOptions.components;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.utilities.ReportingUtilities;

public class CampaignOptionsHighlighter {

    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsHighlighter.class);
    // used to store the original, un-highlighted text in the component's properties
    private static final String ORIGINAL_TEXT_KEY = "OriginalText";

    // spinner highlighting support
    private static final Highlighter.HighlightPainter SPINNER_PAINTER =
          new DefaultHighlighter.DefaultHighlightPainter(MekHQ.getMHQOptions().getFontColorPositive());

    /**
     * Traverses a container and highlights the search term in specific child components.
     *
     * @param container  The root container to start the traversal
     * @param searchTerm The text to highlight. If null or empty, highlights are cleared
     */
    public static void highlightSearchTerm(Container container, String searchTerm) {
        for (Component comp : container.getComponents()) {
            switch (comp) {
                case JLabel label -> updateHighlight(label, label.getText(), searchTerm);
                case JCheckBox checkBox -> updateHighlight(checkBox, checkBox.getText(), searchTerm);
                case JSpinner spinner -> updateSpinnerHighlight(spinner, searchTerm);
                case Container c -> highlightSearchTerm(c, searchTerm);
                default -> {} // ignore other component types
            }
        }
    }

    /**
     * Traverses a container and extracts text from all child components.
     *
     * @param container The root container to start the traversal
     */
    public static List<String> extractAllText(Container container) {
        List<String> extractedTexts = new ArrayList<>();
        extractTextRecursive(container, extractedTexts);
        return extractedTexts;
    }

    private static void updateHighlight(JComponent comp, String currentText, String searchTerm) {
        if (currentText == null) {
            return;
        }

        String originalText = (String) comp.getClientProperty(ORIGINAL_TEXT_KEY);
        if (originalText == null) {
            originalText = currentText;
            comp.putClientProperty(ORIGINAL_TEXT_KEY, originalText);
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            setText(comp, originalText);
            return;
        }

        String highlightedText = applyHighlight(originalText, searchTerm);
        setText(comp, highlightedText);
    }

    private static String applyHighlight(String originalText, String searchTerm) {
        boolean isHtml = originalText.trim().toLowerCase().startsWith("<html>");

        if (isHtml) {
            // match html tags or the search term
            String regex = "(?i)(<[^>]+>)|(" + Pattern.quote(searchTerm) + ")";
            Matcher m = Pattern.compile(regex).matcher(originalText);
            StringBuilder sb = new StringBuilder();

            while (m.find()) {
                if (m.group(1) != null) {
                    // html tag, skip
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1)));
                } else if (m.group(2) != null) {
                    // the search term, highlight
                    String wrapped = ReportingUtilities.messageSurroundedBySpanWithColor(
                          ReportingUtilities.getPositiveColor(), m.group(2));
                    m.appendReplacement(sb, Matcher.quoteReplacement(wrapped));
                }
            }
            m.appendTail(sb);
            return sb.toString();
        } else {
            Matcher m = Pattern.compile("(?i)" + Pattern.quote(searchTerm)).matcher(originalText);
            StringBuilder sb = new StringBuilder("<html>");

            int lastEnd = 0;
            while (m.find()) {
                sb.append(escapeHtml(originalText.substring(lastEnd, m.start())));
                String highligted = ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getPositiveColor(), escapeHtml(m.group()));
                sb.append(highligted);
                lastEnd = m.end();
            }
            // append remaining text
            sb.append(escapeHtml(originalText.substring(lastEnd)));
            sb.append("</html>");
            return sb.toString();
        }
    }

    private static void updateSpinnerHighlight(JSpinner spinner, String searchTerm) {
        JComponent editor = spinner.getEditor();
        if (!(editor instanceof JSpinner.DefaultEditor)) {
            return;
        }

        JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        Highlighter highlighter = textField.getHighlighter();

        for (Highlighter.Highlight highlight : highlighter.getHighlights()) {
            if (highlight.getPainter() == SPINNER_PAINTER) {
                highlighter.removeHighlight(highlight);
            }
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }

        String text = textField.getText().toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();

        int pos = 0;
        while ((pos = text.indexOf(lowerSearch, pos)) >= 0) {
            try {
                highlighter.addHighlight(pos, pos + lowerSearch.length(), SPINNER_PAINTER);
                pos += lowerSearch.length();
            } catch (BadLocationException e) {
                LOGGER.error("Error highlighting JSpinner", e);
            }
        }
    }

    private static void setText(JComponent comp, String text) {
        if (comp instanceof JLabel) {
            ((JLabel) comp).setText(text);
        } else if (comp instanceof JCheckBox) {
            ((JCheckBox) comp).setText(text);
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void extractTextRecursive(Container container, List<String> texts) {
        for (Component comp : container.getComponents()) {
            String text = switch (comp) {
                case JLabel label -> getOriginalOrCurrentText(label, label.getText());
                case JCheckBox checkBox -> getOriginalOrCurrentText(checkBox, checkBox.getText());
                case JSpinner spinner -> getSpinnerText(spinner);
                case Container c -> {
                    extractTextRecursive(c, texts);
                    yield null;
                }
                default -> null;
            };
            if (text != null && !text.trim().isEmpty()) {
                texts.add(text);
            }
        }
    }

    private static String getOriginalOrCurrentText(JComponent comp, String currentText) {
        String originalText = (String) comp.getClientProperty(ORIGINAL_TEXT_KEY);
        return (originalText != null) ? originalText : currentText;
    }

    private static String getSpinnerText(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor) editor).getTextField().getText();
        }
        return spinner.getValue() != null ? spinner.getValue().toString() : null;
    }
}
