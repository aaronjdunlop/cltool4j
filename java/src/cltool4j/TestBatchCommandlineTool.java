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

import static junit.framework.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.concurrent.FutureTask;

import org.junit.Test;

/**
 * Unit tests for {@link LinewiseCommandlineTool}. Mostly duplicated from {@link TestLinewiseCommandlineTool}.
 * Note that this tests the threading functionality in {@link ThreadableCommandlineTool}.
 */
public class TestBatchCommandlineTool extends ToolTestCase {

    /**
     * Tests ordering of multithreaded output, verifying that the lines are returned in the order read even
     * when multiple concurrent threads are processing the file (and sleeping for random intervals).
     * 
     * @throws Exception if an error occurs while executing the tool
     */
    @Test
    public void testBatchCat() throws Exception {
        final String filename = "simple.txt";
        final String expectedOutput = ToolTestCase.unitTestFileAsString(filename);
        // First, single-threaded
        assertEquals(expectedOutput, executeToolFromFile(new BatchCat(), "-xt 1 -batch 10", filename));

        // And now a 2-thread run
        assertEquals(expectedOutput, executeToolFromFile(new BatchCat(), "-xt 2 -batch 3", filename));

        // Finally, 8 threads
        assertEquals(expectedOutput, executeToolFromFile(new BatchCat(), "-xt 8 -batch 5", filename));
    }

    /**
     * Outputs each line as-is.
     */
    private static class BatchCat extends BatchCommandlineTool {
        private final static Random random = new Random();

        @Override
        protected FutureTask<List<String>> task(final List<String> input) {
            return new FutureTask<List<String>>(new Runnable() {
                public void run() {
                    try {
                        final int interval = random.nextInt(100);
                        Thread.sleep(interval);
                    } catch (final InterruptedException ignore) {
                    }
                }
            }, input);
        }
    }
}
