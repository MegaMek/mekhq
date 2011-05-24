/*
 * ContractViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import megamek.common.TechConstants;
import mekhq.campaign.mission.Contract;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ContractViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Contract contract;
	
	private javax.swing.JPanel pnlStats;
	private javax.swing.JTextArea txtDesc;
	
	private javax.swing.JLabel lblStatus;
	private javax.swing.JLabel lblLocation;
	private javax.swing.JTextArea txtLocation;
	private javax.swing.JLabel lblEmployer;
	private javax.swing.JTextArea txtEmployer;
	private javax.swing.JLabel lblStartDate;
	private javax.swing.JTextArea txtStartDate;
	private javax.swing.JLabel lblEndDate;
	private javax.swing.JTextArea txtEndDate;
	private javax.swing.JLabel lblPayout;
	private javax.swing.JTextArea txtPayout;
	private javax.swing.JLabel lblCommand;
	private javax.swing.JTextArea txtCommand;
	private javax.swing.JLabel lblBLC;
	private javax.swing.JTextArea txtBLC;
	private javax.swing.JLabel lblSalvageValueMerc;
	private javax.swing.JTextArea txtSalvageValueMerc;
	private javax.swing.JLabel lblSalvageValueEmployer;
	private javax.swing.JTextArea txtSalvageValueEmployer;
	private javax.swing.JLabel lblSalvagePct1;
	private javax.swing.JLabel lblSalvagePct2;
	
	public ContractViewPanel(Contract c) {
		this.contract = c;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(contract.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
	}

    private void fillStats() {
    	
    	org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(ContractViewPanel.class);
    	
    	lblStatus = new javax.swing.JLabel();
    	lblLocation = new javax.swing.JLabel();
		txtLocation = new javax.swing.JTextArea();
    	lblEmployer = new javax.swing.JLabel();
		txtEmployer = new javax.swing.JTextArea();
		lblStartDate = new javax.swing.JLabel();
		txtStartDate = new javax.swing.JTextArea();
		lblEndDate = new javax.swing.JLabel();
		txtEndDate = new javax.swing.JTextArea();
		lblPayout = new javax.swing.JLabel();
		txtPayout = new javax.swing.JTextArea();
		lblCommand = new javax.swing.JLabel();
		txtCommand = new javax.swing.JTextArea();
		lblBLC = new javax.swing.JLabel();
		txtBLC = new javax.swing.JTextArea();
		
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
		
		lblStatus.setName("lblOwner"); // NOI18N
		lblStatus.setText("<html><b>" + contract.getStatusName() + "</b></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus, gridBagConstraints);
		

		lblLocation.setName("lblLocation"); // NOI18N
		lblLocation.setText(resourceMap.getString("lblLocation.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblLocation, gridBagConstraints);
		
		txtLocation.setName("txtLocation"); // NOI18N
		txtLocation.setText(contract.getPlanetName());
		txtLocation.setEditable(false);
		txtLocation.setLineWrap(true);
		txtLocation.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtLocation, gridBagConstraints);
		
		lblEmployer.setName("lblEmployer"); // NOI18N
		lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEmployer, gridBagConstraints);
		
		txtEmployer.setName("txtEmployer"); // NOI18N
		txtEmployer.setText(contract.getEmployer());
		txtEmployer.setEditable(false);
		txtEmployer.setLineWrap(true);
		txtEmployer.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEmployer, gridBagConstraints);
		
		lblStartDate.setName("lblStartDate"); // NOI18N
		lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStartDate, gridBagConstraints);
		
		txtStartDate.setName("txtStartDate"); // NOI18N
		txtStartDate.setText(shortDateFormat.format(contract.getStartDate()));
		txtStartDate.setEditable(false);
		txtStartDate.setLineWrap(true);
		txtStartDate.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtStartDate, gridBagConstraints);
		
		lblEndDate.setName("lblEndDate"); // NOI18N
		lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEndDate, gridBagConstraints);
		
		txtEndDate.setName("txtEndDate"); // NOI18N
		txtEndDate.setText(shortDateFormat.format(contract.getEndingDate()));
		txtEndDate.setEditable(false);
		txtEndDate.setLineWrap(true);
		txtEndDate.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEndDate, gridBagConstraints);
		
		lblPayout.setName("lblPayout"); // NOI18N
		lblPayout.setText(resourceMap.getString("lblPayout.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblPayout, gridBagConstraints);
		
		DecimalFormat numFormatter = new DecimalFormat();
		txtPayout.setName("txtPayout"); // NOI18N
		txtPayout.setText(numFormatter.format(contract.getMonthlyPayOut()) + " C-Bills");
		txtPayout.setEditable(false);
		txtPayout.setLineWrap(true);
		txtPayout.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtPayout, gridBagConstraints);
		
		lblCommand.setName("lblCommand"); // NOI18N
		lblCommand.setText(resourceMap.getString("lblCommand.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCommand, gridBagConstraints);
		
		txtCommand.setName("txtCommand"); // NOI18N
		txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
		txtCommand.setEditable(false);
		txtCommand.setLineWrap(true);
		txtCommand.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtCommand, gridBagConstraints);
		
		lblBLC.setName("lblBLC"); // NOI18N
		lblBLC.setText(resourceMap.getString("lblBLC.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBLC, gridBagConstraints);
		
		txtBLC.setName("txtBLC"); // NOI18N
		txtBLC.setText(contract.getBattleLossComp() + "%");
		txtBLC.setEditable(false);
		txtBLC.setLineWrap(true);
		txtBLC.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtBLC, gridBagConstraints);
		
		DecimalFormat formatter = new DecimalFormat();
		int i = 8;
		if(contract.getSalvagePct() > 0 && !contract.isSalvageExchange()) {
			lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));   	
	    	gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = i;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
	    	txtSalvageValueMerc = new javax.swing.JTextArea();
	    	txtSalvageValueMerc.setText(formatter.format(contract.getSalvagedByUnit()) + " C-Bills");   	
	    	txtSalvageValueMerc.setEditable(false);
	    	txtSalvageValueMerc.setLineWrap(true);
	    	txtSalvageValueMerc.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = i;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(txtSalvageValueMerc, gridBagConstraints);
	        i++;
	        lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));   	
	    	gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = i;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
	    	txtSalvageValueEmployer = new javax.swing.JTextArea();
	    	txtSalvageValueEmployer.setText(formatter.format(contract.getSalvagedByEmployer()) + " C-Bills");   	
	    	txtSalvageValueEmployer.setEditable(false);
	    	txtSalvageValueEmployer.setLineWrap(true);
	    	txtSalvageValueEmployer.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = i;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
	        i++;
		}
		lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
		lblSalvagePct2 = new JLabel();

		if(contract.isSalvageExchange()) {
			lblSalvagePct2.setText("Exchange (" + contract.getSalvagePct() + "%)"); 
		} else if(contract.getSalvagePct() == 0) {
			lblSalvagePct2.setText("None"); 
		} else {
			lblSalvagePct1.setText(resourceMap.getString("lblSalvagePct.text"));   
			int maxSalvagePct = contract.getSalvagePct();
	    	int currentSalvagePct = (int)(100*((double)contract.getSalvagedByUnit())/(contract.getSalvagedByUnit()+contract.getSalvagedByEmployer()));
	    	String lead = "<html><font color='black'>";
	        if(currentSalvagePct > maxSalvagePct) {
	        	lead = "<html><font color='red'>";
	        }
	    	lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");   	
		}
       	
    	gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct1, gridBagConstraints); 
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = i;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct2, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(contract.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = i;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtDesc, gridBagConstraints);
		
    }
}