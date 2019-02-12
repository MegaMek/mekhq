/*
 * StartUpDialog.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.gui.dialog.DataLoadingDialog;
/**
 *
 * @author Jay
 */
public class StartUpGUI extends javax.swing.JPanel {

	
	private static final long serialVersionUID = 8376874926997734492L;
	MekHQ app;
	JFrame frame;
	File lastSave;
	Image imgSplash;
	   
	public StartUpGUI(MekHQ app) {
        this.app = app;
        lastSave = Utilities.lastFileModified(MekHQ.CAMPAIGN_DIRECTORY, new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".cpnx") || name.toLowerCase().endsWith(".xml");
            }
        });
        
        initComponents();
    }

    private void initComponents() {

    	frame = new JFrame("MekHQ");
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.StartUpDialog", new EncodeControl()); //$NON-NLS-1$
        
        // initialize splash image
        double maxWidth = app.calculateMaxScreenWidth();
        imgSplash = getToolkit().getImage(app.getIconPackage().getStartupScreenImage((int) maxWidth));

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            // really should never come here
        }

        // Determine if the splash screen image is "small"
        // and if so switch to shorter text and smaller buttons
        boolean shortText = false;
        int buttonWidth = 200;
        if (imgSplash.getWidth(null) < 840) {
            shortText = true;
            buttonWidth = 150;
        }

        btnNewGame = new javax.swing.JButton(resourceMap.getString(shortText ? "btnNewGame.text.short" : "btnNewGame.text"));
        btnNewGame.setMinimumSize(new Dimension(buttonWidth, 25));
        btnNewGame.setPreferredSize(new Dimension(buttonWidth, 25));
        btnNewGame.setMaximumSize(new Dimension(buttonWidth, 25));
        btnNewGame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	newCampaign();
            }
        });
        btnLoadGame = new javax.swing.JButton(resourceMap.getString(shortText ? "btnLoadGame.text.short" : "btnLoadGame.text"));
        btnLoadGame.setMinimumSize(new Dimension(buttonWidth, 25));
        btnLoadGame.setPreferredSize(new Dimension(buttonWidth, 25));
        btnLoadGame.setMaximumSize(new Dimension(buttonWidth, 25));
        btnLoadGame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	File f = selectLoadCampaignFile();
            	if(null != f) {
                	loadCampaign(f);
            	}
            }
        });
        btnLastSave = new javax.swing.JButton(resourceMap.getString(shortText ? "btnLastSave.text.short" : "btnLastSave.text"));
        btnLastSave.setMinimumSize(new Dimension(buttonWidth, 25));
        btnLastSave.setPreferredSize(new Dimension(buttonWidth, 25));
        btnLastSave.setMaximumSize(new Dimension(buttonWidth, 25));
        if(null == lastSave) {
        	btnLastSave.setEnabled(false);
        }
        btnLastSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	loadCampaign(lastSave);
            }
        });
        btnQuit = new javax.swing.JButton(resourceMap.getString("btnQuit.text"));
        btnQuit.setMinimumSize(new Dimension(buttonWidth, 25));
        btnQuit.setPreferredSize(new Dimension(buttonWidth, 25));
        btnQuit.setMaximumSize(new Dimension(buttonWidth, 25));
        btnQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.exit(0);
            }
        });
        
        setLayout(new BorderLayout(1, 1));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(btnNewGame);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(btnLoadGame);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(btnLastSave);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(btnQuit);
        add(buttonPanel, BorderLayout.PAGE_END);
                
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    
        frame.setSize(imgSplash.getWidth(null), imgSplash.getHeight(null));
        frame.setResizable(false);
	    // Determine the new location of the window
	    int w = frame.getSize().width;
	    int h = frame.getSize().height;
	    int x = (dim.width-w)/2;
	    int y = (dim.height-h)/2;
	    
	    // Move the window
	    frame.setLocation(x, y);
	    
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(imgSplash, 1, 1, null);
      }

    private void newCampaign() {
    	loadCampaign(null);
    }
    
    private void loadCampaign(File f) {
    	DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(app, frame, f);   	
    	dataLoadingDialog.setVisible(true);
    }
    
    private File selectLoadCampaignFile() {
        return FileDialogs.openCampaign(frame).orElse(null);
	}
    
    private javax.swing.JButton btnNewGame;
    private javax.swing.JButton btnLoadGame;
    private javax.swing.JButton btnLastSave;
    private javax.swing.JButton btnQuit;
}
