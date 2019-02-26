/*
 * NewsReportDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import mekhq.MekHQ;
import mekhq.campaign.universe.NewsItem;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 *
 * @author Jay Lawson
 */
public class NewsReportDialog extends javax.swing.JDialog {
    
    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtNews;

    public NewsReportDialog(java.awt.Frame parent, NewsItem news) {
        super(parent, false);
        setTitle(news.getHeadline());
        initComponents();     
        txtNews.setText(news.getFullDescription());
        txtNews.setCaretPosition(0);
        setMinimumSize(new Dimension(500, 300));
        setPreferredSize(new Dimension(500, 300));
        setLocationRelativeTo(parent);
        setUserPreferences();
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

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(NewsReportDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}
