package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetParameter;
import repast.simphony.context.Context;

public class DebtFirm extends Firm {

	public DebtFirm(Context<Object> context) {
		super(context);
	}

	@Override
	protected void tryMeetCapitalNeeds(FirmState st) {
		double minCap = (Double) GetParameter("minimumCapital");

		if (st.getDebt() > st.getAssets()) {
			// Default
			return;

		} else if (minCap > st.getCapital()) {
			// min capital is not reached
			getFunds(st, minCap - st.getCapital());
		}
	}

	/*
	 * Pecking order theory of financing is applied cf. Myers (1984)
	 * "The Capital Structure Puzzle" Firm tries to get optInv except it cannot
	 * reach it due to funds restrictions
	 */
	@Override
	protected void getFunds(FirmState st, double funds) {
		double maxNewDebt;

		// During SS there is no debt available available
		if (Demand.getSSMagnitude() > 0.0)
			maxNewDebt = 0.0;
		else {
			maxNewDebt = (Double) GetParameter("maxLeverage") * st.getAssets()
					- st.getDebt();
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

		} else {
			// Not all the funds are obtained
			funds = st.cashExcess + maxNewDebt;
			st.debt += maxNewDebt;
			st.cashExcess = 0.0;

		}

		st.capital += funds;

	}

}
