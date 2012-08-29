package suddenStop;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import static repast.simphony.essentials.RepastEssentials.*;

public class SuddenStopBuilder extends DefaultContext<Object> implements
		ContextBuilder<Object> {

	@Override
	public Context<Object> build(Context<Object> context) {

		SupplyManager sm = new SupplyManager(context);

		if (RunEnvironment.getInstance().isBatch()) {
			new SQLDataCollector(context, sm);
		}

		RunEnvironment.getInstance().endAt(
				(Double) GetParameter("stopAt")
						* (Integer) GetParameter("periods"));

		return context;
	}

}
