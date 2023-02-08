package mekhq.gui;

import mekhq.IconPackage;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * A specialized JPanel wrapper for repair tasks. This is different from
 * BasicInfo due to the need for an extra image on the right side.
 *
 * @author Cord Awtry (kipstafoo)
 */
public class RepairTaskInfo extends JPanel {
    private JLabel lblImage;
    private JLabel lblSecondaryImage;

    public RepairTaskInfo(IconPackage i) {
        lblImage = new JLabel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(lblImage, c);
        add(lblImage);

        lblSecondaryImage = new JLabel();
        lblSecondaryImage.setText("");

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 5);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(lblSecondaryImage, c);
        add(lblSecondaryImage);

        this.setBorder(BorderFactory.createEmptyBorder());
    }

    public void setText(String s) {
        lblImage.setText("<html><font size='2'>" + s + "</font></html>");
    }

    public void highlightBorder() {
        this.setBorder(new LineBorder(
            UIManager.getColor("Tree.selectionBorderColor"), 4, true));
    }

    public void unhighlightBorder() {
        this.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    }

    public void setImage(Image img) {
        if (null == img) {
            lblImage.setIcon(null);
        } else {
            lblImage.setIcon(new ImageIcon(img));
        }
    }

    public void setSecondaryImage(Image img) {
        if (null == img) {
            lblSecondaryImage.setIcon(null);
        } else {
            lblSecondaryImage.setIcon(new ImageIcon(img));
        }
    }
}
