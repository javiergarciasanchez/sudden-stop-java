package suddenStop;

import java.util.HashMap;

import cern.jet.random.AbstractContinousDistribution;
import static suddenStop.DepVarsNames.*;

public class IndepVar {
	double[] limit;
	AbstractContinousDistribution distrib;
	HashMap<DepVarsNames, double[]> depVars;

	public IndepVar(int cohorts) {
		limit = new double[cohorts - 1];
		depVars = new HashMap<DepVarsNames, double[]>();
		resetDepVars(cohorts);
	}

	public void resetDepVars(int cohorts) {
		depVars.put(ROE_SUM, new double[cohorts]);
		depVars.put(ROI_SUM, new double[cohorts]);
		depVars.put(QUANTITY_PER_PERIOD_SUM, new double[cohorts]);
		depVars.put(FIRMS_COUNT, new double[cohorts]);
		depVars.put(PERFORMANCE_SUM, new double[cohorts]);
		depVars.put(MKT_SHARE_SUM, new double[cohorts]);
	}
}