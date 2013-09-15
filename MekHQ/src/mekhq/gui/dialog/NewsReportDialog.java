/*
 * NewsReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 *
 * @author Jay Lawson
 */
public class NewsReportDialog extends javax.swing.JDialog {
    
    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtNews;

    public NewsReportDialog(java.awt.Frame parent, String headline, String article) {
        super(parent, false);
        setTitle(headline);
        initComponents();     
        txtNews.setText(article);
        txtNews.setCaretPosition(0);
        setMinimumSize(new Dimension(500, 300));
        setPreferredSize(new Dimension(500, 300));
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {

        txtNews = new JTextPane();
        txtNews.setContentType("text/html");
        
        setLayout(new java.awt.BorderLayout());
        
        txtNews.setEditable(false);
        
        getContentPane().add(new JScrollPane(txtNews), BorderLayout.CENTER);
    }
    
}
