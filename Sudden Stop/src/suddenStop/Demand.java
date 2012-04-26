package suddenStop;

import repast.simphony.ui.probe.*;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

public class Demand {

	/**
	 * 
	 * This is a constant elasticity demand function, with a maximum price
	 * equivalent to the price of a substitute.
	 * 
	 */
	public static double price(double quantity) {

		int periods = (Integer) GetParameter("periods");
		double passthru = (Double) GetParameter("SSPassthru");		
		double elast = (Double) GetParameter("demandElasticity");
		double subst = (Double) GetParameter("priceOfSubstitute");
		double demandParam = (Double) GetParameter("demandParameter");
		double demandShift = (Double) GetParameter("demandShift") / periods;
		double tick = GetTickCount();

		double tmpCrisisImpact = 1.0 - getSSMagnitude() * passthru;

		if (quantity > 0) {
			return min(
					subst,
					demandParam * pow(1.0 + demandShift, tick / elast)
							* pow(quantity * periods, -1.0 / elast))
					* tmpCrisisImpact;
		} else {
			return (Double) GetParameter("priceOfSubstitute") * tmpCrisisImpact;
		}

	}

	public static double getSSMagnitude() {
		int sSDuration = (Integer) GetParameter("suddenStopDuration")
				* (Integer) GetParameter("periods");
		int sSStart = (Integer) GetParameter("suddenStopStart")
				* (Integer) GetParameter("periods");

		if ((sSDuration + sSStart > GetTickCount())
				&& (GetTickCount() >= sSStart)) {
			return (Double) GetParameter("suddenStopMagnitude");
		} else {
			return 0.0;
		}

	}

	@ProbeID()
	public String toString() {
		return "Demand";
	}

	public static boolean isSS() {
		return Demand.getSSMagnitude() > 0.0;
	}
}
