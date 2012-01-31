package suddenStop;

import java.util.ArrayList;
import java.util.List;

import cern.jet.random.*;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.*;
import repast.simphony.parameter.*;
import repast.simphony.random.*;
import repast.simphony.util.collections.IndexedIterable;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

public class SupplyManager {

	private Context<Object> context;

	public SupplyManager(Context<Object> context) {

		this.context = context;
		context.add(this);

		price = (Double) GetParameter("priceOfSubstitute");

		rDEfficiencyNormal = RandomHelper.createNormal(
				(Double) GetParameter("rDEfficiencyMean"),
				(Double) GetParameter("rDEfficiencyStdDev")
						* (Double) GetParameter("rDEfficiencyMean"));
		iniKNormal = RandomHelper.createNormal(
				(Double) GetParameter("iniKMean"),
				(Double) GetParameter("iniKStdDev")
						* (Double) GetParameter("iniKMean"));
		entrantsNormal = RandomHelper.createNormal(
				(Double) GetParameter("entrantsMean"),
				(Double) GetParameter("entrantsStdDev")
						* (Double) GetParameter("entrantsMean"));
		innovationErrorNormal = RandomHelper.createNormal(1.0,
				(Double) GetParameter("innovationErrorStdDev"));
		firstUnitCostNormal = RandomHelper.createNormal(
				(Double) GetParameter("firstUnitCostMean"),
				(Double) GetParameter("firstUnitCostStdDev")
						* (Double) GetParameter("firstUnitCostMean"));

		/*
		 * If the learning rate variance is too big for the mean, it is set to
		 * the minimum value
		 */
		double lRMean = (Double) GetParameter("learningRateMean");

		double lRVar = pow(
				(Double) GetParameter("learningRateStdDev") * lRMean, 2.0);

		if (lRVar >= (lRMean - 0.5) * (1 - lRMean)) {
			lRVar = (lRMean - 0.5) * (1 - lRMean) - 0.000001;
		}

		double alfa = 2 / lRVar * (lRMean - 0.5)
				* ((lRMean - 0.5) * (1 - lRMean) - lRVar);
		double beta = 2 / lRVar * (1 - lRMean)
				* ((lRMean - 0.5) * (1 - lRMean) - lRVar);
		learningRateDistrib = RandomHelper.createBeta(alfa, beta);

		Firm.supplyManager = this;

	}

	@ScheduledMethod(start = 1d, pick = 9223372036854775807l, interval = 1d, shuffle = true)
	public void step() {
	
		// Manage Entry
		double potentialEntrants = entrantsNormal.nextDouble();
		if (potentialEntrants > 0)
			entry((int) round(potentialEntrants));
	
		processOffers();
	
		// Planning
		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		for (Object f : firms)
			((Firm) f).plan();
	
	}

	private void entry(int potentialEntrants) {

		// Check different types of Entry
		Firm tmpFirm;
		bornFirms = 0;

		// This is a loop.
		for (int j = 1; j <= potentialEntrants; j++) {

			tmpFirm = new Firm(context);

			// Destroy if not profitable
			if (!tmpFirm.estimateResponseToDemand(price)) {
				RemoveAgentFromModel(tmpFirm);
			} else {
				bornFirms++;
			}

		}

	}

	private void processOffers() {

		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		totalFirms = firms.size();

		if (totalFirms == 0.0) {
			totalQuantity = 0.0;
		} else {
			double tmpQ = 0.0;
			for (Object f : firms) {
				tmpQ += ((Firm) f).offer();
			}
			totalQuantity = tmpQ;
		}

		price = Demand.price(totalQuantity);

		List<Firm> toKill = new ArrayList<Firm>(firms.size());

		for (Object f : context.getObjects(Firm.class)) {

			// Process Demand Response
			if (!((Firm) f).processResponseToDemand(price)) {
				toKill.add((Firm) f);
			}

		}

		dead = toKill.size();
		for (Firm f : toKill) {
			RemoveAgentFromModel(f);
		}

		firms = context.getObjects(Firm.class);
		totalFBeforeExit = totalFirms;
		totalFirms = firms.size();
		totalQBeforeExit = totalQuantity;

		if (totalFirms == 0.0) {
			totalQuantity = 0.0;
		} else {
			double tmpQ = 0.0;
			for (Object f : firms) {
				tmpQ += ((Firm) f).getQuantity();
			}
			totalQuantity = tmpQ;
		}

	}

	public String toString() {

		return "SupplyManager";

	}

	@Parameter(displayName = "Total Quantity", usageName = "totalQuantity")
	public double getTotalQuantity() {
		return totalQuantity;
	}

	public double totalQuantity = 0;

	@Parameter(displayName = "Price", usageName = "price")
	public double getPrice() {
		return price;
	}

	public double price = 0;

	/**
	 * 
	 * This is an agent property.
	 * 
	 * @field dead
	 * 
	 */
	@Parameter(displayName = "Dead", usageName = "dead")
	public double getDead() {
		return dead;
	}

	public double dead = 0;

	/**
	 * 
	 * This is an agent property.
	 * 
	 * @field bornFirms
	 * 
	 */
	@Parameter(displayName = "Born Firms", usageName = "bornFirms")
	public int getBornFirms() {
		return bornFirms;
	}

	public int bornFirms = 0;

	/**
	 * 
	 * This is an agent property.
	 * 
	 * @field totalFirms
	 * 
	 */
	@Parameter(displayName = "Total Firms", usageName = "totalFirms")
	public double getTotalFirms() {
		return totalFirms;
	}

	public double totalFirms = 1.0;

	/**
	 * 
	 * This is an agent property.
	 * 
	 * @field totalQBeforeExit
	 * 
	 */
	@Parameter(displayName = "Total Q Before Exit", usageName = "totalQBeforeExit")
	public double getTotalQBeforeExit() {
		return totalQBeforeExit;
	}

	public double totalQBeforeExit = 0;

	/**
	 * 
	 * This is an agent property.
	 * 
	 * @field totalFBeforeExit
	 * 
	 */
	@Parameter(displayName = "Total F Before Exit", usageName = "totalFBeforeExit")
	public double getTotalFBeforeExit() {
		return totalFBeforeExit;
	}

	public double totalFBeforeExit = 1.0;

	public Normal iniKNormal = null;

	public Beta learningRateDistrib = null;

	public Normal entrantsNormal = null;

	public Normal rDEfficiencyNormal = null;

	public Normal innovationErrorNormal = null;

	public Normal firstUnitCostNormal = null;

}
