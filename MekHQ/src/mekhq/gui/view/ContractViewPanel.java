/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.common.util.EncodeControl;
import mekhq.campaign.mission.Contract;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ContractViewPanel extends JPanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private Contract contract;
    
    private JPanel pnlStats;
    private JTextArea txtDesc;
    
    private JLabel lblStatus;
    private JLabel lblLocation;
    private JTextArea txtLocation;
    private JLabel lblType;
    private JTextArea txtType;
    private JLabel lblEmployer;
    private JTextArea txtEmployer;
    private JLabel lblStartDate;
    private JTextArea txtStartDate;
    private JLabel lblEndDate;
    private JTextArea txtEndDate;
    private JLabel lblPayout;
    private JTextArea txtPayout;
    private JLabel lblCommand;
    private JTextArea txtCommand;
    private JLabel lblBLC;
    private JTextArea txtBLC;
    private JLabel lblSalvageValueMerc;
    private JTextArea txtSalvageValueMerc;
    private JLabel lblSalvageValueEmployer;
    private JTextArea txtSalvageValueEmployer;
    private JLabel lblSalvagePct1;
    private JLabel lblSalvagePct2;
    
    public ContractViewPanel(Contract c) {
        this.contract = c;
        initComponents();
    }
    
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        pnlStats = new JPanel();
        txtDesc = new JTextArea();
               
        setLayout(new GridBagLayout());

        setBackground(Color.WHITE);

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(contract.getName()));
        pnlStats.setBackground(Color.WHITE);
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 20);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;    
        add(pnlStats, gridBagConstraints);
    }

    private void fillStats() {
        lblStatus = new JLabel();
        lblLocation = new JLabel();
        txtLocation = new JTextArea();
        lblEmployer = new JLabel();
        txtEmployer = new JTextArea();
        lblType = new JLabel();
        txtType = new JTextArea();
        lblStartDate = new JLabel();
        txtStartDate = new JTextArea();
        lblEndDate = new JLabel();
        txtEndDate = new JTextArea();
        lblPayout = new JLabel();
        txtPayout = new JTextArea();
        lblCommand = new JLabel();
        txtCommand = new JTextArea();
        lblBLC = new JLabel();
        txtBLC = new JTextArea();
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel", new EncodeControl()); //$NON-NLS-1$

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());
        
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        lblStatus.setName("lblOwner"); // NOI18N
        lblStatus.setText("<html><b>" + contract.getStatusName() + "</b></html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus, gridBagConstraints);
        

        lblLocation.setName("lblLocation"); // NOI18N
        lblLocation.setText(resourceMap.getString("lblLocation.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblLocation, gridBagConstraints);
        
        txtLocation.setName("txtLocation"); // NOI18N
        txtLocation.setText(contract.getPlanetName(null));
        txtLocation.setEditable(false);
        txtLocation.setLineWrap(true);
        txtLocation.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtLocation, gridBagConstraints);
        
        lblEmployer.setName("lblEmployer"); // NOI18N
        lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEmployer, gridBagConstraints);
        
        txtEmployer.setName("txtEmployer"); // NOI18N
        txtEmployer.setText(contract.getEmployer());
        txtEmployer.setEditable(false);
        txtEmployer.setLineWrap(true);
        txtEmployer.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEmployer, gridBagConstraints);
        
        lblType.setName("lblType"); // NOI18N
        lblType.setText(resourceMap.getString("lblType.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);
        
        txtType.setName("txtType"); // NOI18N
        txtType.setText(contract.getType());
        txtType.setEditable(false);
        txtType.setLineWrap(true);
        txtType.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtType, gridBagConstraints);
        
        lblStartDate.setName("lblStartDate"); // NOI18N
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStartDate, gridBagConstraints);
        
        txtStartDate.setName("txtStartDate"); // NOI18N
        txtStartDate.setText(shortDateFormat.format(contract.getStartDate()));
        txtStartDate.setEditable(false);
        txtStartDate.setLineWrap(true);
        txtStartDate.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtStartDate, gridBagConstraints);
        
        lblEndDate.setName("lblEndDate"); // NOI18N
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEndDate, gridBagConstraints);
        
        txtEndDate.setName("txtEndDate"); // NOI18N
        txtEndDate.setText(shortDateFormat.format(contract.getEndingDate()));
        txtEndDate.setEditable(false);
        txtEndDate.setLineWrap(true);
        txtEndDate.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEndDate, gridBagConstraints);
        
        lblPayout.setName("lblPayout"); // NOI18N
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblPayout, gridBagConstraints);
        
        DecimalFormat numFormatter = new DecimalFormat();
        txtPayout.setName("txtPayout"); // NOI18N
        txtPayout.setText(numFormatter.format(contract.getMonthlyPayOut()) + " C-Bills");
        txtPayout.setEditable(false);
        txtPayout.setLineWrap(true);
        txtPayout.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtPayout, gridBagConstraints);
        
        lblCommand.setName("lblCommand"); // NOI18N
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCommand, gridBagConstraints);
        
        txtCommand.setName("txtCommand"); // NOI18N
        txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
        txtCommand.setEditable(false);
        txtCommand.setLineWrap(true);
        txtCommand.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCommand, gridBagConstraints);
        
        lblBLC.setName("lblBLC"); // NOI18N
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBLC, gridBagConstraints);
        
        txtBLC.setName("txtBLC"); // NOI18N
        txtBLC.setText(contract.getBattleLossComp() + "%");
        txtBLC.setEditable(false);
        txtBLC.setLineWrap(true);
        txtBLC.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBLC, gridBagConstraints);
        
        DecimalFormat formatter = new DecimalFormat();
        int i = 9;
        if(contract.getSalvagePct() > 0 && !contract.isSalvageExchange()) {
            lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));       
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
            txtSalvageValueMerc = new JTextArea();
            txtSalvageValueMerc.setText(formatter.format(contract.getSalvagedByUnit()) + " C-Bills");       
            txtSalvageValueMerc.setEditable(false);
            txtSalvageValueMerc.setLineWrap(true);
            txtSalvageValueMerc.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueMerc, gridBagConstraints);
            i++;
            lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));       
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
            txtSalvageValueEmployer = new JTextArea();
            txtSalvageValueEmployer.setText(formatter.format(contract.getSalvagedByEmployer()) + " C-Bills");       
            txtSalvageValueEmployer.setEditable(false);
            txtSalvageValueEmployer.setLineWrap(true);
            txtSalvageValueEmployer.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
            i++;
        }
        lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
        lblSalvagePct2 = new JLabel();

        if(contract.isSalvageExchange()) {
            lblSalvagePct2.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)"); 
        } else if(contract.getSalvagePct() == 0) {
            lblSalvagePct2.setText(resourceMap.getString("none")); 
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
           
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct1, gridBagConstraints); 
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = i;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct2, gridBagConstraints);
        i++;
        txtDesc.setName("txtDesc");
        txtDesc.setText(contract.getDescription());
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 20);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);
        
    }
}