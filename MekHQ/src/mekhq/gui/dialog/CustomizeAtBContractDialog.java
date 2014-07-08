/**
 * 
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.gui.FactionComboBox;
import mekhq.gui.JSuggestField;

/**
 * @author Neoancient
 *
 */
public class CustomizeAtBContractDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7018467869340880912L;
	private Frame frame;
	private AtBContract contract;
	private Campaign campaign;
	private DirectoryItems camos;
	private String allyCamoCategory;
	private String allyCamoFileName;
	private int allyColorIndex;
	private String enemyCamoCategory;
	private String enemyCamoFileName;
	private int enemyColorIndex;

	protected JTextField txtName;
	protected FactionComboBox cbEmployer;
	protected FactionComboBox cbEnemy;
	protected JCheckBox chkShowAllFactions;

	protected JComboBox<String> cbMissionType;
    protected JTextArea txtDesc;
    protected JSuggestField suggestPlanet;
	protected JComboBox<String> cbAllySkill;
	protected JComboBox<String> cbAllyQuality;
	protected JComboBox<String> cbEnemySkill;
	protected JComboBox<String> cbEnemyQuality;
	protected JSpinner spnRequiredLances;
	protected JComboBox<String> cbEnemyMorale;
	protected JTextField txtAllyBotName;
	protected JTextField txtEnemyBotName;
	protected JButton btnAllyCamo;
	protected JButton btnEnemyCamo;
	
    protected JButton btnClose;
    protected JButton btnOK;
	
	Set<String> currentFactions;
	
	public CustomizeAtBContractDialog(Frame parent, boolean modal, AtBContract contract, Campaign c, DirectoryItems camos) {
		super(parent, modal);
		this.frame = parent;
		this.contract = contract;
		this.camos = camos;
		campaign = c;
		allyCamoCategory = contract.getAllyCamoCategory();
		allyCamoFileName = contract.getAllyCamoFileName();
		allyColorIndex = contract.getAllyColorIndex();
		enemyCamoCategory = contract.getEnemyCamoCategory();
		enemyCamoFileName = contract.getEnemyCamoFileName();
		enemyColorIndex = contract.getEnemyColorIndex();
		
		initComponents();
		setLocationRelativeTo(parent);
	}

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewContractDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        
        getContentPane().setLayout(new java.awt.GridBagLayout());

        RandomFactionGenerator.getInstance().updateTables(campaign.getDate(),
				campaign.getCurrentPlanet(), campaign.getCampaignOptions());
		currentFactions = RandomFactionGenerator.getInstance().getCurrentFactions();

		GridBagConstraints gbc = new GridBagConstraints();
		
		txtName = new JTextField();
        JLabel lblName = new JLabel();
        cbEmployer = new FactionComboBox();
        cbEmployer.addFactionEntries(currentFactions, campaign.getEra());
        JLabel lblEmployer = new JLabel();
		cbEnemy = new FactionComboBox();
        cbEnemy.addFactionEntries(currentFactions, campaign.getEra());
        JLabel lblEnemy = new JLabel();
    	chkShowAllFactions = new JCheckBox();
    	cbMissionType = new JComboBox<String>(AtBContract.missionTypeNames);
        JLabel lblType = new JLabel();
        btnOK = new JButton();
        btnClose = new JButton();
        JScrollPane scrDesc = new JScrollPane();
        txtDesc = new JTextArea();
        JLabel lblPlanetName = new JLabel();
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};
    	cbAllySkill = new JComboBox<String>(skillNames);
    	cbAllyQuality = new JComboBox<String>(ratingNames);
        JLabel lblAllyRating = new JLabel();
    	cbEnemySkill = new JComboBox<String>(skillNames);
    	cbEnemyQuality = new JComboBox<String>(ratingNames);;
    	JLabel lblAllyBotName = new JLabel();
    	txtAllyBotName = new JTextField();
    	JLabel lblEnemyBotName = new JLabel();
    	txtEnemyBotName = new JTextField();
    	JLabel lblAllyCamo = new JLabel();
    	btnAllyCamo = new JButton();
    	JLabel lblEnemyCamo = new JLabel();
    	btnEnemyCamo = new JButton();
        JLabel lblEnemyRating = new JLabel();
    	JLabel lblRequiredLances = new JLabel();
    	spnRequiredLances = new JSpinner(new SpinnerNumberModel(contract.getRequiredLances(), 1, 20	, 1));
    	JLabel lblEnemyMorale = new JLabel();
    	cbEnemyMorale = new JComboBox<String>(AtBContract.moraleLevelNames);
   	
    	int y = 0;
          
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblName, gbc);
        
        txtName.setText(contract.getName());
        txtName.setName("txtName"); // NOI18N
        
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(txtName, gbc);
        
        lblEmployer.setText(resourceMap.getString("lblEmployer.text")); // NOI18N
        lblEmployer.setName("lblEmployer"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEmployer, gbc);

        cbEmployer.setSelectedItemByKey(contract.getEmployerCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbEmployer, gbc);

        lblEnemy.setText(resourceMap.getString("lblEnemy.text")); // NOI18N
        lblEnemy.setName("lblEnemy"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEnemy, gbc);

        cbEnemy.setSelectedItemByKey(contract.getEnemyCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbEnemy, gbc);
 
        chkShowAllFactions.setText(resourceMap.getString("chkShowAllFactions.text"));
        chkShowAllFactions.setName("chkShowAllFactions");
        chkShowAllFactions.setSelected(false);
        
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(chkShowAllFactions, gbc);
        chkShowAllFactions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				showAllFactions(chkShowAllFactions.isSelected());
			}

        });
 
        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text")); // NOI18N
        lblPlanetName.setName("lblPlanetName"); // NOI18N        
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblPlanetName, gbc);
        
        suggestPlanet = new JSuggestField(this, campaign.getPlanetNames());       
        suggestPlanet.setText(contract.getPlanetName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(suggestPlanet, gbc);
        
        lblType.setText(resourceMap.getString("lblType.text")); // NOI18N
        lblType.setName("lblType"); // NOI18N        
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblType, gbc);
        
        cbMissionType.setSelectedItem(contract.getMissionTypeName());
        cbMissionType.setName("cbMissionType"); // NOI18N        
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbMissionType, gbc);
         
        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text")); // NOI18N
        lblEnemy.setName("lblAllyRating"); // NOI18N        
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblAllyRating, gbc);
        
		cbAllySkill.setSelectedIndex(contract.getAllySkill());        
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbAllySkill, gbc);

		cbAllyQuality.setSelectedIndex(contract.getAllyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbAllyQuality, gbc);

        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text")); // NOI18N
        lblEnemyRating.setName("lblEnemyRating"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEnemyRating, gbc);
        
		cbEnemySkill.setSelectedIndex(contract.getEnemySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbEnemySkill, gbc);

		cbEnemyQuality.setSelectedIndex(contract.getEnemyQuality());       
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbEnemyQuality, gbc);
        
        lblRequiredLances.setText(resourceMap.getString("lblRequiredLances.text")); // NOI18N
        lblRequiredLances.setName("lblRequiredLances"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblRequiredLances, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(spnRequiredLances, gbc);

        lblEnemyMorale.setText(resourceMap.getString("lblEnemyMorale.text")); // NOI18N
        lblEnemyMorale.setName("lblEnemyMorale"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEnemyMorale, gbc);
        
        cbEnemyMorale.setSelectedIndex(contract.getMoraleLevel());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(cbEnemyMorale, gbc);

        lblAllyBotName.setText(resourceMap.getString("lblAllyBotName.text")); // NOI18N
        lblAllyBotName.setName("lblAllyBotName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblAllyBotName, gbc);
        
		txtAllyBotName.setText(contract.getAllyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(txtAllyBotName, gbc);

        lblEnemyBotName.setText(resourceMap.getString("lblEnemyBotName.text")); // NOI18N
        lblEnemyBotName.setName("lblEnemyBotName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEnemyBotName, gbc);
        
        txtEnemyBotName.setText(contract.getEnemyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(txtEnemyBotName, gbc);

        lblAllyCamo.setText(resourceMap.getString("lblAllyCamo.text")); // NOI18N
        lblAllyCamo.setName("lblEnemyBotName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblAllyCamo, gbc);
        
        btnAllyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(btnAllyCamo, gbc);
        btnAllyCamo.addActionListener(camoButtonListener);
        setCamoIcon(btnAllyCamo, allyCamoCategory, allyCamoFileName, allyColorIndex);
        
        lblEnemyCamo.setText(resourceMap.getString("lblEnemyCamo.text")); // NOI18N
        lblEnemyCamo.setName("lblEnemyCamo"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(lblEnemyCamo, gbc);
        
        btnEnemyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(btnEnemyCamo, gbc);
        btnEnemyCamo.addActionListener(camoButtonListener);
        setCamoIcon(btnEnemyCamo, enemyCamoCategory, enemyCamoFileName, enemyColorIndex);

        txtDesc.setText(contract.getDescription());
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtDesc.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDesc.setViewportView(txtDesc);
        scrDesc.setPreferredSize(new Dimension(400, 200));
        scrDesc.setMinimumSize(new Dimension(400, 200));
        
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(5, 5, 0, 0);
        add(scrDesc, gbc);

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.EAST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(btnOK, gbc);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        add(btnClose, gbc);
        
        pack();
    }

    ActionListener camoButtonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
	        CamoChoiceDialog ccd;
	        if (e.getSource().equals(btnAllyCamo)) {
	        	ccd = new CamoChoiceDialog(frame, true,
	        			allyCamoCategory, allyCamoFileName,
	        			allyColorIndex, camos);
		        ccd.setVisible(true);
		        allyCamoCategory = ccd.getCategory();
		        allyCamoFileName = ccd.getFileName();
		        if (ccd.getColorIndex() != -1) {
		            allyColorIndex = ccd.getColorIndex();
		        }
		        setCamoIcon(btnAllyCamo, allyCamoCategory,
	        			allyCamoFileName, allyColorIndex);			
	        } else {
	        	ccd = new CamoChoiceDialog(frame, true,
	        			enemyCamoCategory, enemyCamoFileName,
	        			enemyColorIndex, camos);
		        ccd.setVisible(true);
		        enemyCamoCategory = ccd.getCategory();
		        enemyCamoFileName = ccd.getFileName();
		        if (ccd.getColorIndex() != -1) {
		        	enemyColorIndex = ccd.getColorIndex();
		        }
		        setCamoIcon(btnEnemyCamo, enemyCamoCategory,
		        		enemyCamoFileName, enemyColorIndex);			
	        }
		}
    };
    
    /* Copied from CampaignOptionsDialog */    
    private void setCamoIcon(JButton btnCamo, String camoCategory, String camoFileName, int colorIndex) {
        if (null == camoCategory) {
            return;
        }

        if (Player.NO_CAMO.equals(camoCategory)) {
            int colorInd = colorIndex;
            if (colorInd == -1) {
                colorInd = 0;
            }
            BufferedImage tempImage = new BufferedImage(84, 72,
                                                        BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = tempImage.createGraphics();
            graphics.setColor(PlayerColors.getColor(colorInd));
            graphics.fillRect(0, 0, 84, 72);
            btnCamo.setIcon(new ImageIcon(tempImage));
            return;
        }

        // Try to get the camo file.
        try {
            // Translate the root camo directory name.
            if (Player.ROOT_CAMO.equals(camoCategory)) {
                camoCategory = ""; //$NON-NLS-1$
            }
            Image camo = (Image) camos.getItem(camoCategory, camoFileName);
            btnCamo.setIcon(new ImageIcon(camo));
        } catch (Exception err) {
            //err.printStackTrace();
        	JOptionPane.showMessageDialog(
        			this,
        			"Cannot find your camo file.\n"
        			+ "Setting to default color.\n"
        			+ "You should browse to the correct camo file,\n"
        			+ "or if it isn't available copy it into MekHQ's"
        			+ "data/images/camo folder.",
        			"Missing Camo File",
        			JOptionPane.WARNING_MESSAGE);
        	camoCategory = Player.NO_CAMO;
        	colorIndex = 0;
        	setCamoIcon(btnCamo, camoCategory, camoFileName, colorIndex);
        }   	
    }
    
    private void btnOKActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	contract.setName(txtName.getText());
    	contract.setEmployerCode(cbEmployer.getSelectedItemKey(), campaign.getEra());
    	contract.setEnemyCode(cbEnemy.getSelectedItemKey());
    	contract.setMissionType(cbMissionType.getSelectedIndex());
    	contract.setAllySkill(cbAllySkill.getSelectedIndex());
    	contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
    	contract.setEnemySkill(cbEnemySkill.getSelectedIndex());
    	contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
    	contract.setRequiredLances((Integer)spnRequiredLances.getValue());
    	contract.setMoraleLevel(cbEnemyMorale.getSelectedIndex());
    	contract.setAllyBotName(txtAllyBotName.getText());
    	contract.setEnemyBotName(txtEnemyBotName.getText());
    	contract.setAllyCamoCategory(allyCamoCategory);
    	contract.setAllyCamoFileName(allyCamoFileName);
    	contract.setAllyColorIndex(allyColorIndex);
    	contract.setEnemyCamoCategory(enemyCamoCategory);
    	contract.setEnemyCamoFileName(enemyCamoFileName);
    	contract.setEnemyColorIndex(enemyColorIndex);
    	contract.setPlanetName(suggestPlanet.getText());
    	contract.setDesc(txtDesc.getText());
    	this.setVisible(false);
    }
    
    private void btnCloseActionPerformed(ActionEvent evt) {
    	this.setVisible(false);
    }

    private void showAllFactions(boolean allFactions) {
    	cbEmployer.removeAllItems();
    	cbEnemy.removeAllItems();
    	if (allFactions) {
    		cbEmployer.addFactionEntries(Faction.factions.keySet(),	campaign.getEra());
    		cbEnemy.addFactionEntries(Faction.factions.keySet(),	campaign.getEra());
    	} else {
    		cbEmployer.addFactionEntries(currentFactions, campaign.getEra());
    		cbEnemy.addFactionEntries(currentFactions, campaign.getEra());
    	}
    }
    
    public int getMissionId() {
    	return contract.getId();
    }

}
