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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cltool4j.args4j.Option;

/**
 * Threadable tool which processes input in batches (e.g. each task handled 50 lines)
 */
public abstract class BatchCommandlineTool extends ThreadableCommandlineTool<List<String>, List<String>> {

    @Option(name = "-batch", metaVar = "lines", usage = "Batch size")
    private int batchSize = 25;

    private BufferedReader inputReader;

    @Override
    public final List<String> nextInput() throws IOException {
        synchronized (this) {
            if (inputReader == null) {
                inputReader = inputAsBufferedReader();
            }
        }

        // Read in a batch of lines
        synchronized (inputReader) {
            final ArrayList<String> input = new ArrayList<String>(batchSize);
            for (int i = 0; i < batchSize; i++) {
                final String line = inputReader.readLine();
                if (line == null) {
                    // We hit EOF; return the batch we have
                    return input.size() > 0 ? input : null;
                }
                input.add(line);
            }
            return input;
        }
    }

    @Override
    protected final void output(final List<String> result) {
        for (final String s : result) {
            if (s.length() > 0) {
                System.out.println(s);
            }
        }
        System.out.flush();
    }
}
