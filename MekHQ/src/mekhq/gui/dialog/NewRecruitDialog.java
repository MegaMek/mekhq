/*
 * NewRecruitDialog.java
 *
 * Created on July 16, 2009, 5:30 PM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.common.util.DirectoryItems;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Rank;
import mekhq.campaign.personnel.Ranks;
import mekhq.gui.CampaignGUI;
import mekhq.gui.view.PersonViewPanel;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class NewRecruitDialog extends javax.swing.JDialog {

    /**
	 * This dialog is used to both hire new pilots and to edit existing ones
	 * 
	 */
	private static final long serialVersionUID = -6265589976779860566L;
	private Person person;
    private Frame frame;
    private boolean newHire;
    private DirectoryItems portraits;
    
    private Campaign campaign;
    
    private CampaignGUI hqView;
    
    private javax.swing.JComboBox<String> choiceRanks;

    private JScrollPane scrollView;

    /** Creates new form CustomizePilotDialog */
    public NewRecruitDialog(java.awt.Frame parent, boolean modal, Person person, Campaign campaign, CampaignGUI view, DirectoryItems portraits) {
        super(parent, modal);
        this.campaign = campaign;
        this.portraits = portraits;
        this.hqView = view;
        this.frame = parent;
        this.person = person;
        initComponents();
        setLocationRelativeTo(parent);
    }
    
    private void refreshView() {
    	scrollView.setViewportView(new PersonViewPanel(person, campaign, hqView.getIconPackage()));
		//This odd code is to make sure that the scrollbar stays at the top
		//I cant just call it here, because it ends up getting reset somewhere later
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
				scrollView.getVerticalScrollBar().setValue(0);
			}
		});
    }

    private void initComponents() {
        scrollView = new JScrollPane();
        choiceRanks = new javax.swing.JComboBox<String>();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewRecruitDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title")); // NOI18N
        if(newHire) {
            setTitle(resourceMap.getString("Form.title.new")); // NOI18N
        }
        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.BorderLayout());

        JPanel panSidebar = createSidebar(resourceMap);
 
        JPanel panBottomButtons = createButtonPanel(resourceMap);

        scrollView.setMinimumSize(new java.awt.Dimension(450, 180));
        scrollView.setPreferredSize(new java.awt.Dimension(450, 180));
        scrollView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollView.setViewportView(null);
        refreshView();
        
        getContentPane().add(panSidebar, BorderLayout.LINE_START);
        getContentPane().add(scrollView, BorderLayout.CENTER);
        getContentPane().add(panBottomButtons, BorderLayout.PAGE_END);

        pack();
    }

    private JPanel createButtonPanel(ResourceBundle resourceMap) {
        JPanel panButtons = new JPanel();
        panButtons.setName("panButtons"); // NOI18N
        panButtons.setLayout(new GridBagLayout());

        JButton button = new JButton(resourceMap.getString("btnHire.text")); // NOI18N
        button.setName("btnOk"); // NOI18N
        button.addActionListener(e -> hire());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;

        panButtons.add(button, gridBagConstraints);
        gridBagConstraints.gridx++;

        button = new JButton(resourceMap.getString("btnClose.text")); // NOI18N
        button.setName("btnClose"); // NOI18N
        button.addActionListener(e -> setVisible(false));
        panButtons.add(button, gridBagConstraints);
        
        return panButtons;
    }

    private JPanel createSidebar(ResourceBundle resourceMap) {
        JPanel panSidebar = new JPanel();
        panSidebar.setName("panButtons"); // NOI18N
        panSidebar.setLayout(new java.awt.GridLayout(6,1));
        
        choiceRanks.setName("choiceRanks"); // NOI18N
        refreshRanksCombo();
        choiceRanks.addActionListener(e -> changeRank());
        panSidebar.add(choiceRanks);
        
        JButton button = new JButton(resourceMap.getString("btnRandomName.text")); // NOI18N
        button.setName("btnRandomName"); // NOI18N
        button.addActionListener(e -> randomName());
        panSidebar.add(button);
  
        button = new JButton(resourceMap.getString("btnRandomPortrait.text")); // NOI18N
        button.setName("btnRandomPortrait"); // NOI18N
        button.addActionListener(e -> randomPortrait());
        panSidebar.add(button);
      
        button = new JButton(resourceMap.getString("btnChoosePortrait.text")); // NOI18N
        button.setName("btnChoosePortrait"); // NOI18N
        button.addActionListener(e -> choosePortrait());
        panSidebar.add(button);
      
        button = new JButton(resourceMap.getString("btnEditPerson.text")); // NOI18N
        button.setName("btnEditPerson"); // NOI18N
        button.addActionListener(e -> editPerson());
        button.setEnabled(campaign.isGM());
        panSidebar.add(button);
       
        button = new JButton(resourceMap.getString("btnRegenerate.text")); // NOI18N
        button.setName("btnRegenerate"); // NOI18N
        button.addActionListener(e -> regenerate());
        button.setEnabled(campaign.isGM());
        panSidebar.add(button);
        
        return panSidebar;
    }

    private void hire() {
    	if(campaign.recruitPerson(person)) {
        	person = campaign.newPerson(person.getPrimaryRole());
        	refreshRanksCombo();
        	campaign.changeRank(person, campaign.getRanks().getRankNumericFromNameAndProfession(person.getProfession(), (String)choiceRanks.getSelectedItem()), false);
    	}
        refreshView();
    }

    private void randomName() {
    	person.setName(campaign.getRNG().generate(person.getGender() == Person.G_FEMALE));
    	refreshView();
	}
    
    private void randomPortrait() {
    	campaign.assignRandomPortraitFor(person);
    	refreshView();
    }
    
    private void choosePortrait() {
    	ImageChoiceDialog pcd = new ImageChoiceDialog(frame, true,
				person.getPortraitCategory(),
				person.getPortraitFileName(), portraits);
		pcd.setVisible(true);
		person.setPortraitCategory(pcd.getCategory());
		person.setPortraitFileName(pcd.getFileName());
		refreshView();
    }
    
    private void editPerson() {
    	int gender = person.getGender();
    	CustomizePersonDialog npd = new CustomizePersonDialog(frame, true, 
				person, 
				campaign);
		npd.setVisible(true);
		if(gender != person.getGender()) {
			randomPortrait();
		}
		refreshRanksCombo();
    	refreshView();
    }
    
    private void regenerate() {
    	person = campaign.newPerson(person.getPrimaryRole());
    	refreshRanksCombo();
        refreshView();
    }
    
    private void changeRank() {
    	campaign.changeRank(person, campaign.getRanks().getRankNumericFromNameAndProfession(person.getProfession(), (String)choiceRanks.getSelectedItem()), false);
    	refreshView();
    }
   
    private void refreshRanksCombo() {
    	DefaultComboBoxModel<String> ranksModel = new DefaultComboBoxModel<String>();
    	
    	// Determine correct profession to pass into the loop
    	int profession = person.getProfession();
    	while (campaign.getRanks().isEmptyProfession(profession) && profession != Ranks.RPROF_MW) {
    		profession = campaign.getRanks().getAlternateProfession(profession);
    	}
    	
        for(Rank rank : campaign.getRanks().getAllRanks()) {
        	int p = profession;
        	// Grab rank from correct profession as needed
        	while (rank.getName(p).startsWith("--") && p != Ranks.RPROF_MW) {
            	if (rank.getName(p).equals("--")) {
            		p = campaign.getRanks().getAlternateProfession(p);
            	} else if (rank.getName(p).startsWith("--")) {
            		p = campaign.getRanks().getAlternateProfession(rank.getName(p));
            	}
        	}
        	if (rank.getName(p).equals("-")) {
        		continue;
        	}
        	
        	ranksModel.addElement(rank.getName(p));
        }
        choiceRanks.setModel(ranksModel);
        choiceRanks.setSelectedIndex(0);
    }
}
