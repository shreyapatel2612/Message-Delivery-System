/*
    Shreya Patel
    1001646429
*/
package common;

import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import mqs.MqsRequest;
import student.StudentRecordUpdate;

/**
 * Util class provides helper methods to send messages from one process another
 * in format understood by message receiving process
 */
public class Utils {
    /* Constant variable declarations */
    public static final int MQS_CONNECTION_PORT = 10004;
    
    /**
     * Enum indicating various processes
     */
    public enum SenderProcess {
        STUDENT,
        ADVISOR,
        MQS, // Messaging Queue Server
        NOTIFICATION;
    }

    /**
     * Status of the message while it is being processed
     */
    public enum MessageStatus {
        RECEIVED_FROM_STUDENT,
        SENT_TO_ADVISOR,
        RECEIVED_FROM_ADVISOR,
        SENT_TO_NOTIFICATION;
    }

    /**
     * Enum indicating various operation types
     */
    public enum OperationType {
        // for connecting to MQS
        CONNECT,
        // for disconnecting with MQS
        DISCONNECT,
        // PUSH request to MQS (from Advisor/Student process)
        // OR to Student process (from Notification process)
        PUSH,
        // PULL request to MQS (from Advisor/Notification process)
        PULL,
        // Response from MQS to Advisor/Notification process to their pull request
        RESPONSE,
        // Acknowledgement from Notification process to MQS
        ACK;
    }

    /**
     * Enum indicating advisor's decision
     */
    public enum AdvisorDecision {
        PENDING,
        APPROVED,
        DENIED;
    }

    public static MqsRequest getStudentToMqsPushRequest(String studentName, String courseName) {
        StudentRecord studentRecord = new StudentRecord(MessageStatus.RECEIVED_FROM_STUDENT, studentName, courseName, AdvisorDecision.PENDING);
        return new MqsRequest(
                SenderProcess.STUDENT,
                OperationType.PUSH,
                new ArrayList<StudentRecord>(Arrays.asList(studentRecord)));
    }

    public static MqsRequest getNotificationToMqsPullRequest() {
        return new MqsRequest(
                SenderProcess.NOTIFICATION,
                OperationType.PULL,
                null /* studentRecords */);
    }

    public static MqsRequest getNotificationToMqsAcknowledgement(List<StudentRecord> studentRecords) {
        return new MqsRequest(
                SenderProcess.NOTIFICATION,
                OperationType.ACK,
                studentRecords);
    }

    public static MqsRequest getAdvisorToMqsPullRequest() {
        return new MqsRequest(
                SenderProcess.ADVISOR,
                OperationType.PULL,
                null /* studentRecords */);
    }

    public static MqsRequest getAdvisorToMqsPushRequest(List<StudentRecord> studentRecords) {
        return new MqsRequest(
                SenderProcess.ADVISOR,
                OperationType.PUSH,
                studentRecords);
    }

    /**
     * Returns Student record to update (sent from MQS to Advisor/Notification
     * process)
     */
    public static StudentRecordUpdate getStudentRecordUpdateResponse(List<StudentRecord> studentRecords) {
        return new StudentRecordUpdate(
                OperationType.RESPONSE,
                studentRecords);
    }

    /**
     * Returns request for MQS for initial connection
     */
    public static MqsRequest getMqsConnectRequest(SenderProcess senderProcess) {
        return new MqsRequest(senderProcess, OperationType.CONNECT, null /* studentRecord */);
    }

    /**
     * Returns request for MQS for disconnecting
     */
    public static MqsRequest getMqsDisconnectRequest(SenderProcess senderProcess) {
        return new MqsRequest(senderProcess, OperationType.DISCONNECT, null /* studentRecord */);
    }
}
