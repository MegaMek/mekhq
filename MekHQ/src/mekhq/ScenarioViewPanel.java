/*
 * ScenarioViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.Force;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ScenarioViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Scenario scenario;
	private Campaign campaign;
	private Force forces;
	private MekHQView view;
	
	private javax.swing.JPanel pnlStats;
	private javax.swing.JTextArea txtDesc;
	private javax.swing.JTree forceTree;
	private javax.swing.JLabel lblStatus;
	
	private DefaultTreeModel forceModel;
	
	public ScenarioViewPanel(Scenario s, Campaign c, MekHQView v) {
		this.scenario = s;
		this.campaign = c;
		this.forces = s.getForces(campaign);
		this.view = v;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		forceTree = new javax.swing.JTree();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		if(forces.getAllPersonnel().size() > 0) {
			makeTree();
			forceTree.setModel(forceModel);
			//forceTree.addMouseListener(orgMouseAdapter);
			forceTree.setCellRenderer(view.new ForceRenderer());
			forceTree.setRowHeight(50);
			forceTree.setRootVisible(false);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			gridBagConstraints.gridheight = 1;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
			add(forceTree, gridBagConstraints);
		}
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(scenario.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		txtDesc.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Description"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtDesc, gridBagConstraints);
	}

    private void fillStats() {
    	
    	//org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(PersonViewPanel.class);
    	
    	lblStatus = new javax.swing.JLabel();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblStatus.setName("lblOwner"); // NOI18N
		lblStatus.setText("<html><b>" + scenario.getStatusName() + "</b></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus, gridBagConstraints);
		
    }
    
    private void makeTree() {
    	//traverse the force object and assign TreeNodes
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(forces);
		Enumeration<Force> subforces = forces.getSubForces().elements();
		while(subforces.hasMoreElements()) {
			Force subforce = subforces.nextElement();
			addForce(subforce, top);
		}
		//add any personnel
		Enumeration<Integer> personnel = forces.getPersonnel().elements();
		//put them into a temporary array so I can sort it by rank
		ArrayList<Person> people = new ArrayList<Person>();
		while(personnel.hasMoreElements()) {
			Person p = campaign.getPerson(personnel.nextElement());
			if(p != null) {
				people.add(p);
			}
		}
		Collections.sort(people, new Comparator<Person>(){		 
            public int compare(final Person p1, final Person p2) {
               return ((Comparable<Integer>)p2.getRank()).compareTo(p1.getRank());
            }
        });
		for(Person person : people) {
			top.add(new DefaultMutableTreeNode(person));
		}
		if(null == forceModel) {
			forceModel = new DefaultTreeModel(top);
		} else {
			forceModel.setRoot(top);
		}
    }
    
    private void addForce(Force force, DefaultMutableTreeNode top) {
		DefaultMutableTreeNode category = new DefaultMutableTreeNode(force);
		top.add(category);
		Enumeration<Force> subforces = force.getSubForces().elements();
		while(subforces.hasMoreElements()) {
			Force subforce = subforces.nextElement();
			addForce(subforce, category);
		}
		//add any personnel
		Enumeration<Integer> personnel = force.getPersonnel().elements();
		//put them into a temporary array so I can sort it by rank
		ArrayList<Person> people = new ArrayList<Person>();
		while(personnel.hasMoreElements()) {
			Person p = campaign.getPerson(personnel.nextElement());
			if(p != null) {
				people.add(p);
			}
		}
		Collections.sort(people, new Comparator<Person>(){		 
            public int compare(final Person p1, final Person p2) {
               return ((Comparable<Integer>)p2.getRank()).compareTo(p1.getRank());
            }
        });
		for(Person person : people) {
			category.add(new DefaultMutableTreeNode(person));
		}
    }
}