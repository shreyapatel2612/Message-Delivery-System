/*
 Shreya Patel
 1001646429
 */
package mqs;

import common.Utils;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import common.StudentRecord;
import common.Utils.AdvisorDecision;
import common.Utils.MessageStatus;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Class that acts as a Messaging Queue Server (MQS) It handles all incoming
 * connection request and messages to process
 */
public class MqsHandler extends Thread {
    /* keeps track of messages queue */

    private LinkedList<StudentRecord> messagesList;

    /* MQS GUI object to update UI */
    private MqsGUIHandler mqsGUIHandler;

    /* Indicate whether UI is still being displayed and not closed */
    private boolean isMqsUIRunning = true;

    /* MQS server socket */
    private ServerSocket serverSocket = null;

    /* Set to store active clients */
    private Set<String> activeClients;

    /* For DB util class */
    private SqlUtil sqlUtil;

    public MqsHandler(MqsGUIHandler mqsGUIHandler) throws Exception {
        this.mqsGUIHandler = mqsGUIHandler;
        this.messagesList = new LinkedList<StudentRecord>();
        this.activeClients = new HashSet<>();

        /* Load database and messagesList if database already have some records */
        sqlUtil = new SqlUtil();
        sqlUtil.loadMqsMessages(messagesList);
    }

    /**
     * Creates a server socket for listening to incoming connections Processes
     * incoming connections and creates handler thread for them
     */
    @Override
    public void run() {
        //Socket clientSocket = null;
        Set<Socket> clientSockets = new HashSet();
        try {
            /* creates a server socket */
            serverSocket = new ServerSocket(Utils.MQS_CONNECTION_PORT);

            while (isMqsUIRunning) {
                /* accepts incoming request from clients */
                Socket clientSocket = serverSocket.accept();

                /* create a dedicated thread to handle this request */
                new MqsRequestHandler(mqsGUIHandler, clientSocket, this, sqlUtil).start();
                clientSockets.add(clientSocket);
            }
        } catch (Exception ex) {
            isMqsUIRunning = false;
            if (mqsGUIHandler != null) {
                /* Server is not listening anymore */
                mqsGUIHandler.displayMessageOnUI("Server has stopped running. please close UI and try running again!");
            }
            ex.printStackTrace();
        } finally {
            try {
                /* clean up resources */

                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
                /* clean up database resources */
                if (sqlUtil != null) {
                    sqlUtil.cleanup();
                    sqlUtil = null;
                }
                for(Socket clientSocket: clientSockets) {
                    clientSocket.close();
                }
//                if(clientSocket != null) {
//                    clientSocket.close();
//                }
            } catch (Exception ex) {
                System.out.println("Unable to clean up resources");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Provides way to add message to the common queue every thread has same
     * object of "MqsHandler" hence synchronizing this ensures one update at a
     * time
     */
    public void addMessagesToQueue(StudentRecord studentRecord) {
        /* Input validation */
        if (studentRecord == null) {
            return;
        }
        /* synchronize read/write to common message queue */
        synchronized (messagesList) {
            messagesList.add(studentRecord);
        }
    }

    /**
     * Provides a way to delete messages from common queue every thread has same
     * object of "MqsHandler" hence synchronizing this ensures one update at a
     * time
     */
    public void deleteMessagesFromQueue(StudentRecord studentRecord) {
        /* Input validation */
        if (studentRecord == null) {
            return;
        }
        /* synchronize read/write to common message queue */
        synchronized (messagesList) {
            StudentRecord recordToRemove = null;
            for (StudentRecord messageRecord : messagesList) {
                if (messageRecord.getStudentName().equals(studentRecord.getStudentName())
                        && messageRecord.getCourseName().equals(studentRecord.getCourseName())
                        && messageRecord.getMessageStatus() == MessageStatus.SENT_TO_NOTIFICATION
                        && messageRecord.getAdvisorDecision() == studentRecord.getAdvisorDecision()) {
                    recordToRemove = messageRecord;
                    break;
                }
            }
            if (recordToRemove != null) {
                messagesList.remove(recordToRemove);
            } else {
                System.out.println("Unable to delete message from queue Student Name: "
                        + studentRecord.getStudentName() + " Course Name: " + studentRecord.getCourseName());
            }
        }
    }

    /**
     * Provides a way to update messages from common queue every thread has same
     * object of "MqsHandler" hence synchronizing this ensures one update at a
     * time
     */
    public void updateMessageStatus(StudentRecord studentRecord, MessageStatus messageStatus, MessageStatus expectedStatus) {
        /* Input validation */
        if (studentRecord == null) {
            return;
        }
        /* synchronize read/write to common message queue */
        synchronized (messagesList) {
            for (StudentRecord messageRecord : messagesList) {
                if (messageRecord.getStudentName().equals(studentRecord.getStudentName())
                        && messageRecord.getCourseName().equals(studentRecord.getCourseName())
                        && messageRecord.getMessageStatus() == expectedStatus) {
                    messageRecord.setMessageStatus(messageStatus);
                    messageRecord.setAdvisorDecision(studentRecord.getAdvisorDecision());
                    break;
                }
            }
        }
    }

    /**
     * Provides a way to get all messages that needs to be sent to Advisor for
     * their decision
     */
    public List<StudentRecord> getMessagesForAdvisor() {
        List<StudentRecord> studentRecordList = new ArrayList<StudentRecord>();

        /* synchronize read/write to common message queue */
        synchronized (messagesList) {
            ListIterator<StudentRecord> studentRecordIterator = messagesList.listIterator();

            /* Iterate through queue and find messages which are
             * last processed by student process and are without advisor decision
             */
            while (studentRecordIterator.hasNext()) {
                StudentRecord currentMessage = studentRecordIterator.next();
                if (currentMessage.getMessageStatus() == MessageStatus.RECEIVED_FROM_STUDENT
                        && currentMessage.getAdvisorDecision() == AdvisorDecision.PENDING) {
                    StudentRecord studentRecord = new StudentRecord(currentMessage.getMessageStatus(), currentMessage.getStudentName(),
                            currentMessage.getCourseName(), currentMessage.getAdvisorDecision());
                    studentRecordList.add(studentRecord);
                }
            }
        }
        return studentRecordList;
    }

    /**
     * Provides a way to get all messages that needs to be sent to Notification
     * and have decisions made by advisor
     */
    public List<StudentRecord> getMessagesForNotification() {
        List<StudentRecord> studentRecordList = new ArrayList<StudentRecord>();

        /* synchronize read/write to common message queue */
        synchronized (messagesList) {
            ListIterator<StudentRecord> studentRecordIterator = messagesList.listIterator();

            /* Iterate through queue and find messages which are
             * last processed by advisor process and have advisor decision already made
             */
            while (studentRecordIterator.hasNext()) {
                StudentRecord currentMessage = studentRecordIterator.next();
                if (currentMessage.getMessageStatus() == MessageStatus.RECEIVED_FROM_ADVISOR
                        && currentMessage.getAdvisorDecision() != AdvisorDecision.PENDING) {
                    StudentRecord studentRecord = new StudentRecord(currentMessage.getMessageStatus(), currentMessage.getStudentName(),
                            currentMessage.getCourseName(), currentMessage.getAdvisorDecision());
                    studentRecordList.add(studentRecord);
                }
            }
        }
        return studentRecordList;
    }

    /* Helper method to add client to active processes list */
    public synchronized void setActiveClient(String clientName) {
        activeClients.add(clientName);
        mqsGUIHandler.refreshActiveProcessList(String.join("\n", activeClients));
    }

    /* Helper method to remove client from active processes list */
    public synchronized void removeActiveClient(String clientName) {
        activeClients.remove(clientName);
        mqsGUIHandler.refreshActiveProcessList(String.join("\n", activeClients));
    }

    /**
     * Method that provides clean up when MQS UI is closed
     */
    public void shutDown() {
        try {
            isMqsUIRunning = false;
            serverSocket.close(); // close socket to stop listening
            serverSocket = null;
        } catch (Exception ex) {
            System.out.println("Unable to close MQS server socket!");
        }
    }
}
