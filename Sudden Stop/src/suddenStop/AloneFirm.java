package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetParameter;
import repast.simphony.context.Context;
import static java.lang.Math.*;

public class AloneFirm extends Firm {

	public AloneFirm(Context<Object> context) {
		super(context);
	}

	@Override
	protected void tryMeetCapitalNeeds(FirmState st) {
		double minCap = (Double) GetParameter("minimumCapital");

		if (minCap > st.capital) {
			getFunds(st, minCap - st.capital);
		}

	}
	
	@Override
	protected void getFunds(FirmState st, double funds) {
		
		st.capital += min(funds, st.cashExcess);
		st.cashExcess -= min(funds, st.cashExcess);

	}

}
