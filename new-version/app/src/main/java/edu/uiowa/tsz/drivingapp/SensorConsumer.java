package edu.uiowa.tsz.drivingapp;

import android.os.Process;
import android.util.Log;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.File;
import java.util.concurrent.BlockingQueue;

/**
 * Consumes sensor data.
 * Update: now consumes Datum objects!
 */
public class SensorConsumer extends Thread {

    private final String TAG = "SensorConsumer";

    // Reference to file to write to
    private File fh;

    private final int BUFFER_QUEUE_CAPACITY = 15;
    private final BlockingQueue<Datum> queue;
    private CircularFifoQueue<Datum> bufferQueue;

    SensorConsumer(BlockingQueue q, File f) {
        this.queue = q;
        bufferQueue = new CircularFifoQueue(BUFFER_QUEUE_CAPACITY);
        this.fh = f;
    }

    @Override
    public void run() {

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);           // The documentation tells me to do this. Not sure if its the best priority setting for what we're doing...
        Log.i(TAG, "READY TO CONSUME");

        // Try to consume, thread should block if no data is available to get.
        // Blocking might happen automatically. It might not. Who nose?
        while (true) {
            try {
                // pull data off BlockingQueue
                Datum d = queue.take();
                Log.i(TAG, "consumed -> (time: " + d.toString() + ")");

                // push it onto the BufferQueue
                bufferQueue.add(d);

                // Check bufferQueue capacity. If it is full, spin up a DataWriter Thread that
                // writes the contents of bufferQueue, and then wipe bufferQueue so more things
                // can be written to it.
                if (bufferQueue.isAtFullCapacity()) {
                    Datum[] bufferData = bufferQueue.toArray(new Datum[0]);
                    bufferQueue.clear();
                    new DataWriter(bufferData, fh).run();
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Interrupted!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Call to force all buffered data to 'fh' regardless of current queue capacity.
     * Consumer will ignore this call if no data exists on the buffer queue.
     */
    public void forceDumpToFile() {
        if (!bufferQueue.isEmpty()) {
            Log.v(TAG, "Manual write triggered! Writing " + bufferQueue.size() + " elements to '" + fh.getName() + "'");
            Datum[] bufferData = bufferQueue.toArray(new Datum[0]);
            bufferQueue.clear();
            new DataWriter(bufferData, fh).run();
        } else {
            Log.v(TAG, "Manual write command ignored -- buffer queue for file '" + fh.getName() + "' is empty.");
        }
    }
}
