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

import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

import java.lang.reflect.Field;

import repast.simphony.context.Context;

/**
 * 
 * This is an agent.
 * 
 */
public abstract class Firm {

	public static SupplyManager supplyManager;
	public static IndependentVarsManager independentVarsManager;

	private FirmState currentState, nextState;

	private class Decision implements Cloneable {
		private double quantity = 0.0;
		private double rD = 0.0;

		@Override
		protected Decision clone() {
			try {
				return (Decision) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private Decision currentDecision, nextDecision;

	protected static long agentIDCounter = 1;
	protected String agentID = "Firm " + (agentIDCounter++);

	protected abstract void tryMeetCapitalNeeds(FirmState st);

	protected abstract void getFunds(FirmState st, double funds);

	public Firm(Context<Object> context) {

		context.add(this);

		nextState = new FirmState();
		currentState = nextState.clone();

		// Sets next decision
		nextDecision = new Decision();
		nextDecision.rD = 0.0;
		nextDecision.quantity = nextState.capital
				* nextState.capitalProductivity;

		currentDecision = nextDecision.clone();

	}

	/*
	 * Estimates if nextDecision would be an exit given nextState and price It
	 * works like ProcessResponseToDemand but without changing the current
	 * situation
	 */
	public boolean estimateResponseToDemand(double price) {

		FirmState tmpSt = nextState.clone();

		updateState(tmpSt, nextDecision, price);

		return !isExit(tmpSt);

	}

	public double offer() {

		return nextDecision.quantity;

	}

	/**
	 * 
	 * Process demand respond and returns false if firm exits the industry,
	 * otherwise returns true
	 * 
	 * @param price
	 * 
	 * @method processDemandResponse
	 * 
	 */
	public boolean processResponseToDemand(double price) {

		currentState = nextState.clone();
		currentDecision = nextDecision.clone();

		// Modifies state according to decision and price
		updateState(currentState, currentDecision, price);

		// Returns true if firms continues in industry
		return !isExit(currentState);

	}

	private void updateState(FirmState st, Decision dec, double price) {

		// applies depreciation
		st.capital *= (1 - (Double) GetParameter("depreciation"));
		

		/*
		 *  applies profit
		 *  If profit is negative cash Excess is used to absorb it
		 *  In case cash Excess is not sufficient, capital is reduced
		 */
		double profit = calcProfit(st, dec, price);
		st.capital += min(st.cashExcess + profit, 0.0);
		st.cashExcess -= min(-profit, st.cashExcess);


		/*
		 * Check if more capital is needed to avoid exit. Investment needs are
		 * checked later, in the plan stage
		 */
		if (survivalCapitalNeeds(st) > 0.0) {
			tryMeetCapitalNeeds(st);
		}

		// updates performance
		st.performance = calcPerformance(st, dec, price);

		// accumulates Q
		st.acumQ += dec.quantity;
	}

	public void plan() {

		// it is assumed there is no divesture
		double optInvest = max(
				0.0,
				calcNetInvestment(currentState, currentDecision,
						supplyManager.price));

		nextState = currentState.clone();
		getFunds(nextState, optInvest * nextState.capital);

		nextDecision.quantity = nextState.capital
				* nextState.capitalProductivity;

		// Then new R&D is determined to optimize First unit cost.The maxFunding
		// is relevant to speed up the process.
		double optRD = pow(
				nextState.firstUnitCost
						/ nextState.rDEfficiency
						* (pow(nextState.acumQ + nextDecision.quantity,
								1.0 + nextState.expon) - pow(nextState.acumQ,
								1.0 + nextState.expon)), 0.5) - 1.0;

		/*
		 * There is a minimum amount of RD to make FUC decrease
		 */
		nextDecision.rD = max(1.0 / nextState.rDEfficiency - 1.0, optRD);

		// apply innovation
		nextState.firstUnitCost = nextState.firstUnitCost * 1.0
				/ ((nextDecision.rD + 1.0) * nextState.rDEfficiency)
				* supplyManager.innovationErrorNormal.nextDouble();

	}

	public boolean isExit(FirmState st) {

		/*
		 * Exits happens if: a) perf < minPerf OR b) capital requirements are
		 * not met
		 */
		return ((st.performance < (Double) GetParameter("minimumPerformance")) || survivalCapitalNeeds(st) > 0.0);

	}

	private double survivalCapitalNeeds(FirmState st) {
		double minCapNeed = (Double) GetParameter("minimumCapital")
				- getCapital();
		double defaultNeed = getDebt() - getAssets();

		return max(minCapNeed, defaultNeed);
	}

	private double calcProfit(FirmState st, Decision dec, double price) {

		return price * dec.quantity - calcTotVarCost(st, dec)
				- calcFixedCost(st, dec);

	}

	private double calcPerformance(FirmState st, Decision dec, double price) {

		return (Double) GetParameter("performanceWeight") * st.performance
				+ (1 - (Double) GetParameter("performanceWeight"))
				* calcProfit(st, dec, price) / getEquity();
	}

	// Calculates cost using learning curve: cost of new acummulated Q minus
	// old acummulated Q. See http://maaw.info/LearningCurveSummary.htm
	// (Wright model)
	private double calcTotVarCost(FirmState st, Decision dec) {

		return st.firstUnitCost
				* (pow(st.acumQ + dec.quantity, 1.0 + currentState.expon) - pow(
						st.acumQ, 1.0 + currentState.expon));

	}

	private double calcFixedCost(FirmState st, Decision dec) {

		return st.costOfEquity * getEquity() + st.costOfDebt
				* getDebt() + (Double) GetParameter("depreciation") * getCapital()
				- dec.rD - st.fixedCost;

	}

	private double calcNetInvestment(FirmState st, Decision dec, double price) {
		double mktSh = dec.quantity / supplyManager.totalQuantity;

		double optimalMarkUp = ((Double) GetParameter("demandElasticity") + (1 - mktSh)
				* (Double) GetParameter("supplyElasticity"))
				/ ((Double) GetParameter("demandElasticity") + (1 - mktSh)
						* (Double) GetParameter("supplyElasticity") - mktSh);

		// dejo Winter a un lado y pongo el m�ximo igual al mark up del
		// substituto
		return (Double) GetParameter("investmentParam")
				* (1 - optimalMarkUp * calcMarginalCost(st, dec) / price);

	}

	private double calcMarginalCost(FirmState st, Decision dec) {

		return st.firstUnitCost
					* (1.0 + st.expon)
					* pow(st.acumQ + dec.quantity, st.expon)
				+ (st.costOfDebt * getLeverage()
						+ st.costOfEquity * (1.0 - getLeverage())
						+ (Double) GetParameter("depreciation"))
					/ st.capitalProductivity;

	}

	public double getAge() {
		return GetTickCount() - currentState.born;
	}

	public double getMedCost() {
		return (calcTotVarCost(currentState, currentDecision) + calcFixedCost(
				currentState, currentDecision)) / currentDecision.quantity;
	}

	public double getEBIT() {
		return getProfit() + getInterest();
	}

	public double getInterest() {
		return currentState.debt * currentState.costOfDebt;
	}

	public double getProfit() {
		return calcProfit(currentState, currentDecision, supplyManager.price);
	}

	public double getPrice() {
		return supplyManager.price;
	}

	public double getCapital() {
		return currentState.getCapital();
	}

	public double getDebt() {
		return currentState.getDebt();
	}
	
	public double getAssets(){
		return currentState.getAssets();
	}
	
	public double getNetLeverage(){
		return currentState.getNetLeverage();
	}
	
	public double getLeverage(){
		return currentState.getLeverage();
	}

	public double getEquity() {
		return currentState.getEquity();
	}

	public double getQuantity() {
		return currentDecision.quantity;
	}

	public double getPerformance() {
		return currentState.performance;
	}

	public double getRD() {
		return currentDecision.rD;
	}

	public double getFirstUnitCost() {
		return currentState.firstUnitCost;
	}

	public double getAcumQuantity() {
		return currentState.acumQ;
	}

	public String toString() {
		return this.agentID;
	}

	public double getBorn() {
		return currentState.born;
	}

	public double getExpon() {
		return currentState.expon;
	}

	public String getType() {
		return this.getClass().getSimpleName();
	}

	public int getTypeNum() {
		String firmType = this.getClass().getSimpleName();
		if (firmType.equals("AloneFirm")) {
			return 0;
		} else if (firmType.equals("DebtFirm")) {
			return 1;
		} else if (firmType.equals("EquityFirm")) {
			return 2;
		} else {
			System.err.println("Invalid Firm Type");
			System.exit(-1);
			return -1;
		}
	}

	public int getRDEfCohort() {
		return getCohort(currentState.rDEfficiency,
				independentVarsManager.rDEfficiency.limit);
	}

	public int getFUCCohort() {
		return getCohort(currentState.firstUnitCost,
				independentVarsManager.firstUnitCost.limit);
	}

	public int getLevCohort() {
		return getCohort(currentState.targetLeverage,
				independentVarsManager.leverage.limit);
	}
	
	public int getLRCohort() {
		return getCohort(exp(currentState.expon * log(2.0)),
				independentVarsManager.learningRate.limit);
	}
	
	

	private int getCohort(double fVal, double[] lim) {

		for (int i = 0; i < independentVarsManager.cohorts - 1; i++) {
			if (fVal < lim[i])
				return i + 1;
		}

		return independentVarsManager.cohorts;

	}
	
	public double get(String var){
		Field f = null;
		try {
			f = Class.forName("suddenStop.Firm$FirmState").getDeclaredField(var);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			return f.getDouble(this.currentState);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return 0;
		
	}
}
