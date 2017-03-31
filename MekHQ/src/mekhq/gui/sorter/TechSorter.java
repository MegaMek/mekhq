package mekhq.gui.sorter;

import java.util.Comparator;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IPartWork;

/**
     * A comparator that sorts techs by skill level
     * @author Jay Lawson
     *
     */
    public class TechSorter implements Comparator<Person> {
    	private IPartWork partWork;
    	
    	public TechSorter() {
    		this(null);
    	}
    	
    	public TechSorter(IPartWork p) {
    		partWork = p;
    	}

        @Override
        public int compare(Person p0, Person p1) {
        	if (partWork != null && partWork.getUnit() != null) {
        		if (p0.getTechUnitIDs().contains(partWork.getUnit().getId())) {
        			return -1;
        		}
        		if (p1.getTechUnitIDs().contains(partWork.getUnit().getId())) {
        			return 1;
        		}
        	}
            return ((Comparable<Integer>)p0.getBestTechLevel()).compareTo(p1.getBestTechLevel());
        }
        
        public void setPart(IPartWork p) {
        	partWork = p;
        }
        
        public void clearPart() {
        	partWork = null;
        }
    }