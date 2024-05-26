/*
 * Copyright (c) 2017-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.codeUtilities.StringUtility;
import mekhq.MekHQ;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.RepairTab;
import mekhq.service.PartsAcquisitionService;
import mekhq.service.PartsAcquisitionService.PartCountInfo;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kipsta
 */
public class AcquisitionsDialog extends JDialog {
    private CampaignGUI campaignGUI;
    private Map<String, AcquisitionPanel> partPanelMap = new HashMap<>();

    private JPanel pnlSummary;
    private JLabel lblSummary;
    private JButton btnSummary;

    private int numBonusParts = 0;

    public AcquisitionsDialog(JFrame parent, boolean modal, CampaignGUI campaignGUI) {
        super(parent, modal);
        this.campaignGUI = campaignGUI;

        calculateBonusParts();

        initComponents();

        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setTitle("Parts Acquisition");

        final Container content = getContentPane();
        content.setLayout(new BorderLayout());

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        pnlMain.add(createSummaryPanel(), gridBagConstraints);

        int idx = 1;

        for (String key : PartsAcquisitionService.getAcquisitionMap().keySet()) {
            gridBagConstraints.gridy++;

            List<IAcquisitionWork> awList = PartsAcquisitionService.getAcquisitionMap().get(key);
            AcquisitionPanel pnl = new AcquisitionPanel(awList, idx++);
            partPanelMap.put(key, pnl);

            pnlMain.add(pnl, gridBagConstraints);
        }

        pnlSummary.firePropertyChange("counts", -1, 0);

        JScrollPane scrollMain = new JScrollPane(pnlMain);
        scrollMain.setPreferredSize(new Dimension(700, 500));

        content.add(scrollMain, BorderLayout.CENTER);

        pack();
    }

    private JPanel createSummaryPanel() {
        pnlSummary = new JPanel();
        pnlSummary.setLayout(new GridBagLayout());
        pnlSummary.setBorder(BorderFactory.createTitledBorder("Acquisition Summary"));

        pnlSummary.addPropertyChangeListener("counts", evt -> {
            PartsAcquisitionService.buildPartsList(campaignGUI.getCampaign());

            lblSummary.setText(generateSummaryText());

            btnSummary.firePropertyChange("missingCount", -1, PartsAcquisitionService.getMissingCount());

            if (campaignGUI.getTab(MHQTabType.REPAIR_BAY) != null) {
                ((RepairTab) campaignGUI.getTab(MHQTabType.REPAIR_BAY)).refreshPartsAcquisitionService(false);
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 10, 0);

        lblSummary = new JLabel();
        lblSummary.setText(generateSummaryText());
        pnlSummary.add(lblSummary, gbc);

        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.gridx++;

        btnSummary = new JButton();
        btnSummary.setText("Order All");
        btnSummary.setToolTipText("Order all missing parts");
        btnSummary.setName("btnOrderEverything");
        btnSummary.addActionListener(ev -> {
            for (AcquisitionPanel pnl : partPanelMap.values()) {
                pnl.orderAllMissing();
            }
        });
        btnSummary.addPropertyChangeListener("missingCount", evt -> {
            boolean visible = false;

            if ((PartsAcquisitionService.getMissingCount() > 0)
                    && (PartsAcquisitionService.getMissingCount() > PartsAcquisitionService.getUnavailableCount())) {
                visible = true;
            }

            btnSummary.setVisible(visible);
        });

        pnlSummary.add(btnSummary, gbc);

        if ((PartsAcquisitionService.getMissingCount() == 0)
                || (PartsAcquisitionService.getMissingCount() == PartsAcquisitionService.getUnavailableCount())) {
            btnSummary.setVisible(false);
        }

        return pnlSummary;
    }

    private String generateSummaryText() {
        StringBuilder sbText = new StringBuilder();
        sbText.append("<html><font size='3'>");

        sbText.append("Required: ");
        sbText.append(PartsAcquisitionService.getRequiredCount());

        if (PartsAcquisitionService.getMissingCount() > 0) {
            sbText.append(", ");

            sbText.append("<font color='red'>");
            sbText.append("missing: ");
            sbText.append(PartsAcquisitionService.getMissingCount());

            if (PartsAcquisitionService.getUnavailableCount() > 0) {
                sbText.append(", unavailable: ");
                sbText.append(PartsAcquisitionService.getUnavailableCount());
            }

            sbText.append("</font>");
        }

        sbText.append("<br/>");

        String inventoryInfo = "Inventory: " + PartsAcquisitionService.getInTransitCount() + " in transit, "
                + PartsAcquisitionService.getOnOrderCount() + " on order";

        if (PartsAcquisitionService.getOmniPodCount() > 0) {
            inventoryInfo += ", " + PartsAcquisitionService.getOmniPodCount() + " OmniPod";
        }

        sbText.append(inventoryInfo);
        sbText.append("<br/>");

        if (PartsAcquisitionService.getMissingTotalPrice().isPositive()) {
            String price = "Missing item price: "
                    + PartsAcquisitionService.getMissingTotalPrice().toAmountAndSymbolString();

            sbText.append(price);
            sbText.append("<br/>");
        }

        sbText.append("</font></html>");

        return sbText.toString();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AcquisitionsDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    private void calculateBonusParts() {
        numBonusParts = campaignGUI.getCampaign().totalBonusParts();

        if (partPanelMap != null) {
            for (AcquisitionPanel pnl : partPanelMap.values()) {
                pnl.refresh();
            }
        }
    }

    public class AcquisitionPanel extends JPanel {
        private List<IAcquisitionWork> awList;
        private int idx;

        private IAcquisitionWork targetWork;
        private Part part;
        private PartCountInfo partCountInfo = new PartCountInfo();

        private JButton btnOrderAll;
        private JButton btnUseBonus;
        private JButton btnDepod;
        private JLabel lblText;

        public AcquisitionPanel(List<IAcquisitionWork> awList, int idx) {
            this.awList = awList;
            this.idx = idx;

            setLayout(new GridBagLayout());

            initComponents();
        }

        public void orderAllMissing() {
            if (null == partCountInfo) {
                return;
            }

            if (partCountInfo.getMissingCount() > 0) {
                campaignGUI.getCampaign().getShoppingList().addShoppingItem(part.getAcquisitionWork(),
                        partCountInfo.getMissingCount(), campaignGUI.getCampaign());

                refresh();
            }
        }

        private void useBonusPart() {
            if (targetWork instanceof AmmoBin) {
                targetWork = ((AmmoBin) targetWork).getAcquisitionWork();
            }

            campaignGUI.getCampaign().spendBonusPart(targetWork);

            refresh();

            calculateBonusParts();
        }

        private void refresh() {
            pnlSummary.firePropertyChange("counts", -1, 0);

            partCountInfo = PartsAcquisitionService.getPartCountInfoMap().get(targetWork.getAcquisitionDisplayName());

            if (partCountInfo == null) {
                this.setVisible(false);
            } else {
                lblText.setText(generateText());

                if (partCountInfo.getMissingCount() == 0) {
                    btnOrderAll.setVisible(false);
                } else {
                    if (partCountInfo.getMissingCount() == 0) {
                        btnOrderAll.setVisible(false);
                    } else {
                        btnOrderAll.setText(String.format("Order All (%s)", partCountInfo.getMissingCount()));
                        btnOrderAll.setVisible(true);
                    }
                }

                btnDepod.setVisible(partCountInfo.getOmniPodCount() != 0);

                btnUseBonus.setText(String.format("Use Bonus Part (%s)", numBonusParts));
                btnUseBonus.setVisible(numBonusParts > 0);
            }
        }

        private String generateText() {
            StringBuilder sbText = new StringBuilder();
            sbText.append("<html><font size='3'>");

            sbText.append("<b>");
            sbText.append(targetWork.getAcquisitionDisplayName());
            sbText.append("</b><br/>");

            sbText.append("Required: ");
            sbText.append(awList.size());

            if (partCountInfo != null) {
                if (partCountInfo.getMissingCount() > 0) {
                    sbText.append(", ");

                    sbText.append("<font color='red'>");
                    sbText.append("missing: ");
                    sbText.append(partCountInfo.getMissingCount());
                    sbText.append("</font>");
                }

                sbText.append("<br/>");

                String countModifier = partCountInfo.getCountModifier();

                if (!StringUtility.isNullOrBlank(countModifier)) {
                    countModifier = " " + countModifier;
                }

                String inventoryInfo = "Inventory: " + partCountInfo.getInTransitCount() + countModifier
                        + " in transit, " + partCountInfo.getOnOrderCount() + countModifier + " on order";

                if (partCountInfo.getOmniPodCount() > 0) {
                    inventoryInfo += ", " + partCountInfo.getOmniPodCount() + countModifier + " OmniPod";
                }

                sbText.append(inventoryInfo);
                sbText.append("<br/>");

                String price = "Item Price: " + partCountInfo.getStickerPrice().toAmountAndSymbolString();

                sbText.append(price);
                sbText.append("<br/>");

                if (partCountInfo.getMissingCount() > 1) {
                    price = "Missing item price: " +
                            partCountInfo.getStickerPrice().multipliedBy(partCountInfo.getMissingCount()).toAmountAndSymbolString();

                    sbText.append(price);
                    sbText.append("<br/>");
                }

                if (!partCountInfo.isCanBeAcquired()) {
                    sbText.append("<br/><br/><font color='red' size='4'>");
                    sbText.append(partCountInfo.getFailedMessage());
                    sbText.append("</font>");
                }
            }

            sbText.append("</font></html>");

            return sbText.toString();
        }

        private void initComponents() {
            targetWork = awList.get(0);
            part = targetWork.getAcquisitionPart();

            partCountInfo = PartsAcquisitionService.getPartCountInfoMap().get(targetWork.getAcquisitionDisplayName());

            // Generate text
            GridBagConstraints gbcMain = new GridBagConstraints();
            gbcMain.gridx = 0;
            gbcMain.gridy = 0;
            gbcMain.weighty = 0.0;
            gbcMain.weightx = 0.0;
            gbcMain.fill = GridBagConstraints.HORIZONTAL;
            gbcMain.anchor = GridBagConstraints.NORTH;

            Insets insetsOriginal = gbcMain.insets;

            // Set image
            String[] imgData = Part.findPartImage(part);
            String imgPath = imgData[0] + imgData[1] + ".png";

            Image imgTool = getToolkit().getImage(imgPath);
            JLabel lblIcon = new JLabel();
            lblIcon.setIcon(new ImageIcon(imgTool));
            add(lblIcon, gbcMain);

            gbcMain.anchor = GridBagConstraints.NORTHWEST;
            gbcMain.gridx = 1;
            gbcMain.weightx = 1.0;
            gbcMain.insets = new Insets(0, 10, 0, 0);

            lblText = new JLabel(generateText());
            add(lblText, gbcMain);

            gbcMain.gridx = 2;

            add(createActionButtons(), gbcMain);
            gbcMain.gridy++;

            gbcMain.gridx = 0;
            gbcMain.gridwidth = 3;
            gbcMain.insets = insetsOriginal;

            Map<Unit, Integer> unitMap = new HashMap<>();

            for (IAcquisitionWork awUnit : awList) {
                if (!unitMap.containsKey(awUnit.getUnit())) {
                    unitMap.put(awUnit.getUnit(), 1);
                } else {
                    int count = unitMap.get(awUnit.getUnit()) + 1;
                    unitMap.put(awUnit.getUnit(), count);
                }
            }

            JPanel pnlUnits = new JPanel();
            pnlUnits.setLayout(new GridBagLayout());
            pnlUnits.setBorder(BorderFactory.createTitledBorder("Units requiring this part (" + unitMap.size() + ")"));

            GridBagConstraints cUnits = new GridBagConstraints();
            cUnits.gridx = 0;
            cUnits.gridy = 0;
            cUnits.weighty = 0.0;
            cUnits.weightx = 1.0;
            cUnits.fill = GridBagConstraints.HORIZONTAL;
            cUnits.anchor = GridBagConstraints.NORTHWEST;

            for (Unit unit : unitMap.keySet()) {
                int count = unitMap.get(unit);

                JLabel lblUnit = new JLabel();
                lblUnit.setText(unit.getName() + ((count > 1) ? " (" + count + " needed)" : ""));

                pnlUnits.add(lblUnit, cUnits);

                cUnits.gridy++;
            }

            gbcMain.insets = new Insets(10, 0, 10, 0);

            add(pnlUnits, gbcMain);
            gbcMain.gridy++;
            gbcMain.insets = insetsOriginal;

            if (idx != PartsAcquisitionService.getAcquisitionMap().size()) {
                this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
            }
        }

        private JPanel createActionButtons() {
            JPanel actionButtons = new JPanel(new GridBagLayout());

            GridBagConstraints gbcActions = new GridBagConstraints();
            gbcActions.gridx = 0;
            gbcActions.gridy = 0;
            gbcActions.weightx = 0.5;
            gbcActions.insets = new Insets(10, 0, 5, 0);
            gbcActions.fill = GridBagConstraints.NONE;
            gbcActions.anchor = GridBagConstraints.NORTHEAST;

            btnUseBonus = new JButton(String.format("Use Bonus Part (%s)", numBonusParts));
            btnUseBonus.setToolTipText("Use a bonus part to acquire this item");
            btnUseBonus.setName("btnUseBonus");
            btnUseBonus.setVisible(numBonusParts > 0);
            btnUseBonus.addActionListener(ev -> useBonusPart());
            actionButtons.add(btnUseBonus, gbcActions);
            gbcActions.gridy++;

            if (partCountInfo.isCanBeAcquired()) {
                JButton btnOrderOne = new JButton("Order One");
                btnOrderOne.setToolTipText("Order one item");
                btnOrderOne.setName("btnOrderOne");
                btnOrderOne.addActionListener(ev -> {
                    campaignGUI.getCampaign().getShoppingList().addShoppingItem(part.getAcquisitionWork(),
                            1, campaignGUI.getCampaign());
                    refresh();
                });
                actionButtons.add(btnOrderOne, gbcActions);
                gbcActions.gridy++;
            }

            if (!partCountInfo.isCanBeAcquired()) {
                JButton btnOrderOne = new JButton("Order One (TN: Impossible)");
                btnOrderOne.setToolTipText("Order one item");
                btnOrderOne.setName("btnOrderOne");
                btnOrderOne.addActionListener(ev -> {
                    campaignGUI.getCampaign().getShoppingList().addShoppingItem(part.getAcquisitionWork(),
                            1, campaignGUI.getCampaign());
                    refresh();
                });
                actionButtons.add(btnOrderOne, gbcActions);
                gbcActions.gridy++;
            }

            btnOrderAll = new JButton("Order All (" + partCountInfo.getMissingCount() + ")");
            btnOrderAll.setToolTipText("Order all missing");
            btnOrderAll.setName("btnOrderAll");
            btnOrderAll.setVisible(partCountInfo.getMissingCount() > 1);
            btnOrderAll.addActionListener(ev -> orderAllMissing());
            actionButtons.add(btnOrderAll, gbcActions);
            gbcActions.gridy++;

            btnDepod = new JButton("Remove One From Pod");
            btnDepod.setToolTipText("Remove replacement from pod");
            btnDepod.setName("btnDepod");
            btnDepod.setVisible(partCountInfo.getOmniPodCount() > 0);
            btnDepod.addActionListener(ev -> {
                MissingPart podded = part.getMissingPart();
                podded.setOmniPodded(true);
                Part replacement = podded.findReplacement(false);

                if (replacement != null) {
                    campaignGUI.getCampaign().getQuartermaster().depodPart(replacement, 1);
                    MekHQ.triggerEvent(new PartChangedEvent(replacement));
                }
                refresh();
            });
            actionButtons.add(btnDepod, gbcActions);
            gbcActions.gridy++;

            if (campaignGUI.getCampaign().isGM()) {
                JButton btnGM = new JButton("[GM] Acquire Instantly");
                btnGM.setToolTipText("GM Override - Acquire all missing items instantly");
                btnGM.setName("btnGM");
                btnGM.addActionListener(ev -> {
                    IAcquisitionWork actualWork = targetWork;

                    // ammo bins have some internal logic for generating acquisition work?
                    if (actualWork instanceof AmmoBin) {
                        actualWork = ((AmmoBin) actualWork).getAcquisitionWork();
                    }

                    // GM find the actual number required
                    for (int count = 0; count < partCountInfo.getRequiredCount(); count++) {
                        campaignGUI.getCampaign().addReport(String.format("GM Acquiring %s. %s",
                                actualWork.getAcquisitionName(), actualWork.find(0)));
                    }

                    Unit unit = actualWork.getUnit();
                    if (unit != null) {
                        MekHQ.triggerEvent(new UnitChangedEvent(unit));
                    }

                    refresh();
                });
                actionButtons.add(btnGM, gbcActions);
                gbcActions.gridy++;
            }

            return actionButtons;
        }
    }
}
