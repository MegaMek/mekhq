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
package mekhq.gui.baseComponents.hud;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * A dark, HUD-styled rich-text editor: a toolbar of formatting buttons over an HTML editor pane. Styling comes from a
 * stylesheet on each document rather than the look-and-feel, so the text reads correctly on the HUD's dark surface
 * without changing the shared HTML stylesheet other screens use.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudRichTextEditor extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(HudRichTextEditor.class);

    private final JEditorPane editor = new JEditorPane();
    private final HTMLEditorKit kit = new HTMLEditorKit();
    private final JPanel toolbar = new JPanel();
    private final List<HudButton> toolbarButtons = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent event) {
            fireChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            fireChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            fireChanged();
        }
    };
    private Document listenedDocument;
    private boolean loading;

    public HudRichTextEditor() {
        super(new BorderLayout());
        setOpaque(false);

        editor.setEditorKit(kit);
        editor.setTransferHandler(new PlainTextPaste(editor.getTransferHandler()));
        editor.setBackground(SURFACE_DEEP);
        editor.setForeground(TEXT);
        editor.setCaretColor(ACCENT);
        editor.setSelectionColor(SURFACE_HIGHLIGHT);
        editor.setSelectedTextColor(TEXT);
        editor.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12)));
        // Loading HTML can swap the document, so restyle and re-listen whenever it changes.
        editor.addPropertyChangeListener("document", event -> prepareDocument());
        prepareDocument();

        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setOpaque(true);
        toolbar.setBackground(SURFACE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), scaleForGUI(1), 0, scaleForGUI(1), BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(3), scaleForGUI(3), scaleForGUI(3))));

        JScrollPane scroll = new JScrollPane(editor);
        Hud.styleScroll(scroll, SURFACE_DEEP, true);

        add(toolbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Adds the standard formatting buttons: bold, italic, underline, heading and bulleted list.
     *
     * @param bold       the bold button's label
     * @param italic     the italic button's label
     * @param underline  the underline button's label
     * @param heading    the heading button's label
     * @param headingText the placeholder text of an inserted heading
     * @param bullets    the list button's label
     */
    public void addFormattingButtons(String bold, String italic, String underline, String heading, String headingText,
          String bullets) {
        addToolbarAction(bold, new StyledEditorKit.BoldAction());
        addToolbarAction(italic, new StyledEditorKit.ItalicAction());
        addToolbarAction(underline, new StyledEditorKit.UnderlineAction());
        addToolbarAction(heading, new HTMLEditorKit.InsertHTMLTextAction("heading", "<h3>" + headingText + "</h3>",
              HTML.Tag.BODY, HTML.Tag.H3));
        addToolbarAction(bullets, new HTMLEditorKit.InsertHTMLTextAction("bullets", "<ul><li></li></ul>",
              HTML.Tag.BODY, HTML.Tag.UL));
    }

    private void addToolbarAction(String label, Action action) {
        addToolbarButton(label, null, () -> {
            editor.requestFocusInWindow();
            action.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, label));
        });
    }

    /**
     * Adds a button to the end of the toolbar.
     *
     * @param label    the button's label
     * @param tooltip  the button's tooltip, or {@code null}
     * @param onChoose called when the button is clicked
     *
     * @return the button
     */
    public HudButton addToolbarButton(String label, @Nullable String tooltip, Runnable onChoose) {
        HudButton button = new HudButton(label, false, true);
        button.setToolTipText(tooltip);
        button.addActionListener(event -> {
            if (editor.isEditable()) {
                onChoose.run();
            }
        });
        toolbarButtons.add(button);
        toolbar.add(button);
        return button;
    }

    /**
     * @param listener called whenever the player changes the text (not when {@link #setHtml} loads new text)
     */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    /**
     * Loads a document without notifying the change listeners.
     *
     * @param html the HTML document
     */
    public void setHtml(String html) {
        loading = true;
        try {
            editor.setText(html);
            editor.setCaretPosition(0);
        } finally {
            loading = false;
        }
    }

    /**
     * @return the current document as HTML
     */
    public String getHtml() {
        return editor.getText();
    }

    /**
     * @param editable {@code false} to show the text read-only and disarm the toolbar
     */
    public void setEditable(boolean editable) {
        editor.setEditable(editable);
        toolbarButtons.forEach(button -> button.setArmed(editable));
    }

    /**
     * Inserts plain text at the caret, as the player would type it.
     *
     * @param text the text
     */
    public void insertText(String text) {
        try {
            editor.getDocument().insertString(editor.getCaretPosition(), text, null);
        } catch (BadLocationException exception) {
            LOGGER.error("Failed to insert text into the editor", exception);
        }
        editor.requestFocusInWindow();
    }

    /**
     * Inserts a quoted block at the caret.
     *
     * @param html the quote's content, already escaped for HTML
     */
    public void insertQuote(String html) {
        editor.requestFocusInWindow();
        new HTMLEditorKit.InsertHTMLTextAction("quote", "<blockquote>" + html + "</blockquote>", HTML.Tag.BODY,
              HTML.Tag.BLOCKQUOTE).actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "quote"));
    }

    /**
     * @param visible {@code false} to hide the toolbar, for read-only text
     */
    public void setToolbarVisible(boolean visible) {
        toolbar.setVisible(visible);
    }

    public JEditorPane getEditorPane() {
        return editor;
    }

    private void fireChanged() {
        if (!loading) {
            changeListeners.forEach(Runnable::run);
        }
    }

    private void prepareDocument() {
        Document document = editor.getDocument();
        if (document == listenedDocument) {
            return;
        }
        if (listenedDocument != null) {
            listenedDocument.removeDocumentListener(documentListener);
        }
        document.addDocumentListener(documentListener);
        listenedDocument = document;
        if (document instanceof HTMLDocument htmlDocument) {
            applyStyles(htmlDocument.getStyleSheet());
        }
    }

    /**
     * Adds the HUD's text styles to a document's own stylesheet. Each HTML document has its own stylesheet layered
     * over the kit's shared default, so these rules affect only this editor.
     */
    private static void applyStyles(StyleSheet styles) {
        Font font = hudFont(Font.PLAIN, 1.0f, 0.0f);
        styles.addRule("body { color: " + hex(TEXT) + "; font-family: '" + font.getFamily() + "'; font-size: "
                             + font.getSize() + "pt; margin: 0; }");
        styles.addRule("p { margin-top: 0; margin-bottom: 6px; }");
        styles.addRule("h3 { color: " + hex(ACCENT_BRIGHT) + "; font-size: " + (font.getSize() + 4)
                             + "pt; margin-top: 4px; margin-bottom: 4px; }");
        styles.addRule("ul { margin-left: 18px; margin-top: 0; margin-bottom: 6px; }");
        styles.addRule("blockquote { color: " + hex(TEXT_MUTED) + "; margin-left: 10px; margin-top: 2px; "
                             + "margin-bottom: 8px; padding-left: 8px; border-left: 2px solid " + hex(AMBER) + "; }");
        styles.addRule("a { color: " + hex(ACCENT_BRIGHT) + "; }");
    }

    /**
     * Pastes only plain text. Pasted HTML could carry images or links to remote addresses that the editor would fetch
     * when shown; the toolbar is there for formatting. Copying and dragging out still work as before.
     */
    private static final class PlainTextPaste extends TransferHandler {
        private final TransferHandler original;

        private PlainTextPaste(final TransferHandler original) {
            this.original = original;
        }

        @Override
        public boolean canImport(final TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(final TransferSupport support) {
            if (!canImport(support) || !(support.getComponent() instanceof JEditorPane pane)) {
                return false;
            }
            try {
                pane.replaceSelection((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                return true;
            } catch (UnsupportedFlavorException | IOException exception) {
                LOGGER.debug("Could not paste into the editor: {}", exception.getMessage());
                return false;
            }
        }

        @Override
        public int getSourceActions(final JComponent component) {
            return original.getSourceActions(component);
        }

        @Override
        public void exportToClipboard(final JComponent component, final Clipboard clipboard, final int action) {
            original.exportToClipboard(component, clipboard, action);
        }

        @Override
        public void exportAsDrag(final JComponent component, final InputEvent event, final int action) {
            original.exportAsDrag(component, event, action);
        }
    }
}
