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
package org.apache.accumulo.test.functional;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.server.tabletserver.compaction.CompactionPlan;
import org.apache.accumulo.server.tabletserver.compaction.CompactionStrategy;
import org.apache.accumulo.server.tabletserver.compaction.MajorCompactionRequest;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurableCompactionIT extends ConfigurableMacIT {
  
  @Override
  public void configure(MiniAccumuloConfig cfg) {
    cfg.setSiteConfig(Collections.singletonMap(Property.TSERV_MAJC_DELAY.getKey(), "1s"));
  }
  
  public static class SimpleCompactionStrategy extends CompactionStrategy {
    
    @Override
    public void init(Map<String,String> options) {
      String countString = options.get("count");
      if (countString != null)
        count = Integer.parseInt(countString);
    }
    
    int count = 3;
    
    @Override
    public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
      return request.getFiles().size() == count;
      
    }
    
    @Override
    public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
      CompactionPlan result = new CompactionPlan();
      result.inputFiles.addAll(request.getFiles().keySet());
      return result;
    }
    
  }
  
  @Test
  public void test() throws Exception {
    final Connector c = getConnector();
    final String tableName = getTableNames(1)[0];
    c.tableOperations().create(tableName);
    c.tableOperations().setProperty(tableName, Property.TABLE_COMPACTION_STRATEGY.getKey(), SimpleCompactionStrategy.class.getName());
    runTest(c, tableName, 3);
    c.tableOperations().setProperty(tableName, Property.TABLE_COMPACTION_STRATEGY_PREFIX.getKey() + "count", "" + 5);
    runTest(c, tableName, 5);
  }
  
  final static Random r = new Random();
  
  private void makeFile(Connector conn, String tablename) throws Exception {
    BatchWriter bw = conn.createBatchWriter(tablename, new BatchWriterConfig());
    byte[] empty = {};
    byte[] row = new byte[10]; 
    r.nextBytes(row);
    Mutation m = new Mutation(row, 0, 10);
    m.put(empty, empty, empty);
    bw.addMutation(m);
    bw.flush();
    bw.close();
    conn.tableOperations().flush(tablename, null, null, true);
  }
  
  private void runTest(final Connector c, final String tableName, final int n) throws Exception {
    for (int i = countFiles(c, tableName); i < n - 1; i++)
      makeFile(c, tableName);
    Assert.assertEquals(n - 1, countFiles(c, tableName));
    makeFile(c, tableName);
    for (int i = 0; i < 10; i++) {
      int count = countFiles(c, tableName);
      assertTrue(count == 1 || count == n);
      if (count == 1)
        break;
      UtilWaitThread.sleep(1000);
    }
  }
  
  private int countFiles(Connector c, String tableName) throws Exception {
    Scanner s = c.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
    s.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);
    int count = 0;
    for (@SuppressWarnings("unused") Entry<Key,Value> entry : s) {
      count++;
    }
    return count;
  }
  
}
