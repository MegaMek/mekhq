package mekhq.campaign.finances;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

import java.time.LocalDate;

public class Lease implements mekhq.campaign.work.IAcquisitionWork {
    private Money leaseCost;
    private LocalDate acquisitionDate;
    private Entity futureUnit;
    private int daysToWait;
    private Campaign campaign;

    /*
     * Leases are nominally attached to units while they are in the hanger.
     */
    public Lease(LocalDate currentDay, Unit unit) {
        acquisitionDate = currentDay;
        // Campaign Operations 4th., p43
        leaseCost = unit.getSellValue().multipliedBy(0.005);
    }

    /*
     * If a lease is in the shopping list, it doesn't have a unit yet to attach to.
     */
    public Lease(Entity futureUnit, Campaign campaign) {
        this.futureUnit = futureUnit;
        this.campaign = campaign;
    }

    public void realizeLease(LocalDate currentDay, Unit unit) {
        acquisitionDate = currentDay;
        leaseCost = unit.getSellValue().multipliedBy(0.005);
        this.futureUnit = null;
    }

    /**
     * Gets the lease cost, for the accountant. Lease cost is prorated for the first month, so we need to check
     * if yesterday was the first month. Should only be called on the 1st.
     *
     * @params time The current campaign LocalDate
     */
    public Money getLeaseCost(LocalDate time) {
        if (isLeaseFirstMonth(time.minusDays(1))) {
            return getFirstLeaseCost(time);
        }
        return leaseCost;
    }

    /**
     * Utility function for the raw lease cost.
     */
    public Money getLeaseCost() {
        return leaseCost;
    }

    public LocalDate getLeaseStart() {
        return acquisitionDate;
    }

    /**
     * Utility function. Is this the first month of the lease?
     *
     * @param today The LocalDate to check with. No corrections done.
     */
    public boolean isLeaseFirstMonth(LocalDate today) {
        return (today.getYear() == acquisitionDate.getYear() && today.getMonth() == acquisitionDate.getMonth());
    }

    /**
     * Gets the final cost of the lease remaining in the last month for use when ending a lease. If you call this in the
     * same month you acquired the unit, only the days between lease start and now are counted. Can be called on any day
     * of the month
     *
     * @return Money Prorated last payment of lease
     */
    public Money getFinalLeaseCost(LocalDate today) {
        int startDay = 0;
        int currentDay = today.getDayOfMonth();
        if (isLeaseFirstMonth(today)) {
            startDay = acquisitionDate.getDayOfMonth();
        }
        float fractionOfMonth = (float) (currentDay - startDay) / (float) today.lengthOfMonth();
        return leaseCost.multipliedBy(fractionOfMonth);
    }

    /**
     * Gets the cost of the lease, prorated in the first month.
     * Assumes that it's only called on the first day of the month, so we need to find yesterday for the last.
     *
     * @return Money Prorated first payment of lease
     */
    public Money getFirstLeaseCost(LocalDate today) {
        int startDay = acquisitionDate.getDayOfMonth();
        int yesterday = today.minusDays(1).getDayOfMonth();
        float fractionOfMonth = (float) (yesterday - startDay) / (float) yesterday;
        return leaseCost.multipliedBy(fractionOfMonth);
    }

    public static boolean isLeasable(Entity check) {
        return check instanceof megamek.common.Dropship || check instanceof megamek.common.Jumpship;
    }

    @Override
    public String getAcquisitionName() {
        return futureUnit.generalName();
    }

    @Override
    public String getAcquisitionDisplayName() {
        return futureUnit.generalName();
    }

    @Override
    public int getDaysToWait() {
        return daysToWait;
    }

    @Override
    public void resetDaysToWait() {
        this.daysToWait = campaign.getCampaignOptions().getWaitingPeriod();
        ;
    }

    @Override
    public void decrementDaysToWait() {
        daysToWait = daysToWait > 0 ? daysToWait-- : 0;
    }


}

