package old.sql.com.schneider.api.cost_codes.database;

import old.sql.com.schneider.api.cost_codes.data.FileImport;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author lahoudie
 */
public class DbController {

    private DbModel dbModel = null;
    
    /**
     * Creates a new instance of XmlDbConfiguration
     */
    public DbController() {
    }

    public void readDbConfiguration(Properties properties) throws DbError {
       
            dbModel = new DbModel();
            dbModel.setDriverJdbc(properties.getProperty("db.driver"));
            dbModel.setHostname(properties.getProperty("db.host"));
            dbModel.setDatabasename(properties.getProperty("db.name"));
            dbModel.setUsername(properties.getProperty("db.user"));
            dbModel.setPassword(properties.getProperty("db.pass"));
            dbModel.setPort(properties.getProperty("db.port"));
    }

    public DbModel getDbModel() {
        return dbModel;
    }
}
