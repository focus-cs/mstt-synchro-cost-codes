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
import com.sciforma.psnext.api.Session;

public class Runner {
	
	public static final String APP_INFO = "Mstt Synchro Cost Codes 2 v1.6 (2019/03/01)";

	/**
	 * Logger Class instance.
	 */
	private static final Logger LOG = Logger.getLogger(Runner.class);

	/**
	 * UserLog Class instance.
	 */
	private static final UserLog USER_LOG = UserLog.getInstance();

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
				LOG.debug(argsCopy[0] + " properties file loaded");

				try {
					// init session
					final Session session = new Session(properties.getProperty("psnext.url"));
					session.login(properties.getProperty("psnext.login"), properties.getProperty("psnext.password").toCharArray());
					LOG.debug("Connected to PSNext");
					// Launch process
					new ProjectsManager(session).execute(properties, session);
				} catch (Exception e1) {
					// Exception to connect to PSNext
					LOG.error(e1.getMessage());
					// USER_LOG.error("The API cannot connect to MSTT server.");
					USER_LOG.error("", "", "", 1);
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
		Runtime.getRuntime().exit(USER_LOG.getStatus());
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

}
