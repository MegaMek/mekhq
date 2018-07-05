package mekhq.campaign;

import java.io.PrintWriter;
import java.util.Date;

import mekhq.MekHqXmlSerializable;
import mekhq.campaign.personnel.Awards;
import mekhq.campaign.personnel.Person;

public class Award implements MekHqXmlSerializable, Cloneable {

	private Awards award;
	private Date date;

	private String longName;
	private String description;
	private int xpReward;

	private String medalFile;
	private String ribbonFile;

	// Can be awarded multiple times to the same person.
	private boolean stackable = false;

	public Award(Awards award, String longName, String description, int xpReward){
	    this.award = award;
	    this.longName = longName;
	    this.description = description;
	    this.xpReward = xpReward;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		// TODO Auto-generated method stub
	}

	public void setDate(Date date){
	    this.date = date;
    }

    public String getLongName() {
        return longName;
    }

    public String getShortName() {
	    return award.toString();
    }

    public String getDescription() {
        return description;
    }

    public int getXPreward(){
	    return xpReward;
    }

    public Award createCopy(Date date){
	    Award awardCopy = new Award(this.award, this.longName, this.description, this.xpReward);
        awardCopy.setDate(date);
        return awardCopy;
    }

    public boolean canBeAwarded(Person person){
	    // If we wish to force the user to not be able to give awards for some reason (e.g. lack of kill count),
        // we need to create classes for each awards and override this method.

        if(person.hasAward(this) && !stackable) return false;

        return true;
    }
}
