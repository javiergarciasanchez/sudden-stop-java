package suddenStop;

import static cern.jet.stat.Probability.normalInverse;
import static repast.simphony.essentials.RepastEssentials.GetParameter;

import org.apache.commons.math.JGS.MathException;
import org.apache.commons.math.JGS.distribution.BetaDistributionImpl;
import org.apache.commons.math.JGS.stat.descriptive.DescriptiveStatistics;

import repast.simphony.context.Context;
import repast.simphony.util.collections.IndexedIterable;

public class IndependentVarsManager {

	Context<Object> context;

	public int cohorts;

	IndepVar firstUnitCost;
	IndepVar rDEfficiency;
	IndepVar learningRate;
	IndepVar leverage;

	class IndepVar {
		double[] limit = new double[cohorts];
		CollectableData data = new CollectableData();
	}

	// Structure to collect data for each cohort
	private class CollectableData {
		DescriptiveStatistics[] roa = new DescriptiveStatistics[cohorts];
		DescriptiveStatistics[] roe = new DescriptiveStatistics[cohorts];
		DescriptiveStatistics[] output = new DescriptiveStatistics[cohorts];

		public CollectableData() {
			for (int i = 0; i < cohorts; i++) {
				roa[i] = new DescriptiveStatistics();
				roe[i] = new DescriptiveStatistics();
				output[i] = new DescriptiveStatistics();
			}
		}
	}

	// Distribution parameters to estimate limit of each cohort
	public IndependentVarsManager(Context<Object> context) {
		context.add(this);
		this.context = context;

		cohorts = (Integer) GetParameter("cohorts");

		firstUnitCost = new IndepVar();
		rDEfficiency = new IndepVar();
		learningRate = new IndepVar();
		leverage = new IndepVar();

		Firm.independentVarsManager = this;

	}

	/*
	 * Assign cohort limits for each var
	 */
	public void setCohortLimits(IndepVarsDistribParams params) {

		BetaDistributionImpl betDist = new BetaDistributionImpl(
				params.lRParams[0], params.lRParams[1]);
		for (int i = 0; i < cohorts - 1; i++) {

			rDEfficiency.limit[i] = params.rDEfParams[0] + params.rDEfParams[1]
					* normalInverse(1.0 * (i + 1) / cohorts);

			firstUnitCost.limit[i] = params.fUCParams[0] + params.fUCParams[1]
					* normalInverse(1.0 * (i + 1) / cohorts);

			leverage.limit[i] = params.levParams[0] + params.levParams[1]
					* normalInverse(1.0 * (i + 1) / cohorts);

			try {
				learningRate.limit[i] = betDist
						.inverseCumulativeProbability(1.0 * (i + 1) / cohorts);
			} catch (MathException e) {
				e.printStackTrace();
			}

		}
	}

	public void collectPerCohort() {
		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		firstUnitCost.data = new CollectableData();
		rDEfficiency.data = new CollectableData();
		learningRate.data = new CollectableData();
		leverage.data = new CollectableData();

		if (firms.size() > 0.0) {
			for (Object o : firms) {
				Firm f = (Firm) o;

				// Collects ROA
				double roa = f.getEBIT() / f.getCapital();
				firstUnitCost.data.roa[f.getFUCCohort() - 1].addValue(roa);
				rDEfficiency.data.roa[f.getRDEfCohort() - 1].addValue(roa);
				learningRate.data.roa[f.getLRCohort() - 1].addValue(roa);
				leverage.data.roa[f.getLevCohort()-1].addValue(roa);

				// Collects ROE
				double roe = f.getProfit() / f.getEquity();
				firstUnitCost.data.roe[f.getFUCCohort() - 1].addValue(roe);
				rDEfficiency.data.roe[f.getRDEfCohort() - 1].addValue(roe);
				learningRate.data.roe[f.getLRCohort() - 1].addValue(roe);
				leverage.data.roe[f.getLevCohort()-1].addValue(roe);

				// Collects output
				double output = f.getQuantity();
				firstUnitCost.data.output[f.getFUCCohort() - 1]
						.addValue(output);
				rDEfficiency.data.output[f.getRDEfCohort() - 1]
						.addValue(output);
				learningRate.data.output[f.getLRCohort() - 1].addValue(output);
				leverage.data.output[f.getLevCohort()-1].addValue(output);
			}
		}

	}

	// Mean by FUC Cohort
	public double getROAMeanByFUCCohort(int coh) {
		return firstUnitCost.data.roa[coh].getMean();
	}

	public double getROEMeanByFUCCohort(int coh) {
		return firstUnitCost.data.roe[coh].getMean();
	}

	public double getOutputMeanByFUCCohort(int coh) {
		return firstUnitCost.data.output[coh].getMean();
	}
	

	// Mean by R&D Efficiency Cohort
	public double getROAMeanByRDEfficiencyCohort(int coh) {
		return rDEfficiency.data.roa[coh].getMean();
	}

	public double getROEMeanByRDEfficiencyCohort(int coh) {
		return rDEfficiency.data.roe[coh].getMean();
	}

	public double getOutputMeanByRDEfficiencyCohort(int coh) {
		return rDEfficiency.data.output[coh].getMean();
	}
	
	
	// Mean by Learning Rate Cohort
	public double getROAMeanByLRCohort(int coh) {
		return learningRate.data.roa[coh].getMean();
	}

	public double getROEMeanByLRCohort(int coh) {
		return learningRate.data.roe[coh].getMean();
	}

	public double getOutputMeanByLRCohort(int coh) {
		return learningRate.data.output[coh].getMean();
	}
	
	
	// Mean by Leverage Cohort
	public double getROAMeanByLevCohort(int coh) {
		return leverage.data.roa[coh].getMean();
	}

	public double getROEMeanByLevCohort(int coh) {
		return leverage.data.roe[coh].getMean();
	}

	public double getOutputMeanByLevCohort(int coh) {
		return leverage.data.output[coh].getMean();
	}
	
}
