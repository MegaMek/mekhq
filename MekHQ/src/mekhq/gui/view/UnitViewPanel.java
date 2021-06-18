/*
 * Copyright (c) 2011, 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import java.awt.Font;
import java.awt.Image;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.ImgLabel;
import mekhq.gui.utilities.MarkdownRenderer;

/**
 * A custom panel that gets filled in with goodies from a unit record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class UnitViewPanel extends JScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private Unit unit;
    private Entity entity;
    private Campaign campaign;

    private JLabel lblImage;
    private javax.swing.JTextPane txtReadout;
    private javax.swing.JTextPane txtFluff;
    private javax.swing.JPanel pnlStats;

    private javax.swing.JLabel lblType;
    private javax.swing.JLabel lblTech;
    private javax.swing.JLabel txtTech;
    private javax.swing.JLabel lblTonnage;
    private javax.swing.JLabel txtTonnage;
    private javax.swing.JLabel lblBV;
    private javax.swing.JLabel txtBV;
    private javax.swing.JLabel lblCost;
    private javax.swing.JLabel txtCost;
    private javax.swing.JLabel lblQuirk;
    private javax.swing.JLabel txtQuirk;

    public UnitViewPanel(Unit u, Campaign c) {
        super();
        unit = u;
        entity = u.getEntity();
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        txtReadout = new javax.swing.JTextPane();
        txtFluff = new javax.swing.JTextPane();
        pnlStats = new javax.swing.JPanel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitViewPanel", new EncodeControl());

        setLayout(new java.awt.GridBagLayout());

        int compWidth = 1;
        Image image = FluffImageHelper.getFluffImage(entity);
        if (null != image) {
            //fluff image exists so use custom ImgLabel to get full mech porn
            lblImage = new  ImgLabel(image);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridheight = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(lblImage, gridBagConstraints);
        } else {
            //no fluff image, so just use image icon from top-down view
            compWidth=2;
            lblImage = new JLabel();
            image = unit.getImage(lblImage);
            if (null != image) {
                ImageIcon icon = new ImageIcon(image);
                lblImage.setIcon(icon);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
                add(lblImage, gridBagConstraints);
            }
        }

        pnlStats.setName("pnlBasic");
        pnlStats.setBorder(BorderFactory.createTitledBorder(unit.getName()));
        fillStats(resourceMap);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);


        MechView mview = new MechView(entity, false, true);
        txtReadout.setName("txtReadout");
        txtReadout.setContentType(resourceMap.getString("txtReadout.contentType")); // NOI18N
        txtReadout.setEditable(false);
        txtReadout.setFont(Font.decode(resourceMap.getString("txtReadout.font"))); // NOI18N
        txtReadout.setText("<div style='font: 12pt monospaced'>" + mview.getMechReadoutBasic() + "<br>" + mview.getMechReadoutLoadout() + "</div>");
        txtReadout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Technical Readout"),
                BorderFactory.createEmptyBorder(0,2,2,2)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.gridwidth = compWidth;
        if (unit.getHistory().length() == 0) {
            gridBagConstraints.weighty = 1.0;
        }
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(txtReadout, gridBagConstraints);

        if (unit.getHistory().length() > 0) {
            txtFluff.setName("txtFluff");
            txtFluff.setEditable(false);
            txtFluff.setContentType("text/html");
            txtFluff.setText(MarkdownRenderer.getRenderedHtml(unit.getHistory()));
            txtFluff.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Unit History"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.gridwidth = compWidth;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(txtFluff, gridBagConstraints);
        }
    }

    private void fillStats(ResourceBundle resourceMap) {

        lblType = new javax.swing.JLabel();
        lblTech = new javax.swing.JLabel();
        txtTech = new javax.swing.JLabel();
        lblTonnage = new javax.swing.JLabel();
        txtTonnage = new javax.swing.JLabel();
        lblBV = new javax.swing.JLabel();
        txtBV = new javax.swing.JLabel();
        lblCost = new javax.swing.JLabel();
        txtCost = new javax.swing.JLabel();
        lblQuirk = new javax.swing.JLabel();
        txtQuirk = new javax.swing.JLabel();

        java.awt.GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new java.awt.GridBagLayout());

        lblType.setName("lblType"); // NOI18N
        lblType.setText("<html><i>" + UnitType.getTypeDisplayableName(entity.getUnitType()) + "</i></html>");
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

        lblTech.setName("lblTech1"); // NOI18N
        lblTech.setText(resourceMap.getString("lblTech1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTech, gridBagConstraints);

        txtTech.setName("lblTech2"); // NOI18N
        txtTech.setText(TechConstants.getLevelDisplayableName(entity.getTechLevel()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTech, gridBagConstraints);

        lblTonnage.setName("lblTonnage1"); // NOI18N
        lblTonnage.setText(resourceMap.getString("lblTonnage1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage, gridBagConstraints);

        txtTonnage.setName("lblTonnage2"); // NOI18N
        txtTonnage.setText(Double.toString(entity.getWeight()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTonnage, gridBagConstraints);

        lblBV.setName("lblBV1"); // NOI18N
        lblBV.setText(resourceMap.getString("lblBV1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV, gridBagConstraints);

        txtBV.setName("lblBV2"); // NOI18N
        txtBV.setText(Integer.toString(entity.calculateBattleValue(true, true)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBV, gridBagConstraints);


        double weight = 1.0;
        if (campaign.getCampaignOptions().useQuirks() && entity.countQuirks() > 0) {
            weight = 0.0;
        }

        lblCost.setName("lblCost1"); // NOI18N
        lblCost.setText(resourceMap.getString("lblCost1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCost, gridBagConstraints);

        txtCost.setName("lblCost2"); // NOI18N
        txtCost.setText(unit.getSellValue().toAmountAndSymbolString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = weight;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCost, gridBagConstraints);

        if (campaign.getCampaignOptions().useQuirks() && entity.countQuirks() > 0) {
            lblQuirk.setName("lblQuirk1"); // NOI18N
            lblQuirk.setText(resourceMap.getString("lblQuirk1.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 5;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblQuirk, gridBagConstraints);

            txtQuirk.setName("lblQuirk2"); // NOI18N
            txtQuirk.setText(unit.getQuirksList());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 5;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(txtQuirk, gridBagConstraints);
        }

    }
}
