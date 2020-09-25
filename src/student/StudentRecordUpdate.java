/*
    Shreya Patel
    1001646429
*/
package student;

import common.StudentRecord;
import common.Utils.OperationType;
import java.util.*;
import java.io.Serializable;

/**
* Defines a common message format for passing student records
* from MQS to Advisor/Notification processes
*/
public class StudentRecordUpdate implements Serializable {
	/* Indicates operation being performed */
	private OperationType operationType;

	/* List of student record wrapper objects to pass information around */
	private List<StudentRecord> studentRecords;

	public StudentRecordUpdate(OperationType operationType, List<StudentRecord> studentRecords) {
		this.operationType = operationType;
		this.studentRecords = studentRecords;
	}

	/* Getter methods */
	public List<StudentRecord> getStudentRecords() {
		return studentRecords;
	}
}
