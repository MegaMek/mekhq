package mekhq.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Crew;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Unit;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;

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
        DirectoryItems camos;
        DirectoryItems portraits;
        private DirectoryItems forceIcons;
        MechTileset mt;
        

        public BasicInfo(DirectoryItems camos, DirectoryItems portraits, DirectoryItems force, MechTileset mt) {
            this.camos = camos;
            this.portraits = portraits;
            this.forceIcons = force;
            this.mt = mt;
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
            
            if(null == mt) { 
                return null;
            }
            Image base = mt.imageFor(u.getEntity(), this, -1);
            int tint = PlayerColors.getColorRGB(u.campaign.getColorIndex());
            EntityImage entityImage = new EntityImage(base, tint, getCamo(u), this);
            return entityImage.loadPreviewImage();
        }
        
        protected Image getCamo(Unit unit) {
            // Try to get the player's camo file.
            Image camo = null;
            try {
                camo = (Image) camos.getItem(unit.getCamoCategory(), unit.getCamoFileName());
            } catch (Exception err) {
                err.printStackTrace();
            }
            return camo;
        }
        
        protected void setPortrait(Person p) {

            String category = p.getPortraitCategory();
            String file = p.getPortraitFileName();

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == file)) {
                return;
            }

            if (Crew.ROOT_PORTRAIT.equals(category)) {
                category = "";
            }

            if (Crew.PORTRAIT_NONE.equals(file)) {
                file = "default.gif";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
                portrait = (Image) portraits.getItem(category, file);
                if (null == portrait) {
                    // the image could not be found so switch to default one
                    p.setPortraitCategory(Crew.ROOT_PORTRAIT);
                    category = "";
                    p.setPortraitFileName(Crew.PORTRAIT_NONE);
                    file = "default.gif";
                    portrait = (Image) portraits.getItem(category, file);
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
            String file = force.getIconFileName();

            if(Crew.ROOT_PORTRAIT.equals(category)) {
             category = "";
            }

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == file) || Crew.PORTRAIT_NONE.equals(file)) {
             file = "empty.png";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
             portrait = (Image) forceIcons.getItem(category, file);
            if(null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
            } else {
                portrait = (Image) forceIcons.getItem("", "empty.png");
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