package edu.uiowa.tsz.pedometer;

import android.hardware.Sensor;
import android.os.Process;
import android.util.Log;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Created by tony on 9/25/16.
 */
public class SensorConsumer extends Thread {
    private final String TAG = "SensorConsumer";

    // Reference to file to write to
    private File fh;

    private final int BUFFER_QUEUE_CAPACITY = 15;
    private final BlockingQueue<SensorReading> queue;
    private CircularFifoQueue<SensorReading> bufferQueue;

    SensorConsumer(BlockingQueue q, File f) {
        this.queue = q;
        bufferQueue = new CircularFifoQueue(BUFFER_QUEUE_CAPACITY);
        this.fh = f;
    }

    @Override
    public void run() {

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);           // The documentation tells me to do this. Not sure if its the best priority setting for what we're doing...
        Log.i(TAG + ":Consumer", "READY TO CONSUME");

        // Try to consume, thread should block if no data is available to get.
        // Blocking might happen automatically. It might not. Who nose?
        while (true) {
            try {
                // pull data off BlockingQueue
                SensorReading val = queue.take();
                Log.i(TAG + ":Consumer", "consumed -> (time: " + val.toString() + ")");

                // push it onto the BufferQueue
                bufferQueue.add(val);

                // Check bufferQueue capacity. If it is full, spin up a DataWriter Thread that
                // writes the contents of bufferQueue, and then wipe bufferQueue so more things
                // can be written to it.
                if (bufferQueue.isAtFullCapacity()) {
                    SensorReading[] bufferData = bufferQueue.toArray(new SensorReading[0]);
                    bufferQueue.clear();
                    new DataWriter(bufferData, fh).run();
                }
            } catch (InterruptedException e) {
                Log.i(TAG + ":Consumer", "Interrupted!");
                e.printStackTrace();
            }
        }
    }

}
