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

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.concurrent.CancellableLoop;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBuffers;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.nio.ByteBuffer;



class ImagePublisherLoop extends CancellableLoop {

    public ByteBuffer srcY = null;
    public ByteBuffer srcU = null;
    public ByteBuffer srcV = null;
    public int width = 0;
    public int height = 0;
    public int rowStrideY = 0;
    public int rowStrideU = 0;
    public int rowStrideV = 0;
    public int orientation = 0;

    private Publisher<sensor_msgs.Image> publisher;

    private ChannelBufferOutputStream stream;
    private int currentWidth = 0;
    private int currentHeight = 0;
    byte[] rgbBuffer = null;


    public ImagePublisherLoop(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher("android_camera", sensor_msgs.Image._TYPE);
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }


    @Override
    protected void loop() throws InterruptedException {
        ByteBuffer srcY = null;
        ByteBuffer srcU = null;
        ByteBuffer srcV = null;
        int width = 0;
        int height = 0;
        int rowStrideY = 0;
        int rowStrideU = 0;
        int rowStrideV = 0;


        synchronized(this) {
            srcY = this.srcY;
            srcU = this.srcU;
            srcV = this.srcV;

            width = this.width;
            height = this.height;

            rowStrideY = this.rowStrideY;
            rowStrideU = this.rowStrideU;
            rowStrideV = this.rowStrideV;

            this.srcY = null;
            this.srcU = null;
            this.srcV = null;
        }

        if (srcY == null) {
            Thread.sleep(100);
            return;
        }

        if ((rgbBuffer == null) || (width != currentWidth) || (height != currentHeight)) {
            currentWidth = width;
            currentHeight = height;

            rgbBuffer = new byte[currentWidth * currentHeight * 3];
        }

        // Note: For some reason, srcU contains the U and V planes interleaved MINUS the last V byte
        // and srcV contains the V and U planes interleaved MINUS the last U byte.
        // This, the code below treat them as separate planes and offset the reading of each U/V
        // value accordingly.
        int offsetY = 0;
        int offsetU = 0;
        int offsetV = 0;
        int offsetDst = 0;

        int r, g, b, y298, y, i, u, v;
        for (int j = 0; j < currentHeight; j++) {
            srcY.position(offsetY);
            srcU.position(offsetU);
            srcV.position(offsetV);

            u = 0;
            v = 0;

            for (i = 0; i < currentWidth; i++) {
                y = (0xff & ((int) srcY.get())) - 16;
                if (y < 0)
                    y = 0;

                if ((i & 1) == 0) {
                    u = (0xff & srcU.get()) - 128;
                    v = (0xff & srcV.get()) - 128;
                }
                else if ((j < currentHeight / 2 - 1) || (i < currentWidth - 1))
                {
                    srcU.get();
                    srcV.get();
                }

                y298 = 298 * y;
                r = (y298 + 409 * v + 128) >> 8;
                g = (y298 - 100 * u - 208 * v + 128) >> 8;
                b = (y298 + 516 * u + 128) >> 8;

                r = Math.max(0, Math.min(r, 255));
                g = Math.max(0, Math.min(g, 255));
                b = Math.max(0, Math.min(b, 255));

                rgbBuffer[offsetDst] = (byte) r;
                rgbBuffer[offsetDst + 1] = (byte) g;
                rgbBuffer[offsetDst + 2] = (byte) b;

                offsetDst += 3;
            }

            offsetY += rowStrideY;

            if ((j & 1) == 1) {
                offsetU += rowStrideU;
                offsetV += rowStrideV;
            }
        }

        // Rotate the image if necessary
        byte[] rotatedRgbBuffer = null;
        int dst;

        switch (orientation) {
            case Surface.ROTATION_0:
                rotatedRgbBuffer = new byte[currentHeight * currentWidth * 3];
                dst = 0;
                for (int j = 0; j < currentWidth; j++) {
                    int src = (currentHeight - 1) * currentWidth * 3 + j * 3;

                    for (i = 0; i < currentHeight; i++) {
                        rotatedRgbBuffer[dst] = rgbBuffer[src];
                        rotatedRgbBuffer[dst + 1] = rgbBuffer[src + 1];
                        rotatedRgbBuffer[dst + 2] = rgbBuffer[src + 2];
                        src -= currentWidth * 3;
                        dst += 3;
                    }
                }
                break;

            case Surface.ROTATION_270:
                rotatedRgbBuffer = new byte[currentHeight * currentWidth * 3];
                dst = 0;
                for (int j = 0; j < currentHeight; j++) {
                    int src =  (currentHeight - j) * currentWidth * 3 - 3;

                    for (i = 0; i < currentWidth; i++) {
                        rotatedRgbBuffer[dst] = rgbBuffer[src];
                        rotatedRgbBuffer[dst + 1] = rgbBuffer[src + 1];
                        rotatedRgbBuffer[dst + 2] = rgbBuffer[src + 2];
                        src -= 3;
                        dst += 3;
                    }
                }
                break;

            default:
                break;
        }


        sensor_msgs.Image image = publisher.newMessage();
        image.setEncoding("rgb8");

        if (orientation == Surface.ROTATION_0) {
            image.setWidth(currentHeight);
            image.setHeight(currentWidth);
        } else {
            image.setWidth(currentWidth);
            image.setHeight(currentHeight);
        }

        try {
            if (rotatedRgbBuffer != null) {
                stream.write(rotatedRgbBuffer);
            } else {
                stream.write(rgbBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RosRuntimeException(e);
        }

        image.setData(stream.buffer().copy());
        stream.buffer().clear();

        publisher.publish(image);

        Thread.sleep(200);
    }

}


public class ImagePublisher extends AbstractNodeMain {

    private static final String TAG = ImagePublisher.class.getSimpleName();

    private ImagePublisherLoop loop = null;


    public void publish(Image yuvImage, int orientation) {
        if (loop == null) {
            yuvImage.close();
            return;
        }

        synchronized(loop) {
            if (loop.srcY != null) {
                loop.srcY = null;
                loop.srcU = null;
                loop.srcV = null;
            }

            try {
                if (yuvImage.getFormat() != ImageFormat.YUV_420_888) {
                    Log.e(TAG, "Invalid image format, only YUV_420_888 is supported");
                    yuvImage.close();
                    return;
                }

                Image.Plane[] planes = yuvImage.getPlanes();
                ByteBuffer srcY = planes[0].getBuffer();
                ByteBuffer srcU = planes[1].getBuffer();
                ByteBuffer srcV = planes[2].getBuffer();

                loop.srcY = clone(srcY);
                loop.srcU = clone(srcU);
                loop.srcV = clone(srcV);

                loop.width = yuvImage.getWidth();
                loop.height = yuvImage.getHeight();

                loop.rowStrideY = planes[0].getRowStride();
                loop.rowStrideU = planes[1].getRowStride();
                loop.rowStrideV = planes[2].getRowStride();

                loop.orientation = orientation;

            } catch (Throwable t) {
                Log.e(TAG, "Failed to process the image", t);
            }
        }
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("AndroidUrdfViewer/image_publisher");
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {
        // This CancellableLoop will be canceled automatically when the node shuts down.
        loop = new ImagePublisherLoop(connectedNode);
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
