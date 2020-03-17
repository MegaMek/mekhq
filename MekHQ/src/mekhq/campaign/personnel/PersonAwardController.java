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

import mekhq.MekHQ;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.AwardLogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for the awards given to a person.
 * @author Miguel Azevedo
 */
public class PersonAwardController {

    private List<Award> awards;
    private Person person;

    public PersonAwardController(Person person) {
        awards = new ArrayList<>();
        this.person = person;
    }

    /**
     * @return this person's award list.
     */
    public List<Award> getAwards(){
        Collections.sort(awards);
        return awards;
    }

    /**
     * @param award to check if this person has it
     * @return true if it has the award
     */
    public boolean hasAward(Award award){
        return getAward(award.getSet(), award.getName()) != null;
    }

    /**
     * @param set String with the name of the set which the award belongs
     * @param name String with the name of the award
     * @return true if person has an award of that name and set
     */
    private boolean hasAward(String set, String name){
        return getAward(set, name) != null;
    }

    /**
     * @return true if this person has one or more awards.
     */
    public boolean hasAwards() {
        return awards.size() > 0;
    }

    /**
     * @return true if this person has one or more awards that are represented with a ribbon icon.
     */
    public boolean hasAwardsWithRibbons(){
        return awards.stream().filter(a -> a.getNumberOfRibbonFiles() > 0).collect(Collectors.toList()).size() > 0;
    }

    /**
     * @return true if this person has one or more awards that are represented with a medal icon.
     */
    public boolean hasAwardsWithMedals(){
        return awards.stream().filter(a -> a.getNumberOfMedalFiles() > 0).collect(Collectors.toList()).size() > 0;
    }

    /**
     * @return true if this person has one or more awards that are represented by a misc icon.
     */
    public boolean hasAwardsWithMiscs(){
        return awards.stream().filter(a -> a.getNumberOfMiscFiles() > 0).collect(Collectors.toList()).size() > 0;
    }

    /**
     * Adds and logs an award to this person based on
     * @param setName is the name of the set of the award
     * @param awardName is the name of the award
     * @param date is the date it was awarded
     */
    public void addAndLogAward(String setName, String awardName, Date date) {
        Award award;
        if(hasAward(setName, awardName)){
            award = getAward(setName, awardName);
        }
        else{
            award = AwardsFactory.getInstance().generateNew(setName, awardName);
            awards.add(award);
        }
        person.setXp(person.getXp() + award.getXPReward());
        person.setEdge(person.getEdge() + award.getEdgeReward());
        person.resetCurrentEdge(); //Reset the person's edge points

        award.addDate(date);
        logAward(award, date);
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    /**
     * Gives the award to this person
     * @param award is the award it was awarded
     */
    public void addAwardFromXml(Award award){
        if(hasAward(award)){
            Award existingAward = getAward(award.getSet(), award.getName());
            existingAward.mergeDatesFrom(award);
        }
        else{
            awards.add(award);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    /**
     * Removes an award given to this person based on:
     * @param setName is the name of the set of the award
     * @param awardName is the name of the award
     * @param stringDate is the date it was awarded
     */
    public void removeAward(String setName, String awardName, String stringDate){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd"); // TODO : remove inline date format

        Date date = null;
        try {
            date = df.parse(stringDate);
        } catch (ParseException e) {
            MekHQ.getLogger().error(getClass(), "removeAward", e);
        }

        for(Award award : awards){
            if(award.equals(setName, awardName)){
                award.removeDate(date);
                if(!award.hasDates()) awards.remove(award);
                AwardLogger.removedAward(person, date, award);
                MekHQ.triggerEvent(new PersonChangedEvent(person));
                return;
            }
        }
    }

    /**
     * Adds an entry log for a given award.
     * @param award that was given.
     */
    public void logAward(Award award, Date date){
        AwardLogger.award(person, date, award);
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    /**
     * @param set String with the name of the set which the award belongs
     * @param name String with the name of the award
     * @return the award
     */
    public Award getAward(String set, String name){
        for(Award myAward : awards){
            if(name.equals(myAward.getName()) &&
                    set.equals(myAward.getSet())) return myAward;
        }
        return null;
    }

    /**
     * Finds an award with a given name, without taking into account the set name. This is used for backward compatibility
     * and should be avoided.
     * @param name String with the name of the award
     * @return the award
     */
    public Award getFirstAwardIgnoringSet(String name){
        for(Award myAward : awards){
            if(name.equals(myAward.getName())) return myAward;
        }
        return null;
    }

    /**
     * @param award to be counted.
     * @return the number of times this award has been awarded to the same person.
     */
    public int getNumberOfAwards(Award award){
        for(Award myAward : awards){
            if(award.equals(myAward.getSet(), myAward.getName())){
                return myAward.getQuantity();
            }
        }
        return 0;
    }
}
