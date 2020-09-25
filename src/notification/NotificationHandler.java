/*
 Shreya Patel
 1001646429
 */
package notification;

import common.StudentRecord;
import common.Utils;
import common.Utils.SenderProcess;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mqs.MqsRequest;
import student.StudentRecordUpdate;

/**
 * Class that handles Notification process It pull records from MQS for which
 * decision is made by advisor and shows them on its UI
 */
public class NotificationHandler extends Thread {
    /* sleep time for this thread after which it polls MQS */

    private static final int SLEEP_TIME_IN_MILLIS = 7000;

    /* Indicate whether UI is still being displayed and not closed */
    private boolean isNotificationUIRunning = true;

    /* Notification GUI object to update UI */
    private NotificationGUIHandler notificationGUIHandler;

    /* Client socket */
    private Socket clientSocket = null;

    /* Data input/output streams to receive/send messages */
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    public NotificationHandler(NotificationGUIHandler notificationGUIHandler) {
        this.notificationGUIHandler = notificationGUIHandler;
    }

    /**
     * Create a client socket and connect to MQS server socket once connected,
     * it polls MQS based every few seconds for new records If MQS in not
     * reachable, it keeps trying to connect first
     */
    public void run() {
        /* Keep running until UI is killed */
        while (isNotificationUIRunning) {
            try {
                InetAddress ipAddress = InetAddress.getByName("localhost");

                /* establish the connection with the MQS */
                clientSocket = new Socket(ipAddress, Utils.MQS_CONNECTION_PORT);

                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                inputStream = new ObjectInputStream(clientSocket.getInputStream());

                MqsRequest mqsConnectRequest = Utils.getMqsConnectRequest(SenderProcess.NOTIFICATION);

                outputStream.writeObject(mqsConnectRequest);
                /* Display message on UI that MQS is connected */
                notificationGUIHandler.displayMessageOnUI("Notification process is connected to MQS");

                while (isNotificationUIRunning) {
                    /* sleep before polling MQS */
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);

                    /* Create a pull request from MQS */
                    MqsRequest mqsPullRequest = Utils.getNotificationToMqsPullRequest();

                    /* Send pull request to MQS */
                    outputStream.writeObject(mqsPullRequest);

                    /* Wait for Pull repsonse */
                    StudentRecordUpdate studentRecordUpdate = (StudentRecordUpdate) inputStream.readObject();

                    /* Mapping from student to course and advisor decision for it */
                    Map<String, Map<String, String>> studentToDecisionMap = new HashMap<>();

                    /* Update student records with decision */
                    List<StudentRecord> studentRecordsToUpdate = studentRecordUpdate.getStudentRecords();
                    for (StudentRecord studentRecord : studentRecordsToUpdate) {
                        String studentName = studentRecord.getStudentName();
                        Map<String, String> courseToDecisionMap = studentToDecisionMap.getOrDefault(studentName, new HashMap<String, String>());

                        courseToDecisionMap.put(studentRecord.getCourseName(), studentRecord.getAdvisorDecision().name());
                        studentToDecisionMap.put(studentName, courseToDecisionMap);
                    }

                    displayDecisions(studentToDecisionMap);

                    /* Acknowledge back to MQS since decision is received */
                    MqsRequest mqsPushRequest = Utils.getNotificationToMqsAcknowledgement(studentRecordsToUpdate);
                    outputStream.writeObject(mqsPushRequest);
                }
            } catch (Exception ex) {
                notificationGUIHandler.displayMessageOnUI("Notification process is disconnected from MQS. Connection will be retried automatically in few seconds.");
                ex.printStackTrace();
            } finally {
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                        clientSocket = null;
                    }
                } catch (Exception ex) {
                    System.out.println("Unable to clean up resources");
                    ex.printStackTrace();
                }
            }
            try {
                /* sleep before reconnecting to MQS, added to avoid continuous reconnect attempts */
                Thread.sleep(SLEEP_TIME_IN_MILLIS);
            } catch (InterruptedException ex) {
                System.out.println("Exception in NotificationHandler");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to display result for each student on Notification process UI
     */
    private void displayDecisions(Map<String, Map<String, String>> studentToDecisionMap) {
        if (studentToDecisionMap.size() == 0) {
            notificationGUIHandler.displayMessageOnUI("No Student request to process from MQS after PULL request");
            return;
        }
        StringBuilder messageToDisplay = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> entry : studentToDecisionMap.entrySet()) {
            messageToDisplay.append("Student: " + entry.getKey() + "\n");
            for (Map.Entry<String, String> decision : entry.getValue().entrySet()) {
                messageToDisplay.append("CourseName: " + decision.getKey() + " AdvisorDecision: " + decision.getValue() + "\n");
            }
            messageToDisplay.append("\n");
        }
        notificationGUIHandler.displayMessageOnUI(messageToDisplay.toString());

    }

    /**
     * Send MQS message to disconnect
     */
    public void disconnect() {
        try {
            /* Create a disconnect request for MQS */
            MqsRequest mqsDisconnectRequest = Utils.getMqsDisconnectRequest(SenderProcess.NOTIFICATION);

            /* Send disconnect request to MQS */
            outputStream.writeObject(mqsDisconnectRequest);

        } catch (Exception ex) {
            System.out.println("Disconnect request from Notification to MQS failed! ");
        } finally {
            isNotificationUIRunning = false;
        }
    }
}
