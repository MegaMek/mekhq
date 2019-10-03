package mekhq.gui.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mekhq.gui.dialog.MassRepairSalvageDialog;


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
    
    private JButton btnBold;
    private JButton btnItalic;
    
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
        tabPane.add("Write", scrollEditor);

        viewer = new JTextPane();
        viewer.setBackground(Color.WHITE);
        viewer.setEditable(false);
        viewer.setContentType("text/html");
        scrollViewer = new JScrollPane(viewer);
        scrollViewer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabPane.add("Preview", scrollViewer);
        
        tabPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                viewer.setText(MarkdownRenderer.getRenderedHtml(editor.getText()));
                javax.swing.SwingUtilities.invokeLater(() -> {
                    scrollViewer.getVerticalScrollBar().setValue(0);
                });
            }
        });
        add(tabPane, BorderLayout.CENTER);
        
        //set up buttons
        JPanel pnlButtons = new JPanel(new FlowLayout());
        btnBold = new JButton("B");
        btnBold.addActionListener(ev -> {
            boldText();
        });
        pnlButtons.add(btnBold);
        
        btnItalic = new JButton("I");
        btnItalic.addActionListener(ev -> {
            italicizeText();
        });
        pnlButtons.add(btnItalic);
        add(pnlButtons, BorderLayout.PAGE_START);
        
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
    
    public void boldText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd()+2;
        editor.insert("**", start);
        editor.insert("**", end);
        if(start==end) {
            editor.setCaretPosition(start+2); 
        } else {
            editor.setCaretPosition(end+2);
        }
        editor.requestFocusInWindow();
    }
    
    public void italicizeText() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd()+1;
        editor.insert("*", start);
        editor.insert("*", end);
        if(start==end) {
            editor.setCaretPosition(start+1); 
        } else {
            editor.setCaretPosition(end+1);
        }
        editor.requestFocusInWindow();
    }
}