package mekhq.campaign.mission.mission.contractGeneration;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.jspecify.annotations.NonNull;

public class ContractObjectivePay {
    private final ContractObjectiveBasePay basePay;
    private final Money transportPayment;

    public ContractObjectivePay(CampaignOptions campaignOptions, Faction campaignFaction, LocalDate today,
          Hangar hangar, final Collection<Part> spareParts, final Collection<Person> allPersonnel,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap,
          List<Formation> formations, BasePaymentMultiplier basePaymentMultiplier, ILocation currentLocation,
          JumpPath jumpPath, boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode) {
        basePay = getBasePay(campaignOptions,
              campaignFaction,
              today,
              hangar,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              formations,
              basePaymentMultiplier);

        transportPayment = ContractObjectiveTransportPay.calculateObjectiveTransportPay(today,
              hangar,
              spareParts,
              allPersonnel,
              currentLocation,
              jumpPath,
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);
    }

    private static @NonNull ContractObjectiveBasePay getBasePay(CampaignOptions campaignOptions,
          Faction campaignFaction, LocalDate today, Hangar hangar, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap, List<Formation> formations,
          BasePaymentMultiplier basePaymentMultiplier) {
        return new ContractObjectiveBasePay(campaignOptions, campaignFaction, today, hangar,
              temporaryAsTechPoolSize, temporaryMedicPool, tempCrewMap, formations, basePaymentMultiplier);
    }
}
