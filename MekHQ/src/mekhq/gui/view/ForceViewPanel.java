/*
 * ForceViewPanel
 *
 * Created on May 2, 2011
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.EntityImage;

/**
 * A custom panel that gets filled in with goodies from a Force record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ForceViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Force force;
	private Campaign campaign;
	
	private IconPackage icons;

	private javax.swing.JLabel lblIcon;
	private javax.swing.JPanel pnlStats;
	private javax.swing.JPanel pnlSubUnits;
	private javax.swing.JTextArea txtDesc;
	
	private javax.swing.JLabel lblType;
	private javax.swing.JLabel lblAssign1;
	private javax.swing.JLabel lblAssign2;
	private javax.swing.JLabel lblCommander1;
	private javax.swing.JLabel lblCommander2;
	private javax.swing.JLabel lblBV1;
	private javax.swing.JLabel lblBV2;
	private javax.swing.JLabel lblTonnage1;
	private javax.swing.JLabel lblTonnage2;
	private javax.swing.JLabel lblCost1;
	private javax.swing.JLabel lblCost2;
	private javax.swing.JLabel lblTech1;
	private javax.swing.JLabel lblTech2;
	
	
	public ForceViewPanel(Force f, Campaign c, IconPackage icons) {
		this.force = f;
		this.campaign = c;
		this.icons = icons;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		lblIcon = new javax.swing.JLabel();
		pnlStats = new javax.swing.JPanel();
		pnlSubUnits = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);
		
		lblIcon.setName("lblPortait"); // NOI18N
		lblIcon.setBackground(Color.WHITE);
		setIcon(force, lblIcon, 150);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(10,10,0,0);
		add(lblIcon, gridBagConstraints);
		
		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(force.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		pnlSubUnits.setName("pnlSubUnits");
		pnlSubUnits.setBackground(Color.WHITE);
		fillSubUnits();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlSubUnits, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(force.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		txtDesc.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Description"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtDesc, gridBagConstraints);
	}
	
	private void setIcon(Force f, JLabel lbl, int scale) {
        String category = f.getIconCategory();
        String file = f.getIconFileName();

        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == file) || Crew.PORTRAIT_NONE.equals(file)) {
        	file = "empty.png";
        }

        // Try to get the player's portrait file.
        Image portrait = null;        
        try {
            portrait = (Image) icons.getForceIcons().getItem(category, file);
            if(null != portrait) {
            	if(portrait.getWidth(lbl) > scale) { 
            		portrait = portrait.getScaledInstance(scale, -1, Image.SCALE_DEFAULT);  
            	}
            } else {
            	portrait = (Image) icons.getForceIcons().getItem("", "empty.png");
            }
            ImageIcon icon = new ImageIcon(portrait);
            lbl.setIcon(icon);
            if(icon.getIconWidth() > scale) {
            	portrait = portrait.getScaledInstance(scale, -1, Image.SCALE_DEFAULT);  
            	icon = new ImageIcon(portrait);
            	lbl.setIcon(icon);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
	}

	
	private void fillStats() {
		
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceViewPanel");

    	lblType = new javax.swing.JLabel();
    	lblAssign1 = new javax.swing.JLabel();
		lblAssign2 = new javax.swing.JLabel();
    	lblCommander1 = new javax.swing.JLabel();
		lblCommander2 = new javax.swing.JLabel();
		lblBV1 = new javax.swing.JLabel();
		lblBV2 = new javax.swing.JLabel();
		lblCost1 = new javax.swing.JLabel();
		lblCost2 = new javax.swing.JLabel();
		lblTonnage1 = new javax.swing.JLabel();
		lblTonnage2 = new javax.swing.JLabel();
		lblTech1 = new javax.swing.JLabel();
		lblTech2 = new javax.swing.JLabel();
		java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
	 	long bv = 0;
    	long cost = 0;
    	float ton = 0;
    	String commander = "";
    	String LanceTech = "";
    	String assigned = "";
    	String type = null;
    	ArrayList<Person> people = new ArrayList<Person>();
    	for(UUID uid : force.getAllUnits()) {
    		Unit u = campaign.getUnit(uid);
    		if(null != u) {
    			Person p = u.getCommander();
    			bv += u.getEntity().calculateBattleValue(true, !u.hasPilot());
    			cost += u.getEntity().getCost(true);
    			ton += u.getEntity().getWeight();
    			String utype = UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(u.getEntity()));
    			if(null == type) {
    				type = utype;
    			} else if(!utype.equals(type)) {
    				type = resourceMap.getString("mixed");
    			}
    			if(null != p) {
    				people.add(p);
    			}
    		}
    	}
 		//sort person vector by rank
 		Collections.sort(people, new Comparator<Person>(){		 
            public int compare(final Person p1, final Person p2) {
               return ((Comparable<Integer>)p2.getRankNumeric()).compareTo(p1.getRankNumeric());
            }
        });
    	if(people.size() > 0) {
    		commander = people.get(0).getFullTitle();
    	}
    	
    	if (null != force.getTechID()) {
    		Person p = campaign.getPerson(force.getTechID());
    		LanceTech = p.getName();
    	}
    	
    	if(null != force.getParentForce()) {
    		assigned = force.getParentForce().getName();
    	}
    	
    	DecimalFormat format = new DecimalFormat();
    	int nexty = 0;
    	
    	if(null != type) {
    		lblType.setName("lblCommander2"); // NOI18N
			lblType.setText("<html><i>" + type + " " + resourceMap.getString("unit")+ "</i></html>");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblType, gridBagConstraints);
			nexty++;
    	}
    	
    	if(!commander.equals("")) {
	    	lblCommander1.setName("lblCommander1"); // NOI18N
	    	lblCommander1.setText(resourceMap.getString("lblCommander1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblCommander1, gridBagConstraints);
			
			lblCommander2.setName("lblCommander2"); // NOI18N
			lblCommander2.setText(commander);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblCommander2, gridBagConstraints);
			nexty++;
    	}
		if (null != force.getTechID()) {
    		if (!LanceTech.equals("")) {
    			lblTech1.setName("lblTech1"); // NOI18N
    			lblTech1.setText(resourceMap.getString("lblTech1.text"));
    			gridBagConstraints = new java.awt.GridBagConstraints();
    			gridBagConstraints.gridx = 0;
    			gridBagConstraints.gridy = nexty;
    			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    			pnlStats.add(lblTech1, gridBagConstraints);
    			
    			lblTech2.setName("lblTech2"); // NOI18N
    			lblTech2.setText(LanceTech);
    			gridBagConstraints = new java.awt.GridBagConstraints();
    			gridBagConstraints.gridx = 1;
    			gridBagConstraints.gridy = nexty;
    			gridBagConstraints.weightx = 0.5;
    			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    			pnlStats.add(lblTech2, gridBagConstraints);
    			nexty++;
    			}
    	}
    	
    	if(!assigned.equals("")) {
	    	lblAssign1.setName("lblAssign1"); // NOI18N
	    	lblAssign1.setText(resourceMap.getString("lblAssign1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblAssign1, gridBagConstraints);
			
			lblAssign2.setName("lblAssign2"); // NOI18N
			lblAssign2.setText(assigned);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblAssign2, gridBagConstraints);
			nexty++;
    	}
    	
    	lblBV1.setName("lblBV1"); // NOI18N
    	lblBV1.setText(resourceMap.getString("lblBV1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBV1, gridBagConstraints);
		
		lblBV2.setName("lblBV2"); // NOI18N
		lblBV2.setText(format.format(bv));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBV2, gridBagConstraints);
		nexty++;
		
		lblTonnage1.setName("lblTonnage1"); // NOI18N
		lblTonnage1.setText(resourceMap.getString("lblTonnage1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTonnage1, gridBagConstraints);
		
		lblTonnage2.setName("lblTonnage2"); // NOI18N
		lblTonnage2.setText(format.format(ton));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTonnage2, gridBagConstraints);
		nexty++;
		
		lblCost1.setName("lblCost1"); // NOI18N
		lblCost1.setText(resourceMap.getString("lblCost1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCost1, gridBagConstraints);
		
		lblCost2.setName("lblCost2"); // NOI18N
		lblCost2.setText(format.format(cost) + " C-bills");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = nexty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCost2, gridBagConstraints);
		nexty++;
    	
		//BV
		//Tonnage?
		//Cost?
		//Number of units?
		//Assigned to
	}
	
	private void fillSubUnits() {
		
		java.awt.GridBagConstraints gridBagConstraints;

		pnlSubUnits.setLayout(new java.awt.GridBagLayout());
		
		JLabel lblForce;

		int nexty = 0;
		for(Force f : force.getSubForces()) {
			lblForce = new JLabel();
			lblForce.setText(getSummaryFor(f));
			setIcon(f, lblForce, 72);
			nexty++;
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlSubUnits.add(lblForce, gridBagConstraints);
		}
		JLabel lblPerson;
		JLabel lblUnit;		
		ArrayList<Unit> units = new ArrayList<Unit>();
		ArrayList<Unit> unmannedUnits = new ArrayList<Unit>();
 		for(UUID uid : force.getUnits()) {
			Unit u = campaign.getUnit(uid);
			if(null == u) {
				continue;
			}
			if(null == u.getCommander()) {
				unmannedUnits.add(u);
			} else {
				units.add(u);
			}
 		}
 		//sort person vector by rank
 		Collections.sort(units, new Comparator<Unit>(){		 
            public int compare(final Unit u1, final Unit u2) {
               return ((Comparable<Integer>)u2.getCommander().getRankNumeric()).compareTo(u1.getCommander().getRankNumeric());
            }
        });
 		for(Unit u : unmannedUnits) {
 			units.add(u);
 		}
 		for(Unit u : units) {
 			Person p = u.getCommander();
 			lblPerson = new JLabel();
			lblUnit = new JLabel();
			if(null != p) {
				lblPerson.setText(getSummaryFor(p, u));
				setPortrait(p, lblPerson);
			}
			nexty++;
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlSubUnits.add(lblPerson, gridBagConstraints);
			if(null != u) {
				lblUnit.setText(getSummaryFor(u));
				lblUnit.setIcon(new ImageIcon(getImageFor(u, lblUnit)));			
			}
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlSubUnits.add(lblUnit, gridBagConstraints);
		}
	}
	
	/**
     * set the portrait for the given person.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value
     *         will be <code>null</code> if no portrait was selected
     *          or if there was an error loading it.
     */
    public void setPortrait(Person p, JLabel lbl) {

        String category = p.getPortraitCategory();
        String file = p.getPortraitFileName();

        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == file) || Crew.PORTRAIT_NONE.equals(file)) {
        	file = "default.gif";
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) icons.getPortraits().getItem(category, file);
            if(null != portrait) {
                portrait = portrait.getScaledInstance(72, -1, Image.SCALE_DEFAULT);               
            } else {
            	portrait = (Image) icons.getPortraits().getItem("", "default.gif");
            	if(null != portrait) {
                    portrait = portrait.getScaledInstance(72, -1, Image.SCALE_DEFAULT);               
            	}
            }
            lbl.setIcon(new ImageIcon(portrait));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    private Image getImageFor(Unit u, Component c) {
        
		if(null == icons.getMechTiles()) { 
			return null;
		}
        Image base = icons.getMechTiles().imageFor(u.getEntity(), c, -1);
        int tint = PlayerColors.getColorRGB(u.campaign.getColorIndex());
        EntityImage entityImage = new EntityImage(base, tint, getCamo(u), c);
        return entityImage.loadPreviewImage();
    }
    
    private Image getCamo(Unit unit) {
        // Try to get the player's camo file.
        Image camo = null;
        try {
            camo = (Image) icons.getCamos().getItem(unit.getCamoCategory(), unit.getCamoFileName());
        } catch (Exception err) {
            err.printStackTrace();
        }
        return camo;
    }
    
    public String getSummaryFor(Person person, Unit unit) {
        String toReturn = "<html><font size='2'><b>" + person.getFullTitle() + "</b><br/>";
        toReturn += person.getSkillSummary() + " " + person.getRoleDesc();
        if(null != unit && null != unit.getEntity()
        		&& null != unit.getEntity().getCrew() && unit.getEntity().getCrew().getHits() > 0) {
        	toReturn += "<br><font color='red' size='2'>" + unit.getEntity().getCrew().getHits() + " hit(s)";
        }
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public String getSummaryFor(Unit unit) {
        String toReturn = "<html><font size='2'><b>" + unit.getName() + "</b><br/>";
        toReturn += "<b>BV:</b> " + unit.getEntity().calculateBattleValue(true, null == unit.getEntity().getCrew()) + "<br/>";
        toReturn += unit.getStatus();
        Entity entity = unit.getEntity();
    	if (entity.hasC3i()) {
    		toReturn += "<br><i>";
            if (entity.calculateFreeC3Nodes() >= 5) {
            	toReturn += Messages.getString("ChatLounge.C3iNone");
            } else {
            	toReturn += Messages
                        .getString("ChatLounge.C3iNetwork")
                        + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                	toReturn += Messages.getString("ChatLounge.C3Nodes",
                            new Object[] { entity.calculateFreeC3Nodes() });
                }
            }
    		toReturn += "</i>";
        }
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public String getSummaryFor(Force f) {
    	//we are not going to use the campaign methods here because we can be more efficient
    	//by only traversing once
    	int bv = 0;
    	int cost = 0;
    	float ton = 0;
    	int number = 0;
    	String commander = "No personnel found";
    	ArrayList<Person> people = new ArrayList<Person>();
    	for(UUID uid : f.getAllUnits()) {
    		Unit u = campaign.getUnit(uid);
    		if(null != u) {
    			Person p = u.getCommander();
    			number++;
                if (p != null) {
                    bv += u.getEntity().calculateBattleValue(true, false);
                } else {
                    bv += u.getEntity().calculateBattleValue(true, true);
                }
    			cost += u.getEntity().getCost(true);
    			ton += u.getEntity().getWeight();
    			if(null != p) {
    				people.add(p);
    			}
    		}
    	}
 		//sort person vector by rank
 		Collections.sort(people, new Comparator<Person>(){		 
            public int compare(final Person p1, final Person p2) {
               return ((Comparable<Integer>)p2.getRankNumeric()).compareTo(p1.getRankNumeric());
            }
        });
    	if(people.size() > 0) {
    		commander = people.get(0).getFullTitle();
    	}
    	DecimalFormat format = new DecimalFormat();
        String toReturn = "<html><font size='2'><b>" + f.getName() + "</b> (" + commander + ")<br/>";
        toReturn += "<b>Number of Units:</b> " + number + "<br/>";
        toReturn += bv + " BV, ";
        toReturn += format.format(ton) + " tons, ";
        toReturn += format.format(cost) + " C-bills";
        toReturn += "</font></html>";
        return toReturn;
    }

}
	