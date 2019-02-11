package de.tum.in.camp.kuka.ros;


import javax.inject.Inject;

import org.ros.node.topic.Publisher;

import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPIBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;

/**
 * Implementation of a background task.
 * <p>
 * The background task provides a {@link RoboticsAPIBackgroundTask#initialize()} 
 * and a {@link RoboticsAPIBackgroundTask#run()} method, which will be called 
 * successively in the task lifecycle.<br>
 * The task will terminate automatically after the <code>run</code> method has 
 * finished or after stopping the task.
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b>
 * @see UseRoboticsAPIContext
 * 
 */
public class ROSStatePublisher extends RoboticsAPIBackgroundTask {
	@Inject
	private Controller kUKA_Sunrise_Cabinet_1;
	private LBR robot;
	
	private Publisher<sensor_msgs.JointState> jointStatesPublisher;
	private sensor_msgs.JointState js;
	
	private iiwaMessageGenerator helper;

	@Override
	public void initialize() {
		robot = getContext().getDeviceFromType(LBR.class);
		helper = new iiwaMessageGenerator("iiwa");
		js = helper.buildMessage(sensor_msgs.JointState._TYPE);
	}

	@Override
	public void run() {
		// your task execution starts here
	}
	
	public void publish() {
		if (jointStatesPublisher.getNumberOfSubscribers() > 0) {
			helper.getCurrentJointState(js, robot);
			helper.incrementSeqNumber(js.getHeader());
			jointStatesPublisher.publish(js);
		}
	}
}