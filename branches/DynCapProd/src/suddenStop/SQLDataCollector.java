package suddenStop;

import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunInfo;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Schema;
import repast.simphony.util.collections.IndexedIterable;
import static repast.simphony.engine.schedule.ScheduleParameters.*;

public class SQLDataCollector {

	private static int simID;
	private static Connection conn = null;
	private Context<Object> context = null;
	private SupplyManager suppMan = null;
	private RunInfo runInfo;
	private PreparedStatement mktDataPstm;
	private PreparedStatement firmsConstDataPstm;
	private PreparedStatement firmsPerTickDataPstm;

	/*
	 * An instance is created on each run The first time it is created, the
	 * constructor establishes the connection to the database and saves info of
	 * the whole simulation
	 */
	public SQLDataCollector(Context<Object> context, SupplyManager sm) {

		this.context = context;
		suppMan = sm;

		context.add(this);

		runInfo = RunState.getInstance().getRunInfo();

		// Check if the first run
		if (conn == null) {

			// Creates the connection to database server
			try {
				conn = createCon();
				System.out.println("Connection to database established");
			} catch (SQLException e) {
				e.printStackTrace();
				System.err
						.println("Connection to database server could not be established");
				System.exit(-1);
			}

			// Gets the next simID from database
			simID = nextSimID();

			saveSimInfo();

		}

		saveRunParams();

		/*
		 * This is redundat, it will replace saveRunParams and part of
		 * saveSimInfo (param info) The idea is to split params that depend on
		 * run from the ones that depend on Sim, later using sql. This will
		 * provide more flexibility for robustness check
		 */
		saveAllParams();

		createPrepStatments();

	}

	private Connection createCon() throws SQLException {
		
		/*
		 * The class loader used when the GUI calls the batch doesn't find
		 * the sql driver.
		 * Nick Nicollier suggested using another class loader and it worked 
		 */
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Context.class.getClassLoader());
		
		// Creates the connection to SQL Server
		String sqlSrv = (String) GetParameter("SQLServer");
		String db = (String) GetParameter("database");
		String conStr = "jdbc:sqlserver://" + sqlSrv + ";databaseName=" + db
				+ ";integratedSecurity=true;";

		conn = DriverManager.getConnection(conStr);
		
		// Part of Nick suggestion
		Thread.currentThread().setContextClassLoader(current);
		
		return conn;
		
	}

	private static Integer nextSimID() {
		// Get next simulation number
		String sqlStr = "SELECT MAX(SimID) FROM Simulations";

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sqlStr);
			Integer s;
			if (rs.next()) {
				s = rs.getInt(1) + 1;
			} else {
				s = 1;
			}
			return s;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}

		return null;
	}

	/*
	 * Returns Simulation ID number
	 */
	private void saveSimInfo() {

		// Saves simulation data and parameters of the whole simulation
		saveSimulationData();
		saveSimulationParams();

		System.out.println("Simulation Info saved");

	}

	private static void saveSimulationData() {
		String desc = (String) GetParameter("simDescription");

		String sqlStr = "INSERT INTO Simulations (SimID, Description) "
				+ "VALUES (" + simID + ", '" + desc + "' )";

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sqlStr);
			System.out.println("Simulation " + simID
					+ " added to Table \"Simulations\".");

		} catch (SQLException e) {
			e.printStackTrace();
			System.err
					.println("Error trying to write to \"Simulations\" table.");
			System.exit(-1);
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}

	}

	private static void saveSimulationParams() {

		String sqlStr = "INSERT INTO SimulationParameters VALUES (" + simID
				+ ", ? , ? )";

		Schema schema = RunEnvironment.getInstance().getParameters()
				.getSchema();

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement(sqlStr);
			for (String paramName : schema.parameterNames()) {
				if (isSimData(paramName) || isRunParam(paramName)) {
					continue;
				} else {
					pstmt.setString(1, paramName);
					pstmt.setString(2, GetParameter(paramName).toString());
					pstmt.executeUpdate();
				}
			}
			System.out
					.println("Parameters of the whole simulation saved to table \"Simulation Parameters\".");

		} catch (SQLException e) {
			e.printStackTrace();
			System.err
					.println("Error trying to write to \"Simulation Parameters\" table.");
			System.exit(-1);
		} finally {
			try {
				pstmt.close();
			} catch (Throwable ignore) {
			}
		}

	}

	private void saveAllParams() {
		int run = RunState.getInstance().getRunInfo().getRunNumber();

		String sqlStr = "INSERT INTO AllParameters VALUES (" + simID + ", "
				+ run + ", ?, ? )";

		Schema schema = RunEnvironment.getInstance().getParameters()
				.getSchema();

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement(sqlStr);
			for (String paramName : schema.parameterNames()) {
				if (isSimData(paramName)) {
					continue;
				} else {
					pstmt.setString(1, paramName);
					pstmt.setString(2, GetParameter(paramName).toString());
					pstmt.executeUpdate();
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				pstmt.close();
			} catch (Throwable ignore) {
			}
		}

	}

	private void saveRunParams() {
		int run = RunState.getInstance().getRunInfo().getRunNumber();
		double ssM = (Double) GetParameter("suddenStopMagnitude");
		int ssS = (Integer) GetParameter("suddenStopStart");
		int rndSeed = (Integer) GetParameter("randomSeed");
		Double robCheck = null;
		String robCheckName = null;

		String sqlStr = "INSERT INTO RunParameters VALUES (" + simID + ", "
				+ run + ", " + ssM + "," + ssS + ", " + rndSeed + ", "
				+ robCheck + ", " + robCheckName + ")";

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}

		System.out.println("Run: " + run + " started");

	}

	private void createPrepStatments() {
		String mktDataStr = "INSERT INTO MarketData "
				+ "( Simulation, RunNumber, Tick, Price, TotalQuantity ) "
				+ "VALUES ( ?, ? , ?, ?, ? )";

		String firmsConstDataStr = "INSERT INTO IndividualFirms ("
				+ "Simulation, RunNumber, Firm, "
				+ "InitialFUC, RDEfficiency, TargetLeverage, "
				+ "MaxExternalEquity, LearningRate, Born ) "
				+ "VALUES (?,?,?,?,?,?,?,?,?)";

		String firmsPerTickDataStr = "INSERT INTO [IndividualFirmsPerTick] ("
				+ "Simulation, RunNumber, Tick, Firm, "
				+ "Profit, Quantity, RD, FirstUnitCost, "
				+ "Capital, Debt, MinVarCost, ToBeKilled, "

				+ "AcumQ, MedCost, TotFixedCost, TotVarCost, "
				+ "Interest, ExpectedEquityRetribution, Performance, "
				+ "EBITDA, MktShare, ExpectedCapitalRetribution ) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try {
			mktDataPstm = conn.prepareStatement(mktDataStr);
			firmsConstDataPstm = conn.prepareStatement(firmsConstDataStr);
			firmsPerTickDataPstm = conn.prepareStatement(firmsPerTickDataStr);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);

		}

	}

	@ScheduledMethod(start = END, priority = LAST_PRIORITY)
	public void closePrepStatments() {
		try {
			mktDataPstm.close();
			firmsConstDataPstm.close();
			firmsPerTickDataPstm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static boolean isSimData(String paramName) {
		return (paramName == "simDescription");
	}

	private static boolean isRunParam(String paramName) {
		return (paramName == "suddenStopMagnitude"
				|| paramName == "suddenStopStart" || paramName == "randomSeed");
	}

	@ScheduledMethod(start = 1, interval = 1, priority = LAST_PRIORITY)
	public void saveEveryStep() {
		int run = runInfo.getRunNumber();
		double tick = GetTickCount();
		saveMktData(run, tick);
		saveFirmsData(run, tick);

	}

	private void saveMktData(int run, double tick) {

		try {
			mktDataPstm.setInt(1, simID);
			mktDataPstm.setInt(2, run);
			mktDataPstm.setDouble(3, tick);
			mktDataPstm.setDouble(4, suppMan.price);
			mktDataPstm.setDouble(5, suppMan.totalQuantityPerPeriod);
			mktDataPstm.execute();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error trying to write to \"Market Data\" table.");
			System.exit(-1);
		}

	}

	private void saveFirmsData(int run, double tick) {
		IndexedIterable<Object> firms = context.getObjects(Firm.class);

		for (Object o : firms) {
			Firm f = (Firm) o;

			if (f.getAge() == 0) {
				saveConstFirmData(run, f);
			}

			savePerTickFirmData(run, tick, f);

		}
	}

	private void saveConstFirmData(int run, Firm f) {

		try {
			firmsConstDataPstm.setInt(1, simID);
			firmsConstDataPstm.setInt(2, run);
			firmsConstDataPstm.setFloat(3, f.agentIntID);
			firmsConstDataPstm.setDouble(4, f.getInitialFUC());
			firmsConstDataPstm.setDouble(5, f.getRDEfficiency());
			firmsConstDataPstm.setDouble(6, f.getTargetLeverage());
			firmsConstDataPstm.setDouble(7, f.getMaxExternalEquity());
			firmsConstDataPstm.setDouble(8, f.getLearningRate());
			firmsConstDataPstm.setDouble(9, f.getBornInYears());
			firmsConstDataPstm.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			System.err
					.println("Error trying to write constant data of a firm ");
			System.exit(-1);

		}

	}

	private void savePerTickFirmData(int run, double tick, Firm f) {

		try {
			firmsPerTickDataPstm.setInt(1, simID);
			firmsPerTickDataPstm.setInt(2, run);
			firmsPerTickDataPstm.setDouble(3, tick);
			firmsPerTickDataPstm.setFloat(4, f.agentIntID);
			firmsPerTickDataPstm.setDouble(5, f.getProfitPerPeriod());
			firmsPerTickDataPstm.setDouble(6, f.getQuantityPerPeriod());
			firmsPerTickDataPstm.setDouble(7, f.getRDPerPeriod());
			firmsPerTickDataPstm.setDouble(8, f.getFirstUnitCost());
			firmsPerTickDataPstm.setDouble(9, f.getCapital());
			firmsPerTickDataPstm.setDouble(10, f.getDebt() - f.getCash());
			firmsPerTickDataPstm.setDouble(11, f.getMinVarCost());
			firmsPerTickDataPstm.setBoolean(12, f.isToBeKilled());

			firmsPerTickDataPstm.setDouble(13, f.getAcumQ());
			firmsPerTickDataPstm.setDouble(14, f.getMedCost());
			firmsPerTickDataPstm.setDouble(15, f.getTotFixedCostPerPeriod());
			firmsPerTickDataPstm.setDouble(16, f.getTotVarCostPerPeriod());
			firmsPerTickDataPstm.setDouble(17, f.getInterestPerPeriod());
			firmsPerTickDataPstm.setDouble(18,
					f.getExpectedEquityRetributionPerPeriod());
			firmsPerTickDataPstm.setDouble(19, f.getPerformance());
			firmsPerTickDataPstm.setDouble(20, f.getEBITDAPerPeriod());
			firmsPerTickDataPstm.setDouble(21, f.getMktShare());
			firmsPerTickDataPstm.setDouble(22, f.getExpectedCapitalRetributionPerPeriod());
			firmsPerTickDataPstm.execute();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error trying to write per tick data of a firm ");
			System.exit(-1);

		}

	}

}
