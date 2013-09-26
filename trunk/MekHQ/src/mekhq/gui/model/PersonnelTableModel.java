package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.common.options.PilotOptions;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.CampaignGUI;

    /**
     * A table Model for displaying information about personnel
     * @author Jay lawson
     */
    public class PersonnelTableModel extends DataTableModel {

        private static final long serialVersionUID = -5207167419079014157L;

        private Campaign campaign;
        
        public final static int COL_RANK     =    0;
        public final static int COL_NAME     =    1;
        public final static int COL_CALL     =    2;
        public final static int COL_AGE     =     3;
        public final static int COL_GENDER     =  4;
        public final static int COL_SKILL     =   5;
        public final static int COL_TYPE     =    6;
        public final static int COL_ASSIGN     =  7;
        public final static int COL_FORCE      =  8;
        public final static int COL_DEPLOY     =  9;
        public final static int COL_MECH =       10;
        public final static int COL_AERO =       11;
        public final static int COL_JET =        12;
        public final static int COL_VEE =        13;
        public final static int COL_VTOL       = 14;
        public final static int COL_NVEE       = 15;
        public final static int COL_SPACE     =  16;
        public final static int COL_ARTY     =   17;
        public final static int COL_GUN_BA     = 18;
        public final static int COL_SMALL_ARMS = 19;
        public final static int COL_ANTI_MECH  = 20;
        public final static int COL_TACTICS    = 21;
        public final static int COL_STRATEGY   = 22;
        public final static int COL_TECH_MECH  = 23;
        public final static int COL_TECH_AERO  = 24;
        public final static int COL_TECH_VEE   = 25;
        public final static int COL_TECH_BA    = 26;
        public final static int COL_MEDICAL    = 27;
        public final static int COL_ADMIN      = 28;
        public final static int COL_NEG        = 29;
        public final static int COL_SCROUNGE   = 30;
        public final static int COL_TOUGH =   31;
        public final static int COL_EDGE  =   32;
        public final static int COL_NABIL =   33;
        public final static int COL_NIMP  =   34;
        public final static int COL_HITS  =   35;
        public final static int COL_KILLS  =  36;
        public final static int COL_SALARY =  37;
        public final static int COL_XP =      38;
        public final static int N_COL =       39;

        public PersonnelTableModel(Campaign c) {
            data = new ArrayList<Person>();
            campaign = c;
        }
 
        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case COL_RANK:
                    return "Rank";
                case COL_NAME:
                    return "Name";
                case COL_CALL:
                    return "Callsign";
                case COL_AGE:
                    return "Age";
                case COL_GENDER:
                    return "Gender";
                case COL_TYPE:
                    return "Role";
                case COL_MECH:
                    return "Mech";
                case COL_AERO:
                    return "Aero";
                case COL_JET:
                    return "Aircraft";
                case COL_VEE:
                    return "Vehicle";
                case COL_VTOL:
                    return "VTOL";
                case COL_NVEE:
                    return "Naval";
                case COL_SPACE:
                    return "Spacecraft";
                case COL_ARTY:
                    return "Artillery";
                case COL_GUN_BA:
                    return "G/Battlesuit";
                case COL_SMALL_ARMS:
                    return "Small Arms";
                case COL_ANTI_MECH:
                    return "Anti-Mech";
                case COL_TACTICS:
                    return "Tactics";
                case COL_STRATEGY:
                    return "Strategy";
                case COL_TECH_MECH:
                    return "Tech/Mech";
                case COL_TECH_AERO:
                    return "Tech/Aero";
                case COL_TECH_VEE:
                    return "Mechanic";
                case COL_TECH_BA:
                    return "Tech/BA";
                case COL_MEDICAL:
                    return "Medical";
                case COL_ADMIN:
                    return "Admin";
                case COL_NEG:
                    return "Negotiation";
                case COL_SCROUNGE:
                    return "Scrounge";
                case COL_TOUGH:
                    return "Toughness";
                case COL_SKILL:
                    return "Skill Level";
                case COL_ASSIGN:
                    return "Unit Assignment";
                case COL_EDGE:
                    return "Edge";
                case COL_NABIL:
                    return "# Abilities";
                case COL_NIMP:
                    return "# Implants";
                case COL_HITS:
                    return "Hits";
                case COL_XP:
                    return "XP";
                case COL_DEPLOY:
                    return "Deployed";
                case COL_FORCE:
                    return "Force";
                case COL_SALARY:
                    return "Salary";
                case COL_KILLS:
                    return "Kills";
                default:
                    return "?";
            }
        }

        public int getColumnWidth(int c) {
            switch(c) {
            case COL_RANK:
            case COL_DEPLOY:
                return 70;
            case COL_CALL:
            case COL_SALARY:
            case COL_SKILL:
                return 50;
            case COL_TYPE:
            case COL_FORCE:
                return 100;
            case COL_NAME:
            case COL_ASSIGN:
                return 125;
            default:
                return 20;
            }
        }

        public int getAlignment(int col) {
            switch(col) {
            case COL_SALARY:
                return SwingConstants.RIGHT;
            case COL_RANK:
            case COL_NAME:
            case COL_GENDER:
            case COL_TYPE:
            case COL_DEPLOY:
            case COL_FORCE:
            case COL_ASSIGN:
            case COL_SKILL:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
            }
        }

        public String getTooltip(int row, int col) {
            Person p = getPerson(row);
            switch(col) {
            case COL_NABIL:
                return p.getAbilityList(PilotOptions.LVL3_ADVANTAGES);
            case COL_NIMP:
                return p.getAbilityList(PilotOptions.MD_ADVANTAGES);
            case COL_ASSIGN:
                if(p.getTechUnitIDs().size() > 1) {
                    String toReturn = "<html>";
                    for(UUID id : p.getTechUnitIDs()) {
                        Unit u = getCampaign().getUnit(id);
                        if(null != u) {
                            toReturn += u.getName() + "<br>";
                        }
                    }
                    toReturn += "</html>";
                    return toReturn;
                } else {
                    return null;
                }
            default:
                return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Person getPerson(int i) {
            if( i >= data.size()) {
                return null;
            }
            return (Person)data.get(i);
        }

        public boolean isDeployed(int row) {
            return getPerson(row).isDeployed();
        }

        public Object getValueAt(int row, int col) {
            Person p;
            DecimalFormat formatter = new DecimalFormat();
            if(data.isEmpty()) {
                return "";
            } else {
                p = getPerson(row);
            }
            if(col == COL_RANK) {
                return p.getRank().getName();
            }
            if(col == COL_NAME) {
                return p.getName();
            }
            if(col == COL_CALL) {
                return p.getCallsign();
            }
            if(col == COL_GENDER) {
                return p.getGenderName();
            }
            if(col == COL_AGE) {
                return p.getAge(getCampaign().getCalendar());
            }
            if(col == COL_TYPE) {
                return p.getRoleDesc();
            }
            if(col == COL_MECH) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_MECH)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_MECH)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;
            }
            if(col == COL_AERO) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_AERO)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_AERO)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_JET) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_JET)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_JET).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_JET)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_JET).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_SPACE) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_SPACE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_SPACE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_VEE) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_GVEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_NVEE) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_NVEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_NVEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_VTOL) {
                String toReturn = "";
                if(p.hasSkill(SkillType.S_GUN_VEE)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                toReturn += "/";
                if(p.hasSkill(SkillType.S_PILOT_VTOL)) {
                    toReturn += Integer.toString(p.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue());
                } else {
                    toReturn += "-";
                }
                return toReturn;

            }
            if(col == COL_GUN_BA) {
                if(p.hasSkill(SkillType.S_GUN_BA)) {
                    return Integer.toString(p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_ANTI_MECH) {
                if(p.hasSkill(SkillType.S_ANTI_MECH)) {
                    return Integer.toString(p.getSkill(SkillType.S_ANTI_MECH).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_SMALL_ARMS) {
                if(p.hasSkill(SkillType.S_SMALL_ARMS)) {
                    return Integer.toString(p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_ARTY) {
                if(p.hasSkill(SkillType.S_ARTILLERY)) {
                    return Integer.toString(p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TACTICS) {
                if(p.hasSkill(SkillType.S_TACTICS)) {
                    return Integer.toString(p.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_STRATEGY) {
                if(p.hasSkill(SkillType.S_STRATEGY)) {
                    return Integer.toString(p.getSkill(SkillType.S_STRATEGY).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TECH_MECH) {
                if(p.hasSkill(SkillType.S_TECH_MECH)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_MECH).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TECH_AERO) {
                if(p.hasSkill(SkillType.S_TECH_AERO)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_AERO).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TECH_VEE) {
                if(p.hasSkill(SkillType.S_TECH_MECHANIC)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TECH_BA) {
                if(p.hasSkill(SkillType.S_TECH_BA)) {
                    return Integer.toString(p.getSkill(SkillType.S_TECH_BA).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_MEDICAL) {
                if(p.hasSkill(SkillType.S_DOCTOR)) {
                    return Integer.toString(p.getSkill(SkillType.S_DOCTOR).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_ADMIN) {
                if(p.hasSkill(SkillType.S_ADMIN)) {
                    return Integer.toString(p.getSkill(SkillType.S_ADMIN).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_NEG) {
                if(p.hasSkill(SkillType.S_NEG)) {
                    return Integer.toString(p.getSkill(SkillType.S_NEG).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_SCROUNGE) {
                if(p.hasSkill(SkillType.S_SCROUNGE)) {
                    return Integer.toString(p.getSkill(SkillType.S_SCROUNGE).getFinalSkillValue());
                } else {
                    return "-";
                }
            }
            if(col == COL_TOUGH) {
                return "?";
            }
            if(col == COL_EDGE) {
                return Integer.toString(p.getEdge());
            }
            if(col == COL_NABIL) {
                return Integer.toString(p.countOptions(PilotOptions.LVL3_ADVANTAGES));
            }
            if(col == COL_NIMP) {
                return Integer.toString(p.countOptions(PilotOptions.MD_ADVANTAGES));

            }
            if(col == COL_HITS) {
                return p.getHits();
            }
            if(col == COL_SKILL) {
                return p.getSkillSummary();
            }
            if(col == COL_ASSIGN) {
                Unit u = getCampaign().getUnit(p.getUnitId());
                if(null != u) {
                    String name = u.getName();
                    if(u.getEntity() instanceof Tank) {
                        if(u.isDriver(p)) {
                            name = name + " [Driver]";
                        } else {
                            name = name + " [Gunner]";
                        }
                    }
                    if(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                        if(u.isNavigator(p)) {
                            name = name + " [Navigator]";
                        }
                        else if(u.isDriver(p)) {
                            name =  name + " [Pilot]";
                        }
                        else if(u.isGunner(p)) {
                            name = name + " [Gunner]";
                        } else {
                            name = name + " [Crew]";
                        }
                    }
                    return name;
                }
                //check for tech
                if(!p.getTechUnitIDs().isEmpty()) {
                    if(p.getTechUnitIDs().size() == 1) {
                        u = getCampaign().getUnit(p.getTechUnitIDs().get(0));
                        if(null != u) {
                            return u.getName() + " (" + p.getMaintenanceTimeUsing() + "m)";
                        }
                    } else {
                        return "" + p.getTechUnitIDs().size() + " units (" + p.getMaintenanceTimeUsing() + "m)";
                    }
                }             
                return "-";
            }
            if(col == COL_XP) {
                return p.getXp();
            }
            if(col == COL_DEPLOY) {
                Unit u = getCampaign().getUnit(p.getUnitId());
                if(null != u && u.isDeployed()) {
                    return getCampaign().getScenario(u.getScenarioId()).getName();
                } else {
                    return "-";
                }
            }
            if(col == COL_FORCE) {
                Force force = getCampaign().getForceFor(p);
                if(null != force) {
                    return force.getName();
                } else {
                    return "None";
                }
            }
            if(col == COL_SALARY) {
                return formatter.format(p.getSalary());
            }
            if(col == COL_KILLS) {
                return getCampaign().getKillsFor(p.getId()).size();
            }
            return "?";
        }
        
        private Campaign getCampaign() {
            return campaign;
        }
        
        public void refreshData() {
            setData(getCampaign().getPersonnel());
        }

        public TableCellRenderer getRenderer(boolean graphic, IconPackage icons) {
            if(graphic) {
                return new PersonnelTableModel.VisualRenderer(icons);
            }
            return new PersonnelTableModel.Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {

            private static final long serialVersionUID = 9054581142945717303L;

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                setForeground(Color.BLACK);
                if (isSelected) {
                    setBackground(Color.DARK_GRAY);
                    setForeground(Color.WHITE);
                } else {
                    // tiger stripes
                    if (isDeployed(actualRow)) {
                        setBackground(Color.LIGHT_GRAY);
                    } else if((Integer)getValueAt(actualRow,COL_HITS) > 0 || getPerson(actualRow).hasInjuries(true)) {
                        setBackground(Color.RED);
                    } else {
                        setBackground(Color.WHITE);
                    }
                }
                return this;
            }

        }
        
        public class VisualRenderer extends BasicInfo implements TableCellRenderer {

            private static final long serialVersionUID = -9154596036677641620L;

            public VisualRenderer(IconPackage icons) {
                super(icons);
            }
            
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = this;
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setText(getValueAt(actualRow, actualCol).toString(), "black");
                Person p = getPerson(actualRow);
                String color = "black";
                if(isSelected) {
                    color = "white";
                }
                if (actualCol == COL_RANK) {
                    setPortrait(p);
                    setText(p.getFullDesc(), color);
                }
                if(actualCol == COL_ASSIGN) {
                    Unit u = getCampaign().getUnit(p.getUnitId());
                    if(!p.getTechUnitIDs().isEmpty()) {
                        u = getCampaign().getUnit(p.getTechUnitIDs().get(0));
                    }
                    if(null != u) {
                        String desc = "<html><b>" + u.getName() + "</b><br>";
                        desc += u.getEntity().getWeightClassName();
                        if(!(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship)) {
                            desc += " " + UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(u.getEntity()));
                        }
                        desc += "<br>" + u.getStatus() + "</html>";
                        setText(desc, color);
                        Image mekImage = getImageFor(u);
                        if(null != mekImage) {
                            setImage(mekImage);
                        } else {
                            clearImage();
                        }
                    } else {
                        clearImage();
                    }
                }
                if(actualCol == COL_FORCE) {
                    Force force = getCampaign().getForceFor(p);
                    if(null != force) {
                        String desc = "<html><b>" + force.getName() + "</b>";
                        Force parent = force.getParentForce();
                        //cut off after three lines and don't include the top level
                        int lines = 1;
                        while(parent != null && null != parent.getParentForce() && lines < 4) {
                            desc += "<br>" + parent.getName();
                            lines++;
                            parent = parent.getParentForce();
                        }
                        desc += "</html>";
                        setText(desc, color);
                        Image forceImage = getImageFor(force);
                        if(null != forceImage) {
                            setImage(forceImage);
                        } else {
                            clearImage();
                        }
                    } else {
                        clearImage();
                    }
                }
                if(actualCol == COL_HITS) {
                    Image hitImage = getHitsImage(p.getHits());
                    if(null != hitImage) {
                        setImage(hitImage);
                        setText("", color);
                    } else {
                        clearImage();
                        setText("", color);
                    }
                }
                
                if (isSelected) {
                    c.setBackground(Color.DARK_GRAY);
                } else {
                    // tiger stripes
                    if ((row % 2) == 0) {
                        c.setBackground(new Color(220, 220, 220));
                    } else {
                        c.setBackground(Color.WHITE);
                        
                    }
                }
                return c;
            }
            
            private Image getHitsImage(int hits) {
                switch(hits) {
                case 1:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/onehit.png");
                case 2:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/twohits.png");
                case 3:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/threehits.png");
                case 4:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/fourhits.png");
                case 5:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/fivehits.png");
                case 6:
                    return Toolkit.getDefaultToolkit().getImage("data/images/misc/hits/sixhits.png");
                }
                return null;
            }
        }
    }