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
	private UUID id;
	private UUID fatherId;
	private UUID motherId;
	private Campaign campaign;
	private UUID fathersAncestors;
	private UUID mothersAncestors;

	public Ancestors(Campaign c) {
		this(null, null, c);
	}

	public Ancestors(UUID father, UUID mother, Campaign c) {
		fatherId = father;
		motherId = mother;
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
			fathersAncestors = f.getAncestorsId();
		}

		if (m != null) {
			mothersAncestors = m.getAncestorsId();
		}
	}

    public UUID getFatherId() {
		return fatherId;
	}

	public void setFatherId(UUID fatherId) {
		this.fatherId = fatherId;
	}

	public UUID getMotherId() {
		return motherId;
	}

	public void setMotherId(UUID motherId) {
		this.motherId = motherId;
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

        // Don't forget the null check on anc
        if (anc == null) {
            return false;
        }

		// Nulls means we can't possibly have a match, eh?
		if (((fatherId == null) && (motherId == null))
				|| ((anc.getFatherId() == null) && (anc.getMotherId() == null))) {
			return false;
		}

		if (((getFatherId() != null) && getFatherId().equals(anc.getFatherId()))
				|| ((getMotherId() != null) && getMotherId().equals(anc.getMotherId()))) {
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
            return anc.getMothersAncestors().checkMutualAncestors(this, depth);
		}

		return false;
	}

	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(String.format("%s<ancestor id=\"%s\">", MekHqXmlUtil.indentStr(indent), id.toString()));
		indent++;
		pw1.println(String.format("%s<id>%s</id>", MekHqXmlUtil.indentStr(indent), id.toString()));
		if (fatherId != null) {
			pw1.println(String.format("%s<fatherId>%s</fatherId>",
                    MekHqXmlUtil.indentStr(indent), fatherId.toString()));
		}
		if (motherId != null) {
			pw1.println(String.format("%s<motherId>%s</motherId>",
                    MekHqXmlUtil.indentStr(indent), motherId.toString()));
		}
		if (fathersAncestors != null) {
			pw1.println(String.format("%s<fathersAncestors>%s</fathersAncestors>",
                    MekHqXmlUtil.indentStr(indent), fathersAncestors.toString()));
		}
		if (mothersAncestors != null) {
			pw1.println(String.format("%s<mothersAncestors>%s</mothersAncestors>",
                    MekHqXmlUtil.indentStr(indent), mothersAncestors.toString()));
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
	            } else if (wn2.getNodeName().equalsIgnoreCase("fatherId")) {
	            	retVal.fatherId = UUID.fromString(wn2.getTextContent());
	            } else if (wn2.getNodeName().equalsIgnoreCase("motherId")) {
	            	retVal.motherId = UUID.fromString(wn2.getTextContent());
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
		    MekHQ.getLogger().error(Ancestors.class, "generateInstanceFromXML(Node,Campaign,Version)", e); //$NON-NLS-1$
		}

		return retVal;
	}
}
