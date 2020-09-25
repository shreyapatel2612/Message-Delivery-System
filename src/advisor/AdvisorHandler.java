/*
 Shreya Patel
 1001646429
 */
package advisor;

import common.StudentRecord;
import common.Utils;
import common.Utils.AdvisorDecision;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import mqs.MqsRequest;
import student.StudentRecordUpdate;
import common.Utils.SenderProcess;
import java.util.Map;

/**
 * Class that handles Advisor process It pull records from MQS for which
 * decision is required and push data back to MQS once decision is made
 */
public class AdvisorHandler extends Thread {
    /* sleep time for this thread after which it polls MQS */

    private static final int SLEEP_TIME_IN_MILLIS = 3000;

    /* Indicate whether UI is still being displayed and not closed */
    private boolean isAdvisorUIRunning = true;

    /* Advisor GUI object to update UI */
    private AdvisorGUIHandler advisorGUIHandler;

    /* Client socket */
    private Socket clientSocket = null;

    /* Data input/output streams to receive/send messages */
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    public AdvisorHandler(AdvisorGUIHandler advisorGUIHandler) {
        this.advisorGUIHandler = advisorGUIHandler;
    }

    /**
     * Create a client socket and connect to MQS server socket once connected,
     * it polls MQS based every few seconds for new records If MQS in not
     * reachable, it keeps trying to connect first
     */
    @Override
    public void run() {
        /* Keep running until UI is killed */
        while (isAdvisorUIRunning) {
            try {
                InetAddress ipAddress = InetAddress.getByName("localhost");

                /* establish the connection with the MQS */
                clientSocket = new Socket(ipAddress, Utils.MQS_CONNECTION_PORT);

                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                inputStream = new ObjectInputStream(clientSocket.getInputStream());


                /* Display message on UI that MQS is connected */
                advisorGUIHandler.displayMessageOnUI("Advisor process is connected to MQS");

                MqsRequest mqsConnectRequest = Utils.getMqsConnectRequest(SenderProcess.ADVISOR);

                outputStream.writeObject(mqsConnectRequest);

                while (isAdvisorUIRunning) {
                    /* sleep before polling MQS */
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);

                    /* Create a pull request from MQS */
                    MqsRequest mqsPullRequest = Utils.getAdvisorToMqsPullRequest();

                    /* Send pull request to MQS */
                    outputStream.writeObject(mqsPullRequest);

                    /* Wait for Pull repsonse */
                    StudentRecordUpdate studentRecordUpdate = (StudentRecordUpdate) inputStream.readObject();

                    /* Update student records with decision */
                    List<StudentRecord> studentRecordsToUpdate = studentRecordUpdate.getStudentRecords();
                    StringBuilder messageToDisplay = new StringBuilder();
                    boolean isProcessingRequired = false;
                    for (StudentRecord studentRecord : studentRecordsToUpdate) {
                        /* Randomly approve or decline student request */
                        int randomVal = new Random().nextInt(2);
                        AdvisorDecision advisorDecision = randomVal == 0 ? AdvisorDecision.APPROVED : AdvisorDecision.DENIED;
                        studentRecord.setAdvisorDecision(advisorDecision);
                        messageToDisplay.append("Decision: " + studentRecord.getAdvisorDecision()
                                + " Student Name:" + studentRecord.getStudentName() + " Course:" + studentRecord.getCourseName() + "\n");
                        isProcessingRequired = true;
                    }
                    if (isProcessingRequired) {
                        /* Push result back to MQS */
                        MqsRequest mqsPushRequest = Utils.getAdvisorToMqsPushRequest(studentRecordsToUpdate);
                        outputStream.writeObject(mqsPushRequest);
                        advisorGUIHandler.displayMessageOnUI(messageToDisplay.toString().trim());
                    } else {
                        advisorGUIHandler.displayMessageOnUI("No Student request to process from MQS after PULL request");
                    }
                }
            } catch (Exception ex) {
                advisorGUIHandler.displayMessageOnUI("Advisor is disconnected from MQS. Connection will be retried automatically in few seconds.");
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
                System.out.println("Thread waiting fail for AdvisorHandler");
            }

        }
    }

    /**
     * Send MQS message to disconnect
     */
    public void disconnect() {
        try {
            /* Create a disconnect request for MQS */
            MqsRequest mqsDisconnectRequest = Utils.getMqsDisconnectRequest(SenderProcess.ADVISOR);

            /* Send disconnect request to MQS */
            outputStream.writeObject(mqsDisconnectRequest);

        } catch (Exception ex) {
            System.out.println("Disconnect request from Advisor to MQS failed! ");
        } finally {
            isAdvisorUIRunning = false;
        }
    }
}
