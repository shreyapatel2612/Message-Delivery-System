/*
    Shreya Patel
    1001646429
*/
package common;

import common.Utils.AdvisorDecision;
import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author Shreya Patel
 */
/**
 * Wrapper class to hold advisor decision about all courses for given student
 */
public class AdvisorUpdateForStudent implements Serializable{
     /* Name of the student for which this update is */
    private String studentName;

    /* Mapping from course name to advisor decision */
    private Map<String, AdvisorDecision> advisorDecisions;

    public AdvisorUpdateForStudent(String studentName, Map<String, AdvisorDecision> advisorDecisions) {
        this.studentName = studentName;
        this.advisorDecisions = advisorDecisions;
    }
}
