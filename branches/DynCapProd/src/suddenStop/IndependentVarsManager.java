package suddenStop;

import static java.lang.Math.max;
import static repast.simphony.essentials.RepastEssentials.GetParameter;

import java.util.HashMap;

import static cern.jet.stat.Probability.normalInverse;

import repast.simphony.context.Context;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.collections.IndexedIterable;

import static suddenStop.IndepVarsNames.*;
import static suddenStop.DepVarsNames.*;

public class IndependentVarsManager {

	Context<Object> context;

	public int cohorts;

	/*
	 * private IndepVar firstUnitCost; private IndepVar rDEfficiency; private
	 * IndepVar leverage; private IndepVar equityAccess; private IndepVar
	 * learningRate;
	 */

	private HashMap<IndepVarsNames, IndepVar> indepVars;

	// Distribution parameters to estimate limit of each cohort
	public IndependentVarsManager(Context<Object> context) {

		context.add(this);
		this.context = context;

		cohorts = (Integer) GetParameter("cohorts");

		indepVars = new HashMap<IndepVarsNames, IndepVar>();

		// Setting independent vars Distribution and cohort limits
		IndepVar tmpIndepVar;

		// TIME OF ENTRY does not have distribution
		tmpIndepVar = new IndepVar(cohorts);
		String[] tmp = ((String) RepastEssentials.GetParameter("timeCohorts"))
				.split(";");
		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = new Double(tmp[i - 1]);
		}
		indepVars.put(TIME_OF_ENTRY, tmpIndepVar);

		// FIRST UNIT COST
		tmpIndepVar = new IndepVar(cohorts);
		double fUCMean = (Double) GetParameter("firstUnitCostMean");
		double fUCStdDev = (Double) GetParameter("firstUnitCostStdDev")
				* fUCMean;
		tmpIndepVar.distrib = RandomHelper.createNormal(fUCMean, fUCStdDev);

		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = fUCMean + fUCStdDev
					* normalInverse(1.0 * i / cohorts);
		}
		indepVars.put(FIRST_UNIT_COST, tmpIndepVar);

		// EQUITY ACCESS
		tmpIndepVar = new IndepVar(cohorts);
		double equityMean = (Double) GetParameter("equityMean");
		double equityStdDev = (Double) GetParameter("equityStdDev")
				* equityMean;
		tmpIndepVar.distrib = RandomHelper.createNormal(equityMean,
				equityStdDev);
		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = equityMean + equityStdDev
					* normalInverse(1.0 * i / cohorts);
		}
		indepVars.put(EQUITY_ACCESS, tmpIndepVar);

		/*
		 * RD_EFFICIENCY follows a uniform distribution between min and max
		 * parameters
		 */
		tmpIndepVar = new IndepVar(cohorts);
		double rDEfMin = (Double) GetParameter("rDEfficiencyMin");
		double rDEfMax = (Double) GetParameter("rDEfficiencyMax");
		tmpIndepVar.distrib = RandomHelper.createUniform(rDEfMin, rDEfMax);
		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = rDEfMin
					+ ((rDEfMax - rDEfMin) * i / cohorts);
		}
		indepVars.put(RD_EFFICIENCY, tmpIndepVar);

		/*
		 * TARGET_LEVERAGE follows a uniform distribution between min and max
		 * parameters
		 */
		tmpIndepVar = new IndepVar(cohorts);
		double levMin = (Double) GetParameter("leverageMin");
		double levMax = (Double) GetParameter("leverageMax");
		tmpIndepVar.distrib = RandomHelper.createUniform(levMin, levMax);
		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = levMin
					+ ((levMax - levMin) * i / cohorts);
		}

		indepVars.put(TARGET_LEVERAGE, tmpIndepVar);

		/*
		 * LEARNING_RATE follows a uniform distribution between min and max
		 * possible values
		 */
		tmpIndepVar = new IndepVar(cohorts);
		double lRMin = (Double) GetParameter("learningRateMin");
		double lRMax = (Double) GetParameter("learningRateMax");
		tmpIndepVar.distrib = RandomHelper.createUniform(lRMin, lRMax);
		for (int i = 1; i < cohorts; i++) {
			tmpIndepVar.limit[i - 1] = lRMin + ((lRMax - lRMin) * i / cohorts);
		}
		indepVars.put(LEARNING_RATE, tmpIndepVar);

		Firm.independentVarsManager = this;

	}

	/*
	 * Add new firm to the respective cohort counter of each indepvar
	 */
	public void addNewFirm(Firm f) {
		for (IndepVarsNames key : indepVars.keySet()) {
			indepVars.get(key).maxCount[f.getCohort(key) - 1]++;
		}

	}

	/*
	 * Get random indep vars
	 */
	public double getRandfirstUnitCost() {
		// A minimum FUC is set to 10% of mean
		return max(0.1 * (Double) GetParameter("firstUnitCostMean"),
				indepVars.get(FIRST_UNIT_COST).distrib.nextDouble());
	}

	public double getRandRDEfficiency() {
		return max(0.0, indepVars.get(RD_EFFICIENCY).distrib.nextDouble());
	}

	public double getRandTargetLeverage() {
		return indepVars.get(TARGET_LEVERAGE).distrib.nextDouble();
	}

	public double getRandEquityAccess() {
		return indepVars.get(EQUITY_ACCESS).distrib.nextDouble();
	}

	public double getRandLearningRate() {
		return indepVars.get(LEARNING_RATE).distrib.nextDouble();
	}

	/*
	 * Get limits of indep vars cohorts
	 */
	public double[] getCohortLimit(IndepVarsNames indepVar) {
		return indepVars.get(indepVar).limit;
	}

	public void collectData() {

		// Reset Variables
		for (IndepVar v : indepVars.values()) {
			v.resetDepVars(cohorts);
		}

		IndexedIterable<Object> firms = context.getObjects(Firm.class);
		for (Object o : firms) {
			Firm f = (Firm) o;

			if (!f.isToBeKilled()) {
				for (IndepVarsNames key : indepVars.keySet()) {
					collectPerIndVar(f, indepVars.get(key),
							f.getCohort(key) - 1);
				}
			}
		}
	}

	private void collectPerIndVar(Firm f, IndepVar var, int varIdx) {

		double[] tmp;

		tmp = var.depVars.get(ROE_SUM);
		tmp[varIdx] += f.getROE();
		var.depVars.put(ROE_SUM, tmp);

		tmp = var.depVars.get(ROI_SUM);
		tmp[varIdx] += f.getROI();
		var.depVars.put(ROI_SUM, tmp);

		tmp = var.depVars.get(QUANTITY_PER_PERIOD_SUM);
		tmp[varIdx] += f.getQuantityPerPeriod();
		var.depVars.put(QUANTITY_PER_PERIOD_SUM, tmp);

		tmp = var.depVars.get(FIRMS_COUNT);
		tmp[varIdx] += 1;
		var.depVars.put(FIRMS_COUNT, tmp);

		tmp = var.depVars.get(PERFORMANCE_SUM);
		tmp[varIdx] += f.getPerformance();
		var.depVars.put(PERFORMANCE_SUM, tmp);

		tmp = var.depVars.get(MKT_SHARE_SUM);
		tmp[varIdx] += f.getMktShare();
		var.depVars.put(MKT_SHARE_SUM, tmp);

	}

	public int getTotalCountByCohort(IndepVarsNames indepVar, int varCoh) {
		return indepVars.get(indepVar).maxCount[varCoh - 1];
	}

	public double getCountByCohort(IndepVarsNames indepVar, int varCoh) {
		return (indepVars.get(indepVar).depVars.get(FIRMS_COUNT))[varCoh - 1];
	}

	public double getMeanByCohort(IndepVarsNames indepVar, DepVarsNames depVar,
			int varCoh) {
		double firms = (indepVars.get(indepVar).depVars.get(FIRMS_COUNT))[varCoh - 1];

		if (firms != 0.0) {
			return (indepVars.get(indepVar).depVars.get(depVar))[varCoh - 1]
					/ firms;
		} else {
			return 0.0;
		}
	}

	public double getFirmsCountByLevCohort1() {
		return getCountByCohort(TARGET_LEVERAGE, 1);
	}

	public double getFirmsCountByLevCohort2() {
		return getCountByCohort(TARGET_LEVERAGE, 2);
	}

	public double getFirmsCountByLevCohort3() {
		return getCountByCohort(TARGET_LEVERAGE, 3);
	}

	public double getMktShareByLevCohort1() {
		return getMeanByCohort(TARGET_LEVERAGE, MKT_SHARE_SUM, 1);
	}

	public double getMktShareByLevCohort2() {
		return getMeanByCohort(TARGET_LEVERAGE, MKT_SHARE_SUM, 2);
	}

	public double getMktShareByLevCohort3() {
		return getMeanByCohort(TARGET_LEVERAGE, MKT_SHARE_SUM, 3);
	}

	public double getROIByLevCohort1() {
		return getMeanByCohort(TARGET_LEVERAGE, ROI_SUM, 1);
	}

	public double getROIByLevCohort2() {
		return getMeanByCohort(TARGET_LEVERAGE, ROI_SUM, 2);
	}

	public double getROIByLevCohort3() {
		return getMeanByCohort(TARGET_LEVERAGE, ROI_SUM, 3);
	}

	public double getFirmsCountAsPercetangeByLevCohort1() {
		return getCountByCohort(TARGET_LEVERAGE, 1)
				/ getTotalCountByCohort(TARGET_LEVERAGE, 1);
	}

	public double getFirmsCountAsPercetangeByLevCohort2() {
		return getCountByCohort(TARGET_LEVERAGE, 2)
				/ getTotalCountByCohort(TARGET_LEVERAGE, 2);
	}

	public double getFirmsCountAsPercetangeByLevCohort3() {
		return getCountByCohort(TARGET_LEVERAGE, 3)
				/ getTotalCountByCohort(TARGET_LEVERAGE, 3);
	}

	public double getFirmsCountAsPercetangeByTimeCohort1() {
		return getCountByCohort(TIME_OF_ENTRY, 1)
				/ getTotalCountByCohort(TIME_OF_ENTRY, 1);
	}

	public double getFirmsCountAsPercetangeByTimeCohort2() {
		return getCountByCohort(TIME_OF_ENTRY, 2)
				/ getTotalCountByCohort(TIME_OF_ENTRY, 2);
	}

	public double getFirmsCountAsPercetangeByTimeCohort3() {
		return getCountByCohort(TIME_OF_ENTRY, 3)
				/ getTotalCountByCohort(TIME_OF_ENTRY, 3);
	}

}
