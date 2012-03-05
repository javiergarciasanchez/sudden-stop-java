package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetParameter;
import repast.simphony.context.Context;

public class EquityFirm extends Firm {

	public EquityFirm(Context<Object> context) {
		super(context);
	}

	@Override
	protected void tryMeetCapitalNeeds(FirmState st) {

		// Check if in potential default
		if (st.debt > (st.capital + st.cashExcess)) {
			// Equity is increased to meet max leverage
			st.capital = st.debt / (Double) GetParameter("maxLeverage");
			st.cashExcess = 0.0;
		}

		// Check if min capital is not reached
		double minCap = (Double) GetParameter("minimumCapital");
		if (minCap > st.capital) {
			getFunds(st, minCap - st.capital);
		}
	}


	/*
	 * Pecking order theory of financing is applied cf. Myers (1984)
	 * "The Capital Structure Puzzle" Firm tries to get optInv except it cannot
	 * reach it due to funds restrictions
	 */
	@Override
	protected void getFunds(FirmState st, double funds) {

		double maxNewDebt, maxNewEq;

		// During SS there is no external funds available
		if (Demand.getSSMagnitude() > 0.0){			
			maxNewDebt = 0.0;
			maxNewEq = 0.0;			
		}
		else {
			maxNewDebt = (Double) GetParameter("maxLeverage") * st.capital
					- st.debt;
			maxNewEq = Double.POSITIVE_INFINITY;
		}


		if (funds < st.cashExcess) {
			st.cashExcess -= funds;

			// Cash Excess is used to cancel debt after fund needs are met
			if (st.cashExcess > st.debt) {
				st.cashExcess -= st.debt;
				st.debt = 0.0;
			} else {
				st.debt -= st.cashExcess;
				st.cashExcess = 0.0;
			}

		} else if (funds < (st.cashExcess + maxNewDebt)) {

			st.debt += (funds - st.cashExcess);
			st.cashExcess = 0.0;

		} else if (funds < (st.cashExcess + maxNewDebt + maxNewEq)) {

			st.debt = st.debt + maxNewDebt;
			st.cashExcess = 0.0;

		} else {
			// Not all the funds are obtained
			funds = st.cashExcess + maxNewDebt + maxNewEq;
			st.debt = st.debt + maxNewDebt;
			st.cashExcess = 0.0;

		}

		st.capital = st.capital + funds;

	}

}
