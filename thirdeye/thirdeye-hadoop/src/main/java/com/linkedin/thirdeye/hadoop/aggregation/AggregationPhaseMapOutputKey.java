/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.thirdeye.hadoop.aggregation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for the key generated by mapper in Aggregation
 */
public class AggregationPhaseMapOutputKey {

  private long time;
  private List<String> dimensions;

  public AggregationPhaseMapOutputKey(long time, List<String> dimensions) {
    this.time = time;
    this.dimensions = dimensions;
  }

  public long getTime() {
    return time;
  }

  public List<String> getDimensions() {
    return dimensions;
  }

  public byte[] toBytes() throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    byte[] bytes;

    // time
    dos.writeLong(time);

    // dimensions size
    dos.writeInt(dimensions.size());
    // dimension values
    for (String dimension : dimensions) {
      bytes = dimension.getBytes();
      dos.writeInt(bytes.length);
      dos.write(bytes);
    }

    baos.close();
    dos.close();
    return baos.toByteArray();
  }

  public static AggregationPhaseMapOutputKey fromBytes(byte[] buffer) throws IOException {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer));
    int length;
    int size;
    List<String> dimensions = new ArrayList<>();
    byte[] bytes;

    // time
    long time = dis.readLong();

    // dimensions size
    size = dis.readInt();

    // dimension value
    for (int i = 0; i < size; i++) {
      length = dis.readInt();
      bytes = new byte[length];
      dis.read(bytes);
      dimensions.add(new String(bytes));
    }

    AggregationPhaseMapOutputKey wrapper;
    wrapper = new AggregationPhaseMapOutputKey(time, dimensions);
    return wrapper;
  }

}