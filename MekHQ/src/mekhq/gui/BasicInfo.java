package mekhq.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Crew;
import mekhq.IconPackage;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * An extension of JPanel that is intended to be used for visual table renderers
 * allowing for a visual image and html coded text
 * @author Jay Lawson
 *
 */
public class BasicInfo extends JPanel {

        /**
         *
         */
        private static final long serialVersionUID = -7337823041775639463L;

        private JLabel lblImage;
        private JLabel lblLoad;
        IconPackage icons;
        

        public BasicInfo(IconPackage i) {
            this.icons = i;
            lblImage = new JLabel();
            lblLoad = new JLabel();

            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            setLayout(gridbag);

            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(1, 1, 1, 1);
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(lblLoad, c);
            add(lblLoad);

            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(1, 1, 1, 1);
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            gridbag.setConstraints(lblImage, c);
            add(lblImage);

            lblImage.setBorder(BorderFactory.createEmptyBorder());
        }

        public void setText(String s, String color) {
            lblImage.setText("<html><font size='2' color='" + color + "'>" + s
                    + "</font></html>");
        }
        

        public void highlightBorder() {
            lblImage.setBorder(new javax.swing.border.LineBorder(Color.BLACK, 5, true));
        }

        public void unhighlightBorder() {
            lblImage.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        }

        public void clearImage() {
            lblImage.setIcon(null);
        }

        public void setImage(Image img) {
            lblImage.setIcon(new ImageIcon(img));
        }

        public JLabel getLabel() {
            return lblImage;
        }

        public void setLoad(boolean load) {
            // if this is a loaded unit then do something with lblLoad to make
            // it show up
            // otherwise clear lblLoad
            if (load) {
                lblLoad.setText(" +");
            } else {
                lblLoad.setText("");
            }
        }
        
        protected Image getImageFor(Unit u) {
            
            if(null == icons.getMechTiles()) { 
                return null;
            }
            Image base = icons.getMechTiles().imageFor(u.getEntity(), this, -1);
            int tint = PlayerColors.getColorRGB(u.campaign.getColorIndex());
            EntityImage entityImage = new EntityImage(base, tint, getCamo(u), this);
            return entityImage.loadPreviewImage();
        }
        
        protected Image getCamo(Unit unit) {
            // Try to get the player's camo file.
            Image camo = null;
            try {
                camo = (Image) icons.getCamos().getItem(unit.getCamoCategory(), unit.getCamoFileName());
            } catch (Exception err) {
                err.printStackTrace();
            }
            return camo;
        }
        
        protected void setPortrait(Person p) {

            String category = p.getPortraitCategory();
            String filename = p.getPortraitFileName();

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == filename)) {
                return;
            }

            if (Crew.ROOT_PORTRAIT.equals(category)) {
                category = "";
            }

            if (Crew.PORTRAIT_NONE.equals(filename)) {
                filename = "default.gif";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
                portrait = (Image) icons.getPortraits().getItem(category, filename);
                if (null == portrait) {
                    // the image could not be found so switch to default one
                    p.setPortraitCategoryOverride(Crew.ROOT_PORTRAIT);
                    category = "";
                    p.setPortraitFileNameOverride(Crew.PORTRAIT_NONE);
                    filename = "default.gif";
                    portrait = (Image) icons.getPortraits().getItem(category, filename);
                }
                // make sure no images are longer than 72 pixels
                if (null != portrait) {
                    portrait = portrait.getScaledInstance(-1, 58,
                            Image.SCALE_SMOOTH);
                    setImage(portrait);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        
        protected Image getImageFor(Force force) {
            String category = force.getIconCategory();
            String filename = force.getIconFileName();
            LinkedHashMap<String, Vector<String>> iconMap = force.getIconMap();

            if(Crew.ROOT_PORTRAIT.equals(category)) {
             category = "";
            }

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == filename) || (Crew.PORTRAIT_NONE.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
             filename = "empty.png";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
             portrait = IconPackage.buildForceIcon(category, filename, icons.getForceIcons(), iconMap);
            if(null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
            } else {
                portrait = (Image) icons.getForceIcons().getItem("", "empty.png");
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                }
            }
            return portrait;
            } catch (Exception err) {
                err.printStackTrace();
                return null;
            }
       }
    }