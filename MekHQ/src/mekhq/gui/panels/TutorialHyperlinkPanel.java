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
package mekhq.gui.panels;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

import mekhq.gui.dialog.glossary.NewGlossaryDialog;

/**
 * {@code TutorialHyperlinkPanel} is a GUI component for displaying text (typically instructional or tutorial content)
 * with embedded hyperlinks. When a hyperlink is activated, custom actions can be triggered, such as opening glossary
 * dialogs.
 *
 * <p>The text content is retrieved from a resource bundle using a provided key, rendered as HTML, and displayed in a
 * non-editable {@link JEditorPane}. Hyperlinks are handled via a listener that interprets command formats to decide on
 * actions.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class TutorialHyperlinkPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TutorialHyperlinkPanel";

    /** Command string key indicating a glossary lookup request in a hyperlink event. */
    public static final String GLOSSARY_COMMAND_STRING = "GLOSSARY";

    /**
     * Creates a {@code TutorialHyperlinkPanel} displaying text associated with the specified resource key.
     *
     * <p>Displays HTML-formatted, centered text with hyperlinks on a transparent background. Hyperlinks are handled by
     * {@link NewGlossaryDialog#handleGlossaryHyperlinkClick(JDialog, HyperlinkEvent)}.</p>
     *
     * @param key The resource key used to look up the tutorial/instructional text for display.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public TutorialHyperlinkPanel(final String key) {
        setLayout(new BorderLayout());

        JEditorPane paneTutorial = new JEditorPane();
        paneTutorial.setContentType("text/html");
        paneTutorial.setText("<html><div style='text-align:center'>" +
                                   getTextAt(RESOURCE_BUNDLE, key + ".keyText") +
                                   "</div></html>");
        paneTutorial.setEditable(false);
        paneTutorial.setBorder(null);
        paneTutorial.setOpaque(false);
        paneTutorial.addHyperlinkListener(evt -> {
            if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                NewGlossaryDialog.handleGlossaryHyperlinkClick(null, evt);
            }
        });

        add(paneTutorial, BorderLayout.CENTER);
    }
}
