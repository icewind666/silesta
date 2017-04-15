package com.silesta.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database manager for postgresql.
 * Created by icewind on 15.04.17.
 */
public class DatabaseConnectionManager {
    /**
     * getJDBCConnection sets up and returns a JDBC connection
     * @return a JDBC connection
     */
    protected static Connection getJDBCConnection() {
        return null;
    }
//        Connection con = null;
//        Properties props = sm.getProps();
//        String driver = props.getProperty("JDBC_DRIVER", "org.postgresql.Driver");
//        try {
//            Class.forName(driver);
//            if (driver.indexOf("mysql") != -1) {
//                sm.setuseMysql(true);
//            }
//        } catch (java.lang.ClassNotFoundException e) {
//            System.err.println("ClassNotFoundException: " + e.getMessage());
//            System.err.println("you need the jdbc jar for " + driver + " in your classpath!\n");
//            System.err.println("Current classpath is: ");
//            System.err.println(System.getProperty("java.class.path"));
//            e.printStackTrace();
//            return null;
//        }
//
//        try {
//            String userid = props.getProperty("DB_USER", "collabreate");
//            String password = props.getProperty("DB_PASS");
//            if (password == null) {
//                //need to prompt for the password
//            }
//            String url = props.getProperty("JDBC_URL");
//            if (url == null) {
//                String dbname = props.getProperty("DB_NAME", "collabreate");
//                String host = props.getProperty("DB_HOST", "127.0.0.1");
//                String ssl = props.getProperty("USE_SSL", "no");
//                String dbtype = props.getProperty("JDBC_NAME", "postgresql");
//                url = "jdbc:" + dbtype + "://" + host + "/" + dbname;
//                if (ssl.equalsIgnoreCase("yes")) {
//                    url += "?ssl";
//                }
//            }
//            con = DriverManager.getConnection(url, userid, password);
//        } catch(SQLException ex) {
//            System.err.println("SQLException: " + ex.getMessage());
//            System.err.println("check permissions in your database configuration file\n");
//            return null;
//        }
//        try {
//            DatabaseMetaData meta = con.getMetaData();
//            System.out.println("Connected to " + meta.getURL());
//            System.out.print("DB Driver : " + meta.getDriverName());
//            System.out.println(" v: " + meta.getDriverVersion());
//            System.out.println("Database: " + meta.getDatabaseProductName() + " "
//                    + meta.getDatabaseMajorVersion() + "." + meta.getDatabaseMinorVersion());
//            System.out.println("JDBC v: " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
//        } catch(Exception ex1) {
//            System.err.println("Couldn't get driver metadata: " + ex1.getMessage());
//            //Is this a fatal error, do you want to close con here?
//        }
//        return con;
//    }

}
