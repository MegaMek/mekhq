package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents an award given to a person
 * @author Miguel Azevedo
 *
 */
@XmlRootElement(name="award")
@XmlAccessorType(XmlAccessType.FIELD)
public class Award implements MekHqXmlSerializable, Comparable<Award>, Serializable {
    private static final long serialVersionUID = -3290927068079223579L;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "medal")
    private String medal;

    @XmlElement(name = "ribbon")
    private String ribbon;

    @XmlElement(name = "misc")
    private String misc;

    @XmlElement(name = "xp")
    private int xp = 0;

    @XmlElement(name = "edge")
    private int edge = 0;

    @XmlElement(name = "stackable")
    private boolean stackable = false;

    private String set;

    private Date date;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public Award(){}

    public Award(String name, String set,  String description, String medal, String ribbon, String misc, int xp, int edge, boolean stackable) {
        this.name = name;
        this.set = set;
        this.description = description;
        this.medal = medal;
        this.ribbon = ribbon;
        this.misc = misc;
        this.xp = xp;
        this.edge = edge;
        this.stackable = stackable;
    }

    /**
     * Writes this award to xml file and format.
     * @param pw1 printer writter reference to write the xml
     * @param indent indentation
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.append(MekHqXmlUtil.indentStr(indent)).append("<award>");

        pw1.append("<date>").append(DATE_FORMAT.format(date)).append("</date>");
        pw1.append("<set>").append(MekHqXmlUtil.escape(this.set)).append("</set>");
        pw1.append("<name>").append(MekHqXmlUtil.escape(this.name)).append("</name>");

        pw1.append("</award>").println();
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

    public String getMiscFileName(){
        return misc;
    }

    public int getXPReward(){
        return xp;
    }
    public int getEdgeReward(){ return edge; }

    /**
     * Creates a copy of this award and sets a given date.
     * @param date when the award was given
     * @return award with new date
     */
    public Award createCopy(Date date){
        Award awardCopy = new Award(this.name, this.set, this.description, this.medal, this.ribbon, this.misc, this.xp, this.edge, this.stackable);
        awardCopy.setDate(date);
        return awardCopy;
    }


    /**
     * Checks if an award can be awarded to a given person
     * @param person to be given the award
     * @return true if this award can be awarded to the selected person
     */
    public boolean canBeAwarded(Person person){
        // If we wish to force the user to not be able to give awards for some reason (e.g. lack of kill count),
        // we need to create classes for each awards and override this method.

        return (!person.hasAward(this) || stackable);
    }

    /**
     * Checks if two awards are equal
     * @param setName is the name of the set of this award
     * @param name is the name of the award
     * @param date is the date this award was given
     * @return true if it is equal
     */
    public boolean equals(String setName, String name, Date date){
        return (this.set.equals(setName) && this.name.equals(name) && this.date.equals(date));
    }

    /**
     * Compares an award with this one by priority: xp, edge and name. Used for sorting.
     * @param other award to be compared
     * @return int used for sorting
     */
    @Override
    public int compareTo(Award other) {
        int result = Integer.compare(this.xp, other.xp);

        if(result == 0)
            result = Integer.compare(this.edge, other.edge);

        if(result == 0)
            result = this.getName().compareTo(other.getName());

        return result;
    }
}
