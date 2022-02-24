/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.LoanDefaultedEvent;
import mekhq.campaign.event.TransactionCreditEvent;
import mekhq.campaign.event.TransactionDebitEvent;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.io.FileType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Finances {
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());

    private List<Transaction> transactions;
    private List<Loan> loans;
    private List<Asset> assets;
    private int loanDefaults;
    private int failCollateral;
    private LocalDate wentIntoDebt;

    public Finances() {
        transactions = new ArrayList<>();
        loans = new ArrayList<>();
        assets = new ArrayList<>();
        loanDefaults = 0;
        failCollateral = 0;
        wentIntoDebt = null;
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
     * This function will update the starting amount to the current balance and
     * clear transactions By default, this will be called up on Jan 1 of every year
     * in order to keep the transaction record from becoming too large
     */
    public void newFiscalYear(final Campaign campaign) {
        if (campaign.getCampaignOptions().getNewFinancialYearFinancesToCSVExport()) {
            final String exportFileName = campaign.getName() + " Finances for "
                    + campaign.getCampaignOptions().getFinancialYearDuration().getExportFilenameDateString(campaign.getLocalDate())
                    + "." + FileType.CSV.getRecommendedExtension();
            exportFinancesToCSV(new File(MekHQ.getCampaignsDirectory().getValue(),
                            exportFileName).getPath(), FileType.CSV.getRecommendedExtension());
        }

        Money carryover = getBalance();
        transactions = new ArrayList<>();
        credit(TransactionType.CARRYOVER, campaign.getLocalDate(), carryover, resourceMap.getString("Carryover.text"));
    }

    public List<Transaction> getAllTransactions() {
        return transactions;
    }

    public List<Loan> getAllLoans() {
        return loans;
    }

    public List<Asset> getAllAssets() {
        return assets;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "finances");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "loanDefaults", loanDefaults);
        for (Transaction transaction : getAllTransactions()) {
            transaction.writeToXML(pw1, indent + 1);
        }
        for (final Loan loan : getAllLoans()) {
            loan.writeToXML(pw1, indent + 1);
        }
        for (Asset asset : getAllAssets()) {
            asset.writeToXML(pw1, indent + 1);
        }
        if (wentIntoDebt != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "wentIntoDebt",
                    MekHqXmlUtil.saveFormattedDate(wentIntoDebt));
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "finances");
    }

    public static Finances generateInstanceFromXML(Node wn) {
        Finances retVal = new Finances();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("transaction")) {
                    retVal.transactions.add(Transaction.generateInstanceFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("loan")) {
                    retVal.loans.add(Loan.generateInstanceFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("asset")) {
                    retVal.assets.add(Asset.generateInstanceFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("loanDefaults")) {
                    retVal.loanDefaults = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("wentIntoDebt")) {
                    retVal.wentIntoDebt = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }

        return retVal;
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
    }

    public void newDay(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Accountant accountant = campaign.getAccountant();

        // check for a new fiscal year
        if (campaign.getCampaignOptions().getFinancialYearDuration().isEndOfFinancialYear(campaign.getLocalDate())) {
            // clear the ledger
            newFiscalYear(campaign);
        }

        // Handle contract payments
        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            for (Contract contract : campaign.getActiveContracts()) {
                credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(),
                        contract.getMonthlyPayOut(),
                        String.format(resourceMap.getString("MonthlyContractPayment.text"), contract.getName()));
                campaign.addReport(String.format(
                        resourceMap.getString("ContractPaymentCredit.text"),
                        contract.getMonthlyPayOut().toAmountAndSymbolString(),
                        contract.getName()));

                payoutShares(campaign, contract, campaign.getLocalDate());
            }
        }

        // Handle assets
        for (final Asset asset : assets) {
            if (asset.getFinancialTerm().isAnnually() && (campaign.getLocalDate().getDayOfYear() == 1)) {
                credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(), asset.getIncome(),
                        "Income from " + asset.getName());
                campaign.addReport(String.format(
                        resourceMap.getString("AssetPayment.text"),
                        asset.getIncome().toAmountAndSymbolString(),
                        asset.getName()));
            } else if (asset.getFinancialTerm().isMonthly() && (campaign.getLocalDate().getDayOfMonth() == 1)) {
                credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(), asset.getIncome(),
                        "Income from " + asset.getName());
                campaign.addReport(String.format(
                        resourceMap.getString("AssetPayment.text"),
                        asset.getIncome().toAmountAndSymbolString(),
                        asset.getName()));
            }
        }

        // Handle peacetime operating expenses, payroll, and loan payments
        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            if (campaignOptions.usePeacetimeCost()) {
                if (!campaignOptions.showPeacetimeCost()) {
                    // Do not include salaries as that will be tracked below
                    Money peacetimeCost = accountant.getPeacetimeCost(false);

                    if (debit(TransactionType.MAINTENANCE, campaign.getLocalDate(), peacetimeCost,
                            resourceMap.getString("PeacetimeCosts.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCosts.text"),
                                peacetimeCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for operating costs"));
                    }
                } else {
                    Money sparePartsCost = accountant.getMonthlySpareParts();
                    Money ammoCost = accountant.getMonthlyAmmo();
                    Money fuelCost = accountant.getMonthlyFuel();

                    if (debit(TransactionType.MAINTENANCE, campaign.getLocalDate(), sparePartsCost,
                            resourceMap.getString("PeacetimeCostsParts.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsParts.text"),
                                sparePartsCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for spare parts"));
                    }

                    if (debit(TransactionType.MAINTENANCE, campaign.getLocalDate(), ammoCost,
                            resourceMap.getString("PeacetimeCostsAmmunition.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsAmmunition.text"),
                                ammoCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(
                                String.format(resourceMap.getString("NotImplemented.text"), "for training munitions"));
                    }

                    if (debit(TransactionType.MAINTENANCE, campaign.getLocalDate(), fuelCost,
                            resourceMap.getString("PeacetimeCostsFuel.title"))) {
                        campaign.addReport(String.format(
                                resourceMap.getString("PeacetimeCostsFuel.text"),
                                fuelCost.toAmountAndSymbolString()));
                    } else {
                        campaign.addReport(String.format(resourceMap.getString("NotImplemented.text"), "for fuel"));
                    }
                }
            }

            if (campaignOptions.payForSalaries()) {
                Money payRollCost = accountant.getPayRoll();

                if (debit(TransactionType.SALARIES, campaign.getLocalDate(), payRollCost,
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
            if (campaignOptions.payForOverhead()) {
                Money overheadCost = accountant.getOverheadExpenses();

                if (debit(TransactionType.OVERHEAD, campaign.getLocalDate(), overheadCost,
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
        for (Loan loan : loans) {
            if (loan.checkLoanPayment(campaign.getLocalDate())) {
                if (debit(TransactionType.LOAN_PAYMENT, campaign.getLocalDate(), loan.getPaymentAmount(),
                        String.format(resourceMap.getString("Loan.title"), loan))) {
                    campaign.addReport(String.format(
                            resourceMap.getString("Loan.text"),
                            loan.getPaymentAmount().toAmountAndSymbolString(), loan));
                    loan.paidLoan();
                } else {
                    campaign.addReport(String.format(
                            resourceMap.getString("Loan.insufficient"),
                            loan.getPaymentAmount().toAmountAndSymbolString()));
                    loan.setOverdue(true);
                }
            }
            if (loan.getRemainingPayments() > 0) {
                newLoans.add(loan);
            } else {
                campaign.addReport(String.format(resourceMap.getString("Loan.paid"), loan));
            }
        }
        if ((wentIntoDebt != null) && !isInDebt()) {
            wentIntoDebt = null;
        }
        loans = newLoans;
    }


    private void payoutShares(Campaign campaign, Contract contract, LocalDate date) {
        if (campaign.getCampaignOptions().getUseAtB() && campaign.getCampaignOptions().getUseShareSystem()
                && (contract instanceof AtBContract)) {
            Money shares = contract.getMonthlyPayOut().multipliedBy(((AtBContract) contract).getSharesPct())
                    .dividedBy(100);
            if (debit(TransactionType.SALARIES, date, shares,
                    String.format(resourceMap.getString("ContractSharePayment.text"), contract.getName()))) {
                campaign.addReport(String.format(resourceMap.getString("DistributedShares.text"),
                        shares.toAmountAndSymbolString()));

                if (campaign.getCampaignOptions().isTrackTotalEarnings()) {
                    int numberOfShares = 0;
                    boolean sharesForAll = campaign.getCampaignOptions().getSharesForAll();
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
                campaign.addReport(String.format(resourceMap.getString("NotImplemented.text"), "shares"));
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
                    campaign.addReport(String.format(
                            resourceMap.getString("Loan.text"),
                            loan.getPaymentAmount().toAmountAndSymbolString(), loan));
                    loan.paidLoan();
                } else {
                    overdueAmount = overdueAmount.plus(loan.getPaymentAmount());
                }
            }
            if (loan.getRemainingPayments() > 0) {
                newLoans.add(loan);
            } else {
                campaign.addReport(String.format(resourceMap.getString("Loan.paid"), loan));
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
            failCollateral++;
        }
        removeLoan(loan);
        MekHQ.triggerEvent(new LoanDefaultedEvent(loan));
    }

    public int getLoanDefaults() {
        return loanDefaults;
    }

    public int getFailedCollateral() {
        return failCollateral;
    }

    public Money getTotalLoanCollateral() {
        Money amount = Money.zero();
        return amount.plus(loans.stream().map(Loan::determineCollateralAmount).collect(Collectors.toList()));
    }

    public Money getTotalAssetValue() {
        Money amount = Money.zero();
        return amount.plus(assets.stream().map(Asset::getValue).collect(Collectors.toList()));
    }

    public void setAssets(List<Asset> newAssets) {
        assets = newAssets;
    }

    public Money getMaxCollateral(Campaign c) {
        return c.getAccountant().getTotalEquipmentValue()
                .plus(getTotalAssetValue())
                .minus(getTotalLoanCollateral());
    }

    public String exportFinancesToCSV(String path, String format) {
        String report;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Date", "Type", "Description", "Amount", "RunningTotal"))) {

            Money runningTotal = Money.zero();
            for (Transaction transaction : getAllTransactions()) {
                runningTotal = runningTotal.plus(transaction.getAmount());
                csvPrinter.printRecord(
                        MekHQ.getMHQOptions().getDisplayFormattedDate(transaction.getDate()),
                        transaction.getType(),
                        transaction.getDescription(),
                        transaction.getAmount(),
                        runningTotal.toAmountAndNameString());
            }

            csvPrinter.flush();

            report = transactions.size() + resourceMap.getString("FinanceExport.text");
        } catch (IOException ioe) {
            LogManager.getLogger().info("Error exporting finances to " + format);
            report = "Error exporting finances. See log for details.";
        }

        return report;
    }
}
