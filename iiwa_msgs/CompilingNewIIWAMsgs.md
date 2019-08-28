# Compiling a New Version of IIWA Messages
---

*NOTE: This setup has screwed up my workspace on occasion, so it is
recommended that you do this in a "throwaway workspace", just in case...*

## ROS Setup

### From Packages

The following ROS packages need to be installed to compile the new version of
IIWA messages:

* `genjava`
* *TODO:Figure out what else...*


### `rosjava_messages` Setup

1. In your workspace, clone the repository at
https://github.com/rosjava/rosjava_messages.git
1. In the workspace folder, run
`rosdep install --from-paths rosjava_messages --ignore-src -r y`
1. Edit the `CMakeLists.txt` file in the `rosjava_messages` folder. Add
`iiwa_msgs` to the `generate_rosjava_messages` command.
1. Edit the `packages.xml` file, and add
`<build_depend>iiwa_msgs</build_depend>` to the list of `build_depend` entries.
1. Edit the file `/opt/ros/kinetic/lib/python-2.7/dist-packages/genjava/templates/genjava_project/build.grade.in`.  Around lines 73-74, there should be 2 lines
commented out, so it looks something like this:
```java
// sourceCompatability = 1.6
// targetCompatability = 1.6
```
Uncomment the lines so it looks like this
```java
sourceCompatability = 1.6
targetCompatability = 1.6
```

## Building Packages

Once you have made the changes necessary to the `iiwa_stack/iiwa_msgs` folder,
first update the `version` tag to a higher version.  Then build the system as
normal, using `catkin build rosjava_messages`.  This should build the iiwa_msgs
Java package.  The resulting JAR file will be found in the
`<workspace>/build/iiwa_msgs/java/iiwa_msgs/build/lib`, named
`iiwa_msgs-<version>.jar`.  Typically this file should be copied into your
`iiwa_stack/iiwa_ros_java/ROSJavaLib` folder, the old version removed, and the
repo committed so that people building software for the robot can use it.
