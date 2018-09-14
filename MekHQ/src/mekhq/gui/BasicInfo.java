package mekhq.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.IconPackage;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.util.Images;

/**
 * An extension of JPanel that is intended to be used for visual table renderers
 * allowing for a visual image and html coded text
 * @author Jay Lawson
 */
public class BasicInfo extends JPanel {

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
        lblImage.setText("<html><font size='2' color='" + color + "'>" + s + "</font></html>");
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
        // it show up otherwise clear lblLoad
        if (load) {
            lblLoad.setText(" +");
        } else {
            lblLoad.setText("");
        }
    }

    protected Image getImageFor(Unit u) {
        return Images.unit(icons, u, 84, 72);
    }

    protected void setPortrait(Person p) {
        setImage(Images.portrait(icons, p, 58, 58));
    }

    protected Image getImageFor(Force force) {
        return Images.force(icons, force, 58, 58);
    }

}
