/*
 * UnitViewPanel
 *
 * Created on April 28, 2011, 11:32 PM
 */

package mekhq;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.MechView;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.ImageFileFactory;
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
	
	protected static MechTileset mt;
    private DirectoryItems camos;
	
	private javax.swing.JLabel lblImage;
	//private javax.swing.JPanel pnlStats;
	private javax.swing.JTextPane txtReadout;
	private javax.swing.JTextPane txtFluff;	
	private javax.swing.JPanel pnlStats;
	
	private javax.swing.JLabel lblType;
	private javax.swing.JLabel lblTech1;
	private javax.swing.JLabel lblTech2;
	private javax.swing.JLabel lblTonnage1;
	private javax.swing.JLabel lblTonnage2;
	private javax.swing.JLabel lblBV1;
	private javax.swing.JLabel lblBV2;
	private javax.swing.JLabel lblCost1;
	private javax.swing.JLabel lblCost2;
	private javax.swing.JLabel lblQuirk1;
	private javax.swing.JLabel lblQuirk2;
	
	public UnitViewPanel(Unit u, Campaign c, DirectoryItems camos, MechTileset mt) {
		unit = u;
		entity = u.getEntity();
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
		
    	org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(UnitViewPanel.class);

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
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		
		MechView mview = new MechView(entity, true);
		txtReadout.setName("txtReadout");
		txtReadout.setContentType(resourceMap.getString("txtReadout.contentType")); // NOI18N
		txtReadout.setEditable(false);
		txtReadout.setFont(resourceMap.getFont("txtReadout.font")); // NOI18N
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
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtReadout, gridBagConstraints);
		
		txtFluff.setName("txtFluff");
		txtFluff.setContentType(resourceMap.getString("txtFluff.contentType")); // NOI18N
		txtFluff.setEditable(false);
		txtFluff.setFont(resourceMap.getFont("txtFluff.font")); // NOI18N
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
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtFluff, gridBagConstraints);
	}
	
	private void fillStats(org.jdesktop.application.ResourceMap resourceMap) {
		
		lblType = new javax.swing.JLabel();
    	lblTech1 = new javax.swing.JLabel();
		lblTech2 = new javax.swing.JLabel();
		lblTonnage1 = new javax.swing.JLabel();
		lblTonnage2 = new javax.swing.JLabel();
		lblBV1 = new javax.swing.JLabel();
		lblBV2 = new javax.swing.JLabel();
		lblCost1 = new javax.swing.JLabel();
		lblCost2 = new javax.swing.JLabel();
		lblQuirk1 = new javax.swing.JLabel();
		lblQuirk2 = new javax.swing.JLabel();
		
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
		
		lblTech1.setName("lblTech1"); // NOI18N
		lblTech1.setText(resourceMap.getString("lblTech1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTech1, gridBagConstraints);
		
		lblTech2.setName("lblTech2"); // NOI18N
		lblTech2.setText(TechConstants.getLevelDisplayableName(entity.getTechLevel()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTech2, gridBagConstraints);

		lblTonnage1.setName("lblTonnage1"); // NOI18N
		lblTonnage1.setText(resourceMap.getString("lblTonnage1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTonnage1, gridBagConstraints);
		
		lblTonnage2.setName("lblTonnage2"); // NOI18N
		lblTonnage2.setText(Float.toString(entity.getWeight()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTonnage2, gridBagConstraints);

		lblBV1.setName("lblBV1"); // NOI18N
		lblBV1.setText(resourceMap.getString("lblBV1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBV1, gridBagConstraints);
		
		lblBV2.setName("lblBV2"); // NOI18N
		lblBV2.setText(Integer.toString(entity.calculateBattleValue(true, true)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBV2, gridBagConstraints);

		lblCost1.setName("lblCost1"); // NOI18N
		lblCost1.setText(resourceMap.getString("lblCost1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCost1, gridBagConstraints);
		
		lblCost2.setName("lblCost2"); // NOI18N
		DecimalFormat format = new DecimalFormat();
		lblCost2.setText(format.format(entity.getCost(false)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCost2, gridBagConstraints);
		
		if(entity.countQuirks() > 0) {
			lblQuirk1.setName("lblQuirk1"); // NOI18N
			lblQuirk1.setText(resourceMap.getString("lblQuirk1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 5;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblQuirk1, gridBagConstraints);
			
			lblQuirk2.setName("lblQuirk2"); // NOI18N
			lblQuirk2.setText(unit.getQuirksList());
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 5;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblQuirk2, gridBagConstraints);
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
    
    /**
     * A class to handle the image permutations for an entity (borrowed from MegaMek#TileSetManager
     */
    private class EntityImage {
        private Image base;
        private Image wreck;
        private Image icon;
        int tint;
        private Image camo;
        private Component parent;

        private static final int IMG_WIDTH = 84;
        private static final int IMG_HEIGHT = 72;
        private static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

        public EntityImage(Image base, int tint, Image camo, Component comp) {
            this(base, null, tint, camo, comp);
        }

        public EntityImage(Image base, Image wreck, int tint, Image camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.parent = comp;
            this.wreck = wreck;
        }

        public Image loadPreviewImage() {
            base = applyColor(base);
            return base;
        }

        public Image getBase() {
            return base;
        }

        public Image getIcon() {
            return icon;
        }

        private Image applyColor(Image image) {
            Image iMech;
            boolean useCamo = (camo != null);

            iMech = image;

            int[] pMech = new int[IMG_SIZE];
            int[] pCamo = new int[IMG_SIZE];
            PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage()); //$NON-NLS-1$
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                return image;
            }

            if (useCamo) {
                PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH,
                        IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage()); //$NON-NLS-1$
                    return image;
                }
                if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                    return image;
                }
            }

            for (int i = 0; i < IMG_SIZE; i++) {
                int pixel = pMech[i];
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    int pixel1 = useCamo ? pCamo[i] : tint;
                    float red1 = ((float) ((pixel1 >> 16) & 0xff)) / 255;
                    float green1 = ((float) ((pixel1 >> 8) & 0xff)) / 255;
                    float blue1 = ((float) ((pixel1) & 0xff)) / 255;

                    float black = ((pMech[i]) & 0xff);

                    int red2 = Math.round(red1 * black);
                    int green2 = Math.round(green1 * black);
                    int blue2 = Math.round(blue1 * black);

                    pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8)
                            | blue2;
                }
            }

            image = parent.createImage(new MemoryImageSource(IMG_WIDTH,
                    IMG_HEIGHT, pMech, 0, IMG_WIDTH));
            return image;
        }
    }	
}