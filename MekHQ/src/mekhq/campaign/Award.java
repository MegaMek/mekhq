package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="award")
@XmlAccessorType(XmlAccessType.FIELD)
public class Award implements MekHqXmlSerializable, Cloneable, Comparable<Award>, Serializable {

	@XmlElement(name = "name")
	private String name;

	@XmlElement(name = "description")
	private String description;

	@XmlElement(name = "medal")
	private String medal;

	@XmlElement(name = "ribbon")
	private String ribbon;

	@XmlElement(name = "xp")
	private int xp;

	@XmlElement(name = "stackable")
	private boolean stackable = false;

	private String set;

	private Date date;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	public Award(){}

	public Award(String name, String set,  String description, String medal, String ribbon, int xp, boolean stackable) {
		this.name = name;
		this.set = set;
		this.description = description;
		this.medal = medal;
		this.ribbon = ribbon;
		this.xp = xp;
		this.stackable = stackable;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(MekHqXmlUtil.indentStr(indent)).append("<award>");

		sb.append("<date>").append(DATE_FORMAT.format(date)).append("</date>");
		sb.append("<set>").append(MekHqXmlUtil.escape(this.set)).append("</set>");
		sb.append("<name>").append(MekHqXmlUtil.escape(this.name)).append("</name>");

		sb.append("</award>");
		pw1.println(sb.toString());
	}

	public String getName(){
		return name;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getSet(){
		return set;
	}

	public void setSet(String set){
		this.set = set;
	}

	public String getDescription() {
		return description;
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


    public String getRibbonFileName(){
		return ribbon;
    }

    public String getMedalFileName(){
		return medal;
    }

    public int getXPreward(){
	    return xp;
    }

    public Award createCopy(Date date){
		Award awardCopy = new Award(this.name, this.set, this.description, this.medal, this.ribbon, this.xp, this.stackable);
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
	    int result = Integer.compare(this.xp, other.xp);

	    if(result == 0)
	        result = this.getName().compareTo(other.getName());

		return result;
	}
}
