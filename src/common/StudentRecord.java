/*
    Shreya Patel
    1001646429
*/
package common;

import common.Utils.AdvisorDecision;
import common.Utils.MessageStatus;
import java.io.Serializable;


/**
* Wrapper class to hold information about
* student's record and advisor decision
*/
public class StudentRecord implements Serializable{
     /* Status of this message */
    Utils.MessageStatus messageStatus;

    /* Name of student requesting approval */
    private String studentName;

    /* Requested course name */
    private String courseName;

    /* Decision from advisor */
    private AdvisorDecision advisorDecision;

    public StudentRecord(Utils.MessageStatus messageStatus, String studentName, String courseName, AdvisorDecision advisorDecision) {
        this.messageStatus = messageStatus;
        this.studentName = studentName;
        this.courseName = courseName;
        this.advisorDecision = advisorDecision;
    }

    /* Getter methods */
    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public AdvisorDecision getAdvisorDecision() {
        return advisorDecision;
    }

    /* Setter methods */
    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public void setAdvisorDecision(AdvisorDecision advisorDecision) {
        this.advisorDecision = advisorDecision;
    }
}
