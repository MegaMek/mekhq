package mekhq.gui.sorter;

import java.util.Comparator;

import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
     * A comparator that sorts techs by skill level
     * @author Jay Lawson
     *
     */
    public class TechSorter implements Comparator<Person> {
    	private Part part;
    	
    	public TechSorter() {
    		this(null);
    	}
    	
    	public TechSorter(Part p) {
    		part = p;
    	}

        @Override
        public int compare(Person p0, Person p1) {
        	if (part != null && part.getUnitId() != null) {
        		if (p0.getTechUnitIDs().contains(part.getUnitId())) {
        			return -1;
        		}
        		if (p1.getTechUnitIDs().contains(part.getUnitId())) {
        			return 1;
        		}
        	}
            return ((Comparable<Integer>)p0.getBestTechLevel()).compareTo(p1.getBestTechLevel());
        }
        
        public void setPart(Part p) {
        	part = p;
        }
        
        public void clearPart() {
        	part = null;
        }
    }