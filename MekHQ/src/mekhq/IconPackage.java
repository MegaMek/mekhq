package mekhq;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.force.Force;
import mekhq.gui.utilities.PortraitFileFactory;

/**
 * This is a convenience class that will keep all the various directories and tilesets 
 * for tracking graphics and icons
 * @author Jay Lawson
 *
 */
public class IconPackage {
    //the various directory items we need to access
    private DirectoryItems portraits;
    private DirectoryItems camos;
    private DirectoryItems forceIcons;
    protected static MechTileset mt;
    
    /** A map of keys to various gui elements, for future skinning purposes */
    private final Map<String, String> guiElements = new HashMap<>();
    {
        // Skin defaults
        guiElements.put("infirmary_background", "data/images/misc/field_hospital.jpg");
    }
    
    public IconPackage() {

    }
    
    public void loadDirectories() {
        if(null == portraits) {
            try {
                portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        PortraitFileFactory.getInstance());
            } catch (Exception e) {
                portraits = null;
            }
        }
        if(null == camos) {
            try {
                camos = new DirectoryItems(new File("data/images/camo"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        ImageFileFactory.getInstance());
            } catch (Exception e) {
                camos = null;
            }
        }
        if(null == forceIcons) {
            try {
                forceIcons = new DirectoryItems(new File("data/images/force"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        PortraitFileFactory.getInstance());
            } catch (Exception e) {
                forceIcons = null;
            }
        }
        if(null == mt) {
            mt = new MechTileset(Configuration.unitImagesDir());
            try {
                mt.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                MekHQ.logError(ex);
                //TODO: do something here
            }
        }
    }
    
    public DirectoryItems getPortraits() {
        return portraits;
    }
    
    public DirectoryItems getCamos() {
        return camos;
    }
    
    public DirectoryItems getForceIcons() {
        return forceIcons;
    }
    
    public MechTileset getMechTiles() {
        return mt;
    }
    
    public String getGuiElement(String key) {
        return guiElements.get(key);
    }
    
    public static ImageIcon buildLayeredIcon(String category, String filename, DirectoryItems items, LinkedHashMap<String, Vector<String>> iconMap) {
        ImageIcon retVal = null;
        
        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no force icon file.
        if ((null == category) || (null == filename) || (Crew.PORTRAIT_NONE.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
            filename = "empty.png";
        }

        // Layered force icon
        if (Force.ROOT_LAYERED.equals(category)) {
            BufferedImage base = null;
            Graphics g2d = null;
            GraphicsConfiguration config = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            try {
                for (Map.Entry<String,  Vector<String>> entry : iconMap.entrySet()) {
                    if (null != entry.getValue() && !entry.getValue().isEmpty()) {
                        for (String value : entry.getValue()) {
                            // Load up the image piece
                            BufferedImage tmp = (BufferedImage) items.getItem(entry.getKey(), value);
                            

                            // Create the new base if it isn't already
                            if (null == base) {
                                base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                                // Get our Graphics to draw on
                                g2d = base.getGraphics();
                            }
                            
                            // Resize the base if this image is larger than the current base image
                            if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                                BufferedImage oldBase = base;
                                base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                                // Get our Graphics to draw on
                                g2d = base.getGraphics();
                                
                                // Draw the old base onto the new base, aligning bottom right
                                g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                            }

                            // Draw the current buffered image onto the base, aligning bottom and right side
                            g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                        }
                    }
                }
            } catch (Exception err) {
                err.printStackTrace();
            } finally {
                if (null != g2d)
                    g2d.dispose();
                if (null == base) {
                    try {
                        base = (BufferedImage) items.getItem("", "empty.png");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Image finalImage = null;
                if(null != base) {
                    finalImage = base.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                }
                retVal = new ImageIcon(finalImage);
            }
        } else { // Standard force icon
            // Try to get the player's force icon file.
            Image scaledImage = null;
            try {
                scaledImage = (Image) items.getItem(category, filename);
                if(null != scaledImage) {
                    scaledImage = scaledImage.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                } else {
                    scaledImage = (Image) items.getItem("", "empty.png");
                    if(null != scaledImage) {
                        scaledImage = scaledImage.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                    }
                }
                retVal = new ImageIcon(scaledImage);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        
        return retVal;
    }
}