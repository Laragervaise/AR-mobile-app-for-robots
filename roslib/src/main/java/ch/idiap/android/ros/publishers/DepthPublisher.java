/*
 * Copyright (C) 2019 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

package ch.idiap.android.ros.publishers;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;
import android.view.Surface;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import std_msgs.MultiArrayDimension;
import std_msgs.MultiArrayLayout;
import std_msgs.UInt16MultiArray;


class DepthPublisherLoop extends CancellableLoop {

    public ByteBuffer srcBuffer = null;
    public int width = 0;
    public int height = 0;
    public int rowStride = 0;
    public int orientation = 0;

    private Publisher<std_msgs.UInt16MultiArray> publisher;

    private int currentWidth = 0;
    private int currentHeight = 0;
    short[] buffer = null;


    public DepthPublisherLoop(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher("android_depth_camera", UInt16MultiArray._TYPE);
    }


    @Override
    protected void loop() throws InterruptedException {
        ByteBuffer srcBuffer = null;
        int width = 0;
        int height = 0;
        int rowStride = 0;


        synchronized(this) {
            srcBuffer = this.srcBuffer;

            width = this.width;
            height = this.height;

            rowStride = this.rowStride;

            this.srcBuffer = null;
        }

        if (srcBuffer == null) {
            Thread.sleep(100);
            return;
        }

        if ((buffer == null) || (width != currentWidth) || (height != currentHeight)) {
            currentWidth = width;
            currentHeight = height;

            buffer = new short[currentWidth * currentHeight];
        }


        srcBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);


        // Rotate the image if necessary
        short[] rotatedBuffer = null;
        int dst;
        int i;

        switch (orientation) {
            case Surface.ROTATION_0:
                rotatedBuffer = new short[currentHeight * currentWidth];
                dst = 0;
                for (int j = 0; j < currentWidth; j++) {
                    int src = (currentHeight - 1) * currentWidth + j;

                    for (i = 0; i < currentHeight; i++) {
                        rotatedBuffer[dst] = buffer[src];
                        src -= currentWidth;
                        dst += 1;
                    }
                }
                break;

            case Surface.ROTATION_270:
                rotatedBuffer = new short[currentHeight * currentWidth];
                dst = 0;
                for (int j = 0; j < currentHeight; j++) {
                    int src =  (currentHeight - j) * currentWidth - 1;

                    for (i = 0; i < currentWidth; i++) {
                        rotatedBuffer[dst] = buffer[src];
                        src -= 1;
                        dst += 1;
                    }
                }
                break;

            default:
                break;
        }


        ArrayList<MultiArrayDimension> dimensions = new ArrayList<MultiArrayDimension>();

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
        MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();

        if (orientation == Surface.ROTATION_0) {
            MultiArrayDimension dim = messageFactory.newFromType(MultiArrayDimension._TYPE);

            dim.setLabel("height");
            dim.setSize(currentWidth);
            dim.setStride(currentWidth * currentHeight * 2);

            dimensions.add(dim);

            dim = messageFactory.newFromType(MultiArrayDimension._TYPE);

            dim.setLabel("width");
            dim.setSize(currentHeight);
            dim.setStride(currentHeight * 2);

            dimensions.add(dim);
        } else {
            MultiArrayDimension dim = messageFactory.newFromType(MultiArrayDimension._TYPE);

            dim.setLabel("height");
            dim.setSize(currentHeight);
            dim.setStride(currentWidth * currentHeight * 2);

            dimensions.add(dim);

            dim = messageFactory.newFromType(MultiArrayDimension._TYPE);

            dim.setLabel("width");
            dim.setSize(currentWidth);
            dim.setStride(currentWidth * 2);

            dimensions.add(dim);
        }

        std_msgs.UInt16MultiArray image = publisher.newMessage();

        MultiArrayLayout layout = image.getLayout();
        layout.setDim(dimensions);
        layout.setDataOffset(0);
        image.setLayout(layout);

        if (rotatedBuffer != null) {
            image.setData(rotatedBuffer);
        } else {
            image.setData(buffer);
        }


        publisher.publish(image);

        Thread.sleep(200);
    }

}


public class DepthPublisher extends AbstractNodeMain {

    private static final String TAG = DepthPublisher.class.getSimpleName();

    private DepthPublisherLoop loop = null;


    public void publish(Image depthImage, int orientation) {
        if (loop == null) {
            depthImage.close();
            return;
        }

        synchronized(loop) {
            if (loop.srcBuffer != null)
                loop.srcBuffer = null;

            try {
                if (depthImage.getFormat() != ImageFormat.DEPTH16) {
                    Log.e(TAG, "Invalid image format, only DEPTH16 is supported");
                    depthImage.close();
                    return;
                }

                Image.Plane[] planes = depthImage.getPlanes();
                ByteBuffer src = planes[0].getBuffer();

                loop.srcBuffer = clone(src);

                loop.width = depthImage.getWidth();
                loop.height = depthImage.getHeight();

                loop.rowStride = planes[0].getRowStride();

                loop.orientation = orientation;

            } catch (Throwable t) {
                Log.e(TAG, "Failed to process the image", t);
            }
        }
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("AndroidUrdfViewer/depth_publisher");
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {
        // This CancellableLoop will be canceled automatically when the node shuts down.
        loop = new DepthPublisherLoop(connectedNode);
        connectedNode.executeCancellableLoop(loop);
    }


    private static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();

        clone.put(original);
        clone.flip();

        return clone;
    }
}
