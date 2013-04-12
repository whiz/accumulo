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
package org.apache.accumulo.test.randomwalk.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.test.randomwalk.State;
import org.apache.accumulo.test.randomwalk.Test;
import org.apache.hadoop.io.Text;

public class Merge extends Test {
  
  @Override
  public void visit(State state, Properties props) throws Exception {
    Connector conn = state.getConnector();
    
    Random rand = (Random) state.get("rand");
    
    @SuppressWarnings("unchecked")
    List<String> tableNames = (List<String>) state.get("tables");
    tableNames = new ArrayList<String>(tableNames);
    tableNames.add(Constants.METADATA_TABLE_NAME);
    String tableName = tableNames.get(rand.nextInt(tableNames.size()));
    
    // TODO need to sometimes do null start and end ranges
    
    TreeSet<Text> range = new TreeSet<Text>();
    range.add(new Text(String.format("%016x", rand.nextLong() & 0x7fffffffffffffffl)));
    range.add(new Text(String.format("%016x", rand.nextLong() & 0x7fffffffffffffffl)));
    
    try {
      conn.tableOperations().merge(tableName, range.first(), range.last());
      log.debug("merged " + tableName);
    } catch (TableOfflineException toe) {
      log.debug("merge " + tableName + " failed, table is not online");
    } catch (TableNotFoundException tne) {
      log.debug("merge " + tableName + " failed, doesnt exist");
    }
    
  }
}