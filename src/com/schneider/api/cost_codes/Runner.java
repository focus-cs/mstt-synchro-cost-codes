package com.schneider.api.cost_codes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.schneider.api.cost_codes.business.ProjectsManager;
import com.schneider.api.cost_codes.business.UserLog;
import com.schneider.api.cost_codes.dao.BaseTamponDao;
import com.schneider.api.cost_codes.database.DbConnection;
import com.schneider.api.cost_codes.database.DbController;
import com.schneider.api.cost_codes.database.DbError;
import com.sciforma.psnext.api.Session;
import java.util.List;

public class Runner {
	
	public static final String APP_INFO = "Mstt Synchro Cost Codes v1.9";

	/**
	 * Logger Class instance.
	 */
	private static final Logger LOG = Logger.getLogger(Runner.class);

	/**
	 * UserLog Class instance.
	 */
        private static DbConnection dbcon;
        
	private static UserLog USER_LOG;
        private static  List<String> packages;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// configure log4j logger
		PropertyConfigurator.configure(args[1]);

		// display application informations.
		LOG.info(APP_INFO);

		// Copy arguments
		final String[] argsCopy = args;

		if (args.length == 2) {
			
			try {
				// load PSNext properties
				final Properties properties = readPropertiesFromFile(argsCopy[0]);
                                initDB(properties);
                                checkPackageName();
				LOG.debug(argsCopy[0] + " properties file loaded");
                                USER_LOG = UserLog.getInstance(dbcon);
                                USER_LOG.setConnection(dbcon);
				try {
					// init session
					final Session session = new Session(properties.getProperty("psnext.url"));
					session.login(properties.getProperty("psnext.login"), properties.getProperty("psnext.password").toCharArray());
					LOG.debug("Connected to PSNext");
					// Launch process
                                        for (String packageName : packages) {
                                            new ProjectsManager(session, packageName).execute(properties, session, dbcon);
                                        }
				} catch (Exception e1) {
					// Exception to connect to PSNext
					LOG.error(e1.getMessage());
					// USER_LOG.error("The API cannot connect to MSTT server.");
					USER_LOG.error("", "", "", 1, "N/A");
				}
				
			} catch (Exception e) {
				// exception to load properties file.
				LOG.error(e);
			}
			
		} else {
			LOG.error("Use : Main psconnect.properties directory");
		}

		LOG.debug("Exit code=" + USER_LOG.getStatus());
		// Exit process
                System.exit(USER_LOG.getStatus());
		//Runtime.getRuntime().exit(USER_LOG.getStatus());
	}

	/**
	 * Read Properties file using the path in input parameter
	 * 
	 * @return Properties
	 * @throws IOException
	 */
	public static Properties readPropertiesFromFile(final String path) throws IOException {
		final Properties properties = new Properties();
		final File file = new File(path);
		final InputStream resourceAsStream = new FileInputStream(file);
		properties.load(resourceAsStream);
		return properties;
	}
        
        private static void initDB(Properties properties) {
        try {
            DbController dbc = new DbController();
            dbc.readDbConfiguration(properties);
            dbcon = new DbConnection();
            dbcon.setDbModel(dbc.getDbModel());
            dbcon.connexion();
        } catch (DbError ex) {
            LOG.error("Fail to connect DB");
        } catch (Exception ex) {
            LOG.error("Fail to connect DB");
        }
    }

    private static void checkPackageName() {
        BaseTamponDao dao = new BaseTamponDao(dbcon);
        packages = dao.getAllPackageName();
    }

}
