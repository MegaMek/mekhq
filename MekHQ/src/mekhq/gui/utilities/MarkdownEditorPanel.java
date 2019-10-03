package mekhq.gui.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MarkdownEditorPanel extends JPanel {
    
    /**
     * 
     */
    private static final long serialVersionUID = 7534667332172721918L;
    
    private JTabbedPane tabPane;
    private JTextArea editor;
    private JScrollPane scrollEditor;
    private JScrollPane scrollViewer;
    private JTextPane viewer;
    
    private JButton btnH1;
    private JButton btnH2;
    private JButton btnH3;
    private JButton btnBold;
    private JButton btnItalic;
    private JButton btnHR;
    private JButton btnUL;
    private JButton btnOL;
    private JButton btnQuestion;

    
    public MarkdownEditorPanel() {
      
        tabPane = new JTabbedPane();
        
        //set up editor
        setLayout(new BorderLayout());
        editor = new JTextArea();
        editor.setEditable(true);
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        scrollEditor = new JScrollPane(editor);
        scrollEditor.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        //set up buttons
        JPanel pnlButtons = new JPanel(new WrapLayout(FlowLayout.LEFT));
        btnH1 = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-header-1-24.png"));
        btnH1.setToolTipText("Header 1");
        btnH1.setPreferredSize(new Dimension(36, 36));
        btnH1.addActionListener(ev -> {
            insertHeader(1);
        });
        pnlButtons.add(btnH1);
        
        btnH2 = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-header-2-24.png"));
        btnH2.setToolTipText("Header 2");
        btnH2.setPreferredSize(new Dimension(36, 36));
        btnH2.addActionListener(ev -> {
            insertHeader(2);
        });
        pnlButtons.add(btnH2);
        
        btnH3 = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-header-3-24.png"));
        btnH3.setToolTipText("Header 3");
        btnH3.setPreferredSize(new Dimension(36, 36));
        btnH3.addActionListener(ev -> {
            insertHeader(3);
        });
        pnlButtons.add(btnH3);
        
        btnBold = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-bold-24.png"));
        btnBold.setToolTipText("Bold");
        btnBold.setPreferredSize(new Dimension(36, 36));
        btnBold.addActionListener(ev -> {
            boldText();
        });
        pnlButtons.add(btnBold);
        
        btnItalic = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-italic-24.png"));
        btnItalic.setToolTipText("Italicize");
        btnItalic.setPreferredSize(new Dimension(36, 36));
        btnItalic.addActionListener(ev -> {
            italicizeText();
        });
        pnlButtons.add(btnItalic);
        
        btnHR = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-horizontal-line-24.png"));
        btnHR.setToolTipText("Horizontal line");
        btnHR.setPreferredSize(new Dimension(36, 36));
        btnHR.addActionListener(ev -> {
            insertHR();
        });
        pnlButtons.add(btnHR);
        
        btnUL = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-list-24.png"));
        btnUL.setToolTipText("Unordered list");
        btnUL.setPreferredSize(new Dimension(36, 36));
        btnUL.addActionListener(ev -> {
            insertBullet(false);
        });
        pnlButtons.add(btnUL);
        
        btnOL = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-numbered-list-24.png"));
        btnOL.setToolTipText("Ordered list");
        btnOL.setPreferredSize(new Dimension(36, 36));
        btnOL.addActionListener(ev -> {
            insertBullet(true);
        });
        pnlButtons.add(btnOL);
        
        btnQuestion = new JButton(new ImageIcon("data/images/misc/markdown_editor/icons8-question-mark-24.png"));
        btnQuestion.setToolTipText("More information");
        btnQuestion.setPreferredSize(new Dimension(36, 36));
        btnQuestion.addActionListener(ev -> {
            //nothing yet
        });
        pnlButtons.add(btnQuestion);
        
        
        
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(pnlButtons, BorderLayout.NORTH);
        editorPanel.add(scrollEditor, BorderLayout.CENTER);        
        tabPane.add("Write", editorPanel);

        viewer = new JTextPane();
        viewer.setBackground(Color.WHITE);
        viewer.setEditable(false);
        viewer.setContentType("text/html");
        scrollViewer = new JScrollPane(viewer);
        scrollViewer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabPane.add("Preview", scrollViewer);
        
        tabPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(tabPane.getSelectedIndex()==1) {
                viewer.setText(MarkdownRenderer.getRenderedHtml(editor.getText()));
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        scrollViewer.getVerticalScrollBar().setValue(0);
                    });
                }
            }
        });
        add(tabPane, BorderLayout.CENTER);
        
        
    }
    
    public void setText(String text) {
        editor.setText(text);
        javax.swing.SwingUtilities.invokeLater(() -> {
            scrollEditor.getVerticalScrollBar().setValue(0);
        });
    }
    
    public String getText() {
        return editor.getText();
    }
    
    private void boldText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert("**", start);
        editor.insert("**", end+2);
        if(start==end) {
            editor.setCaretPosition(start+2); 
        } else {
            editor.setCaretPosition(end+4);
        }
        editor.requestFocusInWindow();
    }
    
    private void italicizeText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert("*", start);
        editor.insert("*", end+1);
        if(start==end) {
            editor.setCaretPosition(start+1); 
        } else {
            editor.setCaretPosition(end+2);
        }
        editor.requestFocusInWindow();
    }
    
    private void insertHeader(int level) {
        String toInsert = "";
        for(int i = 0; i < level; i++) {
            toInsert = toInsert + "#";
        }
        toInsert = toInsert + " ";
        int start = editor.getSelectionStart();
        editor.insert(toInsert, start);
        editor.setCaretPosition(start+toInsert.length());
        editor.requestFocusInWindow();
    }
    
    private void insertHR() {
        String toInsert = "\n---\n";
        int start = editor.getSelectionStart();
        editor.insert(toInsert, start);
        editor.setCaretPosition(start+toInsert.length());
        editor.requestFocusInWindow();
    }
    
    private void insertBullet(boolean ordered) {
        String toInsert = "\n\n- ";
        if(ordered) {
            toInsert = "\n\n1. ";
        }
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        editor.insert(toInsert, start);
        if(start!=end) {
            editor.insert("\n\n", end+toInsert.length());
        }
        editor.setCaretPosition(start+toInsert.length());
        editor.requestFocusInWindow();
    }
}