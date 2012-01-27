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

		if (quantity > 0) {
			return min(
					(Double) GetParameter("priceOfSubstitute"),
					pow((Double) GetParameter("demandParameter")
							* pow((1.0 + (Double) GetParameter("demandShift")),
									GetTickCount()) / quantity,
							1.0 / (Double) GetParameter("demandElasticity")))
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
}
