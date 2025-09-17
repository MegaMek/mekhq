/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 * This class implements a markdown editor that comes with buttons for common markup as well as a preview tab for seeing
 * what the results look like. It can be embedded as a panel in other components.
 *
 * @author Taharqa (Aaron Gullickson)
 */
public class MarkdownEditorPanel extends JPanel {
    private final JTabbedPane tabPane;
    private final JTextArea editor;
    private final JScrollPane scrollEditor;
    private final JScrollPane scrollViewer;
    private final JTextPane viewer;

    /**
     * Constructor for new MarkdownEditorPanel
     */
    public MarkdownEditorPanel() {
        this(null);
    }

    /**
     * Constructor for new MarkdownEditorPanel
     *
     * @param title - a <code>String</code> to show up as the title of the editor at the top
     */
    public MarkdownEditorPanel(String title) {

        tabPane = new JTabbedPane();

        //set up editor
        setLayout(new BorderLayout());
        editor = new JTextArea();
        editor.setEditable(true);
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        scrollEditor = new JScrollPaneWithSpeed(editor);
        scrollEditor.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up buttons
        JPanel pnlButtons = new JPanel(new WrapLayout(FlowLayout.LEFT));
        JButton btnH1 = new JButton(new ImageIcon("data/images/misc/markdown_editor/iconfinder_header_1608924.png")); // TODO : Remove inline file path
        btnH1.setToolTipText("Header 1");
        btnH1.setPreferredSize(new Dimension(36, 36));
        btnH1.addActionListener(ev -> insertHeader(1));
        pnlButtons.add(btnH1);

        JButton btnH2 = new JButton(new ImageIcon("data/images/misc/markdown_editor/iconfinder_header_1608924_20px.png")); // TODO : Remove inline file path
        btnH2.setToolTipText("Header 2");
        btnH2.setPreferredSize(new Dimension(36, 36));
        btnH2.addActionListener(ev -> insertHeader(2));
        pnlButtons.add(btnH2);

        JButton btnH3 = new JButton(new ImageIcon("data/images/misc/markdown_editor/iconfinder_header_1608924_16px.png")); // TODO : Remove inline file path
        btnH3.setToolTipText("Header 3");
        btnH3.setPreferredSize(new Dimension(36, 36));
        btnH3.addActionListener(ev -> insertHeader(3));
        pnlButtons.add(btnH3);

        JButton btnBold = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_format_bold_48px_352381.png")); // TODO : Remove inline file path
        btnBold.setToolTipText("Bold");
        btnBold.setPreferredSize(new Dimension(36, 36));
        btnBold.addActionListener(ev -> boldText());
        pnlButtons.add(btnBold);

        JButton btnItalic = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_format_italic_48px_352387.png")); // TODO : Remove inline file path
        btnItalic.setToolTipText("Italicize");
        btnItalic.setPreferredSize(new Dimension(36, 36));
        btnItalic.addActionListener(ev -> italicizeText());
        pnlButtons.add(btnItalic);

        JButton btnHR = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_remove_48px_352440.png")); // TODO : Remove inline file path
        btnHR.setToolTipText("Horizontal line");
        btnHR.setPreferredSize(new Dimension(36, 36));
        btnHR.addActionListener(ev -> insertHR());
        pnlButtons.add(btnHR);

        JButton btnUL = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_format_list_bulleted_48px_352389.png")); // TODO : Remove inline file path
        btnUL.setToolTipText("Unordered list");
        btnUL.setPreferredSize(new Dimension(36, 36));
        btnUL.addActionListener(ev -> insertBullet(false));
        pnlButtons.add(btnUL);

        JButton btnOL = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_format_list_numbered_48px_352390.png")); // TODO : Remove inline file path
        btnOL.setToolTipText("Ordered list");
        btnOL.setPreferredSize(new Dimension(36, 36));
        btnOL.addActionListener(ev -> insertBullet(true));
        pnlButtons.add(btnOL);

        JButton btnQuestion = getBtnQuestion();
        pnlButtons.add(btnQuestion);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(pnlButtons, BorderLayout.NORTH);
        editorPanel.add(scrollEditor, BorderLayout.CENTER);
        tabPane.add("Write", editorPanel);

        viewer = new JTextPane();
        viewer.setEditable(false);
        viewer.setContentType("text/html");
        scrollViewer = new JScrollPaneWithSpeed(viewer);
        scrollViewer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabPane.add("Preview", scrollViewer);

        tabPane.addChangeListener(e -> {
            if (tabPane.getSelectedIndex() == 1) {
                viewer.setText(MarkdownRenderer.getRenderedHtml(editor.getText()));
                SwingUtilities.invokeLater(() -> scrollViewer.getVerticalScrollBar().setValue(0));
            }
        });
        add(tabPane, BorderLayout.CENTER);
        if (null != title) {
            add(new JLabel("<html><h4>" + title + "</h4></html>"), BorderLayout.NORTH);
        }

        //set up key bindings
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK), "bold");
        editor.getActionMap().put("bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boldText();
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "italic");
        editor.getActionMap().put("italic", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                italicizeText();
            }
        });
    }

    private static JButton getBtnQuestion() {
        JButton btnQuestion = new JButton(new ImageIcon(
              "data/images/misc/markdown_editor/iconfinder_ic_help_48px_352423.png")); // TODO : Remove inline file path
        btnQuestion.setToolTipText("More information");
        btnQuestion.setPreferredSize(new Dimension(36, 36));
        btnQuestion.addActionListener(ev -> JOptionPane.showMessageDialog(null,
              "<html>You can use the CommonMark markdown syntax to add rich text features such as bolding, heading, and italicizing.<br>To learn more about all of the features available go to https://commonmark.org/help/</html>"));
        return btnQuestion;
    }

    /**
     * Set the text for the editor. This can be used when called up on existing text to initially fill the editor.
     *
     * @param text - a <code>String</code> of text to fill the editor with
     */
    public void setText(String text) {
        editor.setText(text);
        SwingUtilities.invokeLater(() -> scrollEditor.getVerticalScrollBar().setValue(0));
    }

    /**
     * Get the text of the editor
     *
     * @return <code>String</code> of the text in the editor
     */
    public String getText() {
        return editor.getText();
    }

    /**
     * Insert bold (**) markup on the selection. If an existing word or phrase is highlighted, this will put the markup
     * at either ends. Otherwise, it will put an empty markup (****) with the cursor in the middle.
     */
    private void boldText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert("**", start);
        editor.insert("**", end + 2);
        if (start == end) {
            editor.setCaretPosition(start + 2);
        } else {
            editor.setCaretPosition(end + 4);
        }
        editor.requestFocusInWindow();
    }

    /**
     * Insert italic (*) markup on the selection. If an existing word or phrase is highlighted, this will put the markup
     * at either ends. Otherwise, it will put an empty markup (**) with the cursor in the middle.
     */
    private void italicizeText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert("*", start);
        editor.insert("*", end + 1);
        if (start == end) {
            editor.setCaretPosition(start + 1);
        } else {
            editor.setCaretPosition(end + 2);
        }
        editor.requestFocusInWindow();
    }

    /**
     * Insert a header (#) into the text at the currently selected start
     *
     * @param level - the level of heading
     */
    private void insertHeader(int level) {
        StringBuilder toInsert = new StringBuilder();
        toInsert.append("#".repeat(Math.max(0, level)));
        toInsert.append(" ");
        int start = editor.getSelectionStart();
        editor.insert(toInsert.toString(), start);
        editor.setCaretPosition(start + toInsert.length());
        editor.requestFocusInWindow();
    }

    /**
     * Insert a horizontal rule (---) into the text at the currently selected start
     */
    private void insertHR() {
        String toInsert = "\n---\n";
        int start = editor.getSelectionStart();
        editor.insert(toInsert, start);
        editor.setCaretPosition(start + toInsert.length());
        editor.requestFocusInWindow();
    }

    /**
     * Insert a bullet point into the text. To ensure it looks correct, the bullet point is surrounded by two carriage
     * returns on either side.
     *
     * @param ordered - a <code>boolean</code> for whether the bullet point should be ordered or not.
     */
    private void insertBullet(boolean ordered) {
        String toInsert = "\n\n- ";
        if (ordered) {
            toInsert = "\n\n1. ";
        }
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert(toInsert, start);
        if (start != end) {
            editor.insert("\n\n", end + toInsert.length());
        }
        editor.setCaretPosition(start + toInsert.length());
        editor.requestFocusInWindow();
    }
}
