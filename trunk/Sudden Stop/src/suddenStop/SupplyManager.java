package suddenStop;

import java.util.ArrayList;
import java.util.List;

import cern.jet.random.*;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.*;
import repast.simphony.random.*;
import repast.simphony.util.collections.IndexedIterable;
import static repast.simphony.essentials.RepastEssentials.*;

public class SupplyManager {

	private Context<Object> context;

	public Normal entrantsNormal = null;
	public Normal innovationErrorNormal = null;

	public Normal fUCDistrib = null;
	public Normal iniEquityDistrib = null;
	public Uniform rDEfficiencyDistrib = null;
	public Uniform targetLeverageDistrib = null;
	public Uniform learningRateDistrib = null;
	
	public int[] timeCohortLimits = null;

	public double price = 0;

	public double dead = 0;

	public int bornFirms = 0;

	public double totalFirms = 1.0;

	public double totalQuantityPerPeriod = 0;

	public SupplyManager(Context<Object> context) {

		this.context = context;
		context.add(this);

		price = (Double) GetParameter("priceOfSubstitute");

		double entrantsMean = (Double) GetParameter("entrantsMean");
		entrantsNormal = RandomHelper.createNormal(entrantsMean,
				(Double) GetParameter("entrantsStdDev") * entrantsMean);

		innovationErrorNormal = RandomHelper.createNormal(1.0,
				(Double) GetParameter("innovationErrorStdDev"));

		/* Read Time Cohorts limits */
		String[] tmp = ((String) GetParameter("timeCohorts")).split(";");
		timeCohortLimits = new int[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			timeCohortLimits[i] = new Integer(tmp[i]);
		}

		/* Create distributions for initial variables of firms */

		// FIRST UNIT COST
		double fUCMean = (Double) GetParameter("firstUnitCostMean");
		fUCDistrib = RandomHelper.createNormal(fUCMean,
				(Double) GetParameter("firstUnitCostStdDev") * fUCMean);

		// INITIAL EQUITY
		double iniEquityMean = (Double) GetParameter("iniEquityMean");
		iniEquityDistrib = RandomHelper.createNormal(iniEquityMean,
				(Double) GetParameter("iniEquityStdDev") * iniEquityMean);

		// RD_EFFICIENCY
		rDEfficiencyDistrib = RandomHelper.createUniform(
				(Double) GetParameter("rDEfficiencyMin"),
				(Double) GetParameter("rDEfficiencyMax"));

		// TARGET_LEVERAGE
		targetLeverageDistrib = RandomHelper.createUniform(
				(Double) GetParameter("leverageMin"),
				(Double) GetParameter("leverageMax"));

		// LEARNING_RATE
		learningRateDistrib = RandomHelper.createUniform(
				(Double) GetParameter("learningRateMin"),
				(Double) GetParameter("learningRateMax"));

		Firm.supplyManager = this;

	}

	@ScheduledMethod(start = 1d, interval = 1d)
	public void step() {

		// Move to current State. Applies planned decision
		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		for (Object f : firms)
			((Firm) f).moveToNextState();

		manageEntry();

		processOffers();

		killToBeKilledFirms();

		planNextYear();

	}

	private void manageEntry() {

		bornFirms = 0;

		double potentialEntrantsPerPeriod = entrantsNormal.nextDouble()
				/ (Integer) GetParameter("periods");

		if (potentialEntrantsPerPeriod > 0) {

			Firm tmpFirm;

			for (int i = 1; i <= potentialEntrantsPerPeriod; i++) {

				tmpFirm = new Firm(context);

				// Destroy if not profitable
				if (!tmpFirm.estimateResponseToDemand(price)) {
					tmpFirm.killShadowFirms();
					RemoveAgentFromModel(tmpFirm);
				} else {
					bornFirms++;
				}

			}
		}
	}

	private void processOffers() {

		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		Firm f;

		totalQuantityPerPeriod = 0.0;
		for (Object o : firms) {
			f = (Firm) o;
			totalQuantityPerPeriod += f.getQuantityPerPeriod();
		}

		price = Demand.price(totalQuantityPerPeriod);

		for (Object o : context.getObjects(Firm.class)) {
			f = (Firm) o;

			f.processResponseToDemand(price);

		}
	}

	private void killToBeKilledFirms() {

		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		List<Firm> toKill = new ArrayList<Firm>(firms.size());

		for (Object o : context.getObjects(Firm.class)) {
			Firm f = (Firm) o;
			if (f.isToBeKilled()) {
				toKill.add(f);
			}
		}

		dead = toKill.size();
		for (Firm f : toKill) {
			f.killShadowFirms();
			RemoveAgentFromModel(f);
		}

		firms = context.getObjects(Firm.class);
		totalFirms = firms.size();
/*
		totalQuantityPerPeriod = 0.0;
		for (Object o : firms) {
			Firm f = (Firm) o;
			totalQuantityPerPeriod += f.getQuantityPerPeriod();
		}
*/
	}

	private void planNextYear() {
		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		for (Object f : firms) {
			((Firm) f).planNextYear(price);
		}

	}

	public String toString() {

		return "SupplyManager";

	}

	public double getPrice() {
		return price;
	}

	public double getDead() {
		return dead;
	}

	public int getBornFirms() {
		return bornFirms;
	}

	public double getTotalFirms() {
		return totalFirms;
	}

}
