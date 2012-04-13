package suddenStop;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

public class FirmState implements Cloneable {

	double initialFUC;
	double rDEfficiency;
	double targetLeverage;
	double maxExternalEquity;
	double learningRate;
	/*
	 * capital = debt + equity debt could be <0 meaning cash excess
	 */
	double capital;
	double debt;
	double externalEquityAvailable;
	double availableFundsFromOperations;
	double capitalProductivity;
	double born;
	double firstUnitCost;
	double acumQ;
	double structuralCost;
	double fixedCost;
	double profit;
	double performance;
	double quantity;
	double rD;

	public FirmState() {

		born = GetTickCount();

		/*
		 * Obtain independent variables
		 */
		firstUnitCost = Firm.independentVarsManager.getRandfirstUnitCost();
		initialFUC = firstUnitCost;
		rDEfficiency = Firm.independentVarsManager.getRandRDEfficiency();
		targetLeverage = Firm.independentVarsManager.getRandTargetLeverage();
		// External Equity is a percentage of capital
		maxExternalEquity = Firm.independentVarsManager.getRandEquityAccess();
		learningRate = Firm.independentVarsManager.getRandLearningRate();

		/*
		 * Obtain other random state variables
		 */
		capital = max((Double) GetParameter("minimumCapital"),
				Firm.supplyManager.iniKNormal.nextDouble());

		/*
		 * Read constant state variables
		 */
		capitalProductivity = (Double) GetParameter("capitalProductivity");
		structuralCost = (Double) GetParameter("structuralCost");
		fixedCost = (Double) GetParameter("fixedCost");
		performance = (Double) GetParameter("initialPerformance");

		/*
		 * Initialize the remaining variables
		 */
		debt = capital * targetLeverage;

	}

	@Override
	public FirmState clone() {
		try {
			return (FirmState) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public double getLRExpon() {
		return log(learningRate) / log(2.0);
	}

	public double resetExternalEquityAvailable() {

		if (Demand.isSS() && !(Boolean) GetParameter("equityOnSS")) {
			return 0.0;
		} else {
			return maxExternalEquity * getCapital();
		}

	}

	public double getDebtAvailableByNewEquity(double newEquity,
			CashUsage cashUsage) {
		return getDebtAvailable(newEquity, true, cashUsage);
	}

	public double getDebtAvailableByNewCapital(double newEquity,
			CashUsage cashUsage) {
		return getDebtAvailable(newEquity, false, cashUsage);
	}

	/*
	 * Returns the new debt available assuming newEquity is raised
	 * 
	 * The value returned could be negative meaning that more equity than
	 * newEquity needs to be raised in order to fulfill leverage constrain
	 */
	private double getDebtAvailable(double newFunds, boolean isNewEquity,
			CashUsage cashUsage) {
		double newDebt = 0;
		double totalLeverage;

		/*
		 * available leverage is the debt capacity not used plus the extra cash
		 * over the cash needed according to policy
		 * 
		 * availLeverage could be < 0 meaning the leverage constrain is not
		 * currently fulfilled
		 */
		double availLeverage = getTargetLeverage() * getCapital() - getDebt()
				+ getCash();

		if (isNewEquity) {
			// newFunds is equity increase
			totalLeverage = (availLeverage + newFunds * getTargetLeverage())
					/ (1 - getTargetLeverage());
		} else {
			// newFunds is capital increase
			totalLeverage = availLeverage + getTargetLeverage() * newFunds;
		}

		if (Demand.isSS())
			cashUsage = CashUsage.ONLY_CASH;

		switch (cashUsage) {
		case LEVERAGE:
			newDebt = totalLeverage;
			break;

		case ONLY_CASH:
			newDebt = getCash();
			break;

		case CASH:
			newDebt = max(getCash(), totalLeverage);

		}

		return newDebt;

	}

	// It includes equity cost
	public double getMedCost() {
		return (getTotVarCost() + getTotFixedCost() + getInterest() + getExpectedEquityRetribution())
				/ getQuantity();
	}

	// Calculates cost using learning curve: cost of new acummulated Q minus
	// old acummulated Q. See http://maaw.info/LearningCurveSummary.htm
	// (Wright model)
	public double getTotVarCost() {

		return firstUnitCost
				* (pow(getAcumQ() + getQuantity(), 1.0 + getLRExpon()) - pow(
						getAcumQ(), 1.0 + getLRExpon()));

	}

	public double getTotFixedCost() {

		return (Double) GetParameter("depreciation") * getCapital() + getRD()
				+ getStructuralCost() + getFixedCost();

	}

	public double getProfit() {
		return profit;
	}

	public double getCapital() {
		return capital;
	}

	public double getDebt() {
		return max(0.0, debt);
	}

	public double getCash() {
		return max(0.0, -debt);
	}

	public double getEquity() {
		return capital - debt;
	}

	public double getLeverage() {
		return getDebt() / getAssets();
	}

	public double getNetLeverage() {
		return debt / capital;
	}

	public double getAssets() {
		return getCapital() + getCash();
	}

	public double getWACC() {
		return (Double) GetParameter("wACC");
	}

	public double getBorn() {
		return born;
	}

	public double getAcumQ() {
		return acumQ;
	}

	public double getFirstUnitCost() {
		return firstUnitCost;
	}

	public double getInitialFUC() {
		return initialFUC;
	}

	public double getTargetLeverage() {
		return targetLeverage;
	}

	public double getMaxExternalEquity() {
		return maxExternalEquity;
	}

	public double getCapitalProductivity() {
		return capitalProductivity;
	}

	public double getrDEfficiency() {
		return rDEfficiency;
	}

	public double getCostOfDebt() {
		return (Double) GetParameter("costOfDebt");
	}

	// It is assumed M&M, ie. WACC is constant
	public double getCostOfEquity() {
		return getWACC() + (getWACC() - getCostOfDebt())
				* (1 + getDebt() / getEquity());
	}

	public double getStructuralCost() {
		return structuralCost * capital;
	}

	public double getFixedCost() {
		return fixedCost;
	}

	public double getEBITDA() {
		return profit + getInterest() + getDepreciation();
	}

	public double getEBIT() {
		return getProfit() + getInterest();
	}

	public double getInterest() {
		return getCostOfDebt() * getDebt();
	}

	public double getExpectedEquityRetribution() {
		return getCostOfEquity() * getEquity();
	}

	public double getDepreciation() {
		return (Double) GetParameter("depreciation") * getCapital();
	}

	public double getROE() {
		return getProfit() / getEquity();
	}

	public double getROA() {
		return getProfit() / getAssets();
	}

	public double getRONA() {
		return getEBIT() / (getDebt() + getEquity());
	}

	public double getPerformance() {
		return performance;
	}

	public void setBorn(double born) {
		this.born = born;
	}

	public void setAcumQ(double acumQ) {
		this.acumQ = acumQ;
	}

	public void setFirstUnitCost(double firstUnitCost) {
		this.firstUnitCost = firstUnitCost;
	}

	public void setCapital(double capital) {
		this.capital = capital;
	}

	public void setDebt(double debt) {
		this.debt = debt;
	}

	public void setTargetLeverage(double targetLeverage) {
		this.targetLeverage = targetLeverage;
	}

	public void setMaxExternalEquity(double maxExternalEquity) {
		this.maxExternalEquity = maxExternalEquity;
	}

	public void setEquityRaised(double equityRaised) {
		this.externalEquityAvailable = equityRaised;
	}

	public void setCapitalProductivity(double capitalProductivity) {
		this.capitalProductivity = capitalProductivity;
	}

	public void setrDEfficiency(double rDEfficiency) {
		this.rDEfficiency = rDEfficiency;
	}

	public void setProfit(double profit) {
		this.profit = profit;
	}

	public void setPerformance(double performance) {
		this.performance = performance;
	}

	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	public double getRD() {
		return rD;
	}

	public double getExternalEquityAvailable() {
		return externalEquityAvailable;
	}

	public double getAvailableFundsFromOperations() {
		return availableFundsFromOperations;
	}

	public void setRD(double rD) {
		this.rD = rD;
	}

	public double getNetDebt() {
		return debt;
	}

	public double getRDEfficiency() {
		return rDEfficiency;
	}

	public double getLearningRate() {
		return learningRate;
	}
}