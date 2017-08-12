package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Ranks;

 /**
     * A comparator for ranks written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
    public class RankSorter implements Comparator<String> {

        private Campaign campaign;
        private Pattern pattern;
        private Matcher matcher;
        public RankSorter(Campaign c) {
            campaign = c;
            c.getRanks();
            pattern = Pattern.compile("id=\"([^\"]+)\""); //$NON-NLS-1$
        }

        @Override
        public int compare(String s0, String s1) {
            final String METHOD_NAME = "compare(String,String)"; //$NON-NLS-1$

            // get the numbers associated with each rank string, and compare
        	matcher = pattern.matcher(s0);
        	matcher.find();
        	String s00 = matcher.group(1);
            matcher = pattern.matcher(s1);
            matcher.find();
            String s11 = matcher.group(1);
        	try {
	        	Person p0 = campaign.getPerson(UUID.fromString(s00));
	        	Person p1 = campaign.getPerson(UUID.fromString(s11));
	        	// the rank orders match, try comparing the levels
	        	if (p0.getRankNumeric() == p1.getRankNumeric()) {
	        	    // For prisoners: Sort those willing to defect "above" those who don't
	        	    if(p0.getRankNumeric() == Ranks.RANK_PRISONER) {
	        	        return Boolean.compare(p0.isWillingToDefect(), p1.isWillingToDefect());
	        	    }
	        		// the levels match too, try comparing MD rank
	        		if (p0.getRankLevel() == p1.getRankLevel()) {
	        		    if(p0.getManeiDominiRank() == p1.getManeiDominiRank()) {
	        		        return s0.compareTo(s1);
	        		    }
	        		    return Integer.compare(p0.getManeiDominiRank(), p1.getManeiDominiRank());
	        		}
	        		return Integer.compare(p0.getRankLevel(), p1.getRankLevel());
	        	}
	        	return Integer.compare(p0.getRankNumeric(), p1.getRankNumeric());
        	} catch (Exception e) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                        String.format("[DEBUG] RankSorter Exception, s0: %s, s1: %s", s00, s11)); //$NON-NLS-1$
                MekHQ.getLogger().log(getClass(), METHOD_NAME, e);
        		return 0;
        	}
        }
    }