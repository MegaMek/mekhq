package mekhq.gui;

import java.util.Comparator;

import mekhq.campaign.personnel.Person;

/**
     * A comparator that sorts techs by skill level
     * @author Jay Lawson
     *
     */
    public class TechSorter implements Comparator<Person> {

        @Override
        public int compare(Person p0, Person p1) {
            return ((Comparable<Integer>)p0.getBestTechLevel()).compareTo(p1.getBestTechLevel());
        }
    }