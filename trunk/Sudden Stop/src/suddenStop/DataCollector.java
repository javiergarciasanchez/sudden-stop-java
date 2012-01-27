package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

import java.sql.*;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Schema;
import repast.simphony.util.collections.IndexedIterable;
import suddenStop.Firm.Decision;
import suddenStop.Firm.State;
import static repast.simphony.engine.schedule.ScheduleParameters.*;

public class DataCollector {

	private Context<Object> context = null;
	SupplyManager suppMan = null;

	private Connection con;
	private final String conStr = "jdbc:sqlserver://localhost;databaseName=Simulations;integratedSecurity=true;";

	private String Simulation = null;

	public DataCollector(Context<Object> context) {

		this.context = context;
		context.add(this);

		try {
			con = DriverManager.getConnection(conStr);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Simulation = getSimNum();
		
		saveSimStart();

	}

	private void saveSimStart() {
		saveSimulationData();
		saveSimulationParams();
	}

	@ScheduledMethod(start = 1, priority = FIRST_PRIORITY)
	public void saveEveryRun() {
		saveGlobalParams();
	}

	@ScheduledMethod(start = 1, interval = 1, priority = LAST_PRIORITY)
	public void saveEveryStep() {
		saveMktData();
		saveFirmsData();
	}

	private void saveFirmsData() {
		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		if (firms.size() > 0.0) {
			for (Object f : firms) {
				saveFirmData(((Firm) f));
			}
		}
	}

	private void saveFirmData(Firm f) {
		String runNum = ((Integer) RunState.getInstance().getRunInfo()
				.getRunNumber()).toString();
		String tick = ((Double) GetTickCount()).toString();
		State cs = f.currentState;
		Decision cd = f.currentDecision;

		String sqlStr = "INSERT INTO [Individual Firms] ("
				+ "Simulation, [Run Number], Tick, [Firm ID], "
				+ "Profit, quantity, rD, FirstUnitCost, "
				+ "Born, Expon, VarCost, MedCost) VALUES ("
				
				+ Simulation + ", "
				+ runNum + ", "
				+ tick 	+ ", "
				+ "'" + f.toString() + "', "
				
				+ cs.profit + ", "
				+ cd.quantity + ", "
				+ cd.rD + ", "
				+ cs.firstUnitCost 	+ ", "
				
				+ f.born + ", "
				+ f.expon + ", "
				+ f.varCost + ", "
				+ f.medCost + " )";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {stmt.close();} catch (Throwable ignore) {
			}
		}

	}

	private String getSimNum() {
		// Get next simulation number
		String sqlStr = "SELECT MAX(SimulID) FROM Simulations";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sqlStr);
			Integer s;
			if (rs.next()) {
				s = rs.getInt(1) + 1;
			} else {
				s = 1;
			}
			return s.toString();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {stmt.close();} catch (Throwable ignore) {
			}
		}
		return null;
	}

	private void saveSimulationData() {
		String desc = (String) GetParameter("simDescription");

		String sqlStr = "INSERT INTO Simulations (SimulID, Description) "
				+ "VALUES (" + Simulation + ", '" + desc + "' )";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {stmt.close();} catch (Throwable ignore) {
			}		}

	}

	private void saveSimulationParams() {

		String sqlStr = "INSERT INTO [Simulation Parameters] VALUES ("
				+ Simulation + ", ? , ? )";

		Schema schema = RunEnvironment.getInstance().getParameters()
				.getSchema();

		PreparedStatement pstmt = null;

		try {
			pstmt = con.prepareStatement(sqlStr);
			for (String paramName : schema.parameterNames()) {
				if (paramName == "simDescription") {
					continue;
				} else {
					pstmt.setString(1, paramName);
					pstmt.setString(2, GetParameter(paramName).toString());
					pstmt.executeUpdate();
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {pstmt.close();} catch (Throwable ignore) {
			}		}

	}

	private void saveGlobalParams() {

		String runNum = ((Integer) RunState.getInstance().getRunInfo()
				.getRunNumber()).toString();
		double ssM = (Double) GetParameter("suddenStopMagnitude");
		int ssS = (Integer) GetParameter("suddenStopStart");
		int rndSeed = (Integer) GetParameter("randomSeed");
		Double robCheck = null;

		String sqlStr = "INSERT INTO [Global Parameters] VALUES (" + Simulation
				+ ", " + runNum + ", " + ssM + "," + ssS + ", " + rndSeed
				+ ", " + robCheck + ")";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {stmt.close();} catch (Throwable ignore) {
			}		}

	}

	private void saveMktData() {
		String runNum = ((Integer) RunState.getInstance().getRunInfo()
				.getRunNumber()).toString();
		String tick = ((Double) GetTickCount()).toString();
		String price = ((Double) suppMan.price).toString();

		String sqlStr = "INSERT INTO [Market Data] VALUES (" + Simulation
				+ ", " + runNum + ", " + tick + "," + price + ")";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {stmt.close();} catch (Throwable ignore) {
			}
		}
	}

}
