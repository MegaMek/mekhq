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

import mekhq.Utilities;
import mekhq.campaign.parts.Part;
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
	private Map<String, AcquisitionPanel> partPanelMap = new HashMap<String, AcquisitionPanel>();

	private JPanel pnlSummary;
	private JLabel lblSummary;
	private JButton btnSummary;

	public AcquisitionsDialog(Frame _parent, boolean _modal, CampaignGUI _campaignGUI) {
		super(_parent, _modal);
		this.campaignGUI = _campaignGUI;

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
		scrollMain.setPreferredSize(new java.awt.Dimension(500, 400));

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
				PartsAcquisitionService.generateSummaryCounts(campaignGUI.getCampaign());
				
				lblSummary.setText(generateSummaryText());
				
				if (PartsAcquisitionService.getTotalMissingCount() < 1) {
					btnSummary.firePropertyChange("missingCount", -1, PartsAcquisitionService.getTotalMissingCount());
				}
				
		    	if (campaignGUI.getTab(GuiTabType.REPAIR) != null) {
		    		((RepairTab)campaignGUI.getTab(GuiTabType.REPAIR)).refreshPartsAcquisitionService(false);
		    	}
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0.0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 5, 10, 5);

		lblSummary = new JLabel();
		lblSummary.setText(generateSummaryText());
		pnlSummary.add(lblSummary, gbc);

		gbc.gridy++;

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
				int count = (int) evt.getNewValue();

				if (count < 1) {
					btnSummary.setVisible(false);
				}
			}
		});

		pnlSummary.add(btnSummary, gbc);

		return pnlSummary;
	}

	private String generateSummaryText() {
		StringBuilder sbText = new StringBuilder();
		sbText.append("<html><font size='3' color='black'>");

		sbText.append("Required: ");
		sbText.append(PartsAcquisitionService.getRequiredCount());

		if (PartsAcquisitionService.getTotalMissingCount() > 0) {
			sbText.append(", ");

			sbText.append("<font color='red'>");
			sbText.append("missing: ");
			sbText.append(PartsAcquisitionService.getTotalMissingCount());
			sbText.append("</font>");
		}

		sbText.append("<br/>");

		String inventoryInfo = "Inventory: " + PartsAcquisitionService.getInTransitCount() + " in transit, " + PartsAcquisitionService.getOnOrderCount() + " on order";

		sbText.append(inventoryInfo);
		sbText.append("<br/>");

		if (PartsAcquisitionService.getMissingTotalPrice() > 0) {
			String price = "Missing item price: " + Utilities.getCurrencyString(PartsAcquisitionService.getMissingTotalPrice());

			sbText.append(price);
			sbText.append("<br/>");
		}

		sbText.append("</font></html>");

		return sbText.toString();
	}

	public class AcquisitionPanel extends JPanel {
		private static final long serialVersionUID = -205430742799527142L;

		private List<IAcquisitionWork> awList;
		private int idx;

		private IAcquisitionWork targetWork;
		private Part part;
		private int targetMissingCount = 0;
		
		private JButton btnOrderAll = new JButton();
		private JLabel lblText = new JLabel();
		
		public AcquisitionPanel(List<IAcquisitionWork> awList, int idx) {
			this.awList = awList;
			this.idx = idx;

			setLayout(new GridBagLayout());

			initComponents();
		}

		public void orderAllMissing() {
			campaignGUI.getCampaign().getShoppingList().addShoppingItem(targetWork, targetMissingCount,
					campaignGUI.getCampaign());

			btnOrderAll.setVisible(false);

			pnlSummary.firePropertyChange("counts", -1, 0);
			
			lblText.setText(generateText());
		}
		
		private String generateText() {
			PartCountInfo pci = PartsAcquisitionService.getPartCountInfoMap().get(targetWork.getAcquisitionDisplayName());
			
			StringBuilder sbText = new StringBuilder();
			sbText.append("<html><font size='3' color='black'>");

			sbText.append("<b>");
			sbText.append(targetWork.getAcquisitionDisplayName());
			sbText.append("</b><br/>");

			sbText.append("Required: ");
			sbText.append(awList.size());

			if (pci.getMissingCount() > 0) {
				sbText.append(", ");

				sbText.append("<font color='red'>");
				sbText.append("missing: ");
				sbText.append(pci.getMissingCount());
				sbText.append("</font>");
			}

			sbText.append("<br/>");

			String inventoryInfo = "Inventory: " + pci.getInTransitCount() + " in transit, " + pci.getOnOrderCount() + " on order";

			if (pci.getOmniPodCount() > 0) {
				inventoryInfo += ", " + pci.getOmniPodCount() + " OmniPod";
			}

			sbText.append(inventoryInfo);
			sbText.append("<br/>");

			String price = "Item Price: " + Utilities.getCurrencyString(pci.getStickerPrice());

			sbText.append(price);
			sbText.append("<br/>");

			if (pci.getMissingCount() > 1) {
				price = "Missing item price: " + Utilities.getCurrencyString(pci.getStickerPrice() * pci.getMissingCount());

				sbText.append(price);
				sbText.append("<br/>");
			}

			sbText.append("</font></html>");

			targetMissingCount = pci.getMissingCount();
			
			return sbText.toString();
		}

		private void initComponents() {
			targetWork = awList.get(0);
			part = targetWork.getAcquisitionPart();
			
			// Generate text
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 0.0;
			c.weightx = 0.0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.NORTH;

			Insets insetsOriginal = c.insets;

			// Set image
			String[] imgData = Part.findPartImage(part);
			String imgPath = imgData[0] + imgData[1] + ".png";

			Image imgTool = getToolkit().getImage(imgPath);
			JLabel lblIcon = new JLabel();
			lblIcon.setIcon(new ImageIcon(imgTool));
			add(lblIcon, c);

			c.anchor = GridBagConstraints.NORTHWEST;
			c.gridx = 1;
			c.weightx = 1.0;
			c.insets = new Insets(0, 10, 0, 0);

			lblText.setText(generateText());

			add(lblText, c);
			c.gridx = 0;
			c.gridwidth = 2;
			c.gridy++;
			c.insets = insetsOriginal;

			Map<Unit, Integer> unitMap = new HashMap<Unit, Integer>();

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
			pnlUnits.setBorder(BorderFactory.createTitledBorder("Units (" + unitMap.size() + ")"));

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

			c.insets = new Insets(10, 0, 0, 0);

			add(pnlUnits, c);
			c.gridy++;
			c.insets = insetsOriginal;

			GridBagConstraints gbcButtons = new java.awt.GridBagConstraints();
			gbcButtons.gridx = 0;
			gbcButtons.gridy = 0;
			gbcButtons.weightx = 0.5;
			gbcButtons.insets = new Insets(10, 0, 5, 0);
			gbcButtons.fill = java.awt.GridBagConstraints.BOTH;

			JPanel actionButtons = new JPanel(new GridBagLayout());

			JButton btnOrderOne = new JButton();
			btnOrderOne.setText("Order One"); // NOI18N
			btnOrderOne.setToolTipText("Order one item");
			btnOrderOne.setName("btnOrderOne"); // NOI18N
			btnOrderOne.addActionListener(ev -> {
				campaignGUI.getCampaign().getShoppingList().addShoppingItem(targetWork, 1, campaignGUI.getCampaign());

				btnOrderAll.firePropertyChange("missingCount", -1, targetMissingCount);

				pnlSummary.firePropertyChange("counts", -1, 0);
				
				lblText.setText(generateText());
			});

			actionButtons.add(btnOrderOne, gbcButtons);

			btnOrderAll.setText("Order All (" + targetMissingCount + ")"); // NOI18N
			btnOrderAll.setToolTipText("Order all missing");
			btnOrderAll.setName("btnOrderAll"); // NOI18N
			btnOrderAll.addActionListener(ev -> {
				orderAllMissing();
			});

			btnOrderAll.addPropertyChangeListener("missingCount", new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					int count = (int) evt.getNewValue();

					btnOrderAll.setText("Order All (" + count + ")");

					if (count < 1) {
						btnOrderAll.setVisible(false);
					}
				}
			});

			gbcButtons.gridy = 1;

			actionButtons.add(btnOrderAll, gbcButtons);

			if (targetMissingCount <= 1) {
				btnOrderAll.setVisible(false);
			}

			add(actionButtons, c);
			c.gridy++;

			if (idx != PartsAcquisitionService.getAcquisitionMap().size()) {
				this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
			}
		}
	}
}
