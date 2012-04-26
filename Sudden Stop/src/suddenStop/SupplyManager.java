package suddenStop;

import java.util.ArrayList;
import java.util.List;

import cern.jet.random.*;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.*;
import repast.simphony.random.*;
import repast.simphony.util.collections.IndexedIterable;
import static repast.simphony.essentials.RepastEssentials.*;

public class SupplyManager {

	private Context<Object> context;

	public Normal iniKNormal = null;
	public Normal entrantsNormal = null;
	public Normal innovationErrorNormal = null;

	public double price = 0;

	public double dead = 0;

	public int bornFirms = 0;

	public double totalFirms = 1.0;

	public double totalQuantityPerPeriod = 0;

	public SupplyManager(Context<Object> context,
			IndependentVarsManager independentVarsManager) {

		this.context = context;
		context.add(this);

		price = (Double) GetParameter("priceOfSubstitute");

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

		if (!RunEnvironment.getInstance().isBatch()) {
			Firm.independentVarsManager.collectData();
		}

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

		totalQuantityPerPeriod = 0.0;
		for (Object o : firms) {
			Firm f = (Firm) o;
			totalQuantityPerPeriod += f.getQuantityPerPeriod();
		}

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
