package mekhq.gui;

import java.awt.Component;
import java.awt.Image;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.client.ui.Messages;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import mekhq.IconPackage;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class ForceRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -553191867660269247L;

    private final IconPackage icons;
    private final MekHqColors colors = new MekHqColors();

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
        setBackground(UIManager.getColor("Tree.background"));
        setForeground(UIManager.getColor("Tree.textForeground"));
        if(sel) {
            setBackground(UIManager.getColor("Tree.selectionBackground"));
            setForeground(UIManager.getColor("Tree.selectionForeground"));
        }

        if(value instanceof Unit) {
            String name = "<font color='red'>No Crew</font>";
            if (((Unit) value).getEntity() instanceof GunEmplacement) {
                name = "AutoTurret";
            }
            String uname = "";
            String c3network = "";
            String transport = "";
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
            if (entity.hasNavalC3()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.NC3None");
                } else {
                    c3network += Messages
                            .getString("ChatLounge.NC3Network")
                            + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.NC3Nodes",
                                new Object[] { entity.calculateFreeC3Nodes() });
                    }
                }
            } else if (entity.hasC3i()) {
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
            if(u.hasTransportShipId()) {
                for (UUID id : u.getTransportShipId().keySet()) {
                    Unit ship = u.getCampaign().getUnit(id);
                    if (ship != null) {
                        transport += "<br>" + "Transported by: " + ship.getName();
                    }
                }
            }
            setText("<html>" + name + ", " + uname + c3network + transport + "</html>");
            if(u.isDeployed() && !sel) {
                colors.getDeployed().getColor().ifPresent(c -> setBackground(c));
                colors.getDeployed().getAlternateColor().ifPresent(c -> setForeground(c));
            }
        }
        if(value instanceof Force) {
            if(!hasFocus && ((Force)value).isDeployed()) {
                colors.getDeployed().getColor().ifPresent(c -> setBackground(c));
                colors.getDeployed().getAlternateColor().ifPresent(c -> setForeground(c));
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
        if (null == person) {
            return null;
        }
        String category = person.getPortraitCategory();
        String filename = person.getPortraitFileName();

        if (Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the unit has no selected portrait file.
        if ((null == category) || (null == filename) || Crew.PORTRAIT_NONE.equals(filename)) {
            filename = "default.gif";
        }
        // Try to get the unit's portrait file.
        Image portrait;
        try {
            portrait = (Image) MHQStaticDirectoryManager.getPortraits().getItem(category, filename);
            if (null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
            } else {
                portrait = (Image) MHQStaticDirectoryManager.getPortraits().getItem("", "default.gif");
                if (null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_DEFAULT);
                }
            }
            return new ImageIcon(portrait);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
    }

    protected Icon getIconFrom(Force force) {
        Image forceImage = MHQStaticDirectoryManager.buildForceIcon(force.getIconCategory(),
                force.getIconFileName(), force.getIconMap());
        if (null != forceImage) {
            forceImage = forceImage.getScaledInstance(58, -1, Image.SCALE_SMOOTH);
        }
        return new ImageIcon(forceImage);
    }
}
