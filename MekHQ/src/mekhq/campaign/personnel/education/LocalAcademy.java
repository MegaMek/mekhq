package mekhq.campaign.personnel.education;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;

import java.util.ResourceBundle;

public class LocalAcademy {

    public static void localAcademy(Campaign campaign, Person person) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        // if this is changed, the change needs to be reflected in getFee()
        int fee = 30000;

        if (fee <= EducationController.getBalance(campaign)) {
            // this simulates the number of days it takes Person to get from the campaign basecamp and to the Academy
            // we use '2' because it means we don't have to worry about the tooltip saying '1 days'
            // if this is changed, the change needs to be reflected in getTravelTime()
            int travelTime = 2;
            person.setEduDaysOfTravelToAcademy(travelTime);

            // I came to this number by finding the average length of a college year, in the US, including semester breaks
            // if this is changed, the change needs to be reflected in getDuration()
            person.setEduDaysOfEducation(294);

            person.setEduAcademyPlanet(campaign.getCurrentSystem().getPrimaryPlanet());

            campaign.getFinances().debit(TransactionType.EDUCATION, campaign.getLocalDate(), Money.of(fee),
                    resources.getString("payment.text").replace("0", person.getFullName()));

            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.STUDENT);

            campaign.addReport(resources.getString("offToSchool.text")
                    .replace("0", person.getHyperlinkedFullTitle())
                    .replace("1", resources.getString("institutionLocalAcademy.text"))
                    .replace("2", String.valueOf(travelTime)));

            ServiceLogger.beganEducation(person, campaign.getLocalDate(), resources.getString("institutionLocalAcademy.text"));
        } else {
            campaign.addReport(resources.getString("insufficientFunds.text")
                    .replace("0", person.getHyperlinkedFullTitle()));
        }
    }

    public static int getTravelTime() {
        return 2;
    }

    public static int getDuration() {
        return 294;
    }

    public static int getFee() {
        return 30000;
    }
}
