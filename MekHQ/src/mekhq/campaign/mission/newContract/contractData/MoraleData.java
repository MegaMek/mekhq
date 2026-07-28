package mekhq.campaign.mission.newContract.contractData;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.AtBMoraleLevel;

public record MoraleData(AtBMoraleLevel moraleLevel,
      @Nullable LocalDate routEndDate,
      Money routedPayout) {
    public MoraleData(AtBMoraleLevel moraleLevel) {
        this(moraleLevel, null, Money.zero());
    }
}
