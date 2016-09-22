package mekhq.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.client.ui.Messages;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import mekhq.IconPackage;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class ForceRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -553191867660269247L;

    private IconPackage icons;

    public ForceRenderer(IconPackage i) {
        icons = i;
    }

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        setOpaque(true);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
        if(sel) {
            setBackground(Color.DARK_GRAY);
            setForeground(Color.WHITE);
        }

        if(value instanceof Unit) {
            String name = "<font color='red'>No Crew</font>";
            if (((Unit) value).getEntity() instanceof GunEmplacement) {
                name = "AutoTurret";
            }
            String uname = "";
            String c3network = "";
            Unit u = (Unit)value;
            Person pp = u.getCommander();
            if(null != pp) {
                name = pp.getFullTitle();
                name += " (" + u.getEntity().getCrew().getGunnery() + "/" + u.getEntity().getCrew().getPiloting() + ")";
                if(pp.needsFixing() || u.getEntity().getCrew().getHits() > 0) {
                    name = "<font color='red'>" + name + "</font>";
                }
            }
            uname = "<i>" + u.getName() + "</i>";
            if(u.isDamaged()) {
                uname = "<font color='red'>" + uname + "</font>";
            }
            Entity entity = u.getEntity();
            if (entity.hasC3i()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.C3iNone");
                } else {
                    c3network += c3network += Messages
                            .getString("ChatLounge.C3iNetwork")
                            + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3Nodes",
                                new Object[] { entity.calculateFreeC3Nodes() });
                    }
                }
            } else if (entity.hasC3()) {
                if (entity.C3MasterIs(entity)) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3MNodes",
                            new Object[] { entity.calculateFreeC3MNodes() });
                    if(entity.hasC3MM()) {
                        c3network += Messages.getString("ChatLounge.C3SNodes",
                                new Object[] { entity.calculateFreeC3Nodes() });
                    }
                } else if (!entity.hasC3S()) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3SNodes",
                            new Object[] { entity.calculateFreeC3Nodes() });
                    // an independent master might also be a slave to a company
                    // master
                    if (entity.getC3Master() != null) {
                        c3network += "<br>" + Messages.getString("ChatLounge.C3Slave") + entity.getC3Master().getShortName(); //$NON-NLS-1$
                    }
                } else if (entity.getC3Master() != null) {
                    c3network += Messages.getString("ChatLounge.C3Slave") + entity.getC3Master().getShortName(); //$NON-NLS-1$
                } else {
                    c3network += Messages.getString("ChatLounge.C3None");
                }
            }
            if(!c3network.isEmpty()) {
                c3network = "<br><i>" + c3network + "</i>";
            }
            setText("<html>" + name + ", " + uname + c3network + "</html>");
            if(u.isDeployed() && !sel) {
                setBackground(Color.LIGHT_GRAY);
            }
        }
        if(value instanceof Force) {
            if(!hasFocus && ((Force)value).isDeployed()) {
                setBackground(Color.LIGHT_GRAY);
            }
        }
        setIcon(getIcon(value));



        return this;
    }

    private IconPackage getIconPackage() {
        return icons;
    }

    protected Icon getIcon(Object node) {

        if(node instanceof Unit) {
            return getIconFrom((Unit)node);
        } else if(node instanceof Force) {
            return getIconFrom((Force)node);
        } else {
            return null;
        }
    }

    protected Icon getIconFrom(Unit unit) {
        Person person = unit.getCommander();
        if(null == person) {
            return null;
        }
        String category = person.getPortraitCategory();
        String file = person.getPortraitFileName();

        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the unit has no selected portrait file.
        if ((null == category) || (null == file) || Crew.PORTRAIT_NONE.equals(file)) {
            file = "default.gif";
        }
        // Try to get the unit's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) getIconPackage().getPortraits().getItem(category, file);
            if(null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
            } else {
                portrait = (Image) getIconPackage().getPortraits().getItem("", "default.gif");
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                }
            }
            return new ImageIcon(portrait);
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    protected Icon getIconFrom(Force force) {
        String category = force.getIconCategory();
        String file = force.getIconFileName();
        ImageIcon forceIcon = null;

        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no force icon file.
        if ((null == category) || (null == file) || (Crew.PORTRAIT_NONE.equals(file) && !Force.ROOT_LAYERED.equals(category))) {
            file = "empty.png";
        }

        // Layered force icon
        if (Force.ROOT_LAYERED.equals(category)) {
            BufferedImage base = null;
            Graphics2D g2d = null;
            try {
                for (Map.Entry<String,  Vector<String>> entry : force.getIconMap().entrySet()) {
                    if (null != entry.getValue() && !entry.getValue().isEmpty()) {
                        for (String value : entry.getValue()) {
                            // Load up the image piece
                            BufferedImage tmp = (BufferedImage) getIconPackage().getForceIcons().getItem(entry.getKey(), value);

                            // Create the new base if it isn't already
                            if (null == base) {
                                base = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);

                                // Get our Graphics to draw on
                                g2d = base.createGraphics();
                            }

                            // Draw the current buffered image onto the base, aligning bottom and right side
                            g2d.drawImage(base, base.getWidth() - tmp.getWidth(), base.getHeight() - tmp.getHeight(), null);
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
                        base = (BufferedImage) getIconPackage().getForceIcons().getItem("", "empty.png");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(null != base) {
                    base = (BufferedImage) base.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                }
                forceIcon = new ImageIcon(base);
            }
        } else { // Standard force icon
            // Try to get the player's force icon file.
            Image portrait = null;
            try {
                portrait = (Image) getIconPackage().getForceIcons().getItem(category, file);
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                } else {
                    portrait = (Image) getIconPackage().getForceIcons().getItem("", "empty.png");
                    if(null != portrait) {
                        portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                    }
                }
                forceIcon = new ImageIcon(portrait);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }

        return forceIcon;
    }
}