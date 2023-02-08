/*
 * PayCollateralDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A dialog to decide how you want to pay off collateral when you
 * default on a loan
 * @author Taharqa
 */
public class PayCollateralDialog extends JDialog {
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
        java.awt.GridBagConstraints gridBagConstraints;

        JTabbedPane panMain = new JTabbedPane();
        JPanel panInfo = new JPanel(new GridLayout(1,0));
        JPanel panBtn = new JPanel(new GridLayout(0,3));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());

        barAmount = new JProgressBar(0, 100);
        barAmount.setValue(0);
        barAmount.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
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
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (j == units.size()) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlUnits.add(box, gridBagConstraints);
            i++;
        }
        JScrollPane scrUnits = new JScrollPane();
        scrUnits.setViewportView(pnlUnits);
        scrUnits.setMinimumSize(new java.awt.Dimension(400, 300));
        scrUnits.setPreferredSize(new java.awt.Dimension(400, 300));

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
            //TODO: deal with armors
            partSlider.setMajorTickSpacing(1);
            if (quantity < 11) {
                partSlider.setPaintLabels(true);
            }
            partSlider.setPaintTicks(true);
            partSlider.setSnapToTicks(true);
            partSlider.addChangeListener(e -> updateAmount());
            partSlider.setEnabled(p.isPresent() && !p.isReservedForRefit() && !p.isReservedForReplacement());
            partSliders.put(partSlider, p.getId());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (j == spareParts.size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlParts.add(partSlider, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            pnlParts.add(new JLabel("<html>" + p.getName() + "<br>" + p.getDetails()  + ", "
                    + p.getActualValue().toAmountAndSymbolString() + "</html>"), gridBagConstraints);
            i++;
        }
        JScrollPane scrParts = new JScrollPane();
        scrParts.setViewportView(pnlParts);
        scrParts.setMinimumSize(new java.awt.Dimension(400, 300));
        scrParts.setPreferredSize(new java.awt.Dimension(400, 300));

        //TODO: use cash reserves

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
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (j == (campaign.getFinances().getAssets().size())) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlAssets.add(box, gridBagConstraints);
            i++;
        }
        JScrollPane scrAssets = new JScrollPane(pnlAssets);
        scrAssets.setMinimumSize(new java.awt.Dimension(400, 300));
        scrAssets.setPreferredSize(new java.awt.Dimension(400, 300));

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
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public boolean wasPaid() {
        return paid;
    }

    public void payCollateral() {
        //TODO: summary and are you sure dialog
        paid = true;
        setVisible(false);
    }

    public void dontPayCollateral() {
        //TODO: are you sure dialog
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
                amount = amount.plus(campaign.getWarehouse().getPart(m.getValue()).getActualValue().multipliedBy(quantity));
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
                int[] array = {m.getValue(), quantity};
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
