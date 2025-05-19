package mekhq.gui.panels;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

import mekhq.gui.dialog.GlossaryDialog;

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
     * {@link #handleTutorialHyperlinkClick(JDialog, String)}.</p>
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
                handleTutorialHyperlinkClick(null, evt.getDescription());
            }
        });

        add(paneTutorial, BorderLayout.CENTER);
    }

    /**
     * Handles a hyperlink-click event within the tutorial panel.
     *
     * <p>If the hyperlink reference string starts with the {@link #GLOSSARY_COMMAND_STRING} command, it opens a
     * {@link GlossaryDialog} with the provided entry key.</p>
     *
     * <p>The reference is expected to be in the form {@code "COMMAND:entryKey"}.</p>
     *
     * <p>Example: {@code GLOSSARY:PRISONERS_OF_WAR}</p>
     *
     * @param parent    the parent {@link JDialog}, or {@code null} for no parent dialog.
     * @param reference the hyperlink reference string specifying the command and key.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static void handleTutorialHyperlinkClick(JDialog parent, String reference) {
        String[] splitReference = reference.split(":");

        String commandKey = splitReference[0];
        String entryKey = splitReference[1];

        if (commandKey.equalsIgnoreCase(GLOSSARY_COMMAND_STRING)) {
            new GlossaryDialog(parent, entryKey);
        }
    }
}
