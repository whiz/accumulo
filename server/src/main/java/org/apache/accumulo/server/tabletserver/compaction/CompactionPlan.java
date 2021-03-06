/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.server.tabletserver.compaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.accumulo.server.fs.FileRef;

/**
 * A plan for a compaction: the input files, the files that are *not* inputs to a compaction that should
 * simply be deleted, and the optional parameters used to create the resulting output file.
 */
public class CompactionPlan {
  public final List<FileRef> inputFiles = new ArrayList<FileRef>();
  public final List<FileRef> deleteFiles = new ArrayList<FileRef>();
  public WriteParameters writeParameters = null;
  
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(inputFiles.toString());
    if (!deleteFiles.isEmpty()) { 
      b.append(" files to be deleted ");
      b.append(deleteFiles);
      if (writeParameters != null) {
        if (writeParameters.getCompressType() != null)
          b.append(" compress type " + writeParameters.getCompressType());
        if (writeParameters.getHdfsBlockSize() != 0)
          b.append(" hdfs block size " + writeParameters.getHdfsBlockSize());
        if (writeParameters.getBlockSize() != 0)
          b.append(" data block size " + writeParameters.getBlockSize());
        if (writeParameters.getIndexBlockSize() != 0)
          b.append(" index block size " + writeParameters.getIndexBlockSize());
        if (writeParameters.getReplication() != 0)
          b.append(" replication " + writeParameters.getReplication());
      }
    }
    return b.toString(); 
  }
}
