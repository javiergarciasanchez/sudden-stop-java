package suddenStop;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

public class FirmState implements Cloneable {

	double born = 0.0;
	double expon = 0.0;
	double acumQ = 0.0;
	double firstUnitCost = 0.0;
	double performance = 0.0;
	// capital + cashExcess = debt + equity
	double cashExcess = 0.0;
	double capital = 0.0;
	double debt = 0.0;
	double capitalProductivity = 0.0;
	double targetLeverage = 0.0;
	double rDEfficiency = 0.0;
	double costOfEquity = 0.0;
	double costOfDebt = 0.0;
	double fixedCost = 0.0;

	public FirmState() {

		born = GetTickCount();

		// A minimum FUC is set to 10% of mean
		firstUnitCost = max(
				0.1 * (Double) GetParameter("firstUnitCostMean"),
				Firm.supplyManager.firstUnitCostNormal.nextDouble());
		capitalProductivity = (Double) GetParameter("capitalProductivity");

		targetLeverage = min(Firm.supplyManager.leverageNormal.nextDouble(), 1.0);
		
		capital = max((Double) GetParameter("minimumCapital"),
				Firm.supplyManager.iniKNormal.nextDouble());

		// 0.5 < learning rate <= 1.0
		double learningRate = min(1.0,
				max(Firm.supplyManager.learningRateDistrib.nextDouble(), 0.51));
		expon = log(learningRate) / log(2.0);
		rDEfficiency = max(0.0,
				Firm.supplyManager.rDEfficiencyNormal.nextDouble());

		costOfEquity = (Double) GetParameter("costOfEquity");
		costOfDebt = (Double) GetParameter("costOfDebt");
		fixedCost = (Double) GetParameter("fixedCost");
		performance = (Double) GetParameter("initialPerformance");

	}

	@Override
	public FirmState clone() {
		try {
			return (FirmState) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public double getCapital() {
		return capital;
	}

	public double getDebt() {
		return debt;
	}
	
	public double getAssets(){
		return capital + cashExcess;
	}
	
	public double getNetLeverage(){
		return (debt - cashExcess)/capital;
	}
	
	public double getLeverage(){
		return debt / (cashExcess + capital);
	}

	public double getEquity() {
		return cashExcess + capital - debt;
	}


}