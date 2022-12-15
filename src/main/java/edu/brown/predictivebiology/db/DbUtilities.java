/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.db;

import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.db.beans.Coordinates;
import edu.brown.predictivebiology.db.beans.Directory;
import edu.brown.predictivebiology.db.beans.Experiment;
import edu.brown.predictivebiology.db.beans.WellDescription;
import edu.brown.predictivebiology.gui.StartFrame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Haitham
 */
public class DbUtilities {

    private static Connection dbConnection = null;

    /**
     * Displays the contents of a generic-type array
     *
     * @param <T> type of the array
     * @param list the list to be displayed
     */
    public static <T> void display(List<T> list) {
        System.out.println("---------------------");
        for (T t : list) {
            System.out.println(t.toString());
        }
        System.out.println("---------------------");
    }

    /**
     * This utility function returns true only if all the references in the
     * array are nulls.
     *
     * @param objs array of objects to be checked
     * @return true if all references are nulls, false otherwise
     */
    private static boolean allNulls(Object[] objs) {
        for (Object obj : objs) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a connection to the database. This process follows a singleton
     * design pattern, where only one live connection exists at any time.
     *
     * @return a connection to the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed
     */
    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {
        if (dbConnection == null || dbConnection.isClosed()) {
            // load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");
            // create a database connection
            dbConnection = DriverManager.getConnection("jdbc:sqlite:lumen.db");
            // Enable foreign key support (makes the CASCADE option effective)
            dbConnection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");
        }
        return dbConnection;
    }

    /**
     * Drops all tables.
     *
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     */
    public static void deleteDb() throws ClassNotFoundException, SQLException {
        Connection connection = getConnection();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate("drop table if exists coordinates");
            statement.executeUpdate("drop table if exists images");
            statement.executeUpdate("drop table if exists directories");
            statement.executeUpdate("drop table if exists wellsDescription");
            statement.executeUpdate("drop table if exists experiments");
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Creates all tables.
     *
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     */
    public static void createDb() throws ClassNotFoundException, SQLException {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            // Create experiments table
            statement.executeUpdate(
                    "create table if not exists experiments("
                    + "id integer PRIMARY KEY AUTOINCREMENT, "
                    + "name varchar(100) NOT NULL UNIQUE, "
                    + "rowCount integer, "
                    + "colCount integer, "
                    + "plateName varchar(50))");
            // Create wellsDescription table
            statement.executeUpdate(
                    "create table if not exists wellsDescription("
                    + "plateId integer REFERENCES experiments(id) "
                    + "ON DELETE CASCADE, "
                    + "row integer NOT NULL, "
                    + "col integer NOT NULL, "
                    + "compound varchar(50), "
                    + "concentration real, "
                    + "cellType varchar(50), "
                    + "cellCount int, "
                    + "PRIMARY KEY(plateId, row, col))");
            // Create directories table
            statement.executeUpdate(
                    "create table if not exists directories("
                    + "id integer PRIMARY KEY AUTOINCREMENT, "
                    + "path varchar(200) NOT NULL, "
                    + "experimentId integer REFERENCES experiments(id) "
                    + "ON DELETE CASCADE)");
            // Create images table
            statement.executeUpdate(
                    "create table if not exists images("
                    + "id integer PRIMARY KEY AUTOINCREMENT, "
                    + "path varchar(200) NOT NULL, "
                    + "parentDirId integer REFERENCES directories(id) "
                    + "ON DELETE CASCADE)");
            // Create coordinates table
            statement.executeUpdate(
                    "create table if not exists coordinates(id integer PRIMARY KEY "
                    + "AUTOINCREMENT, "
                    + "x integer, y integer, "
                    + "imageID integer REFERENCES images(id) "
                    + "ON DELETE CASCADE)");
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Retrieves columns names (headers) of a specific table in the database.
     *
     * @param tableName the database table whose columns headers are being
     * retrieved
     * @return a list of strings representing columns headers in the same order
     * as the columns in the database.
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     */
    public static List<String> getColumnsNames(String tableName)
            throws SQLException, ClassNotFoundException {
        // Get columns names
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            ResultSet rss = statement.executeQuery("select * from " + tableName);
            ResultSetMetaData rsmd = rss.getMetaData();
            int columnCount = rsmd.getColumnCount();
            List<String> colNames = new ArrayList<>();
            // The column count starts from 1
            for (int i = 1; i <= columnCount; i++) {
                colNames.add(rsmd.getColumnName(i));
            }
            return colNames;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Retrieves columns data types of a specific table in the database.
     *
     * @param tableName the database table whose columns data types are being
     * retrieved
     * @return a list of Integers representing columns data types in the same
     * order as the columns in the database. database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     */
    public static List<Integer> getColumnsTypes(String tableName)
            throws SQLException, ClassNotFoundException {
        // Get columns names
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            ResultSet rss = statement.executeQuery("select * from " + tableName);
            ResultSetMetaData rsmd = rss.getMetaData();
            int columnCount = rsmd.getColumnCount();
            List<Integer> colTypes = new ArrayList<>();
            // The column count starts from 1
            for (int i = 1; i <= columnCount; i++) {
                colTypes.add(rsmd.getColumnType(i));
            }
            return colTypes;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Map SQL data types to their corresponding Java data types.
     *
     * @param columnsTypes SQL types of database columns
     * @return an array of class objects each representing one mapped data type
     * @throws UnsupportedOperationException if no mapping is defined for some
     * some SQL data type
     */
    public static Class<?>[] mapSqlTypes2JavaTypes(List<Integer> columnsTypes)
            throws UnsupportedOperationException {
        // Find parameter types
        Class<?>[] parameterTypes = new Class<?>[columnsTypes.size()];
        for (int i = 0; i < columnsTypes.size(); i++) {
            switch (columnsTypes.get(i)) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                    parameterTypes[i] = String.class;
                    break;
                case java.sql.Types.INTEGER:
                case java.sql.Types.SMALLINT:
                case java.sql.Types.TINYINT:
                    parameterTypes[i] = Integer.class;
                    break;
                case java.sql.Types.DOUBLE:
                case java.sql.Types.REAL:
                case java.sql.Types.FLOAT:
                    parameterTypes[i] = Double.class;
                    break;
                default:
                    throw new UnsupportedOperationException(
                            String.format("No sql-to-java mapping is "
                                    + "defined from the sql datatype (%s)",
                                    columnsTypes.get(i)));
            }
        }
        return parameterTypes;
    }

    /**
     * This method is the core of all select queries executed on the database.
     * It is designed to be completely generic. Other higher level "select"
     * methods should communicate with the database through this method,
     * regardless of the tables they query, if: 1 - The condition clause uses
     * only the (and) operator to connect its parts 2 - The bean (e.g.
     * db.beans.Image) has a constructor that takes one argument for each
     * database column. No columns are skipped. 3 - The order of the arguments
     * of the bean constructor is the same as the oder of the columns in the
     * database. 4 - The delete query references only one table.
     *
     * @param <T>
     * @param tableName
     * @param myClass
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws java.lang.InstantiationException
     */
    public static <T> List<T> selectFromDbTable(
            String tableName, Class<T> myClass, Object... parameters)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Statement statement = null;
        try {
            // Get columns names (headers)
            List<String> colNames = getColumnsNames(tableName);
            // Get columns types (SQL types)
            List<Integer> columnsTypes = getColumnsTypes(tableName);
            // Map SQL types to Java types
            Class<?>[] parameterTypes = mapSqlTypes2JavaTypes(columnsTypes);
            List<T> objList = new ArrayList<>();
            // Form the select query
            String query = "select * from " + tableName;
            if (!allNulls(parameters)) {
                query += " where";
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] != null) {
                        if (!query.endsWith(" where")) {
                            query += " and";
                        }
                        if (parameters[i].getClass() == String.class) {
                            query += String.format(
                                    " " + colNames.get(i) + " = '%s'",
                                    parameters[i].toString());
                        } else {
                            query += String.format(
                                    " " + colNames.get(i) + " = %s",
                                    parameters[i].toString());
                        }
                    }
                }
            }
            // Call the static instantiate method to create an object from T
            Constructor<T> constructor = myClass.getConstructor(parameterTypes);
            // Execute the formed query
            System.out.println(query);
            statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery(query);
            // Fill in the list to be returned
            while (rs.next()) {
                Object[] initialValues = new Object[parameters.length];
                for (int i = 0; i < initialValues.length; i++) {
                    if (parameterTypes[i] == String.class) {
                        initialValues[i] = rs.getString(colNames.get(i));
                    } else if (parameterTypes[i] == Integer.class) {
                        initialValues[i] = rs.getInt(colNames.get(i));
                    } else if (parameterTypes[i] == Double.class) {
                        initialValues[i] = rs.getDouble(colNames.get(i));
                    } else {
                        initialValues[i] = rs.getObject(colNames.get(i));
                    }
                }
                // Create an instance from T
                T t = (T) constructor.newInstance(initialValues);
                // Add the created instance to the final list
                objList.add(t);
            }
            return objList;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * This method is the core of all delete queries executed on the database.
     * It is designed to be completely generic. Other higher level "delete"
     * methods should communicate with the database through this method,
     * regardless of the tables they query, if: 1 - The delete query references
     * only one table.
     *
     * @param <T>
     * @param myClass
     * @param tableName
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws java.lang.InstantiationException
     */
    public static <T> int deleteFromDbTable(
            String tableName, Object... parameters)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Statement statement = null;
        try {
            // Get columns names (headers)
            List<String> colNames = getColumnsNames(tableName);
            // Form the select query
            String query = "delete from " + tableName;
            if (!allNulls(parameters)) {
                query += " where";
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] != null) {
                        if (!query.endsWith(" where")) {
                            query += " and";
                        }
                        if (parameters[i].getClass() == String.class) {
                            query += String.format(
                                    " " + colNames.get(i) + " = '%s'",
                                    parameters[i].toString());
                        } else {
                            query += String.format(
                                    " " + colNames.get(i) + " = %s",
                                    parameters[i].toString());
                        }
                    }
                }
            }
            // Execute the formed query
            System.out.println(query);
            statement = getConnection().createStatement();
            int deletedRowsCount = statement.executeUpdate(query);
            return deletedRowsCount;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * This method is the core of all insert queries executed on the database.
     * It is designed to be completely generic. Other higher level "insert"
     * methods should communicate with the database through this method,
     * regardless of the tables they query, if: 1 - The insert query references
     * only one table.
     *
     * @param <T>
     * @param myClass
     * @param tableName
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws java.lang.InstantiationException
     */
    public static <T> int insertIntoDbTable(
            String tableName, Object... parameters)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Statement statement = null;
        try {
            // Get columns names (headers)
            List<String> colNames = getColumnsNames(tableName);
            // Form the select query
            if (allNulls(parameters)) {
                throw new IllegalArgumentException(
                        "Cannot add a record of null vales into the database");
            } else {
                // Form the columns and the values parts
                String columnsPart = "";
                String valuesPart = "";
                boolean firstNonNullParameterEncountered = false;
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] != null) {
                        if (firstNonNullParameterEncountered) {
                            columnsPart += ", ";
                            valuesPart += ", ";
                        }
                        columnsPart += colNames.get(i);
                        if (parameters[i].getClass() == String.class) {
                            valuesPart += "'" + parameters[i] + "'";
                        } else {
                            valuesPart += parameters[i];
                        }
                        firstNonNullParameterEncountered = true;
                    }
                }
                // Execute the formed query
                String query = String.format("insert into %s (%s) values(%s)",
                        tableName, columnsPart, valuesPart);
                System.out.println(query);
                statement = getConnection().createStatement();
                return statement.executeUpdate(query);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * This method is the core of all update queries executed on the database.
     * It is designed to be completely generic. Other higher level "update"
     * methods should communicate with the database through this method,
     * regardless of the tables they query, if: 1 - The insert query references
     * only one table.
     *
     * @param <T>
     * @param myClass
     * @param tableName
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws java.lang.InstantiationException
     */
    public static <T> int updateDbTable(
            String tableName,
            Object... parameters)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Statement statement = null;
        try {
            // Get columns names (headers)
            List<String> colNames = getColumnsNames(tableName);
            // Form the select query
            if (allNulls(parameters)) {
                throw new IllegalArgumentException(
                        "Cannot add a record of null vales into the database");
            } else {
                // Split the parameters into assignment and condition parameters
                Object[] conditionParameters = new Object[parameters.length / 2];
                Object[] assignmentParameters = new Object[parameters.length / 2];
                System.arraycopy(parameters, 0,
                        conditionParameters, 0,
                        parameters.length / 2);
                System.arraycopy(parameters, parameters.length / 2,
                        assignmentParameters, 0,
                        parameters.length / 2);
                // Form the condition part
                String conditionPart = "";
                if (!allNulls(conditionParameters)) {
                    conditionPart += "where";
                    for (int i = 0; i < conditionParameters.length; i++) {
                        if (conditionParameters[i] != null) {
                            if (!conditionPart.endsWith("where")) {
                                conditionPart += " and";
                            }
                            if (conditionParameters[i].getClass() == String.class) {
                                conditionPart += String.format(
                                        " " + colNames.get(i) + " = '%s'",
                                        conditionParameters[i].toString());
                            } else {
                                conditionPart += String.format(
                                        " " + colNames.get(i) + " = %s",
                                        conditionParameters[i].toString());
                            }
                        }
                    }
                }
                // From the assignments part
                if (allNulls(assignmentParameters)) {
                    throw new IllegalArgumentException(
                            "Cannot update a record with null values.");
                }
                String assignmentsPart = "";
                boolean firstNonNullParameterEncountered = false;
                for (int i = 0; i < assignmentParameters.length; i++) {
                    if (assignmentParameters[i] != null) {
                        if (firstNonNullParameterEncountered) {
                            assignmentsPart += ", ";
                        }
                        if (assignmentParameters[i].getClass() == String.class) {
                            assignmentsPart += String.format(
                                    "%s = '%s'",
                                    colNames.get(i), assignmentParameters[i]);
                        } else {
                            assignmentsPart += String.format(
                                    "%s = %s",
                                    colNames.get(i), assignmentParameters[i]);
                        }
                        firstNonNullParameterEncountered = true;
                    }
                }
                // Execute the formed query
                String query = String.format("update %s set %s %s",
                        tableName, assignmentsPart, conditionPart);
                System.out.println(query);
                statement = getConnection().createStatement();
                return statement.executeUpdate(query);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * This is generic delete method that takes only a table name and a
     * condition.
     *
     * @param tableName the table to delete from
     * @param condition the condition of deletion
     * @return the number of rows deleted
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int customDeleteQuery(String tableName, String condition)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        String query = String.format("Delete from %s where %s",
                tableName,
                condition);
        Statement statement = getConnection().createStatement();
        return statement.executeUpdate(query);
    }

    /**
     * Delete images specified.
     *
     * @param images all images to be deleted
     * @return the number of deleted records
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteImages(List<Image> images)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        StringBuilder conditionSb = new StringBuilder();
        for (Image image : images) {
            if (!conditionSb.toString().equals("")) {
                conditionSb.append(" or ");
            }
            conditionSb.append("id = " + image.getId());
        }
        return customDeleteQuery("images", conditionSb.toString());
    }

    /**
     * This method is the core of all select max column value query executed on
     * the database. It is designed to be completely generic. Other higher level
     * select-max-column-value" methods should communicate with the database
     * through this method, regardless of the tables and the columns they query,
     * if: 1 - The condition clause uses only the (and) operator to connect its
     * parts
     *
     * @param <T>
     * @param tableName
     * @param colName
     * @param myClass
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws java.lang.InstantiationException
     */
    public static Integer selectMaxColValue(
            String tableName, String colName, Object... parameters)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Statement statement = null;
        try {
            // Get columns names (headers)
            List<String> colNames = getColumnsNames(tableName);
            // Get columns types (SQL types)
            List<Integer> columnsTypes = getColumnsTypes(tableName);
            // Map SQL types to Java types
            Class<?>[] parameterTypes = mapSqlTypes2JavaTypes(columnsTypes);
            // Form the select query
            String query = "select max(" + colName + ") from " + tableName;
            if (!allNulls(parameters)) {
                query += " where";
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] != null) {
                        if (!query.endsWith(" where")) {
                            query += " and";
                        }
                        if (parameters[i].getClass() == String.class) {
                            query += String.format(
                                    " " + colNames.get(i) + " = '%s'",
                                    parameters[i].toString());
                        } else {
                            query += String.format(
                                    " " + colNames.get(i) + " = %s",
                                    parameters[i].toString());
                        }
                    }
                }
            }
            // Execute the formed query
            System.out.println(query);
            statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery(query);
            // Fill in the list to be returned
            rs.next();
            // Add the created instance to the final list
            return rs.getInt(1);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Returns all records found in the experiments table.
     *
     * @return all experiments records in the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static List<Experiment> getAllExperiments()
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectFromDbTable("experiments", Experiment.class, null, null, null,
                null, null);
    }

    /**
     * Returns all records found in the directories table.
     *
     * @return all directories records in the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static List<Directory> getAllDirectories()
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectFromDbTable("directories", Directory.class, null, null, null);
    }

    /**
     * Returns all records found in the wellsDescription table.
     *
     * @return all experiments records in the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static List<WellDescription> getAllWellsDescription()
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectFromDbTable("wellsDescription", WellDescription.class, null,
                null, null, null, null, null, null);
    }

    /**
     * Returns all records found in the images table.
     *
     * @return all images information records in the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static List<Image> getAllImages()
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Image> images = selectFromDbTable(
                "images", Image.class, null, null, null);
        Collections.sort(images);
        return images;
    }

    /**
     * Returns all records found in the coordinates table.
     *
     * @return all coordinates information records in the database
     * @throws ClassNotFoundException if the JDBC is missing
     * @throws SQLException if establishing the connection failed or the query
     * failed for any other reason.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static List<Coordinates> getAllCoordinates()
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectFromDbTable("coordinates", Coordinates.class, null, null, null,
                null);
    }

    /**
     * Get all the directories of a specific experiment.
     *
     * @param experimentId id of the experiment under consideration
     * @return a list of directories
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static List<Directory> getDirectoriesOfExperiment(int experimentId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectFromDbTable("directories", Directory.class, null, null,
                experimentId);
    }

    /**
     * Get all the images of a specific directory.
     *
     * @param directoryId id of the directory under consideration
     * @return a list of images
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static List<Image> getImagesOfDirectory(int directoryId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Image> images = selectFromDbTable(
                "images", Image.class, null, null, directoryId);
        Collections.sort(images);
        return images;
    }

    /**
     * Get all the directories of a specific experiment given its name.
     *
     * @param expName The name of the experiment whose directories are to be
     * retrieved.
     * @return a list of Directory objects
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static List<Directory> getDirectoriesOfExperimentByName(
            String expName) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        // The following line should return only one experiments since
        // experiment name is unique.
        List<Experiment> experiments = selectFromDbTable("experiments", Experiment.class, null, expName, null, null,
                null);
        // Return all the directories of this experiment
        return selectFromDbTable("directories", Directory.class, null, null,
                experiments.get(0).getId());
    }

    /**
     * Get a specific experiment given its name.
     *
     * @param expName The name of the experiment to be retrieved.
     * @return the experiment whose name is expName
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Experiment getExperimentByName(
            String expName) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        // The following line should return only one experiments (or none) since
        // experiment name is unique.
        List<Experiment> experiments = selectFromDbTable(
                "experiments",
                Experiment.class,
                null, expName, null, null, null);
        if (experiments.isEmpty()) {
            return null;
        } else {
            return experiments.get(0);
        }
    }

    /**
     * Get a specific experiment given its ID.
     *
     * @param expId The ID of the experiment to be retrieved.
     * @return the experiment whose name is expId
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Experiment getExperimentById(
            Integer expId) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        // The following line should return only one experiments (or none) since
        // experiment name is unique.
        List<Experiment> experiments = selectFromDbTable(
                "experiments",
                Experiment.class,
                expId, null, null, null, null);
        if (experiments.isEmpty()) {
            return null;
        } else {
            return experiments.get(0);
        }
    }

    /**
     * Add a new experiment record to the database.
     *
     * @param experiment the object representing the experiment to be added
     * @return number of rows inserted (typically 1)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int addExperiment(Experiment experiment) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return insertIntoDbTable("experiments",
                experiment.getId(),
                experiment.getName(),
                experiment.getRowCount(),
                experiment.getColCount(),
                experiment.getPlateName());
    }

    /**
     * Deletes one or more experiment records from the database. All the
     * experiment records sharing the same values as the object passed as an
     * argument will be deleted.
     *
     * @param experiment an object representing the experiment(s) to be deleted
     * @return the number of deleted experiments (records)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteExperiment(Experiment experiment) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return deleteFromDbTable("experiments",
                experiment.getId(),
                experiment.getName(),
                experiment.getRowCount(),
                experiment.getColCount(),
                experiment.getPlateName());
    }

    /**
     * Update one or more experiment record(s) in the database
     *
     * @param originalExperiment an object representing the experiment(s) to be
     * updated
     * @param updatedExperiment an object representing the new values that
     * should be used to update the designated records.
     * @return the number of records updated
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int updateExperiment(
            Experiment originalExperiment,
            Experiment updatedExperiment) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return updateDbTable("experiments",
                originalExperiment.getId(),
                originalExperiment.getName(),
                originalExperiment.getRowCount(),
                originalExperiment.getColCount(),
                originalExperiment.getPlateName(),
                updatedExperiment.getId(),
                updatedExperiment.getName(),
                updatedExperiment.getRowCount(),
                updatedExperiment.getColCount(),
                updatedExperiment.getPlateName());
    }

    /**
     * Add a new directory record to the database.
     *
     * @param directory the object representing the directory to be added
     * @return number of rows inserted (typically 1)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int addDirectory(Directory directory) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return insertIntoDbTable("directories",
                directory.getId(),
                directory.getPath(),
                directory.getExperimentId());
    }

    /**
     * Deletes one or more directory records from the database. All the
     * directory records sharing the same values as the object passed as an
     * argument will be deleted.
     *
     * @param directory an object representing the director(y/ies) to be deleted
     * @return the number of deleted directories (records)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteDirectory(Directory directory) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return deleteFromDbTable("directories",
                directory.getId(),
                directory.getPath(),
                directory.getExperimentId());
    }

    /**
     * Update one or more directory record(s) in the database
     *
     * @param originalDirectory an object representing the director(y/ies) to be
     * updated
     * @param updatedDirectory an object representing the new values that should
     * be used to update the designated records.
     * @return the number of records updated
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int updateDirectory(
            Directory originalDirectory,
            Directory updatedDirectory) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return updateDbTable("directories",
                originalDirectory.getId(),
                originalDirectory.getPath(),
                originalDirectory.getExperimentId(),
                updatedDirectory.getId(),
                updatedDirectory.getPath(),
                updatedDirectory.getExperimentId());
    }

    /**
     * Add a new wellDescription record to the database.
     *
     * @param wellDescription the object representing the wellDescription to be
     * added
     * @return number of rows inserted (typically 1)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int addWellDescription(WellDescription wellDescription) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return insertIntoDbTable("wellsDescription",
                wellDescription.getPlateId(),
                wellDescription.getRow(),
                wellDescription.getCol(),
                wellDescription.getCompound(),
                wellDescription.getConcentration(),
                wellDescription.getCellType(),
                wellDescription.getCellCount());
    }

    /**
     * Deletes one or more wellD description records from the database. All the
     * well description records sharing the same values as the object passed as
     * an argument will be deleted.
     *
     * @param wellDescription an object representing the well description(s) to
     * be deleted
     * @return the number of deleted directories (records)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteWellDescription(WellDescription wellDescription) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return deleteFromDbTable("wellsDescription",
                wellDescription.getPlateId(),
                wellDescription.getRow(),
                wellDescription.getCol(),
                wellDescription.getCompound(),
                wellDescription.getConcentration(),
                wellDescription.getCellType(),
                wellDescription.getCellCount());
    }

    /**
     * Update one or more well description record(s) in the database
     *
     * @param originalWellDescription an object representing the well
     * description(s) to be updated
     * @param updatedWellDescription an object representing the new values that
     * should be used to update the designated records.
     * @return the number of records updated
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int updateWellDescription(
            WellDescription originalWellDescription,
            WellDescription updatedWellDescription) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return updateDbTable("wellsDescription",
                originalWellDescription.getPlateId(),
                originalWellDescription.getRow(),
                originalWellDescription.getCol(),
                originalWellDescription.getCompound(),
                originalWellDescription.getConcentration(),
                originalWellDescription.getCellType(),
                originalWellDescription.getCellCount(),
                updatedWellDescription.getPlateId(),
                updatedWellDescription.getRow(),
                updatedWellDescription.getCol(),
                updatedWellDescription.getCompound(),
                updatedWellDescription.getConcentration(),
                updatedWellDescription.getCellType(),
                updatedWellDescription.getCellCount());
    }

    /**
     * Add a new image record to the database.
     *
     * @param image the object representing the image to be added
     * @return number of rows inserted (typically 1)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int addImage(Image image) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return insertIntoDbTable("images",
                image.getId(),
                image.getPath(),
                image.getParentDirId());
    }

    /**
     * Deletes one or more image records from the database. All the image
     * records sharing the same values as the object passed as an argument will
     * be deleted.
     *
     * @param image an object representing the image(s) to be deleted
     * @return the number of deleted images (records)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteImage(Image image) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return deleteFromDbTable("images",
                image.getId(),
                image.getPath(),
                image.getParentDirId());
    }

    /**
     * Update one or more image record(s) in the database
     *
     * @param originalImage an object representing the image(s) to be updated
     * @param updatedImage an object representing the new values that should be
     * used to update the designated records.
     * @return the number of records updated
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int updateImage(
            Image originalImage,
            Image updatedImage) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return updateDbTable("images",
                originalImage.getId(),
                originalImage.getPath(),
                originalImage.getParentDirId(),
                updatedImage.getId(),
                updatedImage.getPath(),
                updatedImage.getParentDirId());
    }

    /**
     * Add a new coordinates record to the database.
     *
     * @param coordinates the object representing the coordinates to be added
     * @return number of rows inserted (typically 1)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int addCoordinates(Coordinates coordinates) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return insertIntoDbTable("coordinates",
                coordinates.getId(),
                coordinates.getX(),
                coordinates.getY(),
                coordinates.getImageId());
    }

    /**
     * Deletes one or more coordinates records from the database. All the
     * coordinates records sharing the same values as the object passed as an
     * argument will be deleted.
     *
     * @param coordinates an object representing the coordinates record(s) to be
     * deleted
     * @return the number of deleted coordinates (records)
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int deleteCoordinates(Coordinates coordinates) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return deleteFromDbTable("coordinates",
                coordinates.getId(),
                coordinates.getX(),
                coordinates.getY(),
                coordinates.getImageId());
    }

    /**
     * Update one or more coordinates record(s) in the database
     *
     * @param originalCoordinates an object representing the coordinates
     * record(s) to be updated
     * @param updatedCoordinates an object representing the new values that
     * should be used to update the designated records.
     * @return the number of records updated
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static int updateCoordinates(
            Coordinates originalCoordinates,
            Coordinates updatedCoordinates) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return updateDbTable("coordinates",
                originalCoordinates.getId(),
                originalCoordinates.getX(),
                originalCoordinates.getY(),
                originalCoordinates.getImageId(),
                updatedCoordinates.getId(),
                updatedCoordinates.getX(),
                updatedCoordinates.getY(),
                updatedCoordinates.getImageId());
    }

    /**
     * Retrieve a well description given the experiment ID, row and column.
     *
     * @param experimentId the experiment corresponding to the sought well
     * @param row the row of the sought well in the experiment
     * @param col the column of the sought well in the experiment
     * @return the designated well
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static WellDescription getWell(int experimentId, int row, int col) throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<WellDescription> wells = DbUtilities.selectFromDbTable(
                "wellsDescription",
                WellDescription.class,
                experimentId, row, col,
                null, null, null, null);
        if (wells.isEmpty()) {
            return null;
        } else {
            return wells.get(0);
        }
    }

    /**
     * Retrieve all the wells of a specific experiment
     *
     * @param experimentId the ID of the designated experiment
     * @return all the wells of the designated experiment
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static List<WellDescription> getWellsOfExperiment(int experimentId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return DbUtilities.selectFromDbTable(
                "wellsDescription",
                WellDescription.class,
                experimentId, null, null,
                null, null, null, null);
    }

    /**
     * Gets the maximum value of the primary key found in the database. Notice
     * that this method should only be available for tables whose primary key
     * consists of only one column.
     *
     * @return maximum value of the primary key column in the database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Integer getMaxExperimentId()
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectMaxColValue(
                "experiments",
                "id",
                null, null, null, null, null);
    }

    /**
     * Gets the maximum value of the primary key found in the database. Notice
     * that this method should only be available for tables whose primary key
     * consists of only one column.
     *
     * @return maximum value of the primary key column in the database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Integer getMaxDirectoryId()
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectMaxColValue(
                "directories",
                "id",
                null, null, null);
    }

    /**
     * Gets the maximum value of the primary key found in the database. Notice
     * that this method should only be available for tables whose primary key
     * consists of only one column.
     *
     * @return maximum value of the primary key column in the database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Integer getMaxImageId()
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectMaxColValue(
                "images",
                "id",
                null, null, null);
    }

    /**
     * Gets the maximum value of the primary key found in the database. Notice
     * that this method should only be available for tables whose primary key
     * consists of only one column.
     *
     * @return maximum value of the primary key column in the database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Integer getMaxCoordinatesId()
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return selectMaxColValue(
                "coordinates",
                "id",
                null, null, null, null);
    }

    /**
     * Gets all the images of a specific experiment (all directories)
     *
     * @param experimentId the ID of the experiment whose images are sought
     * @return a list of Image objects
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static List<Image> getImagesOfExperiment(Integer experimentId)
            throws
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Image> imageList = new ArrayList<>();
        List<Directory> directories = getDirectoriesOfExperiment(experimentId);
        for (Directory directory : directories) {
            List<Image> images
                    = DbUtilities.getImagesOfDirectory(directory.getId());
            imageList.addAll(images);
        }
        // We did not sort the images here as we thought it would be better to
        // have the images of each directory in one uninterrupted block.
        // However, each block is internally sorted during the call to
        // getImagesOfDirectory(...)
        return imageList;
    }

    /**
     * Create a dummy database for testing purposes.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static void createDummyDb() throws SQLException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // Add experiments
        List<Experiment> experimentList = new ArrayList<>();
        experimentList.add(new Experiment(1, "Experiment 1", 2, 3, "Small Plate"));
        experimentList.add(new Experiment(2, "Experiment 2", 20, 50, "Medium Plate"));
        experimentList.add(new Experiment(3, "Experiment 3", 100, 200, "Large Plate"));
        // Add wells
        List<WellDescription> wellList = new ArrayList<>();
        // Wells of experiment 1
        wellList.add(new WellDescription(experimentList.get(0).getId(), 1, 1, "Compound 1", 0.003, "Cell Type 1", 30000));
        wellList.add(new WellDescription(experimentList.get(0).getId(), 1, 2, "Compound 2", 0.05, "Cell Type 2", 40000));
        wellList.add(new WellDescription(experimentList.get(0).getId(), 1, 3, "Compound 3", 0.1, "Cell Type 3", 50000));
        wellList.add(new WellDescription(experimentList.get(0).getId(), 2, 1, "Compound 1", 0.003, "Cell Type 1", 30000));
        wellList.add(new WellDescription(experimentList.get(0).getId(), 2, 2, "Compound 2", 0.05, "Cell Type 2", 40000));
        wellList.add(new WellDescription(experimentList.get(0).getId(), 2, 3, "Compound 3", 0.1, "Cell Type 3", 50000));
        // Wells of experiment 2
        wellList.add(new WellDescription(experimentList.get(1).getId(), 1, 1, "Compound 4", 0.09, "Cell Type 4", 50000));
        wellList.add(new WellDescription(experimentList.get(1).getId(), 2, 1, "Compound 4", 0.09, "Cell Type 4", 50000));
        wellList.add(new WellDescription(experimentList.get(1).getId(), 3, 1, "Compound 4", 0.09, "Cell Type 4", 50000));
        wellList.add(new WellDescription(experimentList.get(1).getId(), 4, 1, "Compound 4", 0.09, "Cell Type 4", 50000));
        wellList.add(new WellDescription(experimentList.get(1).getId(), 5, 1, "Compound 4", 0.09, "Cell Type 4", 50000));
        // Add directories
        List<Directory> directoryList = new ArrayList<>();
        // Directories of experiment 1
        directoryList.add(new Directory(1, "f:/my photos/3d/", experimentList.get(0).getId()));
        directoryList.add(new Directory(2, "f:/my photos/3d/", experimentList.get(0).getId()));
        directoryList.add(new Directory(3, "d:/", experimentList.get(0).getId()));
        // Directories of experiment 3
        directoryList.add(new Directory(4, "e:/temp/images/", experimentList.get(2).getId()));
        directoryList.add(new Directory(5, "e:/12 Nov 2017/my data/post-analysis/", experimentList.get(2).getId()));
        // Add images
        List<Image> imageList = new ArrayList<>();
        // Images of directory 2
        imageList.add(new Image(1, "F:/2D cell cultures/image1.png", directoryList.get(1).getId()));
        imageList.add(new Image(2, "F:/2D cell cultures/image2.png", directoryList.get(1).getId()));
        imageList.add(new Image(3, "F:/2D cell cultures/image3.png", directoryList.get(1).getId()));
        imageList.add(new Image(4, "F:/2D cell cultures/image4.png", directoryList.get(1).getId()));
        imageList.add(new Image(5, "F:/2D cell cultures/image5.png", directoryList.get(1).getId()));
        // Images of directory 5
        imageList.add(new Image(6, "F:/3D/scan1.png", directoryList.get(4).getId()));
        imageList.add(new Image(7, "F:/3D/scan2.png", directoryList.get(4).getId()));
        // Add coordinates
        List<Coordinates> coordinatesList = new ArrayList<>();
        // Coordinates of image 1
        coordinatesList.add(new Coordinates(1, 100, 183, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(2, 618, 884, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(3, 48, 1025, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(4, 178, 555, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(5, 904, 158, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(6, 125, 983, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(7, 759, 147, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(8, 632, 936, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(9, 1024, 57, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(10, 145, 454, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(11, 967, 111, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(12, 549, 271, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(13, 785, 866, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(14, 147, 549, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(15, 987, 601, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(16, 852, 100, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(17, 123, 200, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(18, 321, 900, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(19, 323, 1000, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(20, 672, 1044, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(21, 579, 788, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(22, 199, 439, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(23, 989, 299, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(24, 20, 342, imageList.get(0).getId()));
        coordinatesList.add(new Coordinates(25, 125, 780, imageList.get(0).getId()));
        // Coordinates of image 2
        coordinatesList.add(new Coordinates(26, 100, 183, imageList.get(1).getId()));
        coordinatesList.add(new Coordinates(27, 618, 884, imageList.get(1).getId()));
        coordinatesList.add(new Coordinates(28, 48, 1025, imageList.get(1).getId()));
        coordinatesList.add(new Coordinates(29, 178, 555, imageList.get(1).getId()));
        // Coordinates of image 3
        coordinatesList.add(new Coordinates(30, 100, 183, imageList.get(2).getId()));
        coordinatesList.add(new Coordinates(31, 618, 884, imageList.get(2).getId()));
        coordinatesList.add(new Coordinates(32, 48, 1025, imageList.get(2).getId()));
        coordinatesList.add(new Coordinates(33, 178, 555, imageList.get(2).getId()));
        // Coordinates of image 7
        coordinatesList.add(new Coordinates(34, 100, 183, imageList.get(6).getId()));
        coordinatesList.add(new Coordinates(35, 618, 884, imageList.get(6).getId()));
        // Drop all DB contents
        deleteDb();
        // Create the DB structure
        createDb();
        // Add all data to the DB
        for (Experiment experiment : experimentList) {
            DbUtilities.addExperiment(experiment);
        }
        for (WellDescription wellDescription : wellList) {
            DbUtilities.addWellDescription(wellDescription);
        }
        for (Directory directory : directoryList) {
            DbUtilities.addDirectory(directory);
        }
        for (Image image : imageList) {
            DbUtilities.addImage(image);
        }
        for (Coordinates coordinates : coordinatesList) {
            DbUtilities.addCoordinates(coordinates);
        }
    }

    /**
     * Gets the directory corresponding to a specific ID
     *
     * @param id ID of the directory to be retrieved
     * @return a Directory object
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Directory getDirectorById(Integer id)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return DbUtilities.selectFromDbTable(
                "directories", Directory.class, id, null, null).get(0);
    }

    public static List<Coordinates> getCoordinatesOfImage(Integer id)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return DbUtilities.selectFromDbTable("coordinates",
                Coordinates.class, null, null, null, id);
    }

    /**
     * Export all the contents of the database to selectedFile
     *
     * @param selectedFile the file to which the database should be exported
     * @throws FileNotFoundException
     */
    public static void exportDb(File selectedFile)
            throws
            FileNotFoundException,
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Experiment> allExperiments
                = DbUtilities.getAllExperiments();
        List<WellDescription> allWells
                = DbUtilities.getAllWellsDescription();
        List<Directory> allDirectories
                = DbUtilities.getAllDirectories();
        List<Image> allImages
                = DbUtilities.getAllImages();
        List<Coordinates> allCoordinates
                = DbUtilities.getAllCoordinates();
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(selectedFile);
            printer.println("TABLE$$experiments");
            for (Experiment exp : allExperiments) {
                printer.println("ROW$$" + exp.to$Separated());
            }
            printer.println("TABLE$$wellsDescription");
            for (WellDescription well : allWells) {
                printer.println("ROW$$" + well.to$Separated());
            }
            printer.println("TABLE$$directories");
            for (Directory dir : allDirectories) {
                printer.println("ROW$$" + dir.to$Separated());
            }
            printer.println("TABLE$$images");
            for (Image image : allImages) {
                printer.println("ROW$$" + image.to$Separated());
            }
            printer.println("TABLE$$coordinates");
            for (Coordinates coordinate : allCoordinates) {
                printer.println("ROW$$" + coordinate.to$Separated());
            }
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }

    /**
     * Import all the data in selectedFile and add it to the database. Notice
     * that adding new data to the database requires updating the original
     * primary key values in a way that does not conflict with the target
     * database, which in turn requires updating the foreign key associations of
     * these updated primary keys.
     *
     * @param selectedFile the file from which the data will be imported
     * @throws IOException
     */
    public static void importDb(File selectedFile)
            throws IOException,
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Experiment> allExperiments
                = new ArrayList<>();
        List<WellDescription> allWells
                = new ArrayList<>();
        List<Directory> allDirectories
                = new ArrayList<>();
        List<Image> allImages
                = new ArrayList<>();
        List<Coordinates> allCoordinates
                = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(selectedFile));
            String lastTableEncountered = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.split("\\$\\$");
                switch (splits[0].toLowerCase().trim()) {
                    case "table":
                        // Strore the last table encountered
                        lastTableEncountered = splits[1].trim();
                        break;
                    case "row":
                        switch (lastTableEncountered) {
                            case "experiments":
                                allExperiments.add(
                                        Experiment.string2Object(
                                                splits[1].trim()));
                                break;
                            case "wellsDescription":
                                allWells.add(
                                        WellDescription.string2Object(
                                                splits[1].trim()));
                                break;
                            case "directories":
                                allDirectories.add(
                                        Directory.string2Object(
                                                splits[1].trim()));
                                break;
                            case "images":
                                allImages.add(
                                        Image.string2Object(
                                                splits[1].trim()));
                                break;
                            case "coordinates":
                                allCoordinates.add(
                                        Coordinates.string2Object(
                                                splits[1].trim()));
                                break;
                            default:
                                throw new UnsupportedOperationException(
                                        String.format("Unrecognized table"
                                                + "name in the databse "
                                                + "file (%s)",
                                                lastTableEncountered));
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException(
                                String.format("Unrecognized token in the "
                                        + "databse file (%s)",
                                        splits[0]));
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        appendDatabase(
                allExperiments,
                allWells,
                allDirectories,
                allImages,
                allCoordinates);

    }

    public static void clearCoordinatesOfImage(Integer id)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        DbUtilities.deleteFromDbTable("coordinates", null, null, null, id);
    }

    public static int getLumenCount(Integer expId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        int count = 0;
        List<Directory> dirs = DbUtilities.getDirectoriesOfExperiment(expId);
        for (Directory dir : dirs) {
            List<Image> images = DbUtilities.getImagesOfDirectory(dir.getId());
            for (Image img : images) {
                count += DbUtilities.getCoordinatesOfImage(img.getId()).size();
            }
        }
        return count;
    }

    public static Image getImageById(Integer id)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        return DbUtilities.selectFromDbTable(
                "images", Image.class, id, null, null).get(0);
    }

    /**
     * This is a utility function used by importDb() to fix primary keys along
     * with their foreign key association and append the new data to the
     * database.
     *
     * @param experiments
     * @param wells
     * @param directories
     * @param images
     * @param coordinates
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static void appendDatabase(
            List<Experiment> experiments,
            List<WellDescription> wells,
            List<Directory> directories,
            List<Image> images,
            List<Coordinates> coordinates)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        // Fix experiments ID values, then add the fixed experiments to the 
        // database
        // Get the maximum experiment ID found in the database
        Integer maxExperimentId = getMaxExperimentId();
        for (Experiment experiment : experiments) {
            int oldId = experiment.getId();
            int newId = ++maxExperimentId;
            // Update the primary key to avoid conflicting with those already
            // existing in the target database.
            experiment.setId(newId);
            // If an experiment with the same name exists, change the name of
            // the new experiment
            Experiment experimentWithTheSameName;
            while (true) {
                experimentWithTheSameName
                        = DbUtilities.getExperimentByName(experiment.getName());
                if (experimentWithTheSameName == null) {
                    break;
                } else {
                    experiment.setName(experiment.getName() + "(copy)");
                }
            }
            DbUtilities.addExperiment(experiment);
            // Update the old experiment ID in the directories list
            for (Directory directory : directories) {
                if (directory.getExperimentId() == oldId) {
                    directory.setExperimentId(newId);
                }
            }
            // Update the old experiment ID in the wells list
            for (WellDescription well : wells) {
                if (well.getPlateId() == oldId) {
                    well.setPlateId(newId);
                }
            }
        }
        // Fix directories ID values, then add the fixed directories to the 
        // database
        // Get the maximum directory ID found in the database
        Integer maxDirectoryId = getMaxDirectoryId();
        for (Directory directory : directories) {
            int oldId = directory.getId();
            int newId = ++maxDirectoryId;
            // Update the primary key to avoid conflicting with those already
            // existing in the target database.
            directory.setId(newId);
            DbUtilities.addDirectory(directory);
            // Update the old directory ID in the images list
            for (Image image : images) {
                if (image.getParentDirId() == oldId) {
                    image.setParentDirId(newId);
                }
            }
        }
        // Fix images ID values, then add the fixed images to the database
        Integer maxImageId = getMaxImageId();
        for (Image image : images) {
            int oldId = image.getId();
            int newId = ++maxImageId;
            // Update the primary key to avoid conflicting with those already
            // existing in the target database.
            image.setId(newId);
            DbUtilities.addImage(image);
            // Update the old image ID in the coordinates list
            for (Coordinates coordinate : coordinates) {
                if (coordinate.getImageId() == oldId) {
                    coordinate.setImageId(newId);
                }
            }
        }
        // Fix coordinates ID avlues, then add the fixed coordinates to the
        // database
        Integer maxCoordinatesId = getMaxCoordinatesId();
        for (Coordinates coordinatePair : coordinates) {
            coordinatePair.setId(++maxCoordinatesId);
            DbUtilities.addCoordinates(coordinatePair);
        }
        // Add wells directly, since they do not have a separate ID and their
        // plateId (which is their experiment ID) has already been fixed
        for (WellDescription well : wells) {
            DbUtilities.addWellDescription(well);
        }
    }

    /**
     * Gets the common path part of the paths of all directories and images of
     * some experiments.
     *
     * @param expIds the IDs of the experiments
     * @return the common path section
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static String getExperimentCommonPath(List<Integer> expIds)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Directory> dirs = new ArrayList<>();
        for (int i = 0; i < expIds.size(); i++) {
            dirs.addAll(DbUtilities.getDirectoriesOfExperiment(expIds.get(i)));
        }
        String commonPath = "";
        if (dirs.size() > 0) {
            commonPath = dirs.get(0).getPath();
            for (int i = 1; i < dirs.size(); i++) {
                commonPath = getCommonPath(commonPath, dirs.get(i).getPath());
            }
        }
        return commonPath;
    }

    /**
     * returns the common part of two paths.
     *
     * @param path1 first path
     * @param path2 second path
     * @return common part of the two paths
     */
    public static String getCommonPath(String path1, String path2) {
        int shorterPathLength
                = path1.length() < path2.length()
                ? path1.length() : path2.length();
        StringBuilder commonPathBuilder = new StringBuilder();
        for (int i = 0; i < shorterPathLength; i++) {
            if (path1.charAt(i) == path2.charAt(i)) {
                commonPathBuilder.append(path1.charAt(i));
            } else {
                break;
            }
        }
        return commonPathBuilder.toString();
    }

    /**
     * Copy lumen coordinates of the combined images of an experiment to their
     * corresponding channels.
     *
     * @param expId ID of the experiment
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws IOException
     */
    public static void copyCoordinatesToChannels(
            int expId,
            boolean terminateIfChannelsHaveLumens)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            IOException {
        List<Image> images = DbUtilities.getImagesOfExperiment(expId);
        Pattern pattern = Pattern.compile("r\\d+c\\d+f\\d+p\\d+");
        for (Image image : images) {
            // Make sure that this image is a combined image
            if (image.getPath().toLowerCase().contains("combined")) {
                // Load the coordinates of the image
                List<Coordinates> coordinatesList = DbUtilities.getCoordinatesOfImage(image.getId());
                // Extract the row-column-field-plane text part of the image name
                File combinedImageFile = new File(image.getPath());
                Matcher matcher = pattern.matcher(combinedImageFile.getName());
                if (matcher.find()) {
                    String imageRowColFieldPlane = matcher.group();
                    // Loop over all other images
                    for (Image otherImage : images) {
                        // Avoid combined images (we are looking for channel images only)
                        File otherImageFile = new File(otherImage.getPath());
                        if (!otherImageFile.getName().toLowerCase().contains("combined")) {
                            // Extract the row-column-field-plane text part of the other image name
                            Matcher otherMatcher = pattern.matcher(otherImageFile.getName());
                            if (otherMatcher.find()) {
                                // If the row-column-field-plane text matches,
                                // copy all the cooerdinates of the combined
                                // image to this channel image
                                String otherImageRowColFieldPlane = otherMatcher.group();
                                if (imageRowColFieldPlane.equals(otherImageRowColFieldPlane)) {
                                    // If the other image has any coordinates attached
                                    // to it, throw an exception
                                    List<Coordinates> otherImageCoordinates
                                            = DbUtilities.getCoordinatesOfImage(
                                                    otherImage.getId());
                                    if (otherImageCoordinates.isEmpty()) {
                                        for (Coordinates coordinates : coordinatesList) {
                                            Coordinates coordinatesCopy
                                                    = new Coordinates(
                                                            null,
                                                            coordinates.getX(),
                                                            coordinates.getY(),
                                                            otherImage.getId());
                                            DbUtilities.addCoordinates(
                                                    coordinatesCopy);
                                        }
                                    } else {
                                        if (terminateIfChannelsHaveLumens) {
                                            throw new IllegalArgumentException(
                                                    String.format(
                                                            "Channel image %s "
                                                            + "already has lumen "
                                                            + "coordinates "
                                                            + "attached to it.",
                                                            otherImageFile.getName()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Displays all lumen coordinates associated with channel images (not
     * combined images).
     *
     * @param expId experiment ID
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws IOException
     */
    public static void listChannelLumens(int expId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            IOException {
        List<Image> images = DbUtilities.getImagesOfExperiment(expId);
        Collections.sort(images);
        int count = 0;
        for (Image image : images) {
            File imageFile = new File(image.getPath());
            if (!imageFile.getName().toLowerCase().contains("combined")) {
                List<Coordinates> coordinatesList
                        = DbUtilities.getCoordinatesOfImage(image.getId());
                if (!coordinatesList.isEmpty()) {
                    count++;
                    System.out.println(image.toString());
                    for (Coordinates coordinates : coordinatesList) {
                        System.out.println(coordinates.toString());
                    }
                }
            }
        }
        System.out.println(count + " channel images have lumen coordinates "
                + "attached to them.");
    }

    /**
     * Get the original TIFF file of argument image, given that the tag is part
     * of the retrieved file name. Notice that keeping the conventions is a must
     * for this method to work. Specifically speaking, (1) both the image and
     * its original counterpart must include the same row-column-field-plane
     * text section in their names e.g. r08c11f03p21. Additionally, (2) the path
     * of the image and its original counter part must be identical except for
     * the following differences: (i) the names of the two files may differ
     * without the violation of condition 1, (ii) the path of the image must
     * contain a directory named "polished", (iii) the path of the original
     * image must contain a directory named "selected" and (iv) both "polished"
     * and "selected" must be exactly in the same order in the hierarchy of
     * parent directories of their respective images.
     *
     * @param image a processed image for which the original TIFF image is
     * sought
     * @param tag a text that must be part of the retrieved file name
     * @return a list of files satisfying the aforementioned conditions
     */
    public static List<File> getOriginalTiffFiles(Image image, String tag) {
        File imageFile = new File(image.getPath());
        // Get the row-column-field-plane text part of the image name
        Pattern pattern = Pattern.compile("r\\d+c\\d+f\\d+p\\d+");
        Matcher matcher = pattern.matcher(imageFile.getName());
        if (matcher.find()) {
            String imageRowColFieldPlane = matcher.group();
            // Move one step up the parent directories tree
            imageFile = imageFile.getParentFile();
            // Push all the parent directories one by one into a stack until
            // the parent directory "polished" is reached
            Stack<File> parentDirsStack = new Stack<>();
            while (!imageFile.getName()
                    .equalsIgnoreCase("polished")) {
                parentDirsStack.push(imageFile);
                imageFile = imageFile.getParentFile();
                if (imageFile == null) {
                    throw new IllegalArgumentException("The path of the image "
                            + "passed as an argument does not follow the "
                            + "convention: The path must contain a directory "
                            + "with the name \"polished\".");
                }
            }
            // Reaching this point means that the current directory is the one
            // called "polished" and all subsequent directories are stored in
            // the stack awaiting retrieval
            // Replace polished with "selected"
            imageFile = imageFile.getParentFile();
            imageFile = new File(
                    imageFile.getPath()
                    + File.separator
                    + "selected");
            // Notice that the path of the argument image and the original TIFF
            // image must be exactly the same except for the polished/selected
            // part and the names of the files.
            // Pop all the directories in the stack and append them to the path.
            while (!parentDirsStack.empty()) {
                imageFile = new File(
                        imageFile.getPath()
                        + File.separator
                        + parentDirsStack.pop().getName());
            }
            // Reaching this point means that we have re-built the whole path of
            // the parent directory of the TIFF image. There are two approaches
            // to proceed from this point. The first is to form the name of the
            // original file given the row-column-filed-plane text that we have
            // from the argument image and whatever additions harmony adds to
            // the name. Although this approach is faster, it is risky, since we
            // do not know if/how Harmony can change those additions. Thus, we
            // use the second approach, which is simply listing all the files
            // in the directory and find the one having the same
            // row-column-field-plane text and the additional tag text,
            // regardless of the rest of the name. This option is slower but
            // safer.
            List<File> originalFilesMatched = new ArrayList<>();
            File[] originalImageFiles = imageFile.listFiles();
            for (File originalImageFile : originalImageFiles) {
                if (originalImageFile.getName()
                        .contains(imageRowColFieldPlane)) {
                    if (tag == null
                            || originalImageFile.getName().contains(tag)) {
                        originalFilesMatched.add(originalImageFile);
                    }
                }
            }
            return originalFilesMatched;
        } else {
            throw new IllegalArgumentException("The name of the image passed "
                    + "as an argument does not follow the convention: An image "
                    + "must have row, column, field and plane information in "
                    + "the following form r#c#f#p# e.g. r02c03f01p38");
        }
    }

    /**
     * Displays a list of all original TIFF image files corresponding to the
     * images of the argument experiment
     * @param expId experiment ID
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws IOException 
     */
    public static void listOriginalTiffImages(int expId)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            IOException {
        List<Image> images = DbUtilities.getImagesOfExperiment(expId);
        Collections.sort(images);
        for (Image image : images) {
            List<File> ch1TiffFiles = getOriginalTiffFiles(image, "ch1");
            List<File> ch2TiffFiles = getOriginalTiffFiles(image, "ch2");
            System.out.println(image.toString());
            for (File ch1TiffFile : ch1TiffFiles) {
                System.out.println("\tch1: " + ch1TiffFile.getPath());
            }
            for (File ch2TiffFile : ch2TiffFiles) {
                System.out.println("\tch2: " + ch2TiffFile.getPath());
            }
        }
    }

    /**
     * Imports a version 1 backup file into a version 2 database.
     *
     * @param version1DbFile version 1 backup file
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws IOException
     */
    public static void importVersion1Fromat(File version1DbFile)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            IOException {
        // Parse all images and coordinates
        BufferedReader reader = null;
        try {
            List<Image> imageList = new ArrayList<>();
            List<Coordinates> coordinatesList = new ArrayList<>();
            reader = new BufferedReader(new FileReader(version1DbFile));
            // The following regex is intended to identify the {...} data part
            Pattern imageRegex = Pattern.compile(
                    "\\{id=.*parentDirPath=.*path=.*\\}");
            Pattern coordinatesRegex = Pattern.compile(
                    "\\{id=.*x=.*y=.*imageID=.*\\}");
            String line;
            // Parse line by line
            while ((line = reader.readLine()) != null) {
                Matcher imageMatcher = imageRegex.matcher(line);
                if (imageMatcher.find()) {
                    String data = imageMatcher.group();
                    data = data.substring(1, data.length() - 1);
                    String[] splits = data.split(",");
                    // Read an image
                    Image image = new Image(
                            new Integer(splits[0].trim().split("=")[1]),
                            splits[2].trim().split("=")[1],
                            null);
                    // Image ID and parent dir ID will be updated later
                    imageList.add(image);
                } else {
                    Matcher coordinatesMatcher = coordinatesRegex.matcher(line);
                    if (coordinatesMatcher.find()) {
                        String data = coordinatesMatcher.group();
                        data = data.substring(1, data.length() - 1);
                        String[] splits = data.split(",");
                        // Read coordinates
                        Coordinates coordinates = new Coordinates(
                                new Integer(splits[0].trim().split("=")[1]),
                                new Integer(splits[1].trim().split("=")[1]),
                                new Integer(splits[2].trim().split("=")[1]),
                                new Integer(splits[3].trim().split("=")[1]));
                        // Coordinates ID and Image ID will be updated later
                        coordinatesList.add(coordinates);
                    } else {
                        throw new IllegalArgumentException(
                                "Invalid File Format: \"" + line + "\"");
                    }
                }
            }
            if (!imageList.isEmpty()) {
                // Once all the contents of the outdated database are read we
                // need to fix IDs while maintaining the primary key and foreign
                // key relationship.
                // Create a new list to hold fixed coordinates (this will
                // automatically group them by image as well, if they are not
                // already grouped).
                List<Coordinates> fixedCoordinatesList = new ArrayList<>();
                // Get the maximum image ID in the database
                Integer maxImageId = DbUtilities.getMaxImageId();
                for (Image image : imageList) {
                    // Increment the ID
                    maxImageId++;
                    // Fix the image ID filed (the foreign key) in all
                    // corresponding coordinates
                    for (int i = 0; i < coordinatesList.size();) {
                        Coordinates coordiantes = coordinatesList.get(i);
                        if (coordiantes.getImageId()
                                .equals(image.getId())) {
                            coordiantes.setImageId(maxImageId);
                            fixedCoordinatesList.add(coordinatesList.remove(i));
                        } else {
                            i++;
                        }
                    }
                    // Fix the ID of the image itself (the primary key)
                    image.setId(maxImageId);
                }
                // Get the maximum coordinates ID in the databse
                Integer maxCoordinatesId = DbUtilities.getMaxCoordinatesId();
                // Fix the ID of each coordinates pair (the primary key - notice
                // that there is no foreign key referencing this primary key, at
                // least until the time of writing this)
                for (Coordinates coordinates : fixedCoordinatesList) {
                    coordinates.setId(++maxCoordinatesId);
                }
                // In version 1, all the images were selected from one root
                // directory, maybe recursively i.e. the images need not be
                // direct children of this root directory. But, there must be
                // a shared parent directory for all of them. This parent
                // directory will either be this same root directory through
                // which they were collected, or one of its direct or indirect
                // subdirectories (if all images reside in this subdirectory
                // while all other subdirectories are empty).
                // Let's find the longest shared parent directory of all images
                String commonParentPath
                        = new File(imageList.get(0).getPath()).getParent();
                for (int i = 1; i < imageList.size(); i++) {
                    commonParentPath = DbUtilities.getCommonPath(
                            commonParentPath,
                            imageList.get(i).getPath());
                }
                // Create a new experiment to be added to the database later
                Experiment exp = new Experiment(
                        DbUtilities.getMaxExperimentId() + 1,
                        version1DbFile.getName(),
                        null,
                        null,
                        null);
                // Keep Changing the name until no existing experiments with the
                // same name are left
                if (DbUtilities.getExperimentByName(exp.getName()) != null) {
                    int counter = 1;
                    while (true) {
                        String newName = String.format(
                                "%s_%d",
                                exp.getName(),
                                counter);
                        if (DbUtilities.getExperimentByName(newName) == null) {
                            exp.setName(newName);
                            break;
                        }
                        counter++;
                    }
                }
                // Number of rows, number of columns and plate name will be
                // updated later
                // Create a new parent directory to be added later to the
                // database
                Directory parentDir = new Directory(
                        DbUtilities.getMaxDirectoryId() + 1,
                        commonParentPath,
                        exp.getId());
                // Update the parent dir ID of all images
                for (Image image : imageList) {
                    image.setParentDirId(parentDir.getId());
                }
                // Set the number of rows and columns of the experiment to the
                // maximum row and column indices across all images
                int maxRow = -1;
                int maxCol = -1;
                Pattern rowPattern = Pattern.compile("r\\d+");
                Pattern colPattern = Pattern.compile("c\\d+");
                for (Image image : imageList) {
                    String fileName = new File(image.getPath()).getName();
                    // Parse row index
                    Matcher rowMatcher = rowPattern.matcher(fileName);
                    if (rowMatcher.find()) {
                        int currentRow = Integer.parseInt(
                                rowMatcher.group().substring(1));
                        if (currentRow > maxRow) {
                            // Update the maximum row found so far
                            maxRow = currentRow;
                        }
                    }
                    // Parse column index
                    Matcher colMatcher = colPattern.matcher(fileName);
                    if (colMatcher.find()) {
                        int currentCol = Integer.parseInt(
                                colMatcher.group().substring(1));
                        if (currentCol > maxCol) {
                            // Update the maximum column found so far
                            maxCol = currentCol;
                        }
                    }
                }
                // If the maximum row or column are still equal to -1
                // this means that non of the images follow
                // the correct naming converntion. In such a case experiment
                // details (row count, column count and plate name) are not
                // updated (kept as nulls), otherwise they are updated according
                // to the corresponsing values
                if (maxRow != -1 && maxCol != -1) {
                    exp.setRowCount(maxRow);
                    exp.setColCount(maxCol);
                    exp.setPlateName(String.format(
                            "%s (%dx%d) Plate",
                            exp.getName(),
                            maxRow,
                            maxCol));
                }
                // Now everything is set, add everything to the database
                // Add the experiment
                DbUtilities.addExperiment(exp);
//                System.out.format(
//                        "%20s: %s%n",
//                        "Experiment Added",
//                        exp.toString());
                DbUtilities.addDirectory(parentDir);
//                System.out.format(
//                        "%20s: %s%n",
//                        "Directory Added",
//                        parentDir.toString());
                for (Image image : imageList) {
                    DbUtilities.addImage(image);
//                    System.out.format(
//                            "%20s: %s%n",
//                            "Image Added",
//                            image.toString());
                }
                for (Coordinates coordinates : fixedCoordinatesList) {
                    DbUtilities.addCoordinates(coordinates);
//                    System.out.format(
//                            "%20s: %s%n",
//                            "Coordinates Added",
//                            coordinates.toString());
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void main(String[] args)
            throws
            ClassNotFoundException,
            SQLException,
            FileNotFoundException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            InstantiationException,
            NoSuchMethodException,
            IOException {
        //  ********************************************************************
//        display(getAllExperiments());
//        Experiment exp = new Experiment();
//        exp.setName("Hello Experiment 1");
//        exp.setRowCount(100);
//        exp.setColCount(200);
//        exp.setPlateName("Plate(100x200)");
//        addExperiment(exp);
//        display(getAllExperiments());
//        Experiment updatedExperiment = new Experiment();
//        updatedExperiment.setId(exp.getId());
//        updatedExperiment.setName(exp.getName());
//        updatedExperiment.setRowCount(exp.getRowCount());
//        updatedExperiment.setColCount(exp.getColCount());
//        updatedExperiment.setPlateName("Plate(UNKNOWN)");
//        updateExperiment(exp, updatedExperiment);
//        display(getAllExperiments());
//        display(getAllDirectories());
//        Directory directory = new Directory();
//        directory.setPath("d:/happy");
//        directory.setExperimentId(17);
//        addDirectory(directory);
//        display(getAllDirectories());
//        Directory updatedDirectory = new Directory();
//        //updatedDirectory.setId(directory.getId());
//        updatedDirectory.setPath("Invallid Path!");
//        //updatedDirectory.setExperimentId(5);
//        updateDirectory(directory, updatedDirectory);
//        display(getAllDirectories());
//  ********************************************************************
//        List<Experiment> allExperiments = getAllExperiments();
//
//        display(getAllWellsDescription());
//        WellDescription wellDescription1 = new WellDescription();
//        wellDescription1.setPlateId(allExperiments.get(0).getId());
//        wellDescription1.setRow(1);
//        wellDescription1.setCol(1);
//        wellDescription1.setCompound("compound 1");
//        wellDescription1.setConcentration(0.005);
//        wellDescription1.setCellType("type 1");
//        wellDescription1.setCellCount(3000);
//        WellDescription wellDescription2 = new WellDescription();
//        wellDescription2.setPlateId(allExperiments.get(1).getId());
//        wellDescription2.setRow(1);
//        wellDescription2.setCol(2);
//        wellDescription2.setCompound("compound 2");
//        wellDescription2.setConcentration(0.003);
//        wellDescription2.setCellType("type 2");
//        wellDescription2.setCellCount(5000);
//        WellDescription wellDescription3 = new WellDescription();
//        wellDescription3.setPlateId(allExperiments.get(2).getId());
//        wellDescription3.setRow(7);
//        wellDescription3.setCol(7);
//        wellDescription3.setCompound("compound 1");
//        wellDescription3.setConcentration(0.001);
//        wellDescription3.setCellType("type 77");
//        wellDescription3.setCellCount(10000);
//
//        addWellDescription(wellDescription1);
//        addWellDescription(wellDescription2);
//        addWellDescription(wellDescription3);
//        
//        display(getAllWellsDescription());
//        
//        WellDescription wellsToBeUpdated = new WellDescription();
//        wellsToBeUpdated.setCompound("compound 1");
//        WellDescription updatingWell = new WellDescription();
//        updatingWell.setRow(20);
//        updatingWell.setCol(20);
//        updatingWell.setCompound("Updated Compound");
//        updatingWell.setConcentration(0.1);
//        updatingWell.setCellType("Updated Type");
//        updatingWell.setCellCount(555);
//        
//        updateWellDescription(wellsToBeUpdated, updatingWell);
//        
//        display(getAllWellsDescription());
//        
//        WellDescription wellToBeDeleted = new WellDescription();
//        wellToBeDeleted.setCompound("Updated Compound");
//        
//        deleteWellDescription(wellToBeDeleted);
//        
//        display(getAllWellsDescription());
//  ********************************************************************
//        display(getAllImages());
//        
//        Image image1 = new Image();
//        image1.setPath("c:");
//        image1.setParentDirId(1);
//        Image image2 = new Image();
//        image2.setPath("d:/oops");
//        Image image3 = new Image();
//        image3.setPath("d:/oops");
//        image3.setParentDirId(11);
//        
//        addImage(image1);
//        addImage(image2);
//        addImage(image3);
//        
//        display(getAllImages());
//        
//        Image imagesToBeUpdated = new Image();
//        imagesToBeUpdated.setPath("d:/oops");
//        Image updatingImage = new  Image();
//        updatingImage.setParentDirId(1);
//        
//        updateImage(imagesToBeUpdated, updatingImage);
//        
//        display(getAllImages());
//        
//        Image imagesToBeDeleted = new Image();
//        imagesToBeDeleted.setParentDirId(1);
//        
//        deleteImage(imagesToBeDeleted);
//        
//        display(getAllImages());
//  ********************************************************************
//        display(getAllCoordinates());
//
//        Coordinates coordinates1 = new Coordinates();
//        coordinates1.setX(20);
//        coordinates1.setY(20);
//        coordinates1.setImageId(12);
//        Coordinates coordinates2 = new Coordinates();
//        coordinates2.setX(21);
//        coordinates2.setY(21);
//        coordinates2.setImageId(12);
//        Coordinates coordinates3 = new Coordinates();
//        coordinates3.setX(22);
//        coordinates3.setY(22);
//        coordinates3.setImageId(12);
//        Coordinates coordinates4 = new Coordinates();
//        coordinates4.setX(23);
//        coordinates4.setY(23);
//        coordinates4.setImageId(14);
//
//        addCoordinates(coordinates1);
//        addCoordinates(coordinates2);
//        addCoordinates(coordinates3);
//        addCoordinates(coordinates4);
//        
//        display(getAllCoordinates());
//        
//        Coordinates coordinatesToBeUpdated = new Coordinates();
//        coordinatesToBeUpdated.setImageId(12);
//        Coordinates updatingCoordinates = new Coordinates();
//        updatingCoordinates.setImageId(13);
//        
//        updateCoordinates(coordinatesToBeUpdated, updatingCoordinates);
//        
//        display(getAllCoordinates());
//        
//        Coordinates coordinatesToBeDeleted = new Coordinates();
//        coordinatesToBeDeleted.setImageId(13);
//        
//        deleteCoordinates(coordinatesToBeDeleted);
//        
//        display(getAllCoordinates());
        //createDummyDb();
        //importVersion1Fromat(new File("E:\\Dropbox\\Harmony\\3D Harmony counts\\Lumen Data\\2018-04-24 (outdated format)\\2018-4-24 Lumens from stainless steel plate"));
        //copyCoordinatesToChannels(1, true);
        //listChannelLumens(1);
        //listOriginalTiffImages(1);
        List<Image> images = DbUtilities.getAllImages();
        for (Image image : images) {
            List<Coordinates> coordinates = DbUtilities.getCoordinatesOfImage(image.getId());
            if(coordinates.isEmpty()) {
                System.out.println(image.toString());
            }
        }
    }
}
