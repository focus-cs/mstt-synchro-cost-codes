package com.schneider.api.cost_codes.database;

/**
 *
 * @author lahoudie
 */
public class DbModel
{
  private String driverJdbc = null;
  private String hostname = null;
  private String databasename = null;
  private String username = null;
  private String password = null;
  private String port = null;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabasename() {
        return databasename;
    }

    public void setDatabasename(String databasename) {
        this.databasename = databasename;
    }

    public String getDriverJdbc() {
        return driverJdbc;
    }

    public void setDriverJdbc(String driverJdbc) {
        this.driverJdbc = driverJdbc;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
