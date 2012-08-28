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
		double elast = (Double) GetParameter("demandElasticity");
		double subst = (Double) GetParameter("priceOfSubstitute");
		double demandParam = (Double) GetParameter("demandParameter");
		double demandShift = (Double) GetParameter("demandShift") / periods;

		double tick = GetTickCount();

		if (quantity > 0) {
			return min(
					subst,
					demandParam * pow(1.0 + demandShift, tick / elast)
							* pow(quantity * periods, -1.0 / elast))
					* (1.0 - getSSMagnitude());
		} else {
			return (Double) GetParameter("priceOfSubstitute")
					* (1.0 - getSSMagnitude());
		}

	}

	public static boolean isSS() {
		double sSM = (Double) GetParameter("suddenStopMagnitude");
		double sSD = (Double) GetParameter("suddenStopDuration")
				* (Integer) GetParameter("periods");

		int sSS = (Integer) GetParameter("suddenStopStart")
				* (Integer) GetParameter("periods");

		return (sSM > 0.0) && (sSD + sSS > GetTickCount())
				&& (GetTickCount() >= sSS);

	}

	public static double getSSMagnitude() {

		if (isSS()) {
			return (Double) GetParameter("suddenStopMagnitude");
		} else {
			return 0.0;
		}

	}

	@ProbeID()
	public String toString() {
		return "Demand";
	}

}
