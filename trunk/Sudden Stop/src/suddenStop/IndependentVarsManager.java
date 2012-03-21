package suddenStop;

import static java.lang.Math.max;
import static repast.simphony.essentials.RepastEssentials.GetParameter;

import cern.jet.random.*;
import static cern.jet.stat.Probability.normalInverse;

import repast.simphony.context.Context;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.collections.IndexedIterable;

public class IndependentVarsManager {

	Context<Object> context;

	public int cohorts;
	public double[] timeCohortsLimit;

	IndepVar firstUnitCost;
	IndepVar rDEfficiency;
	IndepVar leverage;
	IndepVar equityAccess;
	IndepVar learningRate;

	private class DepVars {
		public double[][] roeSum;
		public double[][] roaSum;
		public double[][] quantitySum;
		public double[][] firmsCount;

		public DepVars() {
			roeSum = new double[cohorts][timeCohortsLimit.length + 1];
			roaSum = new double[cohorts][timeCohortsLimit.length + 1];
			quantitySum = new double[cohorts][timeCohortsLimit.length + 1];
			firmsCount = new double[cohorts][timeCohortsLimit.length + 1];
		}
	}

	private class IndepVar {
		double[] limit = new double[cohorts-1];
		AbstractContinousDistribution distrib;
		DepVars depVars;
	}

	// Distribution parameters to estimate limit of each cohort
	public IndependentVarsManager(Context<Object> context) {
		context.add(this);
		this.context = context;

		cohorts = (Integer) GetParameter("cohorts");

		// Time Cohorts
		String[] tmp = ((String) RepastEssentials.GetParameter("timeCohorts"))
				.split(";");
		timeCohortsLimit = new double[tmp.length];
		for (int i = 0; i < timeCohortsLimit.length; i++) {
			timeCohortsLimit[i] = new Double(tmp[i]);
		}

		firstUnitCost = new IndepVar();
		rDEfficiency = new IndepVar();
		leverage = new IndepVar();
		equityAccess = new IndepVar();
		learningRate = new IndepVar();

		double fUCMean = (Double) GetParameter("firstUnitCostMean");
		double fUCStdDev = (Double) GetParameter("firstUnitCostStdDev")
				* fUCMean;
		firstUnitCost.distrib = RandomHelper.createNormal(fUCMean, fUCStdDev);

		double equityMean = (Double) GetParameter("equityMean");
		double equityStdDev = (Double) GetParameter("equityStdDev")
				* equityMean;
		equityAccess.distrib = RandomHelper.createNormal(equityMean,
				equityStdDev);

		/*
		 * R&D Efficiency follows a uniform distribution between min and max
		 * parameters
		 */

		double rDEfMin = (Double) GetParameter("rDEfficiencyMin");
		double rDEfMax = (Double) GetParameter("rDEfficiencyMax");
		rDEfficiency.distrib = RandomHelper.createUniform(rDEfMin, rDEfMax);

		/*
		 * Leverage follows a uniform distribution between min and max
		 * parameters
		 */
		double levMin = (Double) GetParameter("leverageMin");
		double levMax = (Double) GetParameter("leverageMax");
		leverage.distrib = RandomHelper.createUniform(levMin, levMax);

		/*
		 * Learning rate follows a uniform distribution between min and max
		 * possible values
		 */
		double lRMin = (Double) GetParameter("learningRateMin");
		double lRMax = (Double) GetParameter("learningRateMax");
		learningRate.distrib = RandomHelper.createUniform(lRMin, lRMax);

		// set Cohort limits
		for (int i = 1; i < cohorts; i++) {

			firstUnitCost.limit[i - 1] = fUCMean + fUCStdDev
					* normalInverse(1.0 * i / cohorts);

			equityAccess.limit[i - 1] = equityMean + equityStdDev
					* normalInverse(1.0 * i / cohorts);

			rDEfficiency.limit[i - 1] = rDEfMin
					+ ((rDEfMax - rDEfMin) * i / cohorts);

			leverage.limit[i - 1] = levMin + ((levMax - levMin) * i / cohorts);

			learningRate.limit[i-1] = lRMin + ((lRMax - lRMin) * i / cohorts);

		}

		Firm.independentVarsManager = this;

	}

	/*
	 * Get random indep vars
	 */
	public double getRandfirstUnitCost() {
		// A minimum FUC is set to 10% of mean
		return max(0.1 * (Double) GetParameter("firstUnitCostMean"),
				firstUnitCost.distrib.nextDouble());
	}

	public double getRandRDEfficiency() {
		return max(0.0, rDEfficiency.distrib.nextDouble());
	}

	public double getRandTargetLeverage() {
		return leverage.distrib.nextDouble();
	}

	public double getRandEquityAccess() {
		return equityAccess.distrib.nextDouble();
	}

	public double getRandLearningRate() {
		return learningRate.distrib.nextDouble();
	}

	/*
	 * Get limits of indep vars cohorts
	 */
	public double[] getFirstUnitCostLimit() {
		return firstUnitCost.limit;
	}

	public double[] getRDEfficiencyLimit() {
		return rDEfficiency.limit;
	}

	public double[] getLeverageLimit() {
		return leverage.limit;
	}

	public double[] getEquityAccessLimit() {
		return equityAccess.limit;
	}

	public double[] getLearningRateLimit() {
		return learningRate.limit;
	}

	public double[] getTimeCohortLimit() {
		return timeCohortsLimit;
	}

	public void collectData() {

		// Reset Variables
		firstUnitCost.depVars = new DepVars();
		rDEfficiency.depVars = new DepVars();
		leverage.depVars = new DepVars();
		equityAccess.depVars = new DepVars();
		learningRate.depVars = new DepVars();

		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		for (Object o : firms) {
			Firm f = (Firm) o;

			collectPerIndVar(f, firstUnitCost, f.getFUCCohort() - 1);
			collectPerIndVar(f, rDEfficiency, f.getRDEfCohort() - 1);
			collectPerIndVar(f, leverage, f.getLevCohort() - 1);
			collectPerIndVar(f, equityAccess, f.getEquityCohort() - 1);
			collectPerIndVar(f, learningRate, f.getLRCohort() - 1);
		}
	}

	private void collectPerIndVar(Firm f, IndepVar var, int varIdx) {

		double roe = f.getProfit() / f.getEquity();
		double roa = f.getEBIT() / f.getAssets();
		double quantity = f.getQuantity();

		int timeIdx = f.getTimeCohort() - 1;

		var.depVars.roeSum[varIdx][timeIdx] += roe;
		var.depVars.roaSum[varIdx][timeIdx] += roa;
		var.depVars.quantitySum[varIdx][timeIdx] += quantity;
		var.depVars.firmsCount[varIdx][timeIdx] += 1;

	}

	// Mean by FUC Cohort
	public double getROAMeanByFUCCohort(int varCoh, int timeCoh) {
		double firms = firstUnitCost.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return firstUnitCost.depVars.roaSum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getROEMeanByFUCCohort(int varCoh, int timeCoh) {
		double firms = firstUnitCost.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return firstUnitCost.depVars.roeSum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getQuantityMeanByFUCCohort(int varCoh, int timeCoh) {
		double firms = firstUnitCost.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {

			return firstUnitCost.depVars.quantitySum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getFirmsCountByFUCCohort(int varCoh, int timeCoh) {
		return firstUnitCost.depVars.firmsCount[varCoh - 1][timeCoh - 1];
	}

	// Mean by R&D Efficiency Cohort
	public double getROAMeanByRDEfficiencyCohort(int varCoh, int timeCoh) {
		double firms = rDEfficiency.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return rDEfficiency.depVars.roaSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getROEMeanByRDEfficiencyCohort(int varCoh, int timeCoh) {
		double firms = rDEfficiency.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return rDEfficiency.depVars.roeSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}

	}

	public double getQuantityMeanByRDEfficiencyCohort(int varCoh, int timeCoh) {
		double firms = rDEfficiency.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return rDEfficiency.depVars.quantitySum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}

	}

	public double getFirmsCountByRDEfficiencyCohort(int varCoh, int timeCoh) {
		return rDEfficiency.depVars.firmsCount[varCoh - 1][timeCoh - 1];
	}

	// Mean by Learning Rate Cohort
	public double getROAMeanByLRCohort(int varCoh, int timeCoh) {
		double firms = learningRate.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return learningRate.depVars.roaSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getROEMeanByLRCohort(int varCoh, int timeCoh) {
		double firms = learningRate.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return learningRate.depVars.roeSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getQuantityMeanByLRCohort(int varCoh, int timeCoh) {
		double firms = learningRate.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return learningRate.depVars.quantitySum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getFirmsCountByLRCohort(int varCoh, int timeCoh) {
		return learningRate.depVars.firmsCount[varCoh - 1][timeCoh - 1];
	}

	
	// Mean by Leverage Cohort
	public double getROAMeanByLevCohort(int varCoh, int timeCoh) {
		double firms = leverage.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return leverage.depVars.roaSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getROEMeanByLevCohort(int varCoh, int timeCoh) {
		double firms = leverage.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return leverage.depVars.roeSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getQuantityMeanByLevCohort(int varCoh, int timeCoh) {
		double firms = leverage.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return leverage.depVars.quantitySum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getFirmsCountByLevCohort(int varCoh, int timeCoh) {
		return leverage.depVars.firmsCount[varCoh - 1][timeCoh - 1];
	}

	// Mean by Equity Access Cohort
	public double getROAMeanByEquityCohort(int varCoh, int timeCoh) {
		double firms = equityAccess.depVars.firmsCount[varCoh - 1][timeCoh - 1];
		
		if (firms!=0.0){
			return equityAccess.depVars.roaSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getROEMeanByEquityCohort(int varCoh, int timeCoh) {
		double firms = equityAccess.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return equityAccess.depVars.roeSum[varCoh - 1][timeCoh - 1] / firms;
		} else {
			return 0.0;
		}
	}

	public double getQuantityMeanByEquityCohort(int varCoh, int timeCoh) {
		double firms = equityAccess.depVars.firmsCount[varCoh - 1][timeCoh - 1];

		if (firms != 0.0) {
			return equityAccess.depVars.quantitySum[varCoh - 1][timeCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getFirmsCountByEquityCohort(int varCoh, int timeCoh) {
		return equityAccess.depVars.firmsCount[varCoh - 1][timeCoh - 1];
	}

}
