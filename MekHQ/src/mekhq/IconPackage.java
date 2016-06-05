package mekhq;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.util.DirectoryItems;
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
}