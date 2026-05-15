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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** Shared input filters and normalizers for the planetary system editor. */
final class PlanetarySystemEditorInputFormatter {

    private PlanetarySystemEditorInputFormatter() {

    }

    static void attachPopulationFormatter(JTextField textField) {
        if (textField.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(new PopulationDocumentFilter(textField));
        }
    }

    static void attachSocioIndustrialFormatter(JTextField textField) {
        if (textField.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(new SocioIndustrialDocumentFilter(textField));
        }
    }

    static String compactPopulationInput(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder compactText = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            int digit = Character.digit(text.charAt(index), 10);
            if (digit >= 0) {
                compactText.append(digit);
            }
        }
        return compactText.toString();
    }

    static String formatSocioIndustrialInput(String text) {
        List<String> ratings = parseSocioIndustrialInputTokens(text);
        return ratings.isEmpty() ? "" : String.join("-", ratings);
    }

    static boolean isValidSocioIndustrialTechCode(String code) {
        return Set.of("ADV", "A", "B", "C", "D", "F", "R", "X").contains(code.trim());
    }

    static boolean isValidSocioIndustrialRatingCode(String code) {
        return Set.of("A", "B", "C", "D", "F", "X").contains(code.trim());
    }

    private static String formatPopulationInput(String text) {
        return compactPopulationInput(text);
    }

    private static List<String> parseSocioIndustrialInputTokens(String text) {
        String compactText = compactSocioIndustrialInput(text);
        if (compactText.isEmpty()) {
            return List.of();
        }

        List<String> ratings = new ArrayList<>();
        while (!compactText.isEmpty() && ratings.isEmpty()) {
            if (compactText.startsWith("ADVANCED")) {
                ratings.add("ADV");
                compactText = compactText.substring("ADVANCED".length());
            } else if (compactText.startsWith("REGRESSED")) {
                ratings.add("R");
                compactText = compactText.substring("REGRESSED".length());
            } else if (compactText.startsWith("ADV")) {
                ratings.add("ADV");
                compactText = compactText.substring("ADV".length());
            } else {
                String code = String.valueOf(compactText.charAt(0));
                compactText = compactText.substring(1);
                if (isValidSocioIndustrialTechCode(code)) {
                    ratings.add(code);
                }
            }
        }

        for (int index = 0; (index < compactText.length()) && (ratings.size() < 5); index++) {
            String code = String.valueOf(compactText.charAt(index));
            if (isValidSocioIndustrialRatingCode(code)) {
                ratings.add(code);
            }
        }
        return ratings;
    }

    private static String compactSocioIndustrialInput(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder compactText = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isLetter(character)) {
                compactText.append(Character.toUpperCase(character));
            }
        }
        return compactText.toString();
    }

    private static final class PopulationDocumentFilter extends DocumentFilter {
        private final JTextField textField;

        private PopulationDocumentFilter(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
              throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
              throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.replace(offset, offset + length, text == null ? "" : text);
            replaceText(bypass, formatPopulationInput(proposedText.toString()), attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.delete(offset, offset + length);
            replaceText(bypass, formatPopulationInput(proposedText.toString()), null);
        }

        private StringBuilder getCurrentText(FilterBypass bypass) throws BadLocationException {
            return new StringBuilder(bypass.getDocument().getText(0, bypass.getDocument().getLength()));
        }

        private void replaceText(FilterBypass bypass, String text, AttributeSet attributes)
              throws BadLocationException {
            bypass.replace(0, bypass.getDocument().getLength(), text, attributes);
            SwingUtilities.invokeLater(() -> textField.setCaretPosition(textField.getText().length()));
        }
    }

    private static final class SocioIndustrialDocumentFilter extends DocumentFilter {
        private final JTextField textField;

        private SocioIndustrialDocumentFilter(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
              throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
              throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.replace(offset, offset + length, text == null ? "" : text);
            replaceText(bypass, formatSocioIndustrialInput(proposedText.toString()), attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            StringBuilder proposedText = getCurrentText(bypass);
            proposedText.delete(offset, offset + length);
            replaceText(bypass, formatSocioIndustrialInput(proposedText.toString()), null);
        }

        private StringBuilder getCurrentText(FilterBypass bypass) throws BadLocationException {
            return new StringBuilder(bypass.getDocument().getText(0, bypass.getDocument().getLength()));
        }

        private void replaceText(FilterBypass bypass, String text, AttributeSet attributes)
              throws BadLocationException {
            bypass.replace(0, bypass.getDocument().getLength(), text, attributes);
            SwingUtilities.invokeLater(() -> textField.setCaretPosition(textField.getText().length()));
        }
    }
}