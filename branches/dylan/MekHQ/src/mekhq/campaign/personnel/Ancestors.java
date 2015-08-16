package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Ancestors implements Serializable, MekHqXmlSerializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6350146649504329173L;
	// UUID
	private UUID id;
	private UUID fatherID;
	private UUID motherID;
	private Campaign campaign;
	private UUID fathersAncestors;
	private UUID mothersAncestors;
	
	public Ancestors(Campaign c) {
		this(null, null, c);
	}
	
	public Ancestors(UUID father, UUID mother, Campaign c) {
		fatherID = father;
		motherID = mother;
		campaign = c;
		
		// Generate ID
		id = UUID.randomUUID();
		while (c.getAncestors(id) != null) {
			id = UUID.randomUUID();
		}
		
		// Find the parents and if they exist set the ancestors
		Person f = c.getPerson(father);
		Person m = c.getPerson(mother);
		
		if (f != null) {
			fathersAncestors = f.getAncestorsID();
		}
		
		if (m != null) {
			mothersAncestors = m.getAncestorsID();
		}
	}

    public UUID getFatherID() {
		return fatherID;
	}

	public void setFatherID(UUID fatherID) {
		this.fatherID = fatherID;
	}

	public UUID getMotherID() {
		return motherID;
	}

	public void setMotherID(UUID motherID) {
		this.motherID = motherID;
	}
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Ancestors getFathersAncestors() {
		return campaign.getAncestors(fathersAncestors);
	}

	public void setFathersAncestors(UUID fathersAncestors) {
		this.fathersAncestors = fathersAncestors;
	}

	public Ancestors getMothersAncestors() {
		return campaign.getAncestors(mothersAncestors);
	}

	public void setMothersAncestors(UUID mothersAncestors) {
		this.mothersAncestors = mothersAncestors;
	}

	// Designed for recursive searching
	public boolean checkMutualAncestors(Ancestors anc) {
		return checkMutualAncestors(anc, 0);
	}
	
	public boolean checkMutualAncestors(Ancestors anc, int depth) {
		// We only go 4 generations back looking for a connection
		// TODO: Make this a setting
		if (depth > 4) {
			return false;
		}
		
		depth++;
		
		// Nulls means we can't possibly have a match, eh?
		if ((fatherID == null && motherID == null)
				|| (anc.getFatherID() == null && anc.getMotherID() == null)){
			return false;
		}
		
		if ((getFatherID() != null && getFatherID().equals(anc.getFatherID()))
				|| (getMotherID() != null && getMotherID().equals(anc.getMotherID()))) {
			return true;
		}
		
		// Check father...
		if (fathersAncestors != null) {
			if (campaign.getAncestors(fathersAncestors).checkMutualAncestors(anc, depth)) {
				return true;
			}
		}
		
		// Check mother...
		if (mothersAncestors != null) {
			if (campaign.getAncestors(mothersAncestors).checkMutualAncestors(anc, depth)) {
				return true;
			}
		}
		
		// Check their father...
		if (anc.getFathersAncestors() != null) {
			if (anc.getFathersAncestors().checkMutualAncestors(this, depth)) {
				return true;
			}
		}
		
		// Check their mother...
		if (anc.getMothersAncestors() != null) {
			if (anc.getMothersAncestors().checkMutualAncestors(this, depth)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<ancestor id=\""
                + id.toString()
                + "\">");
		indent++;
		pw1.println(MekHqXmlUtil.indentStr(indent)
				+ "<id>"
				+ id.toString()
				+ "</id>");
		if (fatherID != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent)
					+ "<fatherID>"
					+ fatherID.toString()
					+ "</fatherID>");
		}
		if (motherID != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent)
					+ "<motherID>"
					+ motherID.toString()
					+ "</motherID>");
		}
		if (fathersAncestors != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent)
					+ "<fathersAncestors>"
					+ fathersAncestors.toString()
					+ "</fathersAncestors>");
		}
		if (mothersAncestors != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent)
					+ "<mothersAncestors>"
					+ mothersAncestors.toString()
					+ "</mothersAncestors>");
		}
		indent--;
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</ancestor>");
	}
	
	public static Ancestors generateInstanceFromXML(Node wn, Campaign c, Version version) {
		Ancestors retVal = null;
		
		try {
			retVal = new Ancestors(c);

            // Okay, now load Ancestor-specific fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
            
	            if (wn2.getNodeName().equalsIgnoreCase("id")) {
	                retVal.id = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("fatherID")) {
	            	retVal.fatherID = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("motherID")) {
	            	retVal.motherID = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("fathersAncestors")) {
	            	retVal.fathersAncestors = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("mothersAncestors")) {
	            	retVal.mothersAncestors = UUID.fromString(wn2.getTextContent());
	            }
            }
		} catch (Exception e) {
			// Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(e);
		}
		
		return retVal;
	}
}
