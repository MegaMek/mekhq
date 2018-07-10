package mekhq.campaign;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.AwardNames;
import mekhq.campaign.personnel.Person;

public class Award implements MekHqXmlSerializable, Cloneable, Comparable<Award> {

	private AwardNames award;
	private Date date;

	private String longName;
	private String description;
	private int xpReward;

	private String medalFile;
	private String ribbonFile;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	// Can be awarded multiple times to the same person.
	private boolean stackable = false;

	public Award(AwardNames award, String longName, String description, int xpReward, String medalFile, String ribbonFile){
	    this.award = award;
	    this.longName = longName;
	    this.description = description;
	    this.xpReward = xpReward;
	    this.medalFile = medalFile;
	    this.ribbonFile = ribbonFile;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(MekHqXmlUtil.indentStr(indent)).append("<award>");
		if(null != date) {
			sb.append("<date>").append(DATE_FORMAT.format(date)).append("</date>");
		}
		sb.append("<name>").append(MekHqXmlUtil.escape(award.toString())).append("</name>");
		sb.append("</award>");
		pw1.println(sb.toString());
	}

	public void setDate(Date date){
	    this.date = date;
    }

    public Date getDate(){
		return date;
	}

    public String getFormatedDate(){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(date.getTime());
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

    public String getRibbonFileName(){
	    //return "ClanInvasion.png";
		return ribbonFile;
    }

    public String getMedalFileName(){
	    //return "ClanInvasionM.png";
		return medalFile;
    }

    public int getXPreward(){
	    return xpReward;
    }

    public Award createCopy(Date date){
	    Award awardCopy = new Award(this.award, this.longName, this.description, this.xpReward, this.medalFile, this.ribbonFile);
        awardCopy.setDate(date);
        return awardCopy;
    }

    public boolean canBeAwarded(Person person){
	    // If we wish to force the user to not be able to give awards for some reason (e.g. lack of kill count),
        // we need to create classes for each awards and override this method.

        if(person.hasAward(this) && !stackable) return false;

        return true;
    }

	@Override
	public int compareTo(Award other) {
		return Integer.compare(this.xpReward, other.xpReward);
	}
}
