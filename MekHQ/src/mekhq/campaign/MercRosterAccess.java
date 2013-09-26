package mekhq.campaign;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import javax.swing.SwingWorker;

import megamek.common.UnitType;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Rank;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.Kill;

public class MercRosterAccess extends SwingWorker<Void, Void> {
   
    Campaign campaign;
    String username;
    String hostname;
    String passwd;
    String table;
    int port;
    private Statement statement = null;
    private Connection connect = null;
    private PreparedStatement preparedStatement = null;
    private Properties conProperties;

    //we also need some hashes to cross-reference stuff by id
    HashMap<String, Integer> skillHash;
    HashMap<UUID, Integer> personHash;
    HashMap<UUID, Integer> forceHash;

    //to track progress
    private String progressNote;
    private int progressTicker;
    
    public MercRosterAccess(String h, int port, String t, String u, String p, Campaign c) {
      username = u;
      hostname = h;
      table = t;
      this.port = port;
      passwd = p;
      campaign = c;
      skillHash = new HashMap<String, Integer>();
      personHash = new HashMap<UUID, Integer>();
      forceHash = new HashMap<UUID, Integer>();
      progressNote = "";
      progressTicker = 0;
    }
  
    public void connect() throws SQLException {
    	conProperties = new Properties();
    	conProperties.put("user", username);
    	conProperties.put("password", passwd);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + table, conProperties);
        } catch (SQLException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writeCampaignData() {
        
        //TODO throw all SQLExceptions - but then where do they go from doInBackground?
        try {
            statement = connect.createStatement();
        } catch (SQLException e2) {
            e2.printStackTrace();
        };
        
        writeBasicData();
        writeForceData();
        writePersonnelData();
        writeEquipmentData();
        //TODO: writeContractData
        //TODO: write logs?
        
        // Needed because otherwise progress isn't reaching 100 and the progress meter stays open
        setProgress(100);

    }
    
    private void writeBasicData() {
        
        try {
            preparedStatement = connect.prepareStatement("UPDATE " + table + ".command SET name=? where id=1");
            preparedStatement.setString(1, truncateString(campaign.getName(), 100));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } 
        
        progressNote = "Uploading dates";
        determineProgress();
        //write dates
        try {
            preparedStatement = connect.prepareStatement("UPDATE " + table + ".dates SET currentdate=?");
            preparedStatement.setDate(1, new java.sql.Date(campaign.getCalendar().getTimeInMillis()));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        progressTicker++;
        progressNote = "Uploading ranks";
        determineProgress();
        //write ranks
        try {
            statement.execute("TRUNCATE TABLE " + table + ".ranks");
            int i = 0;
            for(Rank rank : campaign.getRanks().getAllRanks()) {
                preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".ranks (number, rankname) VALUES (?, ?)");
                preparedStatement.setInt(1, i);
                preparedStatement.setString(2, truncateString(rank.getName(), 45));
                preparedStatement.executeUpdate();
                i++;
                progressTicker++;
                determineProgress();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //write skill types
        progressNote = "Uploading skill types";
        determineProgress();
        try {
            statement.execute("TRUNCATE TABLE " + table + ".skilltypes");
            for(int i = 0; i < SkillType.skillList.length; i++) {
                preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skilltypes (name, shortname) VALUES (?, ?)");
                preparedStatement.setString(1, truncateString(SkillType.skillList[i], 60));
                preparedStatement.setString(2, truncateString(getShortSkillName(SkillType.skillList[i]), 60));
                preparedStatement.executeUpdate();
                skillHash.put(SkillType.skillList[i], i+1);
                progressTicker++;
                determineProgress();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //write crewtypes
        progressNote = "Uploading personnel types";
        determineProgress();
        //TODO: get correct vehicle types and squad status
        try {
            statement.execute("TRUNCATE TABLE " + table + ".crewtypes");
            statement.execute("TRUNCATE TABLE " + table + ".skillrequirements");
            for(int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
                //write skill requirements
                int equipment = 0;
                switch(i) {
                case Person.T_MECHWARRIOR:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_MECH));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_MECH));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_AERO_PILOT:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_AERO));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_AERO));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_GVEE_DRIVER:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_GVEE));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_NVEE_DRIVER:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_NVEE));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_VTOL_PILOT:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_VTOL));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_VEE_GUNNER:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_VEE));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_BA:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_BA));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_INFANTRY:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_SMALL_ARMS));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_CONV_PILOT:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_JET));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_JET));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_SPACE_PILOT:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_PILOT_SPACE));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_SPACE_GUNNER:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_GUN_SPACE));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_SPACE_CREW:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_TECH_VESSEL));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_NAVIGATOR:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_NAV));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    equipment = 1;
                    break;
                case Person.T_MECH_TECH:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_TECH_MECH));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                case Person.T_AERO_TECH:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_TECH_AERO));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                case Person.T_MECHANIC:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_TECH_MECHANIC));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                case Person.T_BA_TECH:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_TECH_BA));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                case Person.T_DOCTOR:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_DOCTOR));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                case Person.T_ADMIN_COM:
                case Person.T_ADMIN_HR:
                case Person.T_ADMIN_LOG:
                case Person.T_ADMIN_TRA:
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skillrequirements (skilltype, personneltype) VALUES (?, ?)");
                    preparedStatement.setInt(1, skillHash.get(SkillType.S_ADMIN));
                    preparedStatement.setInt(2, i);
                    preparedStatement.executeUpdate();
                    break;
                }
                preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".crewtypes (type, squad, vehicletype, prefpos, equipment) VALUES (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, truncateString(Person.getRoleDesc(i, campaign.getFaction().isClan()), 45));
                preparedStatement.setInt(2, 0);
                preparedStatement.setInt(3, 1);
                preparedStatement.setInt(4, i);
                preparedStatement.setInt(5, equipment);
                preparedStatement.executeUpdate();
                progressTicker += 2;
                determineProgress();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        //write equipment types
        progressNote = "Uploading equipment types";
        determineProgress();
        try {
            statement.execute("TRUNCATE TABLE " + table + ".equipmenttypes");
            for(int i = 0; i < UnitType.SIZE; i++) {
                int maxweight = 100;
                int minweight = 20;
                int weightstep = 5;
                String weightscale = "ton";
                //TODO: get these right for various unit types
                preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".equipmenttypes (id, name, license, maxweight, minweight, weightstep, weightscale, prefpos, used, requirement) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                preparedStatement.setInt(1, i+1);
                preparedStatement.setString(2, truncateString(UnitType.getTypeDisplayableName(i), 45));
                preparedStatement.setInt(3, i+1);
                preparedStatement.setInt(4, maxweight);
                preparedStatement.setInt(5, minweight);
                preparedStatement.setInt(6, weightstep);
                preparedStatement.setString(7, weightscale);
                preparedStatement.setInt(8, i+1);
                preparedStatement.setInt(9, 1);
                preparedStatement.setInt(10, 1);
                preparedStatement.executeUpdate();
                progressTicker += 1;
                determineProgress();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void writeForceData() {
        
        //clear the table and re-enter a top level command
        try {
            statement.execute("TRUNCATE TABLE " + table + ".unit");  
            preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".unit (type, name, parent, prefpos, text) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, "1");
            preparedStatement.setString(2, "Command");
            preparedStatement.setInt(3, Integer.MAX_VALUE);
            preparedStatement.setInt(4, 0);
            preparedStatement.setString(5, campaign.getForces().getDescription());
            preparedStatement.executeUpdate();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        progressNote = "Uploading force data";
        determineProgress();
        //TODO: top level force gets written to ?WHERE?
        //now iterate through subforces
        for(Force sub : campaign.getForces().getSubForces()) {
            writeForce(sub, 1);
        }
        for(UUID uid : campaign.getForces().getUnits()) {
            Unit u = campaign.getUnit(uid);
            if(u != null && u.getCommander() != null) {
                forceHash.put(u.getCommander().getId(), 1);
            }
        }
    }
    
    private void writeForce(Force force, int parent) {
        
        try {           
            preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".unit (type, name, parent, prefpos, text) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, "1");
            preparedStatement.setString(2, force.getName());
            preparedStatement.setInt(3, parent);
            preparedStatement.setInt(4, 1);
            preparedStatement.setString(5, force.getDescription());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } 
        //retrieve the mercroster id of this force
        ResultSet rs;
        int id = parent;
        try {
            rs = statement.executeQuery("SELECT id FROM " + table + ".unit ORDER BY id DESC LIMIT 1");
            rs.next();
            id = rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        progressTicker += 2;
        determineProgress();
        //loop through subforces and call again
        for(Force sub : force.getSubForces()) {
            writeForce(sub, id);
        }
        //assign personnel uuids to hash

        for(UUID uid : force.getUnits()) {
            Unit u = campaign.getUnit(uid);
            if(u != null && u.getCommander() != null) {
                forceHash.put(u.getCommander().getId(), id);
            }
        }

    }

    private void writePersonnelData() {
       
        
        //check for a uuid column
        try {
            //add in a UUID column if not already present
            if(!hasColumn(statement.executeQuery("SELECT * FROM " + table + ".crew"), "uuid")) {
                statement.execute("TRUNCATE TABLE " + table + ".crew");
                statement.execute("ALTER TABLE " + table + ".crew ADD uuid VARCHAR(40)");
            }             
            statement.execute("TRUNCATE TABLE " + table + ".personnelpositions");
            statement.execute("TRUNCATE TABLE " + table + ".skills");
            statement.execute("TRUNCATE TABLE " + table + ".kills");

        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        progressNote = "Uploading personnel data";
        determineProgress();
        for(Person p: campaign.getPersonnel()) {
            int forceId = 0;
            //assign parent id from force hash
            if(null != forceHash.get(p.getId())) {
                forceId = forceHash.get(p.getId());
            }
            try {
                preparedStatement = connect.prepareStatement("UPDATE " + table + ".crew SET rank=?, lname=?, fname=?, callsign=?, status=?, parent=?, crewnumber=?, joiningdate=?, notes=?, bday=? WHERE uuid=?"); 
                preparedStatement.setInt(1, p.getRankOrder());
                preparedStatement.setString(2, truncateString(parseLastName(p.getName()),30));
                preparedStatement.setString(3, truncateString(parseFirstName(p.getName()), 30));
                preparedStatement.setString(4, truncateString(p.getCallsign(), 30));
                preparedStatement.setString(5, getMercRosterStatusName(p.getStatus()));
                preparedStatement.setInt(6, forceId);
                preparedStatement.setInt(7, 1);
                //TODO: get joining date right
                preparedStatement.setDate(8, new java.sql.Date(p.getBirthday().getTimeInMillis()));
                //TODO: combine personnel log with biography
                preparedStatement.setString(9, p.getBiography());
                preparedStatement.setDate(10, new java.sql.Date(p.getBirthday().getTimeInMillis()));
                preparedStatement.setString(11, p.getId().toString());
                if(preparedStatement.executeUpdate() < 1) {
                    //no prior record so insert
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".crew (rank, lname, fname, callsign, status, parent, crewnumber, joiningdate, notes, bday, uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    preparedStatement.setInt(1, p.getRankOrder());
                    preparedStatement.setString(2, truncateString(parseLastName(p.getName()),30));
                    preparedStatement.setString(3, truncateString(parseFirstName(p.getName()), 30));
                    preparedStatement.setString(4, truncateString(p.getCallsign(), 30));
                    preparedStatement.setString(5, getMercRosterStatusName(p.getStatus()));
                    preparedStatement.setInt(6, forceId);
                    preparedStatement.setInt(7, 1);
                    preparedStatement.setDate(8, new java.sql.Date(p.getBirthday().getTimeInMillis()));
                    preparedStatement.setString(9, p.getBiography());
                    preparedStatement.setDate(10, new java.sql.Date(p.getBirthday().getTimeInMillis()));
                    preparedStatement.setString(11, p.getId().toString());
                    preparedStatement.executeUpdate();
                }
                //retrieve the mercroster id of this person
                preparedStatement = connect.prepareStatement("SELECT id FROM " + table + ".crew WHERE uuid=?");
                preparedStatement.setString(1, p.getId().toString());
                ResultSet rs = preparedStatement.executeQuery();
                rs.next();
                int id = rs.getInt("id");
                //put id in a hash for equipment assignment
                personHash.put(p.getId(), id);
                //assign the personnel position
                preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".personnelpositions (personneltype, person) VALUES (?, ?)");
                preparedStatement.setInt(1, (p.getPrimaryRole()));
                preparedStatement.setInt(2, id);
                preparedStatement.executeUpdate();
                //write out skills to skills table
                for(int i = 0; i < SkillType.skillList.length; i++) {
                    if(p.hasSkill(SkillType.skillList[i])) {
                        Skill skill = p.getSkill(SkillType.skillList[i]);
                        preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".skills (person, skill, value) VALUES (?, ?, ?)");
                        preparedStatement.setInt(1, id);
                        preparedStatement.setInt(2, i+1);
                        preparedStatement.setInt(3, skill.getFinalSkillValue());
                        preparedStatement.executeUpdate();
                    }
                }
                //add kills
                //FIXME: the only issue here is we get duplicate kills for crewed vehicles
                //TODO: clean up the getWhatKilled string
                for(Kill k : campaign.getKillsFor(p.getId())) {
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".kills (parent, type, killdate, equipment) VALUES (?, ?, ?, ?)");
                    preparedStatement.setInt(1, id);
                    preparedStatement.setString(2, truncateString(k.getWhatKilled(), 45));
                    preparedStatement.setDate(3, new java.sql.Date(k.getDate().getTime()));
                    preparedStatement.setString(4, truncateString(k.getKilledByWhat(), 45));
                    preparedStatement.executeUpdate();
                }
                progressTicker += 4;
                determineProgress();
            } catch (SQLException e) {
                e.printStackTrace();
            }          
        }
    }
    
    private void writeEquipmentData() {
        //TODO: we need to clear the equipment table because equipment will come and go
        
        //check for a uuid column
        try {
            //add in a UUID column if not already present
            if(!hasColumn(statement.executeQuery("SELECT * FROM " + table + ".equipment"), "uuid")) {
                statement.execute("ALTER TABLE " + table + ".equipment ADD uuid VARCHAR(40)");
            }             
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        progressNote = "Uploading equipment data";
        determineProgress();
        for(Unit u : campaign.getUnits()) {
            try {
                preparedStatement = connect.prepareStatement("UPDATE " + table + ".equipment SET type=?, name=?, subtype=?, crew=?, weight=?, regnumber=?, notes=? WHERE uuid=?"); 
                preparedStatement.setInt(1, UnitType.determineUnitTypeCode(u.getEntity())+1);
                preparedStatement.setString(2, truncateString(u.getEntity().getChassis(), 45));
                preparedStatement.setString(3, truncateString(u.getEntity().getModel(), 45));
                if(null != u.getCommander()) {
                    preparedStatement.setInt(4, personHash.get(u.getCommander().getId()));
                } else {
                    preparedStatement.setInt(4, 0);
                }
                preparedStatement.setInt(5, Math.round(u.getEntity().getWeight()));
                preparedStatement.setInt(6, 1);
                preparedStatement.setString(7, u.getHistory());
                preparedStatement.setString(8, u.getId().toString());
                if(preparedStatement.executeUpdate() < 1) {
                    //no prior record so insert
                    preparedStatement = connect.prepareStatement("INSERT INTO " + table + ".equipment (type, name, subtype, crew, weight, regnumber, notes, uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                    preparedStatement.setInt(1, 1);
                    preparedStatement.setString(2, truncateString(u.getEntity().getChassis(), 45));
                    preparedStatement.setString(3, truncateString(u.getEntity().getModel(), 45));
                    if(null != u.getCommander()) {
                        preparedStatement.setInt(4, personHash.get(u.getCommander().getId()));
                    } else {
                        preparedStatement.setInt(4, 0);
                    }
                    preparedStatement.setInt(5, Math.round(u.getEntity().getWeight()));
                    preparedStatement.setInt(6, 1);
                    preparedStatement.setString(7, u.getHistory());
                    preparedStatement.setString(8, u.getId().toString());
                    preparedStatement.executeUpdate();
                }
                //TODO: connect to a TRO
                progressTicker += 1;
                determineProgress();
            } catch (SQLException e) {
                e.printStackTrace();
            }          
        }
    }
    
    public void close() {
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }

            if (connect != null) {
                connect.close();
            }
        } catch (Exception e) {
            
        }
    }
    
    private static String parseFirstName(String name) {
        return name.split(" ")[0];
    }
    
    private static String parseLastName(String name) {
        String[] names = name.split(" ");
        return names[names.length-1];
    }
    
    private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
    
    private static String getShortSkillName(String name) {
        name = name.split("/")[0];
        name = name.replaceAll("\\s","");
        name = name.replaceAll("Hyperspace", "");
        return name;
    }
    
    private static String getMercRosterStatusName(int status) {
        switch(status) {
        case Person.S_ACTIVE:
            return "Active";
        case Person.S_KIA:
            return "Deceased";
        case Person.S_RETIRED:
            return "Retired";
        case Person.S_MIA:
            return "Missing in Action";
        default:
            return "?";
        }
    }
    
    private static String truncateString(String s, int len) {
        if(s.length() < len) {
            return s;
        }
        return s.substring(0, len-1);
    }

    @Override
    protected Void doInBackground() {
        writeCampaignData();
        return null;
    }
    
    @Override
    public void done() {
        close();
    }
    
    public String getProgressNote() {
        return progressNote;
    }
    
    private int getLengthOfTask() {
        return 2 + campaign.getRanks().getAllRanks().size() + SkillType.skillList.length + Person.T_NUM * 2 + UnitType.SIZE + campaign.getPersonnel().size() * 4 + campaign.getUnits().size() + campaign.getAllForces().size() * 2;
    }
    
    public void determineProgress() {
        double percent = ((double)progressTicker) / getLengthOfTask();
        percent = Math.min(percent, 1.0);
        setProgress((int)Math.ceil(percent * 100));
    }
}