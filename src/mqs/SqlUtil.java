/*
    Shreya Patel
    1001646429
*/
package mqs;

import common.StudentRecord;
import common.Utils;
import common.Utils.AdvisorDecision;
import common.Utils.MessageStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

/**
* Utility class to provide helper functions for
* creating and executing SQL queries
*/
public class SqlUtil {
    /* JDBC driver name and database URL */
    private final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String DB_URL = "jdbc:mysql://localhost:3306/mqs";

    /*  Database credentials */
    private final String USER = "root";
    private final String PASSWD = "";

    /* Database value specific constants */
    private final String TABLE_NAME = "MqsMessageDetails";
    private final String STUDENT_NAME_COLUMN = "student_name";
    private final String COURSE_NAME_COLUMN = "course_name";
    private final String ADVISOR_DECISION_COLUMN = "advisor_decision";
    private final String MESSAGE_STATUS = "message_status";

    /* Database object variables */
    Connection connection;
    Statement statement = null;
    Utils utils = new Utils();

    public SqlUtil() throws SQLException, ClassNotFoundException {
        boolean isSuccess = false;
        try {
            /* Create a DB connection and create table */
            Class.forName(JDBC_DRIVER);
            connection = DriverManager.getConnection(DB_URL, USER, PASSWD);
            statement = connection.createStatement();
            createTableIfNotExists();
            isSuccess = true;
        } finally {
            if (!isSuccess) {
                cleanup(); //clean up if failed
            }
        }
    }

    /**
     * Create a database table which store MQS messages creates only if it
     * doesn't exists throws SQL Exception if operation is not successful,
     * program should halt if it happens
     */
    private void createTableIfNotExists() throws SQLException {
        boolean isSuccess = false;
        try {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS " + "mqsmessagedetails"
                    + " (" + MESSAGE_STATUS + " VARCHAR(50) not NULL, "
                    + STUDENT_NAME_COLUMN + " VARCHAR(50) not NULL, "
                    + COURSE_NAME_COLUMN + " VARCHAR(50) not NULL, "
                    + ADVISOR_DECISION_COLUMN + " VARCHAR(50) not NULL, "
                    + "PRIMARY KEY ( " + MESSAGE_STATUS + ", " + STUDENT_NAME_COLUMN + ", " + COURSE_NAME_COLUMN + " ))";

            statement.executeUpdate(createTableQuery);
            isSuccess = true;
            System.out.println("Created " + TABLE_NAME + " successfully!");
        } finally {
            if (!isSuccess) {
                cleanup(); //clean up if failed
            }
        }
    }

    /**
     * Load initial database and fill out messaging queue for MQS LinkedList
     * holds messages queue within MQS and synchronized by caller of this method
     * throws SQL Exception if operation is not successful, program should halt
     * if it happens
     */
    public void loadMqsMessages(LinkedList<StudentRecord> studentRecordList) throws SQLException {
        boolean isSuccess = false;
        try {
            String loadQuery = "SELECT * FROM " + "mqsmessagedetails";
            ResultSet results = statement.executeQuery(loadQuery);

            /* Update LinkedList with database records */
            while (results.next()) {
                StudentRecord studentRecord
                        = new StudentRecord(MessageStatus.valueOf(results.getString(MESSAGE_STATUS)),
                                results.getString(STUDENT_NAME_COLUMN),
                                results.getString(COURSE_NAME_COLUMN),
                                AdvisorDecision.valueOf(results.getString(ADVISOR_DECISION_COLUMN)));
                studentRecordList.add(studentRecord);
            }
            isSuccess = true;
        } finally {
            if (!isSuccess) {
                cleanup(); //clean up if failed
            }
        }
    }

    /**
     * Add Mqs Message record to database Return true if successful else return
     * false
     */
    public boolean addMessageToDatabase(StudentRecord studentRecord) {
        boolean isSuccess = false;
        try {
            String insertQuery = "INSERT INTO " + "mqsmessagedetails"
                    + " VALUES ( " + "'" + studentRecord.getMessageStatus().name() + "', "
                    + "'" + studentRecord.getStudentName() + "', "
                    + "'" + studentRecord.getCourseName() + "', "
                    + "'" + studentRecord.getAdvisorDecision().name() + "')";
            int numRowsUpdated = statement.executeUpdate(insertQuery);
            if (numRowsUpdated != 1) {
                throw new Exception("Unable to add new record to database!");
            }
            isSuccess = true;
        } catch (Exception ex) {
            isSuccess = false;
            System.out.println("Failed to add message to database! ");
            ex.printStackTrace();
        }
        return isSuccess;
    }

    /**
     * Update Mqs message status to database Return true if successful else
     * return false
     */
    public boolean updateMessageStatus(StudentRecord studentRecord, MessageStatus messageStatus, MessageStatus expectedStatus) {
        boolean isSuccess = false;
        try {
            String updateQuery = "UPDATE " + "mqsmessagedetails"
                    + " SET " + MESSAGE_STATUS + " = '" + messageStatus.name() + "', " + ADVISOR_DECISION_COLUMN + " = '" + studentRecord.getAdvisorDecision().name() +"' "
                    +" WHERE " + STUDENT_NAME_COLUMN + " = '" + studentRecord.getStudentName() + "' "
                    + " AND " + COURSE_NAME_COLUMN + " = '" + studentRecord.getCourseName() + "' "
                    + " AND " + MESSAGE_STATUS + " = '" + expectedStatus.name() + "'";
            System.out.println("" + studentRecord.getStudentName() + " " + studentRecord.getCourseName() + " " + studentRecord.getMessageStatus().name());
            int numRowsUpdated = statement.executeUpdate(updateQuery);
            if (numRowsUpdated != 1) {
                throw new Exception("Unable to update record to database!");
            }
            isSuccess = true;
        } catch (Exception ex) {
            isSuccess = false;
            System.out.println("Failed to add message to database! ");
            ex.printStackTrace();
        }
        return isSuccess;
    }

    /**
     * Removes message from database Returns true if successful else return
     * false
     */
    public boolean removeMessageFromDatabase(StudentRecord studentRecord) {
        boolean isSuccess = false;
        try {
            String deleteQuery = "DELETE FROM " + "mqsmessagedetails"
                    + " WHERE " + MESSAGE_STATUS + " = '" + MessageStatus.SENT_TO_NOTIFICATION.name() + "' "
                    + " AND " + ADVISOR_DECISION_COLUMN + " = '" + studentRecord.getAdvisorDecision().name() +"' "
                    + " AND " + STUDENT_NAME_COLUMN + " = '" + studentRecord.getStudentName() +"' "
                    + " AND " + COURSE_NAME_COLUMN + " = '" + studentRecord.getCourseName() +"' ";
            int numRowsDeleted = statement.executeUpdate(deleteQuery);
            if (numRowsDeleted != 1) {
                throw new Exception("Failed to delete message from database!");
            }
            isSuccess = true;
        } catch (Exception ex) {
            isSuccess = false;
            System.out.println("Failed to delete message from database! ");
            ex.printStackTrace();
        }
        return isSuccess;
    }

    /**
     * Utility method to clean up Database resources
     */
    public void cleanup() {
        try {
            if (statement != null) {
                connection.close();
            }
        } catch (Exception ex) {
            System.out.println("Failed to clean up SQL resources");
        }
    }
}
