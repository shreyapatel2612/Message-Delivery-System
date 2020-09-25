/*
    Shreya Patel
    1001646429
*/
package student;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import mqs.MqsRequest;
import common.Utils;
import common.Utils.OperationType;
import common.Utils.SenderProcess;
import java.io.IOException;

/**
* Class that handles student process actions
* It keeps connection with MQS and sends student requests to it
*/
public class StudentHandler extends Thread {
    /* Student GUI Handler */

    private StudentGUIHandler studentGUIHandler;

    /* Indicate whether UI is still being displayed and not closed */
    private boolean isStudentUIRunning = true;

    /* Client socket */
    private Socket clientSocket = null;

    /* Data output streams to send messages */
    ObjectOutputStream outputStream;

    /* Wait time before retrying connection with MQS */
    private static final int WAIT_BEFORE_RETRY_TIME_MILLIS = 5000;

    Utils utils = new Utils();

    public StudentHandler(StudentGUIHandler studentGUIHandler) {
        this.studentGUIHandler = studentGUIHandler;
    }

    /**
     * Keeps a client connection to the MQS Processes GUI action and sends
     * messages to MQS
     */
    @Override
    public void run() {
        while (isStudentUIRunning) {
            try {
                if (clientSocket == null) {
                    InetAddress ipAddress = InetAddress.getByName("localhost");

                    /* establish the connection with the MQS */
                    clientSocket = new Socket(ipAddress, Utils.MQS_CONNECTION_PORT);

                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                    MqsRequest mqsConnectRequest = Utils.getMqsConnectRequest(SenderProcess.STUDENT);

                    outputStream.writeObject(mqsConnectRequest);

                    studentGUIHandler.displayMessageOnUI("Student process is connected with MQS.");
                } else {
                    MqsRequest mqsRequest = new MqsRequest(SenderProcess.STUDENT, OperationType.RESPONSE, null);
                    outputStream.writeObject(mqsRequest);
                }

            } catch (Exception ex) {
                if(clientSocket != null) {
                    try {
                        clientSocket.close();
                        clientSocket = null;
                    } catch (IOException ex1) {
                        System.out.println("Exception caugth in studentHanler");
                        ex1.printStackTrace();
                    }
                }
                studentGUIHandler.displayMessageOnUI("Connection with MQS is lost. It will be retried in few seconds!");
                ex.printStackTrace();
            } 
            
            try {
                /* sleep before reconnecting to MQS, added to avoid continuous reconnect attempts */
                Thread.sleep(WAIT_BEFORE_RETRY_TIME_MILLIS);
            } catch (InterruptedException ex) {
                System.out.println("Thread waiting fail for StudentHandler");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to handle Student process shutdown
     */
    public void disconnect() throws IOException {
        try {

            MqsRequest mqsDisconnectRequest;
            mqsDisconnectRequest = utils.getMqsDisconnectRequest(SenderProcess.STUDENT);

            /* Send disconnect request to MQS */
            outputStream.writeObject(mqsDisconnectRequest);

        } catch (Exception ex) {
            System.out.println("Disconnect request from Student to MQS failed! ");
        } finally {
            isStudentUIRunning = false;
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        }
    }

    /**
     * Send student course request to MQS
     */
    public void sendMqsRequest(String studentName, String courseName) throws IOException {
        /* Check whether MQS is available first */
        if (clientSocket == null) {
            studentGUIHandler.displayMessageOnUI("Student is not connected with MQS. Please check if MQS is up and running!");
            return;
        }
        boolean isSuccess = false;
        try {
            /* Get MQS request message */
            MqsRequest mqsPushRequest = Utils.getStudentToMqsPushRequest(studentName, courseName);

            /* Send push request to MQS */
            outputStream.writeObject(mqsPushRequest);

            isSuccess = true;
            studentGUIHandler.displayMessageOnUI("Student request is successfully sent to MQS. Check notification process for Advisor update");
        } catch (Exception e) {
            studentGUIHandler.displayMessageOnUI("Failed to send MQS request for student: " + studentName
                    + " course: " + courseName + "Student will be reconnect to MQS");
            e.printStackTrace();
        } finally {
            if (!isSuccess && clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        }
    }
}
