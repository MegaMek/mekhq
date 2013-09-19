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
import javax.swing.border.EmptyBorder;

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
        txtNews.setEditable(false);
        JScrollPane scrNews = new JScrollPane(txtNews);
        scrNews.setBorder( new EmptyBorder(2,10,2,2));

        setLayout(new java.awt.BorderLayout());
        
        txtNews.setEditable(false);
        
        getContentPane().add(scrNews, BorderLayout.CENTER);
    }
    
}
