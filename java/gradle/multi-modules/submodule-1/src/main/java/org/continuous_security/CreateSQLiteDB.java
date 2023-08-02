package org.continuous_security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateSQLiteDB {
  public static void main(String[] args) {
    String databaseFilePath =
        "your_database.db"; // Replace this with the desired path to the database file.

    try {
      // Step 1: Register the SQLite JDBC driver (you need to have the JDBC driver JAR in your
      // classpath)
      Class.forName("org.sqlite.JDBC");

      // Step 2: Create a connection to the database (If the database doesn't exist, it will be
      // created)
      Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath);

      // Step 3: Create a table (optional)
      createTable(conn);

      // Step 4: Close the connection
      conn.close();

      System.out.println("Database and table (if not already present) are created successfully.");
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
  }

  private static void createTable(Connection conn) {
    try {
      Statement stmt = conn.createStatement();

      // Replace "your_table" and "your_column" with the desired table and column names.
      String createTableQuery =
          "CREATE TABLE IF NOT EXISTS your_table ("
              + "id INTEGER PRIMARY KEY,"
              + "name TEXT NOT NULL,"
              + "age INTEGER"
              + ");";
      stmt.execute(createTableQuery);

      stmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
