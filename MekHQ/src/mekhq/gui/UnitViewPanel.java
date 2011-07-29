/*
 * UnitViewPanel
 *
 * Created on April 28, 2011, 11:32 PM
 */

package mekhq.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.MechView;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;
/**
 * A custom panel that gets filled in with goodies from a unit record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class UnitViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Unit unit;
	private Entity entity;
	private Campaign campaign;
	
	private MechTileset mt;
    private DirectoryItems camos;
	
	private javax.swing.JLabel lblImage;
	//private javax.swing.JPanel pnlStats;
	private javax.swing.JTextPane txtReadout;
	private javax.swing.JTextPane txtFluff;	
	private javax.swing.JPanel pnlStats;
	
	private javax.swing.JLabel lblType;
	private javax.swing.JLabel lblTech;
	private javax.swing.JTextArea txtTech;
	private javax.swing.JLabel lblTonnage;
	private javax.swing.JTextArea txtTonnage;
	private javax.swing.JLabel lblBV;
	private javax.swing.JTextArea txtBV;
	private javax.swing.JLabel lblCost;
	private javax.swing.JTextArea txtCost;
	private javax.swing.JLabel lblQuirk;
	private javax.swing.JTextArea txtQuirk;
	
	public UnitViewPanel(Unit u, Campaign c, DirectoryItems camos, MechTileset mt) {
		unit = u;
		entity = u.getEntity();
		campaign = c;
		this.camos = camos;
		this.mt = mt;
		initComponents();
		//setMinimumSize(new Dimension(400, 200));
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		lblImage = new javax.swing.JLabel();
		txtReadout = new javax.swing.JTextPane();
		txtFluff = new javax.swing.JTextPane();
		pnlStats = new javax.swing.JPanel();
		
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitViewPanel");

		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);
		
		lblImage.setName("lblImage"); // NOI18N
		lblImage.setBackground(Color.WHITE);
		Image image = FluffImageHelper.getFluffImage(entity);
		if(null == image) {
			image = getImageFor(unit, lblImage);     
		}
        Icon icon = null;
		if(null != image) {
			if(image.getWidth(lblImage) > 200) {
                image = image.getScaledInstance(200, -1, Image.SCALE_DEFAULT);               
            }
            icon = new ImageIcon(image);
            lblImage.setIcon(icon);
        }
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		add(lblImage, gridBagConstraints);
	
		pnlStats.setName("pnlBasic");
		pnlStats.setBorder(BorderFactory.createTitledBorder(entity.getDisplayName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats(resourceMap);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		
		MechView mview = new MechView(entity, true);
		txtReadout.setName("txtReadout");
		txtReadout.setContentType(resourceMap.getString("txtReadout.contentType")); // NOI18N
		txtReadout.setEditable(false);
		txtReadout.setFont(Font.decode(resourceMap.getString("txtReadout.font"))); // NOI18N
		txtReadout.setText("<div style='font: 12pt monospaced'>" + mview.getMechReadoutBasic() + "<br>" + mview.getMechReadoutLoadout() + "</div>");
		txtReadout.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Technical Readout"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtReadout, gridBagConstraints);
		
		txtFluff.setName("txtFluff");
		txtFluff.setContentType(resourceMap.getString("txtFluff.contentType")); // NOI18N
		txtFluff.setEditable(false);
		txtFluff.setFont(Font.decode(resourceMap.getString("txtFluff.font"))); // NOI18N
		txtFluff.setText(mview.getMechReadoutFluff());
		txtFluff.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Unit History"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtFluff, gridBagConstraints);
	}
	
	private void fillStats(ResourceBundle resourceMap) {
		
		lblType = new javax.swing.JLabel();
    	lblTech = new javax.swing.JLabel();
		txtTech = new javax.swing.JTextArea();
		lblTonnage = new javax.swing.JLabel();
		txtTonnage = new javax.swing.JTextArea();
		lblBV = new javax.swing.JLabel();
		txtBV = new javax.swing.JTextArea();
		lblCost = new javax.swing.JLabel();
		txtCost = new javax.swing.JTextArea();
		lblQuirk = new javax.swing.JLabel();
		txtQuirk = new javax.swing.JTextArea();
		
		java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblType.setName("lblType"); // NOI18N
		lblType.setText("<html><i>" + UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(entity)) + "</i></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblType, gridBagConstraints);
		
		lblTech.setName("lblTech1"); // NOI18N
		lblTech.setText(resourceMap.getString("lblTech1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTech, gridBagConstraints);
		
		txtTech.setName("lblTech2"); // NOI18N
		txtTech.setText(TechConstants.getLevelDisplayableName(entity.getTechLevel()));
		txtTech.setEditable(false);
		txtTech.setLineWrap(true);
		txtTech.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTech, gridBagConstraints);

		lblTonnage.setName("lblTonnage1"); // NOI18N
		lblTonnage.setText(resourceMap.getString("lblTonnage1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTonnage, gridBagConstraints);
		
		txtTonnage.setName("lblTonnage2"); // NOI18N
		txtTonnage.setText(Float.toString(entity.getWeight()));
		txtTonnage.setEditable(false);
		txtTonnage.setLineWrap(true);
		txtTonnage.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTonnage, gridBagConstraints);

		lblBV.setName("lblBV1"); // NOI18N
		lblBV.setText(resourceMap.getString("lblBV1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBV, gridBagConstraints);
		
		txtBV.setName("lblBV2"); // NOI18N
		txtBV.setText(Integer.toString(entity.calculateBattleValue(true, true)));
		txtBV.setEditable(false);
		txtBV.setLineWrap(true);
		txtBV.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtBV, gridBagConstraints);

		
		double weight = 1.0;
		if(campaign.getCampaignOptions().useQuirks() && entity.countQuirks() > 0) {
			weight = 0.0;
		}
		
		lblCost.setName("lblCost1"); // NOI18N
		lblCost.setText(resourceMap.getString("lblCost1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCost, gridBagConstraints);
		
		txtCost.setName("lblCost2"); // NOI18N
		DecimalFormat format = new DecimalFormat();
		txtCost.setText(format.format(entity.getCost(false)));
		txtCost.setEditable(false);
		txtCost.setLineWrap(true);
		txtCost.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = weight;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtCost, gridBagConstraints);
		
		if(campaign.getCampaignOptions().useQuirks() && entity.countQuirks() > 0) {
			lblQuirk.setName("lblQuirk1"); // NOI18N
			lblQuirk.setText(resourceMap.getString("lblQuirk1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 5;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblQuirk, gridBagConstraints);
			
			txtQuirk.setName("lblQuirk2"); // NOI18N
			txtQuirk.setText(unit.getQuirksList());
			txtQuirk.setEditable(false);
			txtQuirk.setLineWrap(true);
			txtQuirk.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 5;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(txtQuirk, gridBagConstraints);
		}
		
	}
	
	private Image getImageFor(Unit u, Component c) {
        
		if(null == mt) { 
			return null;
		}
        Image base = mt.imageFor(u.getEntity(), c, -1);
        int tint = PlayerColors.getColorRGB(u.campaign.getColorIndex());
        EntityImage entityImage = new EntityImage(base, tint, getCamo(u.campaign), c);
        return entityImage.loadPreviewImage();
    }
    
    private Image getCamo(Campaign c) {

        // Return a null if the campaign has selected no camo file.
        if (null == c.getCamoCategory()
                || Player.NO_CAMO.equals(c.getCamoCategory())) {
            return null;
        }

        // Try to get the player's camo file.
        Image camo = null;
        try {

            // Translate the root camo directory name.
            String category = c.getCamoCategory();
            if (Player.ROOT_CAMO.equals(category))
                category = ""; //$NON-NLS-1$
            camo = (Image) camos.getItem(category, c.getCamoFileName());

        } catch (Exception err) {
            err.printStackTrace();
        }
        return camo;
    }
}