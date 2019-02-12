package de.tum.in.camp.kuka.ros;


import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.time.NtpTimeProvider;

import com.kuka.connectivity.motionModel.smartServo.SmartServo;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplicationState;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPIBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.task.ITaskManager;

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

    private NodeMainExecutor nodeMainExecutor;
	private NodeConfiguration nodeConfConfiguration;
    private NodeConfiguration nodeConfPublisher;

    private iiwaConfiguration config;
    private iiwaJointPublisher publisher;

    private boolean initialized = false;
    private boolean running = true;

    //@Override
    public void initialize() {
        robot = getContext().getDeviceFromType(LBR.class);

        config = new iiwaConfiguration();
        publisher = new iiwaJointPublisher(config.getRobotName());

        // ROS initialization.
        try {
            URI uri = new URI(iiwaConfiguration.getMasterURI());
            
			nodeConfConfiguration = NodeConfiguration.newPublic(iiwaConfiguration.getRobotIp());
			nodeConfConfiguration.setTimeProvider(iiwaConfiguration.getTimeProvider());
			nodeConfConfiguration.setNodeName(iiwaConfiguration.getRobotName() + "/iiwa_configuration");
			nodeConfConfiguration.setMasterUri(uri);

            nodeConfPublisher = NodeConfiguration.newPublic(iiwaConfiguration.getRobotIp());
            nodeConfPublisher.setTimeProvider(iiwaConfiguration.getTimeProvider());
            nodeConfPublisher.setNodeName(iiwaConfiguration.getRobotName() + "/joint_state_publisher");
            nodeConfPublisher.setMasterUri(uri);
        }
        catch (Exception e) {
            getLogger().info("Node Configuration failed. " + "Please check the ROS master IP in the Sunrise configuration.");
            getLogger().info(e.toString());
            return;
        }

        try {
            // Start the Publisher node with the set up configuration.
            nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
            nodeMainExecutor.execute(config, nodeConfConfiguration);
            nodeMainExecutor.execute(publisher, nodeConfPublisher);

            getLogger().info("ROS Node Executor initialized.");
        }
        catch(Exception e) {
            getLogger().info("ROS Node Executor initialization failed.");
            getLogger().info(e.toString());
            return;
        }

        initialized = true;  // We cannot throw here.
    }

    @Override
    public void run() {
        if (! initialized) {
            throw new RuntimeException("Could not init the RoboticApplication successfully");
        }

        try {
            getLogger().info("Waiting for ROS Master to connect... ");
            config.waitForInitialization();
            getLogger().info("ROS Master is connected!");
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return;
        }

        getLogger().info("Using time provider: " + iiwaConfiguration.getTimeProvider().getClass().getSimpleName());

        if (iiwaConfiguration.getTimeProvider() instanceof org.ros.time.NtpTimeProvider) {
            ((NtpTimeProvider) iiwaConfiguration.getTimeProvider()).startPeriodicUpdates(100, TimeUnit.MILLISECONDS); // TODO: update time as param
        }

        // The run loop
        getLogger().info("Starting the ROS publishing loop...");
        try {
        	double pubFreq = config.getJointStatePublishFreq();
            while(running) {
                publisher.publishCurrentState(robot);
                Thread.sleep((long) (1000.0 / pubFreq));
            }
        }
        catch (Exception e) {
            getLogger().info("ROS publishing loop aborted" + "" +
            		         " (" + e.toString() + ") :" + e.getMessage());
        } finally {
            getLogger().info("ROS publishing loop has ended. Shutting down...");
        	cleanup();
        }
        getLogger().info("Application finished");
    }
    
    @Override
    public void dispose() {
    	try {
    		cleanup();
    	}
    	catch (Exception e) {
    		getLogger().error("Shutdown error: " + e.getMessage());
    	}
    	finally {
    		super.dispose();
    	}
    }

    /*
    @Override
	public void onApplicationStateChanged(RoboticsAPIApplicationState state) {
		if (state == RoboticsAPIApplicationState.STOPPING) {
			running = false;
		}
		super.onApplicationStateChanged(state);
	};
	*/
	
	void cleanup() {
		running = false;
		if (nodeMainExecutor != null) {
			getLogger().info("Stopping ROS nodes");
			nodeMainExecutor.shutdown();	
			nodeMainExecutor.getScheduledExecutorService().shutdownNow();
		}
		getLogger().info("Stopped ROS nodes");
	}
}
