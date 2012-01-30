package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetTickCount;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunInfo;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.util.collections.IndexedIterable;
import suddenStop.Firm.Decision;
import suddenStop.Firm.State;
import static repast.simphony.engine.schedule.ScheduleParameters.*;

public class DataCollector {

	private int simID;
	private Connection con;
	private Context<Object> context = null;
	private SupplyManager suppMan = null;
	private RunInfo runInfo;

	public DataCollector(Connection con, int simID, Context<Object> context, SupplyManager sm) {

		this.con = con;
		this.simID = simID;

		this.context = context;
		suppMan = sm;

		context.add(this);
	
		runInfo = RunState.getInstance().getRunInfo();

	}


	@ScheduledMethod(start = 1, interval = 1, priority = LAST_PRIORITY)
	public void saveEveryStep() {
		int run = runInfo.getRunNumber();
		double tick = GetTickCount();
		saveMktData(run, tick);
		saveFirmsData(run, tick);

	}

	private void saveMktData(int run, double tick) {

		String price = ((Double) suppMan.price).toString();

		String sqlStr = "INSERT INTO [Market Data] VALUES (" + simID + ", "
				+ run + ", " + tick + "," + price + ")";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error trying to write to \"Market Data\" table.");
			System.exit(-1);
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}
	}

	private void saveFirmsData(int run, double tick) {
		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		if (firms.size() > 0.0) {
			for (Object f : firms) {
				saveFirmData(run, tick, (Firm) f);
			}
		}
	}

	private void saveFirmData(int run, double tick, Firm f) {

		State cs = f.currentState;
		Decision cd = f.currentDecision;

		String sqlStr = "INSERT INTO [Individual Firms] ("
				+ "Simulation, [Run Number], Tick, [Firm ID], "
				+ "Profit, quantity, rD, FirstUnitCost, "
				+ "Born, Expon, VarCost, MedCost) VALUES ("

				+ simID
				+ ", "
				+ run
				+ ", "
				+ tick
				+ ", "
				+ "'"
				+ f.toString()
				+ "', "

				+ cs.profit
				+ ", "
				+ cd.quantity
				+ ", "
				+ cd.rD
				+ ", "
				+ cs.firstUnitCost
				+ ", "

				+ f.born
				+ ", "
				+ f.expon
				+ ", "
				+ f.varCost
				+ ", "
				+ f.medCost
				+ " )";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error trying to write to \"Individual Firms\" table.");
			System.exit(-1);
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}

	}

}
