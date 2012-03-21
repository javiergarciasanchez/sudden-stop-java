package suddenStop;

import java.util.ArrayList;
import java.util.List;

import cern.jet.random.*;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.*;
import repast.simphony.random.*;
import repast.simphony.util.collections.IndexedIterable;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

public class SupplyManager {

	private Context<Object> context;

	public Normal iniKNormal = null;
	public Normal entrantsNormal = null;
	public Normal innovationErrorNormal = null;

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

		// Manage Entry
		double potentialEntrants = entrantsNormal.nextDouble();
		if (potentialEntrants > 0)
			entry((int) round(potentialEntrants));

		processOffers();
		
		manageExit();
		
		Firm.independentVarsManager.collectData();
		
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
		Firm f;

		totalQuantity = 0.0;
		for (Object o : firms) {
			f = (Firm)o;
			totalQuantity += f.getQuantity();
		}
		price = Demand.price(totalQuantity);

		for (Object o : context.getObjects(Firm.class)) {
			f = (Firm) o;

			// Process Demand Response
			f.processResponseToDemand(price);
			f.plan();

		}
	}

	private void manageExit() {

		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		List<Firm> toKill = new ArrayList<Firm>(firms.size());

		for (Object o : context.getObjects(Firm.class)) {
			Firm f = (Firm) o;
			if (f.isExit()) {
				toKill.add(f);
			}
		}

		dead = toKill.size();
		for (Firm f : toKill) {
			RemoveAgentFromModel(f);
		}
		
		firms = context.getObjects(Firm.class);
		totalFirms = firms.size();

		totalQuantity = 0.0;
		for (Object f : firms) {
			totalQuantity += ((Firm) f).getQuantity();
		}
		
	}

	public String toString() {

		return "SupplyManager";

	}


	public double getTotalQuantity() {
		return totalQuantity;
	}
	public double totalQuantity = 0;


	public double getPrice() {
		return price;
	}
	public double price = 0;


	public double getDead() {
		return dead;
	}
	public double dead = 0;


	public int getBornFirms() {
		return bornFirms;
	}
	public int bornFirms = 0;


	public double getTotalFirms() {
		return totalFirms;
	}
	public double totalFirms = 1.0;

}
