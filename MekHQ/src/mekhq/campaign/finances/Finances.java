/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.LoanDefaultedEvent;
import mekhq.campaign.event.TransactionCreditEvent;
import mekhq.campaign.event.TransactionDebitEvent;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.io.FileType;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Finances {
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale());

    private List<Transaction> transactions;
    private List<Loan> loans;
    private List<Asset> assets;
    private int loanDefaults;
    private int failedCollateral;
    private Money previousTermFiscalBalance;
    private LocalDate wentIntoDebt;

    public Finances() {
        transactions = new ArrayList<>();
        loans = new ArrayList<>();
        assets = new ArrayList<>();
        loanDefaults = 0;
        failedCollateral = 0;
        previousTermFiscalBalance = null;
        wentIntoDebt = null;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(final List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void setLoans(final List<Loan> loans) {
        this.loans = loans;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(final List<Asset> assets) {
        this.assets = assets;
    }

    public int getLoanDefaults() {
        return loanDefaults;
    }

    public void setLoanDefaults(final int loanDefaults) {
        this.loanDefaults = loanDefaults;
    }

    public int getFailedCollateral() {
        return failedCollateral;
    }

    public void setFailedCollateral(final int failedCollateral) {
        this.failedCollateral = failedCollateral;
    }

    public @Nullable Money getPreviousTermFiscalBalance() {
        return previousTermFiscalBalance;
    }

    public void setPreviousTermFiscalBalance(@Nullable final Money previousTermFiscalBalance) {
        this.previousTermFiscalBalance = previousTermFiscalBalance;
    }

    public @Nullable LocalDate getWentIntoDebt() {
        return wentIntoDebt;
    }

    public void setWentIntoDebt(final @Nullable LocalDate wentIntoDebt) {
        this.wentIntoDebt = wentIntoDebt;
    }

    public Money getBalance() {
        Money balance = Money.zero();
        return balance.plus(transactions.stream().map(Transaction::getAmount).collect(Collectors.toList()));
    }

    public Money getLoanBalance() {
        Money balance = Money.zero();
        return balance.plus(loans.stream().map(Loan::determineRemainingValue).collect(Collectors.toList()));
    }

    public boolean isInDebt() {
        return getLoanBalance().isPositive();
    }

    public int getFullYearsInDebt(LocalDate date) {
        if (wentIntoDebt == null) {
            return 0;
        } else {
            return Math.toIntExact(ChronoUnit.YEARS.between(wentIntoDebt, date));
        }
    }

    public int getPartialYearsInDebt(LocalDate date) {
        if (wentIntoDebt != null) {
            Period period = Period.between(wentIntoDebt, date);
            if ((period.getMonths() > 0) || (period.getDays() > 0)) {
                return 1;
            }
        }

        return 0;
    }

    public boolean debit(final TransactionType type, final LocalDate date, final Money amount,
                         final String reason) {
        if (getBalance().isLessThan(amount)) {
            return false;
        }
        Transaction t = new Transaction(type, date, amount.multipliedBy(-1), reason);
        transactions.add(t);
        if ((wentIntoDebt != null) && !isInDebt()) {
            wentIntoDebt = null;
        }
        MekHQ.triggerEvent(new TransactionDebitEvent(t));
        return true;
    }

    public void credit(final TransactionType type, final LocalDate date, final Money amount,
                       final String reason) {
        Transaction t = new Transaction(type, date, amount, reason);
        transactions.add(t);
        if ((wentIntoDebt == null) && isInDebt()) {
            wentIntoDebt = date;
        }
        MekHQ.triggerEvent(new TransactionCreditEvent(t));
    }

    /**
     * This function will update the starting amount to the current balance and clear transactions.
     * This will be called at the beginning of each new financial term
     */
    public void newFiscalYear(final Campaign campaign) {
        if (campaign.getCampaignOptions().isNewFinancialYearFinancesToCSVExport()) {
            final String exportFileName = campaign.getName() + " Finances for "
                    + campaign.getCampaignOptions().getFinancialYearDuration().getExportFilenameDateString(campaign.getLocalDate())
                    + '.' + FileType.CSV.getRecommendedExtension();
            exportFinancesToCSV(new File(MekHQ.getCampaignsDirectory().getValue(),
                            exportFileName).getPath(), FileType.CSV.getRecommendedExtension());
        }

        Money carryover = getBalance();
        transactions = new ArrayList<>();

        credit(
                TransactionType.FINANCIAL_TERM_END_CARRYOVER,
                campaign.getLocalDate(),
                carryover,
                resourceMap.getString("FinancialTermEndCarryover.finances")
        );
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
    }

    public void newDay(final Campaign campaign, final LocalDate yesterday, final LocalDate today) {
        // check for a new fiscal year
        if (campaign.getCampaignOptions().getFinancialYearDuration().isEndOfFinancialYear(campaign.getLocalDate())) {
            // clear the ledger
            newFiscalYear(campaign);

            // calculate and store profits
            Money profits = getProfits();

            // pay taxes
            if (campaign.getCampaignOptions().isUseTaxes()) {
                payTaxes(campaign, profits);
            }

            // store profits
        }

        // Handle contract payments
        if (today.getDayOfMonth() == 1) {
            for (Contract contract : campaign.getActiveContracts()) {
                credit(TransactionType.CONTRACT_PAYMENT, today,
                        contract.getMonthlyPayOut(),
                        String.format(resourceMap.getString("MonthlyContractPayment.text"), contract.getName()));
                campaign.addReport(String.format(
                        resourceMap.getString("ContractPaymentCredit.text"),
                        contract.getMonthlyPayOut().toAmountAndSymbolString(),
                        contract.getName()));

                payoutShares(campaign, contract, today);
            }
        }

        // Handle assets
        getAssets().forEach(asset -> asset.processNewDay(campaign, yesterday, today, this));

        // Handle peacetime operating expenses, payroll, and loan payments
        if (today.getDayOfMonth() == 1) {
            if (campaign.getCampaignOptions().isUsePeacetimeCost()) {
                if (!campaign.getCampaignOptions().isShowPeacetimeCost()) {
                    // Do not include salaries as that will be tracked below
                    Money peacetimeCost = campaign.getAccountant().getPeacetimeCost(false);

                    if (debit(TransactionType.MAINTENANCE, today, peacetimeCost,
                            resourceMap.getString("PeacetimeCosts.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCosts.text"),
                                peacetimeCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for operating costs"));
                    }
                } else {
                    Money sparePartsCost = campaign.getAccountant().getMonthlySpareParts();
                    Money ammoCost = campaign.getAccountant().getMonthlyAmmo();
                    Money fuelCost = campaign.getAccountant().getMonthlyFuel();

                    if (debit(TransactionType.MAINTENANCE, today, sparePartsCost,
                            resourceMap.getString("PeacetimeCostsParts.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsParts.text"),
                                sparePartsCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for spare parts"));
                    }

                    if (debit(TransactionType.MAINTENANCE, today, ammoCost,
                            resourceMap.getString("PeacetimeCostsAmmunition.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsAmmunition.text"),
                                ammoCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for training munitions"));
                    }

                    if (debit(TransactionType.MAINTENANCE, today, fuelCost,
                            resourceMap.getString("PeacetimeCostsFuel.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsFuel.text"),
                                fuelCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(String.format(resourceMap.getString("NotImplemented.text"), "for fuel"));
                    }
                }
            }

            if (campaign.getCampaignOptions().isPayForSalaries()) {
                Money payRollCost = campaign.getAccountant().getPayRoll();

                if (debit(TransactionType.SALARIES, today, payRollCost,
                        resourceMap.getString("Salaries.title"))) {
                    campaign.addReport(
                            String.format(resourceMap.getString("Salaries.text"),
                                    payRollCost.toAmountAndSymbolString()));

                    if (campaign.getCampaignOptions().isTrackTotalEarnings()) {
                        for (Person person : campaign.getActivePersonnel()) {
                            person.payPersonSalary(campaign);
                        }
                    }
                } else {
                    campaign.addReport(
                            String.format(resourceMap.getString("NotImplemented.text"), "payroll costs"));
                }
            }

            // Handle overhead expenses
            if (campaign.getCampaignOptions().isPayForOverhead()) {
                Money overheadCost = campaign.getAccountant().getOverheadExpenses();

                if (debit(TransactionType.OVERHEAD, today, overheadCost,
                        resourceMap.getString("Overhead.title"))) {
                    campaign.addReport(String.format(
                            resourceMap.getString("Overhead.text"),
                            overheadCost.toAmountAndSymbolString()));
                } else {
                    campaign.addReport(
                            String.format(resourceMap.getString("NotImplemented.text"), "overhead costs"));
                }
            }
        }

        List<Loan> newLoans = new ArrayList<>();
        for (Loan loan : getLoans()) {
            if (loan.checkLoanPayment(today)) {
                if (debit(TransactionType.LOAN_PAYMENT, today, loan.getPaymentAmount(),
                        String.format(resourceMap.getString("Loan.title"), loan))) {
                    campaign.addReport(resourceMap.getString("Loan.text"),
                            loan.getPaymentAmount().toAmountAndSymbolString(), loan);
                    loan.paidLoan();
                } else {
                    campaign.addReport(resourceMap.getString("Loan.insufficient.report"),
                            loan, loan.getPaymentAmount().toAmountAndSymbolString());
                    loan.setOverdue(true);
                }
            }

            if (loan.getRemainingPayments() > 0) {
                newLoans.add(loan);
            } else {
                campaign.addReport(resourceMap.getString("Loan.paid.report"), loan);
            }
        }

        if ((getWentIntoDebt() != null) && !isInDebt()) {
            setWentIntoDebt(null);
        }

        loans = newLoans;
    }

    /**
     * Calculates the profits for the current fiscal term.
     *
     * @return The profits for the current fiscal term.
     */
    private Money getProfits() {
        Money profits;

        if (getPreviousTermFiscalBalance() == null) {
            setPreviousTermFiscalBalance(getBalance());

            return Money.zero();
        } else {
            profits = getBalance().minus(getPreviousTermFiscalBalance());

            if (profits.isPositive()) {
                return profits;
            } else {
                return Money.zero();
            }
        }
    }

    /**
     * Calculates and pays the taxes for the given campaign based on the profits.
     *
     * @param campaign The campaign for which taxes are to be paid.
     * @param profits  The profits made by the campaign.
     * @return True if the taxes are paid successfully, false otherwise. (included for debugging)
     */
    private boolean payTaxes(Campaign campaign, Money profits) {
        if ((campaign.getCampaignOptions().isUseNotMercenaryExemption())
                && (!campaign.getFaction().isMercenary())) {
            return false;
        }

        if ((campaign.getCampaignOptions().isUseClanExemption())
                && (campaign.getFaction().isClan())) {
            return false;
        }

        Money taxAmount = profits.multipliedBy((double) campaign.getCampaignOptions().getTaxesPercentage() / 100).round();

        return debit(
                TransactionType.TAXES,
                campaign.getLocalDate(),
                taxAmount,
                resourceMap.getString("Taxes.finances")
        );
    }

    private void payoutShares(Campaign campaign, Contract contract, LocalDate date) {
        if (campaign.getCampaignOptions().isUseAtB() && campaign.getCampaignOptions().isUseShareSystem()
                && (contract instanceof AtBContract)) {
            Money shares = contract.getMonthlyPayOut().multipliedBy(((AtBContract) contract).getSharesPct())
                    .dividedBy(100);
            if (debit(TransactionType.SALARIES, date, shares,
                    String.format(resourceMap.getString("ContractSharePayment.text"), contract.getName()))) {
                campaign.addReport(resourceMap.getString("DistributedShares.text"), shares.toAmountAndSymbolString());

                if (campaign.getCampaignOptions().isTrackTotalEarnings()) {
                    int numberOfShares = 0;
                    boolean sharesForAll = campaign.getCampaignOptions().isSharesForAll();
                    for (Person person : campaign.getActivePersonnel()) {
                        numberOfShares += person.getNumShares(campaign, sharesForAll);
                    }

                    Money singleShare = shares.dividedBy(numberOfShares);
                    for (Person person : campaign.getActivePersonnel()) {
                        person.payPersonShares(campaign, singleShare, sharesForAll);
                    }
                }
            } else {
                /*
                 * This should not happen, as the shares payment should be less than the contract
                 * payment that has just been made.
                 */
                campaign.addReport(resourceMap.getString("NotImplemented.text"), "shares");
                LogManager.getLogger().error("Attempted to payout share amount larger than the payment of the contract");
            }
        }
    }

    public Money checkOverdueLoanPayments(Campaign campaign) {
        List<Loan> newLoans = new ArrayList<>();
        Money overdueAmount = Money.zero();
        for (Loan loan : loans) {
            if (loan.isOverdue()) {
                if (debit(TransactionType.LOAN_PAYMENT, campaign.getLocalDate(), loan.getPaymentAmount(),
                        String.format(resourceMap.getString("Loan.title"), loan))) {
                    campaign.addReport(resourceMap.getString("Loan.text"),
                            loan.getPaymentAmount().toAmountAndSymbolString(), loan);
                    loan.paidLoan();
                } else {
                    overdueAmount = overdueAmount.plus(loan.getPaymentAmount());
                }
            }
            if (loan.getRemainingPayments() > 0) {
                newLoans.add(loan);
            } else {
                campaign.addReport(resourceMap.getString("Loan.paid.report"), loan);
            }
        }
        loans = newLoans;
        if ((wentIntoDebt != null) && !isInDebt()) {
            wentIntoDebt = null;
        }
        return overdueAmount;
    }

    public void removeLoan(Loan loan) {
        loans.remove(loan);
        if ((wentIntoDebt != null) && !isInDebt()) {
            wentIntoDebt = null;
        }
    }

    public void defaultOnLoan(Loan loan, boolean paidCollateral) {
        loanDefaults++;
        if (!paidCollateral) {
            failedCollateral++;
        }
        removeLoan(loan);
        MekHQ.triggerEvent(new LoanDefaultedEvent(loan));
    }

    public Money getTotalLoanCollateral() {
        Money amount = Money.zero();
        return amount.plus(loans.stream().map(Loan::determineCollateralAmount).collect(Collectors.toList()));
    }

    public Money getTotalAssetValue() {
        Money amount = Money.zero();
        return amount.plus(assets.stream().map(Asset::getValue).collect(Collectors.toList()));
    }

    public Money getMaxCollateral(Campaign c) {
        return c.getAccountant().getTotalEquipmentValue()
                .plus(getTotalAssetValue())
                .minus(getTotalLoanCollateral());
    }

    //region File I/O
    //region CSV
    public String exportFinancesToCSV(String path, String format) {
        String report;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("Date", "Type", "Description", "Amount", "RunningTotal").build())) {
            Money runningTotal = Money.zero();
            for (Transaction transaction : getTransactions()) {
                runningTotal = runningTotal.plus(transaction.getAmount());
                csvPrinter.printRecord(
                        MekHQ.getMHQOptions().getDisplayFormattedDate(transaction.getDate()),
                        transaction.getType(),
                        transaction.getDescription(),
                        transaction.getAmount(),
                        runningTotal.toAmountAndSymbolString());
            }

            csvPrinter.flush();

            report = String.format(resourceMap.getString("FinanceExport.format"), transactions.size());
        } catch (Exception ex) {
            LogManager.getLogger().error("Error exporting finances to " + format, ex);
            report = "Error exporting finances. See log for details.";
        }

        return report;
    }
    //endregion CSV

    //region XML
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "finances");
        if (!getTransactions().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transactions");
            for (final Transaction transaction : getTransactions()) {
                transaction.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transactions");
        }

        if (!getLoans().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "loans");
            for (final Loan loan : getLoans()) {
                loan.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "loans");
        }

        if (!getAssets().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "assets");
            for (final Asset asset : getAssets()) {
                asset.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "assets");
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loanDefaults", getLoanDefaults());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "failedCollateral", getFailedCollateral());
        if (getWentIntoDebt() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wentIntoDebt", getWentIntoDebt());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "finances");
    }

    public static Finances generateInstanceFromXML(Node wn) {
        Finances retVal = new Finances();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                switch (wn2.getNodeName()) {
                    case "transactions":
                        retVal.setTransactions(parseTransactionsFromXML(wn2));
                        break;
                    case "loans":
                        retVal.setLoans(parseLoansFromXML(wn2));
                        break;
                    case "assets":
                        retVal.setAssets(parseAssetsFromXML(wn2));
                        break;
                    case "loanDefaults":
                        retVal.setLoanDefaults(Integer.parseInt(wn2.getTextContent().trim()));
                        break;
                    case "failedCollateral":
                        retVal.setFailedCollateral(Integer.parseInt(wn2.getTextContent().trim()));
                        break;
                    case "wentIntoDebt":
                        retVal.setWentIntoDebt(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                        break;
                    //region Legacy
                    case "transaction": // Removed in 0.49.8
                        retVal.getTransactions().add(Transaction.generateInstanceFromXML(wn2));
                        break;
                    case "loan": // Removed in 0.49.8
                        retVal.getLoans().add(Loan.generateInstanceFromXML(wn2));
                        break;
                    case "asset": // Removed in 0.49.8
                        retVal.getAssets().add(Asset.generateInstanceFromXML(wn2));
                        break;
                    //endregion Legacy
                    default:
                        break;
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }

        return retVal;
    }

    private static List<Transaction> parseTransactionsFromXML(final Node wn) {
        if (!wn.hasChildNodes()) {
            return new ArrayList<>();
        }

        final NodeList nl = wn.getChildNodes();
        return IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(node -> "transaction".equals(node.getNodeName()))
                .map(Transaction::generateInstanceFromXML)
                .collect(Collectors.toList());
    }

    private static List<Loan> parseLoansFromXML(final Node wn) {
        if (!wn.hasChildNodes()) {
            return new ArrayList<>();
        }

        final NodeList nl = wn.getChildNodes();
        return IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(node -> "loan".equals(node.getNodeName()))
                .map(Loan::generateInstanceFromXML)
                .collect(Collectors.toList());
    }

    private static List<Asset> parseAssetsFromXML(final Node wn) {
        if (!wn.hasChildNodes()) {
            return new ArrayList<>();
        }

        final NodeList nl = wn.getChildNodes();
        return IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(node -> "asset".equals(node.getNodeName()))
                .map(Asset::generateInstanceFromXML)
                .collect(Collectors.toList());
    }
    //endregion XML
    //endregion File I/O
}
