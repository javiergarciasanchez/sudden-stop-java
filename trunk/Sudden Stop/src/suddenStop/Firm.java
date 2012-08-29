/**
 * 
 * This file was automatically generated by the Repast Simphony Agent Editor.
 * Please see http://repast.sourceforge.net/ for details.
 * 
 */

/**
 *
 * Set the package name.
 *
 */
package suddenStop;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;
import static repast.simphony.essentials.RepastEssentials.RemoveAgentFromModel;
import static suddenStop.CashUsage.CASH;
import static suddenStop.CashUsage.LEVERAGE;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;

public class Firm {

	public static SupplyManager supplyManager;

	private FirmState currentState, nextState;
	private boolean toBeKilled = false;
	private ArrayList<Cohort> shadowFirms;

	protected static long agentIDCounter = 1;
	protected long agentIntID = agentIDCounter++;
	protected String agentID = "Firm " + agentIntID;

	public Firm(Context<Object> context) {

		context.add(this);

		nextState = new FirmState();

		nextState.quantityPerPeriod = nextState.getCapital()
				* nextState.getCapitalProductivityPerPeriod();

		currentState = nextState.clone();

		if (!RunEnvironment.getInstance().isBatch()) {
			shadowFirms = new ArrayList<Cohort>(3);
			addToCohorts(context, 3);
		}

	}

	private void addToCohorts(Context<Object> context, int cohorts) {
		Object c = null;
		switch (getLevCohort(cohorts)) {
		case 1:
			c = new Lev1(this);
			context.add(c);
			break;
		case 2:
			c = new Lev2(this);
			context.add(c);
			break;
		case 3:
			c = new Lev3(this);
			context.add(c);
			break;
		}
		shadowFirms.add((Cohort) c);

	}

	private int getLevCohort(int cohorts) {
		
		double min = (Double) GetParameter("leverageMin");
		double max = (Double) GetParameter("leverageMax");
		double cohortSize = (max - min) / cohorts;
		
		return   (int) (Math.floor((getNetLeverage() - min) / cohortSize) + 1);
	}

	public void moveToNextState() {

		// apply innovation
		nextState.firstUnitCost = nextState.firstUnitCost
				/ ((nextState.getRDPerPeriod() + 1.0) * nextState.rDEfficiency)
				* supplyManager.innovationErrorNormal.nextDouble();

		// Define quantityPerPeriod offered
		nextState.quantityPerPeriod = nextState.getCapital()
				* nextState.getCapitalProductivityPerPeriod();

		nextState.resetExternalEquityAvailable();

		currentState = nextState.clone();

	}

	/**
	 * Estimates if nextDecision would be an exit given nextState and price It
	 * works like ProcessResponseToDemand but without changing the current
	 * situation
	 */
	public boolean estimateResponseToDemand(double price) {

		FirmState tmpSt = nextState.clone();

		return processProfit(tmpSt, price);

	}

	/**
	 * 
	 * Process demand respond and returns false if firm exits the industry,
	 * otherwise returns true
	 * 
	 */
	public void processResponseToDemand(double price) {

		if (!processProfit(currentState, price)) {
			toBeKilled = true;
			return;
		}

		// From here onward all modifications are done in nextState
		nextState = currentState.clone();

		acumulateVariables();

		if (!checkMinCapital(price)) {
			toBeKilled = true;
		}

	}

	public void killShadowFirms() {
		if (RunEnvironment.getInstance().isBatch())
			return;

		for (Cohort c : shadowFirms) {
			RemoveAgentFromModel(c);
		}
	}

	public void planNextYear(double price) {

		raiseFunds(nextState, LEVERAGE, getNetInvestment(currentState, price));

		selectRD();

	}

	/*
	 * Returns true is profit was processed OK, i.e. if it is not an exit
	 */
	private boolean processProfit(FirmState st, double price) {
		boolean perfStatus, cashStatus;

		double profit = calcProfitPerPeriod(st, price);

		st.profitPerPeriod = profit;

		/*
		 * Check cash Funds Generated by Operations (fgo) should be > 0 or I
		 * need funds to compensate fgo < 0 funds should be external or cash
		 * excess kept for this situation
		 */
		double fgo = profit + st.getDepreciationPerPeriod();

		if (fgo < 0) {
			st.setAvailableFundsFromOperationsPerPeriod(0.0);

			/*
			 * Raise funds will add -fgo to capital, so to keep capital
			 * unaffected by loss -fgo should be subtracted before
			 */
			st.capital -= -fgo;
			cashStatus = (raiseFunds(st, CASH, -fgo) >= -fgo);
		} else {
			st.setAvailableFundsFromOperationsPerPeriod(fgo);
			cashStatus = true;
		}

		// Check Performance
		if (!Demand.isSS()) {
			perfStatus = (calcPerformance(st) >= getMinimumPerformance(st));
		} else {
			perfStatus = true;
		}

		return (perfStatus && cashStatus);
	}

	private double getMinimumPerformance(FirmState st) {
		return st.getMinimumPerformance();
	}

	private double calcProfitPerPeriod(FirmState st, double price) {

		return price * st.getQuantityPerPeriod() - st.getTotVarCostPerPeriod()
				- st.getTotFixedCostPerPeriod() - st.getInterestPerPeriod();
	}

	private double calcPerformance(FirmState st) {
		double w = (Double) GetParameter("performanceWeight");
		return w * st.getPerformance() + (1 - w) * st.getROI();
	}

	private void acumulateVariables() {

		nextState.setPerformance(calcPerformance(currentState));

		nextState.setAcumQ(currentState.getAcumQ()
				+ currentState.getQuantityPerPeriod());

		nextState.setAcumProfit(currentState.getAcumProfit()
				+ currentState.getProfitPerPeriod());

	}

	private boolean checkMinCapital(double price) {

		// Applies depreciation
		nextState.capital = currentState.getCapital()
				- currentState.getDepreciationPerPeriod();

		// Meet minimum capital
		double minimalNeeds = (Double) GetParameter("minimumCapital")
				- nextState.getCapital();

		if (minimalNeeds > 0) {
			return (raiseFunds(nextState, CASH, minimalNeeds) >= minimalNeeds);
		} else {
			return true;
		}

	}

	private double raiseFunds(FirmState st, CashUsage cashUsage, double funds) {

		double fgoUsed = st.getAvailableFundsFromOperationsPerPeriod();
		double externalEquityUsed = st.getExternalEquityAvailablePerPeriod();

		double debtUsed = st.getDebtAvailableByNewEquity(fgoUsed
				+ externalEquityUsed, cashUsage);

		/*
		 * Uses pecking order (but respecting target leverage) Uses: debt, fgo
		 * and then external equity
		 */
		if (debtUsed + fgoUsed + externalEquityUsed > funds) {
			debtUsed = min(st.getDebtAvailableByNewCapital(funds, cashUsage),
					funds);
			fgoUsed = min(fgoUsed, funds - debtUsed);
			externalEquityUsed = funds - debtUsed - fgoUsed;
		}

		st.debt += debtUsed;
		st.capital += (fgoUsed + externalEquityUsed + debtUsed);
		st.setAvailableFundsFromOperationsPerPeriod(st
				.getAvailableFundsFromOperationsPerPeriod() - fgoUsed);
		st.setExternalEquityAvailablePerPeriod(st
				.getExternalEquityAvailablePerPeriod() - externalEquityUsed);

		return fgoUsed + externalEquityUsed + debtUsed;

	}

	/*
	 * Maximizes Economic Profit
	 * 
	 * includes the cost of equity invested, not cash excess
	 * 
	 * Returns the increment suggested. It is >= than zero
	 */
	private double getNetInvestment(FirmState st, double price) {
		double optCapIncrPercent;

		optCapIncrPercent = (Double) GetParameter("investmentParam")
				* (1 - getOptimalMarkUp() * getMarginalCost(st) / price);

		// net investment should be >=0
		return max(
				optCapIncrPercent * st.getCapital()
						+ st.getDepreciationPerPeriod(), 0.0);

	}

	private double getMarginalCost(FirmState st) {
		int periods = (Integer) GetParameter("periods");
		double wACCxPer = st.getWACC() / periods;
		double deprecxPer = (Double) GetParameter("depreciation") / periods;

		return st.getFirstUnitCost()
				* (1.0 + st.getLRExpon())
				* pow(st.getAcumQ() + st.getQuantityPerPeriod(),
						st.getLRExpon()) + st.getMinVarCost()
				+ (wACCxPer + deprecxPer)
				/ st.getCapitalProductivityPerPeriod();

	}

	private void selectRD() {

		// Then new R&D is determined to optimize First unit cost.
		double optRD = pow(
				nextState.getFirstUnitCost()
						/ nextState.getRDEfficiency()
						* (pow(nextState.getAcumQ()
								+ nextState.getQuantityPerPeriod(),
								1.0 + nextState.getLRExpon()) - pow(
								nextState.getAcumQ(),
								1.0 + nextState.getLRExpon())), 0.5) - 1.0;

		/*
		 * There is a minimum amount of RD to make FUC decrease
		 */
		nextState
				.setRDPerPeriod(max(1.0 / nextState.rDEfficiency - 1.0, optRD));

	}

	public double getOptimalMarkUp() {
		double mktSh = getMktShare();
		double demElast = (Double) GetParameter("demandElasticity");
		double supElast = (Double) GetParameter("supplyElasticity");

		return (demElast + (1 - mktSh) * supElast)
				/ (demElast + (1 - mktSh) * supElast - mktSh);

	}

	public double getMarginalCost() {
		return getMarginalCost(currentState);
	}

	public boolean isToBeKilled() {
		return toBeKilled;
	}

	public double getWACC() {
		return currentState.getWACC();
	}

	public double getAge() {
		return (GetTickCount() - currentState.getBorn())
				/ (Integer) GetParameter("periods");
	}

	// It includes equity cost
	public double getMedCost() {
		return currentState.getMedCost();
	}

	public double getEBITPerPeriod() {
		return currentState.getEBITPerPeriod();
	}

	public double getEBITDAPerPeriod() {
		return currentState.getEBITDAPerPeriod();
	}

	public double getInterestPerPeriod() {
		return currentState.getInterestPerPeriod();
	}

	public double getExpectedEquityRetributionPerPeriod() {
		return currentState.getExpectedEquityRetributionPerPeriod();
	}

	public double getExpectedCapitalRetributionPerPeriod() {
		return currentState.getExpectedCapitalRetributionPerPeriod();
	}

	public double getTotFixedCostPerPeriod() {
		return currentState.getTotFixedCostPerPeriod();
	}

	public double getCashFixedCostsPerPeriod() {
		return currentState.getCashFixedCostsPerPeriod();
	}

	public double getTotVarCostPerPeriod() {
		return currentState.getTotVarCostPerPeriod();
	}

	public double getROE() {
		return currentState.getROE();
	}

	public double getROI() {
		return currentState.getROI();
	}

	public double getRONA() {
		return currentState.getRONA();
	}

	public double getProfitPerPeriod() {
		return currentState.getProfitPerPeriod();
	}

	public double getPrice() {
		return supplyManager.price;
	}

	public double getCapital() {
		return currentState.getCapital();
	}

	public double getCash() {
		return currentState.getCash();
	}

	public double getAssets() {
		return currentState.getAssets();
	}

	public double getDebt() {
		return currentState.getDebt();
	}

	public double getNetDebt() {
		return currentState.getNetDebt();
	}

	public double getLeverage() {
		return currentState.getLeverage();
	}

	public double getNetLeverage() {
		return currentState.getNetLeverage();
	}

	public double getEquity() {
		return currentState.getEquity();
	}

	public double getPerformance() {
		return currentState.getPerformance();
	}

	public double getDepreciationPerPeriod() {
		return currentState.getDepreciationPerPeriod();
	}

	public double getFirstUnitCost() {
		return currentState.getFirstUnitCost();
	}

	public double getQuantityPerPeriod() {
		return currentState.getQuantityPerPeriod();
	}

	public double getSalesPerPeriod() {
		return currentState.getQuantityPerPeriod() * supplyManager.price;
	}

	public double getMktShare() {
		return currentState.getQuantityPerPeriod()
				/ supplyManager.totalQuantityPerPeriod;
	}

	public double getAcumQ() {
		return currentState.getAcumQ();
	}

	public double getAcumProfit() {
		return currentState.getAcumProfit();
	}

	public double getRDPerPeriod() {
		return currentState.getRDPerPeriod();
	}

	public double getInitialFUC() {
		return currentState.getInitialFUC();
	}

	public double getRDEfficiency() {
		return currentState.getRDEfficiency();
	}

	public double getTargetLeverage() {
		return currentState.getTargetLeverage();
	}

	public double getLearningRate() {
		return currentState.getLearningRate();
	}

	public double getMinVarCost() {
		return currentState.getMinVarCost();
	}

	public double getBorn() {
		return currentState.getBorn();
	}

	public double getBornInYears() {
		return currentState.getBornInYears();
	}

	public String toString() {
		return this.agentID;
	}

}
