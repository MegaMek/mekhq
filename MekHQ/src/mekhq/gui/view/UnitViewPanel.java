/*
 * Copyright (c) 2011-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.common.Entity;
import megamek.common.MekView;
import megamek.common.TechConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.ImgLabel;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * A custom panel that gets filled in with goodies from a unit record
 * @author  Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitViewPanel extends JScrollablePanel {
    private Unit unit;
    private Entity entity;
    private Campaign campaign;

    private JLabel lblImage;
    private JTextPane txtReadout;
    private JTextPane txtFluff;
    private JPanel pnlStats;
    private JLabel lblType;
    private JLabel lblTech;
    private JLabel txtTech;
    private JLabel lblTonnage;
    private JLabel txtTonnage;
    private JLabel lblBV;
    private JLabel txtBV;
    private JLabel lblCost;
    private JLabel txtCost;
    private JLabel lblQuirk;
    private JLabel txtQuirk;

    public UnitViewPanel(Unit u, Campaign c) {
        super();
        unit = u;
        entity = u.getEntity();
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        txtReadout = new JTextPane();
        txtFluff = new JTextPane();
        pnlStats = new JPanel();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitViewPanel",
                MekHQ.getMHQOptions().getLocale());

        setLayout(new GridBagLayout());

        int compWidth = 1;
        Image image = FluffImageHelper.getFluffImage(entity);
        if (null != image) {
            // fluff image exists so use custom ImgLabel to get full mek porn
            lblImage = new  ImgLabel(image);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridheight = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(lblImage, gridBagConstraints);
        } else {
            // no fluff image, so just use image icon from top-down view
            compWidth = 2;
            lblImage = new JLabel();
            image = unit.getImage(lblImage);
            if (null != image) {
                ImageIcon icon = new ImageIcon(image);
                lblImage.setIcon(icon);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.fill = GridBagConstraints.BOTH;
                gridBagConstraints.anchor = GridBagConstraints.CENTER;
                add(lblImage, gridBagConstraints);
            }
        }

        pnlStats.setName("pnlBasic");
        pnlStats.setBorder(BorderFactory.createTitledBorder(unit.getName()));
        fillStats(resourceMap);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        MekView mview = new MekView(entity, false, true);
        txtReadout.setName("txtReadout");
        txtReadout.setContentType(resourceMap.getString("txtReadout.contentType"));
        txtReadout.setEditable(false);
        txtReadout.setFont(Font.decode(resourceMap.getString("txtReadout.font")));
        txtReadout.setText("<div style='font: 12pt monospaced'>" + mview.getMekReadoutBasic() + "<br>" + mview.getMekReadoutLoadout() + "</div>");
        txtReadout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Technical Readout"),
                BorderFactory.createEmptyBorder(0,2,2,2)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.gridwidth = compWidth;
        if (unit.getHistory().isBlank()) {
            gridBagConstraints.weighty = 1.0;
        }
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(txtReadout, gridBagConstraints);

        if (!unit.getHistory().isBlank()) {
            txtFluff.setName("txtFluff");
            txtFluff.setEditable(false);
            txtFluff.setContentType("text/html");
            txtFluff.setText(MarkdownRenderer.getRenderedHtml(unit.getHistory()));
            txtFluff.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Unit History"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.gridwidth = compWidth;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtFluff, gridBagConstraints);
        }
    }

    private void fillStats(ResourceBundle resourceMap) {
        lblType = new JLabel();
        lblTech = new JLabel();
        txtTech = new JLabel();
        lblTonnage = new JLabel();
        txtTonnage = new JLabel();
        lblBV = new JLabel();
        txtBV = new JLabel();
        lblCost = new JLabel();
        txtCost = new JLabel();
        lblQuirk = new JLabel();
        txtQuirk = new JLabel();

        pnlStats.setLayout(new GridBagLayout());

        lblType.setName("lblType");
        lblType.setText("<html><i>" + unit.getTypeDisplayableNameWithOmni() + "</i></html>");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);

        lblTech.setName("lblTech1");
        lblTech.setText(resourceMap.getString("lblTech1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTech, gridBagConstraints);

        txtTech.setName("lblTech2");
        txtTech.setText(TechConstants.getLevelDisplayableName(entity.getTechLevel()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTech, gridBagConstraints);

        lblTonnage.setName("lblTonnage1");
        lblTonnage.setText(resourceMap.getString("lblTonnage1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage, gridBagConstraints);

        txtTonnage.setName("lblTonnage2");
        txtTonnage.setText(Double.toString(entity.getWeight()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTonnage, gridBagConstraints);

        lblBV.setName("lblBV1");
        lblBV.setText(resourceMap.getString("lblBV1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV, gridBagConstraints);

        txtBV.setName("lblBV2");
        txtBV.setText(Integer.toString(entity.calculateBattleValue(true, true)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBV, gridBagConstraints);

        double weight = 1.0;
        if (campaign.getCampaignOptions().isUseQuirks() && (entity.countQuirks() > 0)) {
            weight = 0.0;
        }

        lblCost.setName("lblCost1");
        lblCost.setText(resourceMap.getString("lblCost1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCost, gridBagConstraints);

        txtCost.setName("lblCost2");
        txtCost.setText(unit.getSellValue().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = weight;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCost, gridBagConstraints);

        if (campaign.getCampaignOptions().isUseQuirks() && (entity.countQuirks() > 0)) {
            lblQuirk.setName("lblQuirk1");
            lblQuirk.setText(resourceMap.getString("lblQuirk1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 5;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblQuirk, gridBagConstraints);

            txtQuirk.setName("lblQuirk2");
            txtQuirk.setText(unit.getQuirksList());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 5;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtQuirk, gridBagConstraints);
        }
    }
}
