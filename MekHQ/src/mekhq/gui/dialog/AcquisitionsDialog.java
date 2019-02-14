package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.logging.LogLevel;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.RepairTab;
import mekhq.service.PartsAcquisitionService;
import mekhq.service.PartsAcquisitionService.PartCountInfo;

/**
 * @author Kipsta
 *
 */

public class AcquisitionsDialog extends JDialog {
    private static final long serialVersionUID = -1942823778220741544L;

    private CampaignGUI campaignGUI;
    private Map<String, AcquisitionPanel> partPanelMap = new HashMap<>();

    private JPanel pnlSummary;
    private JLabel lblSummary;
    private JButton btnSummary;

    int numBonusParts = 0;

    public AcquisitionsDialog(Frame _parent, boolean _modal, CampaignGUI _campaignGUI) {
        super(_parent, _modal);
        this.campaignGUI = _campaignGUI;

        calculateBonusParts();

        initComponents();

        setLocationRelativeTo(_parent);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle("Parts Acquisition");

        final Container content = getContentPane();
        content.setLayout(new BorderLayout());

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
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
        scrollMain.setPreferredSize(new java.awt.Dimension(700, 500));

        content.add(scrollMain, BorderLayout.CENTER);

        pack();
    }

    private JPanel createSummaryPanel() {
        pnlSummary = new JPanel();
        pnlSummary.setLayout(new GridBagLayout());
        pnlSummary.setBorder(BorderFactory.createTitledBorder("Acquisition Summary"));

        pnlSummary.addPropertyChangeListener("counts", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                PartsAcquisitionService.buildPartsList(campaignGUI.getCampaign());

                lblSummary.setText(generateSummaryText());

                btnSummary.firePropertyChange("missingCount", -1, PartsAcquisitionService.getMissingCount());

                if (campaignGUI.getTab(GuiTabType.REPAIR) != null) {
                    ((RepairTab) campaignGUI.getTab(GuiTabType.REPAIR)).refreshPartsAcquisitionService(false);
                }
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
        btnSummary.setText("Order All"); // NOI18N
        btnSummary.setToolTipText("Order all missing parts");
        btnSummary.setName("btnOrderEverything"); // NOI18N
        btnSummary.addActionListener(ev -> {
            for (AcquisitionPanel pnl : partPanelMap.values()) {
                pnl.orderAllMissing();
            }
        });
        btnSummary.addPropertyChangeListener("missingCount", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                boolean visible = false;

                if ((PartsAcquisitionService.getMissingCount() > 0) && (PartsAcquisitionService
                        .getMissingCount() > PartsAcquisitionService.getUnavailableCount())) {
                    visible = true;
                }

                if (!visible) {
                    btnSummary.setVisible(false);
                }
            }
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
        sbText.append("<html><font size='3' color='black'>");

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

    private void calculateBonusParts() {
        Campaign campaign = campaignGUI.getCampaign();

        List<Unit> unitList = campaign.getServiceableUnits();
        Unit unit = null;

        if ((null != unitList) && !unitList.isEmpty()) {
            unit = unitList.get(0);
        }

        if (campaign.getCampaignOptions().getUseAtB() && (null != unit)) {
            numBonusParts = 0;
            AtBContract contract = campaign.getAttachedAtBContract(unit);

            if (null == contract) {
                numBonusParts = campaign.totalBonusParts();
            } else {
                numBonusParts = contract.getNumBonusParts();
            }
        }

        if (null != partPanelMap) {
            for (AcquisitionPanel pnl : partPanelMap.values()) {
                pnl.refresh();
            }
        }
    }

    public class AcquisitionPanel extends JPanel {
        private static final long serialVersionUID = -205430742799527142L;

        private List<IAcquisitionWork> awList;
        private int idx;

        private IAcquisitionWork targetWork;
        private Part part;
        private PartCountInfo partCountInfo = new PartCountInfo();

        private JButton btnOrderAll = new JButton();
        private JButton btnUseBonus = new JButton();
        private JButton btnDepod = new JButton();
        private JLabel lblText = new JLabel();

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

            if ((partCountInfo.getMissingCount() > 0) && partCountInfo.isCanBeAcquired()) {
                campaignGUI.getCampaign().getShoppingList().addShoppingItem(targetWork, partCountInfo.getMissingCount(),
                        campaignGUI.getCampaign());

                refresh();
            }
        }

        private void useBonusPart() {
            if (targetWork instanceof AmmoBin) {
                targetWork = ((AmmoBin) targetWork).getAcquisitionWork();
            }

            String report = targetWork.find(0);

            if (report.endsWith("0 days.")) {
                AtBContract contract = campaignGUI.getCampaign().getAttachedAtBContract(targetWork.getUnit());

                if (null == contract) {
                    for (Mission m : campaignGUI.getCampaign().getMissions()) {
                        if (m.isActive() && m instanceof AtBContract && ((AtBContract) m).getNumBonusParts() > 0) {
                            contract = (AtBContract) m;
                            break;
                        }
                    }
                }

                if (null == contract) {
                    MekHQ.getLogger().log(getClass(), "useBonusPart()", LogLevel.ERROR, //$NON-NLS-1$
                            "AtB: used bonus part but no contract has bonus parts available."); //$NON-NLS-1$
                } else {
                    contract.useBonusPart();
                }
            }

            refresh();

            calculateBonusParts();
        }

        private void refresh() {
            pnlSummary.firePropertyChange("counts", -1, 0);

            partCountInfo = PartsAcquisitionService.getPartCountInfoMap().get(targetWork.getAcquisitionDisplayName());

            if (null == partCountInfo) {
                ((AcquisitionPanel) this).setVisible(false);
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

                if (partCountInfo.getOmniPodCount() == 0) {
                    btnDepod.setVisible(false);
                } else {
                    btnDepod.setVisible(true);
                }

                if (numBonusParts == 0) {
                    btnUseBonus.setVisible(false);
                } else {
                    btnUseBonus.setText(String.format("Use Bonus Part (%s)", numBonusParts));
                    btnUseBonus.setVisible(true);
                }			
            }
        }

        private String generateText() {
            StringBuilder sbText = new StringBuilder();
            sbText.append("<html><font size='3' color='black'>");

            sbText.append("<b>");
            sbText.append(targetWork.getAcquisitionDisplayName());
            sbText.append("</b><br/>");

            sbText.append("Required: ");
            sbText.append(awList.size());

            if (null != partCountInfo) {
                if (partCountInfo.getMissingCount() > 0) {
                    sbText.append(", ");

                    sbText.append("<font color='red'>");
                    sbText.append("missing: ");
                    sbText.append(partCountInfo.getMissingCount());
                    sbText.append("</font>");
                }

                sbText.append("<br/>");

                String countModifier = partCountInfo.getCountModifier();

                if (!StringUtil.isNullOrEmpty(countModifier)) {
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

            lblText.setText(generateText());
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
                lblUnit.setText(unit.getName() + (count > 1 ? " (" + count + " needed)" : ""));

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

            GridBagConstraints gbcActions = new java.awt.GridBagConstraints();
            gbcActions.gridx = 0;
            gbcActions.gridy = 0;
            gbcActions.weightx = 0.5;
            gbcActions.insets = new Insets(10, 0, 5, 0);
            gbcActions.fill = java.awt.GridBagConstraints.NONE;
            gbcActions.anchor = GridBagConstraints.NORTHEAST;

            btnUseBonus = new JButton();
            btnUseBonus.setText(String.format("Use Bonus Part (%s)", numBonusParts)); // NOI18N
            btnUseBonus.setToolTipText("Use a bonus part to acquire this item");
            btnUseBonus.setName("btnUseBonus"); // NOI18N
            btnUseBonus.addActionListener(ev -> useBonusPart());

            if (numBonusParts == 0) {
                btnUseBonus.setVisible(false);
            }

            actionButtons.add(btnUseBonus, gbcActions);
            gbcActions.gridy++;

            if (partCountInfo.isCanBeAcquired()) {
                JButton btnOrderOne = new JButton();
                btnOrderOne.setText("Order One"); // NOI18N
                btnOrderOne.setToolTipText("Order one item");
                btnOrderOne.setName("btnOrderOne"); // NOI18N
                btnOrderOne.addActionListener(ev -> {
                    campaignGUI.getCampaign().getShoppingList().addShoppingItem(targetWork, 1,
                            campaignGUI.getCampaign());

                    refresh();
                });

                actionButtons.add(btnOrderOne, gbcActions);
                gbcActions.gridy++;

                btnOrderAll.setText("Order All (" + partCountInfo.getMissingCount() + ")"); // NOI18N
                btnOrderAll.setToolTipText("Order all missing");
                btnOrderAll.setName("btnOrderAll"); // NOI18N
                btnOrderAll.addActionListener(ev -> orderAllMissing());

                actionButtons.add(btnOrderAll, gbcActions);
                gbcActions.gridy++;

                if (partCountInfo.getMissingCount() <= 1) {
                    btnOrderAll.setVisible(false);
                }
            }

            if (partCountInfo.getOmniPodCount() > 0) {				
                btnDepod.setText("Remove One From Pod"); // NOI18N
                btnDepod.setToolTipText("Remove replacement from pod");
                btnDepod.setName("btnDepod"); // NOI18N
                btnDepod.addActionListener(ev -> {
                    MissingPart podded = part.getMissingPart();
                    podded.setOmniPodded(true);
                    Part replacement = podded.findReplacement(false);

                    if (null != replacement) {
                        campaignGUI.getCampaign().depodPart(replacement, 1);
                        MekHQ.triggerEvent(new PartChangedEvent(replacement));
                    }

                    refresh();
                });

                actionButtons.add(btnDepod, gbcActions);
                gbcActions.gridy++;
            }

            if (campaignGUI.getCampaign().isGM()) {
                JButton btnGM = new JButton();
                btnGM.setText("[GM] Acquire Instantly"); // NOI18N
                btnGM.setToolTipText("GM Override - Acquire all missing items instantly");
                btnGM.setName("btnGM"); // NOI18N
                btnGM.addActionListener(ev -> {
                    IAcquisitionWork actualWork = targetWork;

                    // ammo bins have some internal logic for generating acquisition work?
                    if(targetWork instanceof AmmoBin) {
                        actualWork = ((AmmoBin) targetWork).getAcquisitionWork();
                    }

                    campaignGUI.getCampaign().addReport(actualWork.find(0));
                    Unit unit = actualWork.getUnit();
                    if (null != unit) {
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
