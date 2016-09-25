package mekhq;

import java.awt.Graphics2D;
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
    
    // Static defines for layered force icons
    public static String FORCE_FRAME                = "Pieces/Frames/"; //$NON-NLS-1$
    public static String FORCE_TYPE                 = "Pieces/Type/"; //$NON-NLS-1$
    public static String FORCE_FORMATIONS           = "Pieces/Formations/"; //$NON-NLS-1$
    public static String FORCE_ADJUSTMENTS          = "Pieces/Adjustments/"; //$NON-NLS-1$
    public static String FORCE_ALPHANUMERICS        = "Pieces/Alphanumerics/"; //$NON-NLS-1$
    public static String FORCE_SPECIAL_MODIFIERS    = "Pieces/Special Modifiers/"; //$NON-NLS-1$
    
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
    
    public static Image buildForceIcon(String category, String filename, DirectoryItems items, LinkedHashMap<String, Vector<String>> iconMap) {
        Image retVal = null;
        
        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no force icon file.
        if ((null == category) || (null == filename) || (Crew.PORTRAIT_NONE.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
            filename = "empty.png";
        }

        // Layered force icon
        if (Force.ROOT_LAYERED.equals(category)) {
            GraphicsConfiguration config = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            BufferedImage base = null;
            Graphics2D g2d = null;
            BufferedImage tmp = null;
            try {
                // Draw the frame, as needed
                if (iconMap.containsKey(IconPackage.FORCE_FRAME)) {
                    for (String value : iconMap.get(IconPackage.FORCE_FRAME)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_FRAME, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                    }
                }
                
                // Draw the selected type
                if (iconMap.containsKey(IconPackage.FORCE_TYPE)) {
                    for (String value : iconMap.get(IconPackage.FORCE_TYPE)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_TYPE, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                    }
                }
                
                // Draw the selected formation
                if (iconMap.containsKey(IconPackage.FORCE_FORMATIONS)) {
                    for (String value : iconMap.get(IconPackage.FORCE_FORMATIONS)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_FORMATIONS, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                    }
                }
                
                // Draw any selected adjustments
                if (iconMap.containsKey(IconPackage.FORCE_ADJUSTMENTS)) {
                    for (String value : iconMap.get(IconPackage.FORCE_ADJUSTMENTS)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_ADJUSTMENTS, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                    }
                }
                
                // Draw any selected alphanumerics
                if (iconMap.containsKey(IconPackage.FORCE_ALPHANUMERICS)) {
                    for (String value : iconMap.get(IconPackage.FORCE_ALPHANUMERICS)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_ALPHANUMERICS, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
                    }
                }
                
                // Draw the selected special modifiers
                if (iconMap.containsKey(IconPackage.FORCE_SPECIAL_MODIFIERS)) {
                    for (String value : iconMap.get(IconPackage.FORCE_SPECIAL_MODIFIERS)) {
                     // Load up the image piece
                        tmp = (BufferedImage) items.getItem(IconPackage.FORCE_SPECIAL_MODIFIERS, value);
                        
                        // Create the new base if it isn't already
                        if (null == base) {
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                        }
                        
                        // Resize the base if this image is larger than the current base image
                        if (tmp.getWidth() > base.getWidth() || tmp.getHeight() > base.getHeight()) {
                            BufferedImage oldBase = base;
                            base = config.createCompatibleImage(tmp.getWidth(), tmp.getHeight(), Transparency.TRANSLUCENT);

                            // Get our Graphics to draw on
                            g2d = base.createGraphics();
                            
                            // Draw the old base onto the new base, aligning bottom right
                            g2d.drawImage(base, base.getWidth() - oldBase.getWidth(), base.getHeight() - oldBase.getHeight(), null);
                        }

                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(tmp, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
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
                retVal = base;
            }
        } else { // Standard force icon
            // Try to get the player's force icon file.
            Image scaledImage = null;
            try {
                scaledImage = (Image) items.getItem(category, filename);
                if(null == scaledImage) {
                    scaledImage = (Image) items.getItem("", "empty.png");
                    if(null != scaledImage) {
                        scaledImage = scaledImage.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                    }
                }
                retVal = scaledImage;
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        
        return retVal;
    }
}