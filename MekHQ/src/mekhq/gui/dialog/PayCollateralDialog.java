/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * A dialog to decide how you want to pay off collateral when you
 * default on a loan
 *
 * @author Taharqa
 */
public class PayCollateralDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(PayCollateralDialog.class);

    private JFrame frame;
    private Campaign campaign;
    private boolean cancelled;
    private boolean paid;
    private Loan loan;

    private Map<JCheckBox, UUID> unitBoxes;
    private ArrayList<JCheckBox> assetBoxes;
    private Map<JSlider, Integer> partSliders;
    private JProgressBar barAmount;
    private JButton btnPay;
    private JButton btnDontPay;
    private JButton btnCancel;

    public PayCollateralDialog(final JFrame frame, final boolean modal, final Campaign campaign,
            final Loan loan) {
        super(frame, modal);
        this.frame = frame;
        this.campaign = campaign;
        this.loan = loan;
        cancelled = false;
        paid = false;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PayCollateralDialog",
                MekHQ.getMHQOptions().getLocale());

        JTabbedPane panMain = new JTabbedPane();
        JPanel panInfo = new JPanel(new GridLayout(1, 0));
        JPanel panBtn = new JPanel(new GridLayout(0, 3));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());

        barAmount = new JProgressBar(0, 100);
        barAmount.setValue(0);
        barAmount.setStringPainted(true);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panInfo.add(barAmount, gridBagConstraints);

        unitBoxes = new LinkedHashMap<>();
        JCheckBox box;
        int i = 0;
        int j = 0;
        JPanel pnlUnits = new JPanel(new GridBagLayout());
        Collection<Unit> units = campaign.getHangar().getUnits();
        for (Unit u : units) {
            j++;
            box = new JCheckBox(u.getName() + " (" + u.getSellValue().toAmountAndSymbolString() + ")");
            box.setSelected(false);
            box.setEnabled(u.isPresent() && !u.isDeployed());
            box.addItemListener(evt -> updateAmount());
            unitBoxes.put(box, u.getId());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (j == units.size()) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlUnits.add(box, gridBagConstraints);
            i++;
        }
        JScrollPane scrUnits = new JScrollPaneWithSpeed();
        scrUnits.setViewportView(pnlUnits);
        scrUnits.setMinimumSize(new Dimension(400, 300));
        scrUnits.setPreferredSize(new Dimension(400, 300));

        partSliders = new LinkedHashMap<>();
        JPanel pnlParts = new JPanel(new GridBagLayout());
        i = 0;
        j = 0;
        JSlider partSlider;
        List<Part> spareParts = campaign.getWarehouse().getSpareParts();
        for (Part p : spareParts) {
            j++;
            int quantity = p.getQuantity();
            if (p instanceof AmmoStorage) {
                quantity = ((AmmoStorage) p).getQuantity();
            }
            partSlider = new JSlider(JSlider.HORIZONTAL, 0, quantity, 0);
            // TODO: deal with armors
            partSlider.setMajorTickSpacing(1);
            if (quantity < 11) {
                partSlider.setPaintLabels(true);
            }
            partSlider.setPaintTicks(true);
            partSlider.setSnapToTicks(true);
            partSlider.addChangeListener(e -> updateAmount());
            partSlider.setEnabled(p.isPresent() && !p.isReservedForRefit() && !p.isReservedForReplacement());
            partSliders.put(partSlider, p.getId());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (j == spareParts.size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlParts.add(partSlider, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            pnlParts.add(new JLabel("<html>" + p.getName() + "<br>" + p.getDetails() + ", "
                    + p.getActualValue().toAmountAndSymbolString() + "</html>"), gridBagConstraints);
            i++;
        }
        JScrollPane scrParts = new JScrollPaneWithSpeed();
        scrParts.setViewportView(pnlParts);
        scrParts.setMinimumSize(new Dimension(400, 300));
        scrParts.setPreferredSize(new Dimension(400, 300));

        // TODO: use cash reserves

        btnPay = new JButton(resourceMap.getString("btnPay.text"));
        btnPay.addActionListener(evt -> payCollateral());
        btnPay.setEnabled(false);
        panBtn.add(btnPay);

        btnDontPay = new JButton(resourceMap.getString("btnDontPay.text"));
        btnDontPay.addActionListener(evt -> dontPayCollateral());
        panBtn.add(btnDontPay);

        btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(evt -> {
            cancelled = true;
            setVisible(false);
        });
        panBtn.add(btnCancel);

        assetBoxes = new ArrayList<>();
        i = 0;
        j = 0;
        JPanel pnlAssets = new JPanel(new GridBagLayout());
        for (Asset a : campaign.getFinances().getAssets()) {
            j++;
            box = new JCheckBox(a.getName() + " (" + a.getValue().toAmountAndSymbolString() + ")");
            box.setSelected(false);
            box.addItemListener(evt -> updateAmount());
            assetBoxes.add(box);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (j == (campaign.getFinances().getAssets().size())) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlAssets.add(box, gridBagConstraints);
            i++;
        }
        JScrollPane scrAssets = new JScrollPaneWithSpeed(pnlAssets);
        scrAssets.setMinimumSize(new Dimension(400, 300));
        scrAssets.setPreferredSize(new Dimension(400, 300));

        updateAmount();

        panMain.add("Units", scrUnits);
        panMain.add("Parts", scrParts);
        panMain.add("Assets", scrAssets);
        getContentPane().add(panInfo, BorderLayout.PAGE_START);
        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(PayCollateralDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public boolean wasPaid() {
        return paid;
    }

    public void payCollateral() {
        // TODO: summary and are you sure dialog
        paid = true;
        setVisible(false);
    }

    public void dontPayCollateral() {
        // TODO: are you sure dialog
        paid = false;
        setVisible(false);
    }

    private void updateAmount() {
        Money amount = Money.zero();
        for (Map.Entry<JCheckBox, UUID> m : unitBoxes.entrySet()) {
            if (m.getKey().isSelected()) {
                amount = amount.plus(campaign.getUnit(m.getValue()).getSellValue());
            }
        }

        for (Map.Entry<JSlider, Integer> m : partSliders.entrySet()) {
            int quantity = m.getKey().getValue();
            if (quantity > 0) {
                amount = amount
                        .plus(campaign.getWarehouse().getPart(m.getValue()).getActualValue().multipliedBy(quantity));
            }
        }

        for (int i = 0; i < assetBoxes.size(); i++) {
            JCheckBox box = assetBoxes.get(i);
            if (box.isSelected()) {
                amount = amount.plus(campaign.getFinances().getAssets().get(i).getValue());
            }
        }

        int percent = 0;
        if (loan.determineCollateralAmount().isPositive()) {
            percent = amount
                    .multipliedBy(100)
                    .dividedBy(loan.determineCollateralAmount())
                    .getAmount().intValue();
        }

        if (percent < 100) {
            btnPay.setEnabled(false);
        } else {
            btnPay.setEnabled(true);
        }
        barAmount.setValue(percent);
        barAmount.setString(amount.toAmountString() + "/" + loan.determineCollateralAmount().toAmountString());
    }

    public ArrayList<UUID> getUnits() {
        ArrayList<UUID> uid = new ArrayList<>();
        for (Map.Entry<JCheckBox, UUID> u : unitBoxes.entrySet()) {
            if (u.getKey().isSelected()) {
                uid.add(u.getValue());
            }
        }
        return uid;
    }

    public ArrayList<int[]> getParts() {
        ArrayList<int[]> parts = new ArrayList<>();
        for (Map.Entry<JSlider, Integer> m : partSliders.entrySet()) {
            int quantity = m.getKey().getValue();
            if (quantity > 0) {
                int[] array = { m.getValue(), quantity };
                parts.add(array);
            }
        }
        return parts;
    }

    public ArrayList<Asset> getRemainingAssets() {
        ArrayList<Asset> newAssets = new ArrayList<>();
        for (int i = 0; i < assetBoxes.size(); i++) {
            JCheckBox box = assetBoxes.get(i);
            if (!box.isSelected()) {
                newAssets.add(campaign.getFinances().getAssets().get(i));
            }
        }
        return newAssets;
    }

}
