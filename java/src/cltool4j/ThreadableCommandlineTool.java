/*
 INTEL CONFIDENTIAL
 Copyright 2014 Intel Corporation All Rights Reserved.
 The source code contained or described herein and all documents related to the
 source code ('Material') are owned by Intel Corporation or its suppliers or
 licensors. Title to the Material remains with Intel Corporation or its
 suppliers and licensors. The Material contains trade secrets and proprietary
 and confidential information of Intel or its suppliers and licensors. The
 Material is protected by worldwide copyright and trade secret laws and treaty
 provisions. No part of the Material may be used, copied, reproduced, modified,
 published, uploaded, posted, transmitted, distributed, or disclosed in any way
 without Intel's prior express written permission.
 
 No license under any patent, copyright, trade secret or other intellectual
 property right is granted to or conferred upon you by disclosure or delivery
 of the Materials, either expressly, by implication, inducement, estoppel or
 otherwise. Any license under such intellectual property rights must be express
 and approved by Intel in writing.
 */
package cltool4j;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implements threading, and queuing such that output remains in input order. Processes input from files or
 * STDIN, optionally using multiple threads. Subclasses must implement a {@link FutureTask} task to do the
 * processing.
 * 
 * @author Aaron Dunlop
 * 
 * @param <I> Input type. Generally a String (for a single line) or a List<String> (for a batch of input
 *            lines)
 * @param <R> Type produced by processing of each input line
 */
@Threadable
public abstract class ThreadableCommandlineTool<I, R> extends BaseCommandlineTool {

    // A simple marker denoting the end of input lines.
    protected final FutureTask<R> END_OF_INPUT_MARKER = new FutureTask<R>(new Callable<R>() {
        @Override
        public R call() throws Exception {
            return null;
        }
    });

    /**
     * Note: Implementations of this method should usually be synchronized
     * 
     * @return The next item of input (usually a single line or a List of lines)
     * @throws IOException If the read fails
     */
    protected abstract I nextInput() throws IOException;

    @Override
    public final void run() throws Exception {

        if (maxThreads == 1) {
            // Single-threaded version is simple...
            for (I input = nextInput(); input != null; input = nextInput()) {
                final FutureTask<R> task = task(input);
                task.run();
                output(task.get());
            }
        } else {
            // For the multi-threaded version, we need to create a separate thread which will
            // collect the output and spit it out in-order

            // Allocate a queue large enough to contain several pending tasks for each thread, but small
            // enough to avoid attempting to schedule all future jobs at once.
            final BlockingQueue<FutureTask<R>> outputQueue = new LinkedBlockingQueue<FutureTask<R>>(
                    maxThreads * 4);
            final OutputThread outputThread = new OutputThread(outputQueue);
            outputThread.start();

            final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            for (I input = nextInput(); input != null; input = nextInput()) {
                final FutureTask<R> task = task(input);
                outputQueue.put(task);
                executor.execute(task);
            }

            // Enqueue a marker
            outputQueue.put(END_OF_INPUT_MARKER);

            // The output thread will exit when it comes to the termination marker
            outputThread.join();
            executor.shutdown();
        }
    }

    /**
     * @return a {@link FutureTask} which will process an element of input and return a {@link FutureTask} as
     *         output.
     */
    protected abstract FutureTask<R> task(I input);

    /**
     * Outputs the result to STDOUT
     * 
     * @param result Result to write
     */
    protected void output(final R result) {
        final String s = result.toString();
        if (s.length() > 0) {
            System.out.println(s);
            System.out.flush();
        }
    }

    private class OutputThread extends Thread {

        private final BlockingQueue<FutureTask<R>> queue;

        public OutputThread(final BlockingQueue<FutureTask<R>> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final FutureTask<R> task = queue.take();
                    if (task == END_OF_INPUT_MARKER) {
                        return;
                    }
                    output(task.get());
                } catch (final InterruptedException ignore) {
                } catch (final ExecutionException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
