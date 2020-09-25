/*
    Shreya Patel
    1001646429
*/
package mqs;

import common.StudentRecord;
import common.Utils;
import common.Utils.MessageStatus;
import common.Utils.SenderProcess;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import student.StudentRecordUpdate;

/**
* Class that handles given connection to MQS
*/
public class MqsRequestHandler extends Thread {
    /* MQS GUI object to update UI */
    private MqsGUIHandler mqsGUIHandler;

    /* MQS Handler class which has common message queue */
    private MqsHandler mqsHandler;

    /* socket object for given incoming connection */
    private Socket clientSocket;

    /* Sql utility handler object */
    private SqlUtil sqlUtil;

    /* Name of the connected process */
    private String connectedProcess = "";

    /* Data input/output streams to receive/send messages */
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public MqsRequestHandler(MqsGUIHandler mqsGUIHandler, Socket clientSocket, MqsHandler mqsHandler, SqlUtil sqlUtil) {
        this.mqsGUIHandler = mqsGUIHandler;
        this.mqsHandler = mqsHandler;
        this.clientSocket = clientSocket;
        this.sqlUtil = sqlUtil;
    }

    @Override
    public void run() {
        try {
            boolean connected = true;
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            while (connected) {
                /* Read deserialized request object from socket input stream */
                MqsRequest mqsRequest = (MqsRequest) inputStream.readObject();

                /* Handle based on operation type */
                switch (mqsRequest.getOperationType()) {
                    case CONNECT:
                        connectedProcess = mqsRequest.getSenderProcess().name();
                        mqsHandler.setActiveClient(connectedProcess);
                        String connectMsg= mqsRequest.getSenderProcess().name() + " process is connected";
                        mqsGUIHandler.displayMessageOnUI(connectMsg);
                        break;
                    case PUSH:
                        handlePushRequest(mqsRequest);
                        break;
                    case PULL:
                        handlePullRequest(mqsRequest);
                        break;
                    case DISCONNECT:
                        connected = false;
                        break;
                    case ACK:
                        handleAckFromNotification(mqsRequest);
                        break;
                    default:
                        break;
                }

            }
        } catch (Exception ex) {
            System.out.println("Disconnected " + connectedProcess + " from MQS");
            ex.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (Exception ex) {
                System.out.println("Unable to clean up resources");
            }
            mqsGUIHandler.displayMessageOnUI("Process " + connectedProcess + " is disconnected from MQS");
            mqsHandler.removeActiveClient(connectedProcess);
        }
    }

    /**
     * Handler for PUSH request to MQS PUSH request is either from Student or
     * Advisor process
     */
    public void handlePushRequest(MqsRequest mqsRequest) {
        /* Persists all incoming messages to database first followed by in-memory object creation */
        if (mqsRequest.getSenderProcess() == SenderProcess.STUDENT) {
            StudentRecord studentRecord = mqsRequest.getStudentRecordList().get(0);
            boolean isSuccess = sqlUtil.addMessageToDatabase(studentRecord);
            /* If successfully added to database then create in-memory object */
            if (isSuccess) {
                mqsHandler.addMessagesToQueue(studentRecord);
            }
            mqsGUIHandler.displayMessageOnUI("Successfully executed PUSH request for Student process");
        } else {
            for (StudentRecord studentRecord : mqsRequest.getStudentRecordList()) {
                boolean isSuccess = sqlUtil.updateMessageStatus(studentRecord, MessageStatus.RECEIVED_FROM_ADVISOR, MessageStatus.SENT_TO_ADVISOR);
                /* If successfully added to database then update in-memory object */
                if (isSuccess) {
                    mqsHandler.updateMessageStatus(studentRecord, MessageStatus.RECEIVED_FROM_ADVISOR, MessageStatus.SENT_TO_ADVISOR);
                }
                mqsGUIHandler.displayMessageOnUI("Successfully executed PUSH request for Advisor process");
            }
        }
    }

    /**
     * Handler for ACK received from Notification process Message is completely
     * processed hence it can be removed from database/in-memory queue
     */
    public void handleAckFromNotification(MqsRequest mqsRequest) {
        /* Persists all incoming messages to database first followed by in-memory object update */
        for (StudentRecord studentRecord : mqsRequest.getStudentRecordList()) {
            boolean isSuccess = sqlUtil.removeMessageFromDatabase(studentRecord);
            /* If successfully deleted from database then remove in-memory object */
            if (isSuccess) {
                mqsHandler.deleteMessagesFromQueue(studentRecord);
            }
        }
    }

    /**
     * Handler for PULL request to MQS PULL request is either from Advisor or
     * Notification process
     */
    public void handlePullRequest(MqsRequest mqsRequest) {
        boolean isSuccess = false;
        List<StudentRecord> studentRecordsToSend;
        if (mqsRequest.getSenderProcess() == SenderProcess.ADVISOR) {
            /* Get all messages that require Advisor's decision */
            studentRecordsToSend = mqsHandler.getMessagesForAdvisor();
        } else { /* Sender is Notification */
            /* Get all messages that require to be sent to Notification process */

            studentRecordsToSend = mqsHandler.getMessagesForNotification();
        }

        /* Create a response to send for this pull request */
        StudentRecordUpdate studentRecordUpdate = Utils.getStudentRecordUpdateResponse(studentRecordsToSend);

        /* Send this response first */
        try {
            outputStream.writeObject(studentRecordUpdate);
            isSuccess = true;
        } catch (Exception ex) {
            isSuccess = false;
            System.out.println("Unable to send PULL response to " + mqsRequest.getSenderProcess().name() + " from MQS ");
            ex.printStackTrace();
        }

        /* If messsages are sent successfully then update message status to reflect that */
        if (isSuccess) {
            for (StudentRecord studentRecord : studentRecordsToSend) {
                MessageStatus status;
                MessageStatus expectedStatus;
                if (mqsRequest.getSenderProcess() == SenderProcess.ADVISOR) {
                    expectedStatus = MessageStatus.RECEIVED_FROM_STUDENT;
                    status = MessageStatus.SENT_TO_ADVISOR;
                } else {
                    expectedStatus = MessageStatus.RECEIVED_FROM_ADVISOR;
                    status = MessageStatus.SENT_TO_NOTIFICATION;
                    System.out.println("inside nofication of handle oull request");
                }
                if (sqlUtil.updateMessageStatus(studentRecord, status, expectedStatus)) {
                    /* Update in memory object if database update is successful */
                    mqsHandler.updateMessageStatus(studentRecord, status, expectedStatus);
                } else {
                    System.out.println("Unable to update message status to " + status.name() + " for " + studentRecord.getStudentName());
                }
            }
        }
        mqsGUIHandler.displayMessageOnUI("Successfully processed PULL request from " + mqsRequest.getSenderProcess().name() + " process");
    }
}
