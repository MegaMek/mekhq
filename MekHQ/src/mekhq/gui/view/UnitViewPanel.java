/*
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.view;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.TechConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.ImgLabel;
import mekhq.gui.utilities.MarkdownRenderer;

/**
 * A custom panel that gets filled in with goodies from a unit record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitViewPanel extends JScrollablePanel {
    private final Unit unit;
    private final Entity entity;
    private final Campaign campaign;

    private JPanel pnlStats;

    public UnitViewPanel(Unit u, Campaign c) {
        super();
        unit = u;
        entity = u.getEntity();
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        JTextPane txtReadout = new JTextPane();
        JTextPane txtFluff = new JTextPane();
        pnlStats = new JPanel();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitViewPanel",
              MekHQ.getMHQOptions().getLocale());

        setLayout(new GridBagLayout());

        boolean isSpritesOnly = PreferenceManager.getClientPreferences().getSpritesOnly();
        int compWidth = 1;
        Image image = isSpritesOnly ? null : FluffImageHelper.getFluffImage(entity);
        JLabel lblImage;
        if (null != image) {
            // fluff image exists so use custom ImgLabel to get full mek porn
            lblImage = new ImgLabel(image);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridheight = 3;
            gridBagConstraints.weightx = 0.5;
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
                icon = ImageUtilities.scaleImageIcon(icon, UIUtil.scaleForGUI(150), true);
                lblImage.setIcon(icon);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 1;
                gridBagConstraints.weightx = 0.0;
                gridBagConstraints.fill = GridBagConstraints.BOTH;
                gridBagConstraints.anchor = GridBagConstraints.CENTER;
                add(lblImage, gridBagConstraints);
            }
        }

        pnlStats.setName("pnlBasic");
        pnlStats.setBorder(RoundedLineBorder.createRoundedLineBorder(unit.getName()));
        fillStats(resourceMap);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        EntityReadout entityReadout = EntityReadout.createReadout(entity, false, true);
        txtReadout.setName("txtReadout");
        txtReadout.setContentType(resourceMap.getString("txtReadout.contentType"));
        txtReadout.setEditable(false);
        txtReadout.setFont(Font.decode(resourceMap.getString("txtReadout.font")));
        txtReadout.setText("<div style='font: 12pt monospaced'>" +
                                 entityReadout.getBasicSection(ViewFormatting.HTML) +
                                 "<br>" +
                                 entityReadout.getLoadoutSection(ViewFormatting.HTML) +
                                 "</div>");
        txtReadout.setBorder(RoundedLineBorder.createRoundedLineBorder("Technical Readout"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
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
            txtFluff.setBorder(RoundedLineBorder.createRoundedLineBorder("Unit History"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.gridwidth = compWidth;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtFluff, gridBagConstraints);
        }
    }

    private void fillStats(ResourceBundle resourceMap) {
        JLabel lblType = new JLabel();
        JLabel lblTech = new JLabel();
        JLabel txtTech = new JLabel();
        JLabel lblTonnage = new JLabel();
        JLabel txtTonnage = new JLabel();
        JLabel lblBV = new JLabel();
        JLabel txtBV = new JLabel();
        JLabel lblCost = new JLabel();
        JLabel txtCost = new JLabel();
        JLabel lblQuirk = new JLabel();
        JLabel txtQuirk = new JLabel();

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
