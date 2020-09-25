/*
    Shreya Patel
    1001646429
*/
package mqs;

import common.Utils.SenderProcess;
import common.Utils.OperationType;
import common.StudentRecord;
import java.util.*;
import java.io.Serializable;

/**
* Defines a common message format for all incoming messages to MQS
*/
public class MqsRequest implements Serializable{
    /* Indicates process which is sending request */
	private SenderProcess senderProcess;

	/* Indicates operation being performed */
	private OperationType operationType;

	/* List of student record wrapper objects to pass information around */
	private List<StudentRecord> studentRecordList;

	public MqsRequest(
		SenderProcess senderProcess,
		OperationType operationType,
		List<StudentRecord> studentRecordList) {
	    this.senderProcess = senderProcess;
	    this.operationType = operationType;
	    this.studentRecordList = studentRecordList;
	}

	/* Getter methods */
	public SenderProcess getSenderProcess() {
		return senderProcess;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public List<StudentRecord> getStudentRecordList() {
		return studentRecordList;
	}
}
