package mekhq.campaign.unit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;

/**
 * This class is used to store information about a particular unit that is
 * lost when a unit is mothballed, so that it may be restored to as close to
 * its prior state as possible when the unit is reactivated.
 * @author NickAragua
 *
 */
public class MothballInfo implements MekHqXmlSerializable {
    private UUID techID;
    private int forceID;
    private List<UUID> driverIDs;
    private List<UUID> gunnerIDs;
    private List<UUID> vesselCrewIDs;
    private UUID techOfficerID;
    private UUID navigatorID;

    /**
     * Parameterless constructor, used for deserialization.
     */
    private MothballInfo() {
        driverIDs = new ArrayList<>();
        gunnerIDs = new ArrayList<>();
        vesselCrewIDs = new ArrayList<>();
    }
    
    /**
     * Creates a set of mothball info for a given unit
     * @param unit The unit to work with
     */
    public MothballInfo(Unit unit) {
        techID = unit.getTechId();
        forceID = unit.getForceId();
        driverIDs = (List<UUID>) unit.getDriverIDs().clone();
        gunnerIDs = (List<UUID>) unit.getGunnerIDs().clone();
        vesselCrewIDs = (List<UUID>) unit.getVesselCrewIDs().clone();
        techOfficerID = unit.getTechOfficerID();
        navigatorID = unit.getNavigatorID();
    }
    
    /**
     * Restore a unit's pilot, assigned tech and force, to the best of our ability
     * @param unit The unit to restore
     * @param campaign The campaign in which this is happening
     */
    public void restorePreMothballInfo(Unit unit, Campaign campaign) 
    {
        Person tech = campaign.getPerson(techID);
        if(tech != null) {
            unit.setTech(tech);
        }
        
        for(UUID driverID : driverIDs) {
            Person driver = campaign.getPerson(driverID);
            
            if(driver != null && driver.isActive() && driver.getUnitId() == null) {
                unit.addDriver(driver);
            }
        }
        
        for(UUID gunnerID : gunnerIDs) {
            Person gunner = campaign.getPerson(gunnerID);
            
            // add the gunner if they exist, aren't dead/retired/etc and aren't already assigned to some
            // other unit. Caveat: single-person units have the same driver and gunner.
            if(gunner != null && gunner.isActive() && 
                    ((gunner.getUnitId() == null) || (gunner.getUnitId() == unit.getId()))) {
                unit.addGunner(gunner);
            }
        }
        
        for(UUID vesselCrewID : vesselCrewIDs) {
            Person vesselCrew = campaign.getPerson(vesselCrewID);
            
            if(vesselCrew != null && vesselCrew.isActive() && vesselCrew.getUnitId() == null) {
                unit.addVesselCrew(vesselCrew);
            }
        }
        
        Person techOfficer = campaign.getPerson(techOfficerID);
        if(techOfficer != null && techOfficer.isActive() && techOfficer.getUnitId() == null) {
            unit.setTechOfficer(techOfficer);
        }
        
        Person navigator = campaign.getPerson(navigatorID);
        if(navigator != null && navigator.isActive() && navigator.getUnitId() == null) {
            unit.setNavigator(navigator);
        }
        
        if(campaign.getForce(forceID) != null) {
            campaign.addUnitToForce(unit, forceID);
        }
    }

    /**
     * Serializer method implemented in MekHQ pattern
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<mothballInfo>");
        
        if(techID != null) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<techID>" + techID.toString() + "</techID>");
        }
        
        if(forceID > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<forceID>" + forceID + "</forceID>");
        }
        
        if(driverIDs.size() > 0) {
            for(UUID driverID : driverIDs) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<driverID>" + driverID.toString() + "</driverID>");
            }
        }
        
        if(gunnerIDs.size() > 0) {
            for(UUID gunnerID : gunnerIDs) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<gunnerID>" + gunnerID.toString() + "</gunnerID>");
            }
        }
        
        if(vesselCrewIDs.size() > 0) {
            for(UUID vesselCrewID : vesselCrewIDs) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<vesselCrewID>" + vesselCrewID.toString() + "</vesselCrewID>");
            }
        }
        
        if(techOfficerID != null) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<techOfficerID>" + techOfficerID.toString() + "</techOfficerID>");
        }
        
        if(navigatorID != null) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<navigatorID>" + navigatorID.toString() + "</navigatorID>");
        }
        
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</mothballInfo>");
    }
    
    /**
     * Deserializer method implemented in standard MekHQ pattern.
     * @return Instance of MothballInfo
     */
    public static MothballInfo generateInstanceFromXML(Node wn, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Version)";
        
        MothballInfo retVal = new MothballInfo();
        
        NodeList nl = wn.getChildNodes();

        try {
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("techID")) {
                    retVal.techID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceID")) {
                    retVal.forceID = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("driverID")) {
                    retVal.driverIDs.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("gunnerID")) {
                    retVal.gunnerIDs.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("vesselCrewID")) {
                    retVal.vesselCrewIDs.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("techOfficerID")) {
                    retVal.techOfficerID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("navigatorID")) {
                    retVal.navigatorID = UUID.fromString(wn2.getTextContent());
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().error(Unit.class, METHOD_NAME, ex);
        }
        
        return retVal;
    }
}
