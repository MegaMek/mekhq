/*
 * Copyright (C) 2018 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private ArrayList<String> medals;

    @XmlElement(name = "ribbon")
    private ArrayList<String> ribbons;

    @XmlElement(name = "misc")
    private ArrayList<String> miscs;

    @XmlElement(name = "xp")
    private int xp = 0;

    @XmlElement(name = "edge")
    private int edge = 0;

    @XmlElement(name = "stackable")
    private boolean stackable = false;

    private int id;

    private String set;

    private List<Date> dates;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public Award(){}

    public Award(String name, String set,  String description, ArrayList<String> medals, ArrayList<String> ribbons, ArrayList<String> miscs, int xp, int edge, boolean stackable, int id) {
        this.name = name;
        this.set = set;
        this.description = description;
        this.medals = medals;
        this.ribbons = ribbons;
        this.miscs = miscs;
        this.xp = xp;
        this.edge = edge;
        this.stackable = stackable;
        dates = new ArrayList<>();
        this.id = id;
    }

    /**
     * Writes this award to xml file and format.
     * @param pw1 printer writter reference to write the xml
     * @param indent indentation
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.append(MekHqXmlUtil.indentStr(indent)).append("<award>");

        for(Date date : dates){
            pw1.append("<date>").append(DATE_FORMAT.format(date)).append("</date>");
        }
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

    public void setId(int id) { this.id = id; }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the file name of an award given i times.
     * @param i times given the award
     * @param fileNames list containing all of the file names
     * @return the file name
     */
    private String getFileName(int i, List<String> fileNames){
        if(i > fileNames.size()) return fileNames.get(fileNames.size()-1);

        return fileNames.get(i-1);
    }

    /**
     * @param i number of times this award has been awarded
     * @return the filename of the ribbon
     */
    public String getRibbonFileName(int i){
        return getFileName(i, ribbons);
    }

    public int getNumberOfRibbonFiles(){
        return ribbons == null ? 0 : ribbons.size();
    }

    /**
     * @param i number of times this award has been awarded
     * @return the filename of the medal
     */
    public String getMedalFileName(int i){
        return getFileName(i, medals);
    }

    public int getNumberOfMedalFiles() {
        return medals == null ? 0 : medals.size();
    }

    /**
     * @param i number of times this award has been awarded
     * @return the filename of the misc
     */
    public String getMiscFileName(int i){
        return getFileName(i, miscs);
    }

    public int getNumberOfMiscFiles() {
        return miscs == null ? 0 : miscs.size();
    }

    public int getXPReward(){
        return xp;
    }
    public int getEdgeReward(){ return edge; }

    /**
     * Creates a copy of this award and sets a given date.
     * @return award with new date
     */
    public Award createCopy(){
        Award awardCopy = new Award(this.name, this.set, this.description, this.medals, this.ribbons, this.miscs, this.xp, this.edge, this.stackable, this.id);
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

        return (!person.awardController.hasAward(this) || stackable);
    }

    /**
     * Checks if two awards are equal
     * @param set is the name of the set of this award
     * @param name is the name of the award
     * @return true if it is equal
     */
    public boolean equals(String set, String name){
        return (this.set.equals(set) && this.name.equals(name));
    }

    /**
     * Compares an award with this one by priority: xp, edge and name. Used for sorting.
     * @param other award to be compared
     * @return int used for sorting
     */
    @Override
    public int compareTo(Award other) {
        return Integer.compare(this.id, other.id);
    }

    /**
     * Adds a date to the award, as if the award was given again.
     * @param date to be added.
     */
    public void addDate(Date date) {
        dates.add(date);
    }

    public void setDates(List<Date> dates){
        this.dates = dates;
    }

    /**
     * Merges all dates from one award into the other
     * @param award from the dates will be collected
     */
    public void mergeDatesFrom(Award award) {
        this.dates.addAll(award.dates);
    }

    /**
     * Generates a list of strings of formated dates yyyy-MM-dd
     * @return a list of strings representing the dates in the format yyyy-MM-dd
     */
    public List<String> getFormatedDates(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        ArrayList<String> formatedDates = new ArrayList<>();
        for(Date date : dates){
            formatedDates.add(df.format(date.getTime()));
        }
        return formatedDates;
    }

    /**
     * @param date date to be removed from this award
     */
    public void removeDate(Date date){
        dates.remove(date);
    }

    /**
     * @return true if this award has any dates
     */
    public boolean hasDates(){
        return dates.size() > 0;
    }

    /**
     * @return the number of times this award has been awarded to the person.
     */
    public int getQuantity(){
        return dates.size();
    }

    /**
     * @return an html formatted string to be used as tooltip.
     */
    public String getTooltip(){

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder string = new StringBuilder();
        string.append("<html>");

        string.append(getName());

        string.append("<br>");

        string.append(getDescription());

        string.append("<br>");
        string.append("<br>");
        for(Date date : dates){
            string.append("(");
            string.append(df.format(date.getTime()));
            string.append(")");
            string.append("<br>");
        }

        string.append("</html>");

        return string.toString();
    }

    /**
     * Replaces a date of an award
     * @param toReplace date to replace
     * @param newDate new date that will replace the other
     */
    public void replaceDate(Date toReplace, Date newDate){
        if(toReplace.equals(newDate)) return;

        int i = 0;
        for(Date date : dates){
            if(date.equals(toReplace)) continue;
            i++;
        }
        dates.set(i, newDate);
    }
}
