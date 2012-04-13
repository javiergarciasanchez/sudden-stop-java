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

		double tmpCrisisImpact = 1.0 - getSSMagnitude();
		double elast = (Double) GetParameter("demandElasticity");
		double subst = (Double) GetParameter("priceOfSubstitute");
		double demandParam = (Double) GetParameter("demandParameter");
		double demandShift = (Double) GetParameter("demandShift");
		double tick = GetTickCount();

		if (quantity > 0) {
			return min(subst,
					demandParam * pow(1.0 + demandShift, tick / elast)
							* pow(quantity, -1.0 / elast))
					* tmpCrisisImpact;
		} else {
			return (Double) GetParameter("priceOfSubstitute") * tmpCrisisImpact;
		}

	}

	public static double getSSMagnitude() {

		if (((Integer) GetParameter("suddenStopDuration")
				+ (Integer) GetParameter("suddenStopStart") > GetTickCount())
				&& (GetTickCount() >= (Integer) GetParameter("suddenStopStart"))) {
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
