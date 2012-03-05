package suddenStop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.awt.Component;
import javax.swing.JOptionPane;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunInfo;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Schema;
import static repast.simphony.essentials.RepastEssentials.*;

public class SuddenStopBuilder extends DefaultContext<Object> implements
		ContextBuilder<Object> {

	private static int simID;
	private static Connection con;

	@Override
	public Context<Object> build(Context<Object> context) {

		if (checkParam()) {

			IndependentVarsManager ivm = new IndependentVarsManager(context);
			SupplyManager sm = new SupplyManager(context,ivm);

			if (RunEnvironment.getInstance().isBatch()) {

				if (firstRun()) {
					// Creates the connection to database server
					try {
						con = createCon();
						System.out
								.println("Connection to database established");
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

				new SQLDataCollector(con, simID, context, sm);

				saveRunParams();

			}

			RunEnvironment.getInstance().endAt((Double) GetParameter("stopAt"));

		} else {
			EndSimulationRun();
		}
		return context;
	}

	private boolean checkParam() {
		/*
		 * Check consistency of Learning rate Mean
		 */
		double lRMean = (Double) GetParameter("learningRateMean");

		if (lRMean >= 1.0 || lRMean <= 0.5) {
			Component frame = null;
			JOptionPane.showMessageDialog(frame,
					"The Learning Rate Mean should be < 1 and > 0.5",
					"Inconsistent Learning Rate Parameter",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	private Connection createCon() throws SQLException {
		// Creates the connection to SQL Server
		String sqlSrv = (String) GetParameter("SQLServer");
		String db = (String) GetParameter("database");
		String conStr = "jdbc:sqlserver://" + sqlSrv + ";databaseName=" + db
				+ ";integratedSecurity=true;";

		Connection c = DriverManager.getConnection(conStr);

		return c;
	}

	private boolean firstRun() {
		RunInfo rInfo = RunState.getInstance().getRunInfo();

		return ((rInfo.getBatchNumber() == 1) && (rInfo.getRunNumber() == 1));

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

	private static Integer nextSimID() {
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

	private static void saveSimulationData() {
		String desc = (String) GetParameter("simDescription");

		String sqlStr = "INSERT INTO Simulations (SimulID, Description) "
				+ "VALUES (" + simID + ", '" + desc + "' )";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
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

		String sqlStr = "INSERT INTO [Simulation Parameters] VALUES (" + simID
				+ ", ? , ? )";

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

	private void saveRunParams() {
		int run = RunState.getInstance().getRunInfo().getRunNumber();
		double ssM = (Double) GetParameter("suddenStopMagnitude");
		int ssS = (Integer) GetParameter("suddenStopStart");
		int rndSeed = (Integer) GetParameter("randomSeed");
		Double robCheck = null;

		String sqlStr = "INSERT INTO [Global Parameters] VALUES (" + simID
				+ ", " + run + ", " + ssM + "," + ssS + ", " + rndSeed + ", "
				+ robCheck + ")";

		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(sqlStr);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch (Throwable ignore) {
			}
		}

		System.out.println("Run: " + run + " started");

	}

	public int getSimID() {
		return simID;
	}

	public Connection getCon() {
		return con;
	}

}
