package old.sql.com.schneider.api.cost_codes.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lahoudie
 */
public class DbConnection {

    private Connection connection = null;
    private Statement statement = null;
    private String requete = null;
    private DbModel dbModel = null;

    /**
     * Creates a new instance of DataBase
     */
    public DbConnection() {
    }

    /**
     * @return Renvoit le modele
     */
    public DbModel getDbModel() {
        return dbModel;
    }

    public void setDbModel(DbModel dbModel) {
        this.dbModel = dbModel;
    }

    public void setRequete(String p_requete) {
        requete = p_requete;
    }

    public String getRequete() {
        return requete;
    }

    public String getDatabaseUrl() {
        return ("jdbc:postgresql://" + dbModel.getHostname() + ":" + dbModel.getPort() + "/" + dbModel.getDatabasename());
    }

    /**
     * Connexion a la base de donnees
     *
     * @param ()
     * @return void
     */
    public void connexion() throws Exception {
        Class.forName(dbModel.getDriverJdbc());
        setConnection(DriverManager.getConnection(getDatabaseUrl(), dbModel.getUsername(), dbModel.getPassword()));
        statement = getConnection().createStatement();
    }

    public void deconnexion() throws Exception {
        getConnection().close();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public ResultSet executeRequete(String r) throws SQLException {
        setRequete(r);
        return statement.executeQuery(r);
    }

    public int executeUpdate(String r) throws SQLException {
        setRequete(r);
        return statement.executeUpdate(r);
    }

}
