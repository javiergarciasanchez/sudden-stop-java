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

		/*
		 * SSMagnitude is the fall of P x Q
		 * 
		 * Demand function is adjusted to get this result
		 * 
		 * See mathematica "SS impact on q.nb"
		 */
		double sSDemandImpact = (1.0 - getSSMagnitude())
				* pow(1.0 - getSSQuantityImpact(), -1.0 + 1.0 / elast);

		if (quantity > 0) {
			return min(
					subst,
					demandParam * pow(1.0 + demandShift, tick / elast)
							* pow(quantity * periods, -1.0 / elast))
					* sSDemandImpact;
		} else {
			return (Double) GetParameter("priceOfSubstitute") * sSDemandImpact;
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

	public static double getSSQuantityImpact() {
		return getSSQuantityImpact(GetTickCount());
	}

	public static double getSSQuantityImpact(double tick) {

		return getSSImpactSmoothing(tick,
				(Double) GetParameter("suddenStopQuantityImpact"))
				* (Double) GetParameter("SSPassthru");

	}

	public static double getSSMagnitude() {
		return getSSMagnitude(GetTickCount());
	}

	public static double getSSMagnitude(double tick) {

		return getSSImpactSmoothing(tick, (Double) GetParameter("suddenStopMagnitude"))
				* (Double) GetParameter("SSPassthru");

	}

	public static double getSSImpactSmoothing(double tick, double magn) {

		double sSD = (Double) GetParameter("suddenStopDuration")
				* (Integer) GetParameter("periods");

		int sSS = (Integer) GetParameter("suddenStopStart")
				* (Integer) GetParameter("periods");
		if (isSS()) {
			return magn - (tick - sSS) * magn / sSD;
		} else {
			return 0.0;
		}

	}

	@ProbeID()
	public String toString() {
		return "Demand";
	}

}
