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
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPIBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

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
    private iiwaConfiguration config;

    private NodeMainExecutor nodeMainExecutor;
    private NodeConfiguration nodeConfPublisher;
    private iiwaJointPublisher publisher;

    @Override
    public void initialize() {
        robot = getContext().getDeviceFromType(LBR.class);

        config = new iiwaConfiguration();
        publisher = new iiwaJointPublisher(config.getRobotName());

        // ROS initialization.
        try {
            URI uri = new URI(iiwaConfiguration.getMasterURI());

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
            nodeMainExecutor.execute(publisher, nodeConfPublisher);

            getLogger().info("ROS Node Executor initialized.");
        }
        catch(Exception e) {
            getLogger().info("ROS Node Executor initialization failed.");
            getLogger().info(e.toString());
            return;
        }

        //initSuccessful = true;  // We cannot throw here.
    }

    @Override
    public void run() {
        //if (!initSuccessful) {
        //    throw new RuntimeException("Could not init the RoboticApplication successfully");
        //}

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
        boolean running = true;
        try {
            while(running) {
                // This will publish the current robot state on the various ROS topics.
                publisher.publishCurrentState(robot);
            }
        }
        catch (Exception e) {
            getLogger().info("ROS control loop aborted. " + e.toString());
        } finally {
            getLogger().info("ROS control loop has ended. Application terminated.");
        }
    }
}
