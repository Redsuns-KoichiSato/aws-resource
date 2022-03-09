package ProvaDb;

import java.io.File;
import java.io.IOException;

// Configuration dependencies
import Employee.Employee;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import Config.*;

// SQL dependencies
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import Employee.*;

import java.util.*;

public class ProvaDb {
    private static Connection CONNECTION = null;
    private static Config CONFIG = null;

    public String handler() throws IOException, SQLException, ClassNotFoundException {
        List<Employee> employeeList = new ArrayList<Employee>();

        // Read configuration
        ClassLoader classLoader = getClass().getClassLoader();
        loadConfig(classLoader);
        System.out.println("Config: " + CONFIG.toString());

        // Connessione al database
        connectToDb();

        // Creazione tabella
        System.out.println("DEBUG: Creating Employee table...");
        Statement stmt = CONNECTION.createStatement();
        stmt.addBatch("CREATE TABLE Employee ( EmpID  INT NOT NULL, Name VARCHAR(255) NOT NULL, PRIMARY KEY (EmpID))");
        stmt.executeBatch();

        // Inserimento records
        System.out.println("DEBUG: Adding Employee record to table...");
        stmt.addBatch("INSERT INTO Employee (EmpID, Name) VALUES(1, \"Joe\")");
        stmt.addBatch("INSERT INTO Employee (EmpID, Name) VALUES(2, \"Bob\")");
        stmt.addBatch("INSERT INTO Employee (EmpID, Name) VALUES(3, \"Mary\")");
        stmt.executeBatch();

        // Lettura records
        System.out.println("DEBUG: Reading Employee table recors...");
        ResultSet rs = stmt.executeQuery("SELECT * from Employee");
        while (rs.next()) {
            Employee employee = new Employee();
            employee.setId(rs.getInt("EmpID"));
            employee.setName(rs.getString("Name"));
            employeeList.add(employee);
        }

        return "Added " +  employeeList.size() + " items from RDS MySQL table\n";
    }

    private static void loadConfig(ClassLoader classLoader) throws IOException {
        File file = new File(classLoader.getResource("config.yml").getFile());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        CONFIG = objectMapper.readValue(file, Config.class);
    }

    private static void connectToDb() throws SQLException, ClassNotFoundException {
        System.out.println("DEBUG: Connecting to a selected database " + CONFIG.databaseConf.DbUrl() + "...");
        CONNECTION = DriverManager.getConnection(
                CONFIG.databaseConf.DbUrl(),
                CONFIG.credentials.getUsername(),
                CONFIG.credentials.getPassword()
        );
        System.out.println("DEBUG: Connected database successfully...");
    }
}
