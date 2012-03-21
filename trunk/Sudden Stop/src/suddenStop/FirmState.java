package suddenStop;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

public class FirmState implements Cloneable {

	double born;
	double expon;
	double acumQ;
	double firstUnitCost;
	/*
	 * capital = debt + equity debt could be <0 meaning cash excess
	 */
	double capital;
	double debt;
	double targetLeverage;
	double maxExternalEquity;
	double equityAvailable;
	double capitalProductivity;
	double rDEfficiency;
	double costOfDebt;
	double costOfEquity;
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
		rDEfficiency = Firm.independentVarsManager.getRandRDEfficiency();
		targetLeverage = Firm.independentVarsManager.getRandTargetLeverage();
		// External Equity is a percentage of capital
		maxExternalEquity = Firm.independentVarsManager.getRandEquityAccess();
		// Learning Rate is used to determine exponent of learning curve
		expon = log(Firm.independentVarsManager.getRandLearningRate())
				/ log(2.0);

		/*
		 * Obtain other random state variables
		 */
		capital = max((Double) GetParameter("minimumCapital"),
				Firm.supplyManager.iniKNormal.nextDouble());

		/*
		 * Read constant state variables
		 */
		capitalProductivity = (Double) GetParameter("capitalProductivity");
		costOfDebt = (Double) GetParameter("costOfDebt");
		costOfEquity = (Double) GetParameter("costOfEquity");
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

	public double getAssets() {
		return getCapital() + getCash();
	}

	public double getBorn() {
		return born;
	}

	public double getExpon() {
		return expon;
	}

	public double getAcumQ() {
		return acumQ;
	}

	public double getFirstUnitCost() {
		return firstUnitCost;
	}

	public double getTargetLeverage() {
		return targetLeverage;
	}

	public double getMaxExternalEquity() {
		return maxExternalEquity;
	}

	public double getEquityRaised() {
		return equityAvailable;
	}

	public double getCapitalProductivity() {
		return capitalProductivity;
	}

	public double getrDEfficiency() {
		return rDEfficiency;
	}

	public double getCostOfDebt() {
		return costOfDebt;
	}

	public double getCostOfEquity() {
		return costOfEquity;
	}

	public double getFixedCost() {
		return fixedCost;
	}

	public double getPerformance() {
		return performance;
	}

	public void setBorn(double born) {
		this.born = born;
	}

	public void setExpon(double expon) {
		this.expon = expon;
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
	
	public double getExternalEquityAvailable(){

		if (Demand.isSS() && !(Boolean) GetParameter("equityOnSS")) {
			return 0.0;
		}

		return maxExternalEquity * getCapital();

	}
	
	public void setMaxExternalEquity(double maxExternalEquity) {
		this.maxExternalEquity = maxExternalEquity;
	}

	public void setEquityRaised(double equityRaised) {
		this.equityAvailable = equityRaised;
	}

	public void setCapitalProductivity(double capitalProductivity) {
		this.capitalProductivity = capitalProductivity;
	}

	public void setrDEfficiency(double rDEfficiency) {
		this.rDEfficiency = rDEfficiency;
	}

	public void setCostOfDebt(double costOfDebt) {
		this.costOfDebt = costOfDebt;
	}

	public void setCostOfEquity(double costOfEquity) {
		this.costOfEquity = costOfEquity;
	}

	public void setFixedCost(double fixedCost) {
		this.fixedCost = fixedCost;
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

	public double getEquityAvailable() {
		return equityAvailable;
	}

	public void setRD(double rD) {
		this.rD = rD;
	}
}