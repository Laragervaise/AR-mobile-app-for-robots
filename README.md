# android-urdfviewer

This **Android library** implements a **ROS** (Robot Operating System) node on an Android device
that display a 3D representation of a robot (using **OpenGL**).

The 3D representation of the robot is loaded from its **URDF description**.


The URDF parsing and rendering code is a modified version of the corresponding classes from the
[Rviz for Android](https://bitbucket.org/zimmrmn3/rviz-for-android/wiki/Home) project, originally
released under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).


## Examples

The library comes with two examples:

  * **opengl-example**: A basic OpenGL application that only display the robot and allows to rotate
  around it using one finger
  * **arcore-example**: An augmented-reality application in which the user can place robots in its
  environment by touching a location on the screen. This is a modified version of the example
  application *HelloAR* from [ARCore](https://developers.google.com/ar/).
  * **arengine-example**: An augmented-reality application in which the user can place robots in its
  environment by touching a location on the screen. This is a modified version of the example
  application *WorldAR_java* from
  [Huawei AR Engine](https://developer.huawei.com/consumer/en/doc/HUAWEI-AR-Guides/20302). Note that
  it requires a Huawei phone supporting the AR Engine, like a Mate 20 Pro.

You can compile and test them by opening this project in *Android Studio*.


## How to use it in your own application

In a nutshell (more detailed instructions are found below):

  * Add this repository as a *Git submodule* of your application
  * Import the *urdfviewer* module in your project in *Android Studio*
  * Modify your Gradle scripts
  * Modify your `Activity` class to inherit from `org.ros.android.RosActivity`, and implement the
  needed methods
  * In the appropriate parts of your code, load the 3D model of the robot and display it


### First: A note about ROS

You don't need to install a ROS development environment to compile the library. The necessary
dependencies are automatically downloaded by *Gradle* (the build system used by *Android Studio*).

But you'll need a running ROS master to connect to, and a node that publish the transforms of the
robot on the topics `/tf` and `/tf_static`.

Note that the [Android ROS library](http://wiki.ros.org/android) is still based on the *kinetic*
version of ROS (at the time of this writing).


### 1: Add this repository as a *Git submodule* of your application

In your application directory:

```sh
$ git submodule add git@gitlab.idiap.ch:rli/android-urdfviewer.git
```


### 2: Import the *urdfviewer* module in your project in *Android Studio*

In *Android Studio*:

1. Menu *File* > *Project Structure...*
2. Go to the *Modules* page
3. Click on the + button at the top of the modules list
4. Select *Import Gradle Project*
5. Select the `android-urdfviewer` submodule folder
6. (Optional) Deselect the examples (*opengl-example*, *arcore-example* and *arengine-example*),
only keep *urdfviewer*


### 3: Modify your Gradle scripts

In the `build.gradle` file of your project (the one in the root folder), replace:

```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.3.0'
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
```

by:

```
buildscript {
    apply from: "https://github.com/rosjava/android_core/raw/kinetic/buildscript.gradle"
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }
}

subprojects {
    apply plugin: 'ros-android'

    afterEvaluate { project ->
        android {
            // Exclude a few files that are duplicated across our dependencies and
            // prevent packaging Android applications.
            packagingOptions {
                exclude "META-INF/LICENSE.txt"
                exclude "META-INF/NOTICE.txt"
            }
        }
    }
}
```


In the `build.gradle` file of your application, add one line in the `dependencies` section:

```
dependencies {
	...

    implementation project(':urdfviewer')
}
```


At this point you should be able to compile the project.


### 4: Modify your `Activity` class to inherit from `org.ros.android.RosActivity`

Import the following packages:

```java
import ch.idiap.android.urdfviewer.UrdfViewer;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;
```

Inherit from `RosActivity`:

```java
public class MyActivity extends RosActivity {

    ...

}
```

Modify the constructor of your class (replace `MyApplication` by your application name):

```java
    public MyActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("MyApplication ", "MyApplication");
    }
```

Override the `org.ros.android.RosActivity#init()` method:

```java
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
    	// At this point, the user has already been prompted to enter the URI of a master to use
        UrdfViewer.startRos(nodeMainExecutor, getRosHostname(), getMasterUri());
    }
```

Your application is now a ROS node, that can connect to a master and listen to the `/tf` and
`/tf_static` topics.

At launch, the user will be asked to provide the URI of the master.


### 5: Load the 3D model of the robot

In your implementation of `android.opengl.GLSurfaceView.GLSurfaceView.Renderer#onSurfaceCreated()`:

```java
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    	...

        // Initialise the UrdfViewer API
        UrdfViewer.init(this);

        // Create the robot model
        robot = UrdfViewer.createPandaArm();

        ...

    }
```

**Note:** The OpenGL surface might be destroyed and recreated several times during the lifetime of
the application (for instance, when the current activity is changed, or the application goes in the
background). When this occurs, OpenGL releases all its resources. For this reason,
`UrdfViewer.init()` must be called each time the surface is recreated, and the models must be
loaded again (**after** the call to `UrdfViewer.init()`).

Moreover, `UrdfViewer.init()` expect a `android.content.Context` as a parameter. Use a reference to
your `Activity` (in the above example, extracted from the application *arcore-example*, this method
is implemented in the `Activity` class. See *opengl-example* for an example where the
`GLSurfaceView.Renderer` interface is implemented in a separate class).

At the moment, the only robot supported by this library is the
[Panda arm from Franka Emika](https://www.franka.de).


### 6: Draw the robot

In your implementation of `android.opengl.GLSurfaceView.GLSurfaceView.Renderer#onDrawFrame()`:

```java
    @Override
    public void onDrawFrame(GL10 unused) {

    	...

        // Use the view and projection matrices used by your OpenGL code for the visualization
        // of the robot models
        UrdfViewer.camera.setViewMatrix(viewMatrix);
        UrdfViewer.camera.getViewport().setProjectionMatrix(projectionMatrix);

        // Draw the robot
        robot.setTransforms(robotMatrix);
        robot.draw(RobotModel.DisplayMode.VISUAL_COMPONENT);

    	...

    }
```

See the examples for more details about the matrices used in that code snippet.


### 7: (Optional) Stream the video frames captured by the camera

Optionally, an image publisher node can be started, on the `/android_camera` topic.

After your call to `ch.idiap.android.urdfviewer.UrdfViewer#startRos()`, do:

```java
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        ...
        UrdfViewer.enableRGBImagePublishing(nodeMainExecutor);
    }
```

The **arcode-example** and **arengine-example** applications use this feature to stream
the video frames captured by the camera, like that:

```java
    @Override
    public void onDrawFrame(GL10 unused) {

        ...

        // Publish the current camera image if necessary
        if (UrdfViewer.imagePublisher != null) {
            Image image = frame.acquireCameraImage();
            int screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            UrdfViewer.imagePublisher.publish(image, screenOrientation);
        }

        ...

    }
```

Note that at the moment, only images in `YUV_420_888` format are supported (this is the default
format used by Android).


### 8: (Optional) Stream the depth frames captured by the camera

**Note: At time of writing, the AR Engine returns an invalid image, so this feature
can't be used!**

Optionally, a depth image publisher node can be started, on the `/android_depth_camera` topic.

After your call to `ch.idiap.android.urdfviewer.UrdfViewer#startRos()`, do:

```java
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        ...
        UrdfViewer.enableDepthImagePublishing(nodeMainExecutor);
    }
```

The **arengine-example** application use this feature to stream the depth frames captured by
the camera, like that:

```java
    @Override
    public void onDrawFrame(GL10 unused) {

        ...

        // Publish the current camera image if necessary
        if (UrdfViewer.imagePublisher != null) {
            Image image = frame.acquireDepthImage();
            int screenOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            UrdfViewer.imagePublisher.publish(image, screenOrientation);
        }

        ...

    }
```

Note that at the moment, only images in `DEPTH16` format are supported (this is the default
format used by Android).
