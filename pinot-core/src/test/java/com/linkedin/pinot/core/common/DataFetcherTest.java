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
package com.linkedin.pinot.core.common;

import com.linkedin.pinot.common.data.DimensionFieldSpec;
import com.linkedin.pinot.common.data.FieldSpec;
import com.linkedin.pinot.common.data.MetricFieldSpec;
import com.linkedin.pinot.common.data.Schema;
import com.linkedin.pinot.common.segment.ReadMode;
import com.linkedin.pinot.core.data.GenericRow;
import com.linkedin.pinot.core.indexsegment.IndexSegment;
import com.linkedin.pinot.core.indexsegment.generator.SegmentGeneratorConfig;
import com.linkedin.pinot.core.segment.creator.impl.SegmentIndexCreationDriverImpl;
import com.linkedin.pinot.core.segment.index.loader.Loaders;
import com.linkedin.pinot.util.TestDataRecordReader;
import java.io.File;
import java.util.HashMap;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class DataFetcherTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataFetcherTest.class);

  private static final String SEGMENT_NAME = "dataFetcherTestSegment";
  private static final String INDEX_DIR_PATH = FileUtils.getTempDirectoryPath() + File.separator + SEGMENT_NAME;
  private static final int NUM_ROWS = 1000;
  private static final String DIMENSION_NAME = "dimension";
  private static final String INT_METRIC_NAME = "int_metric";
  private static final String LONG_METRIC_NAME = "long_metric";
  private static final String FLOAT_METRIC_NAME = "float_metric";
  private static final String DOUBLE_METRIC_NAME = "double_metric";
  private static final int MAX_STEP_LENGTH = 5;

  private final long _randomSeed = System.currentTimeMillis();
  private final Random _random = new Random(_randomSeed);
  private final String[] _dimensionValues = new String[NUM_ROWS];
  private final int[] _intMetricValues = new int[NUM_ROWS];
  private final long[] _longMetricValues = new long[NUM_ROWS];
  private final float[] _floatMetricValues = new float[NUM_ROWS];
  private final double[] _doubleMetricValues = new double[NUM_ROWS];
  private DataFetcher _dataFetcher;

  @BeforeClass
  private void setup() throws Exception {
    GenericRow[] segmentData = new GenericRow[NUM_ROWS];

    // Generate random dimension and metric values.
    for (int i = 0; i < NUM_ROWS; i++) {
      double randomDouble = _random.nextDouble();
      String randomDoubleString = String.valueOf(randomDouble);
      _dimensionValues[i] = randomDoubleString;
      _intMetricValues[i] = (int) randomDouble;
      _longMetricValues[i] = (long) randomDouble;
      _floatMetricValues[i] = (float) randomDouble;
      _doubleMetricValues[i] = randomDouble;
      HashMap<String, Object> map = new HashMap<>();
      map.put(DIMENSION_NAME, _dimensionValues[i]);
      map.put(INT_METRIC_NAME, _intMetricValues[i]);
      map.put(LONG_METRIC_NAME, _longMetricValues[i]);
      map.put(FLOAT_METRIC_NAME, _floatMetricValues[i]);
      map.put(DOUBLE_METRIC_NAME, _doubleMetricValues[i]);
      GenericRow genericRow = new GenericRow();
      genericRow.init(map);
      segmentData[i] = genericRow;
    }

    // Create an index segment with the random dimension and metric values.
    final Schema schema = new Schema();
    schema.addField(DIMENSION_NAME, new DimensionFieldSpec(DIMENSION_NAME, FieldSpec.DataType.STRING, true));
    schema.addField(INT_METRIC_NAME, new MetricFieldSpec(INT_METRIC_NAME, FieldSpec.DataType.INT));
    schema.addField(LONG_METRIC_NAME, new MetricFieldSpec(LONG_METRIC_NAME, FieldSpec.DataType.LONG));
    schema.addField(FLOAT_METRIC_NAME, new MetricFieldSpec(FLOAT_METRIC_NAME, FieldSpec.DataType.FLOAT));
    schema.addField(DOUBLE_METRIC_NAME, new MetricFieldSpec(DOUBLE_METRIC_NAME, FieldSpec.DataType.DOUBLE));

    SegmentGeneratorConfig config = new SegmentGeneratorConfig(schema);
    config.setOutDir(INDEX_DIR_PATH);
    config.setSegmentName(SEGMENT_NAME);

    SegmentIndexCreationDriverImpl driver = new SegmentIndexCreationDriverImpl();
    driver.init(config, new TestDataRecordReader(schema, segmentData));
    driver.build();

    IndexSegment indexSegment = Loaders.IndexSegment.load(new File(INDEX_DIR_PATH, SEGMENT_NAME), ReadMode.heap);

    // Get a data fetcher for the index segment.
    _dataFetcher = new DataFetcher(indexSegment);
  }

  @Test
  public void testFetchSingleIntValues() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    int[] intValues = new int[length];

    _dataFetcher.fetchSingleDictIds(INT_METRIC_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleIntValues(INT_METRIC_NAME, dictIds, 0, length, intValues, 0);

    for (int i = 0; i < length; i++) {
      if (intValues[i] != _intMetricValues[docIds[i]]) {
        LOGGER.error("For index {}, int value does not match: fetched value: {}, expected value: {}", i, intValues[i],
            _intMetricValues[docIds[i]]);
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }

  @Test
  public void testFetchSingleLongValues() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    long[] longValues = new long[length];

    _dataFetcher.fetchSingleDictIds(LONG_METRIC_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleLongValues(LONG_METRIC_NAME, dictIds, 0, length, longValues, 0);

    for (int i = 0; i < length; i++) {
      if (longValues[i] != _longMetricValues[docIds[i]]) {
        LOGGER.error("For index {}, long value does not match: fetched value: {}, expected value: {}", i, longValues[i],
            _longMetricValues[docIds[i]]);
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }

  @Test
  public void testFetchSingleFloatValues() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    float[] floatValues = new float[length];

    _dataFetcher.fetchSingleDictIds(FLOAT_METRIC_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleFloatValues(FLOAT_METRIC_NAME, dictIds, 0, length, floatValues, 0);

    for (int i = 0; i < length; i++) {
      if (floatValues[i] != _floatMetricValues[docIds[i]]) {
        LOGGER.error("For index {}, float value does not match: fetched value: {}, expected value: {}", i,
            floatValues[i], _floatMetricValues[docIds[i]]);
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }

  @Test
  public void testFetchSingleDoubleValues() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    double[] doubleValues = new double[length];

    _dataFetcher.fetchSingleDictIds(DOUBLE_METRIC_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleDoubleValues(DOUBLE_METRIC_NAME, dictIds, 0, length, doubleValues, 0);

    for (int i = 0; i < length; i++) {
      if (doubleValues[i] != _doubleMetricValues[docIds[i]]) {
        LOGGER.error("For index {}, double value does not match: fetched value: {}, expected value: {}", i,
            doubleValues[i], _doubleMetricValues[docIds[i]]);
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }

  @Test
  public void testFetchSingleStringValues() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    String[] stringValues = new String[length];

    _dataFetcher.fetchSingleDictIds(DIMENSION_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleStringValues(DIMENSION_NAME, dictIds, 0, length, stringValues, 0);

    for (int i = 0; i < length; i++) {
      if (!stringValues[i].equals(_dimensionValues[docIds[i]])) {
        LOGGER.error("For index {}, string value does not match: fetched value: {}, expected value: {}", i,
            stringValues[i], _dimensionValues[docIds[i]]);
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }

  @Test
  public void testFetchSingleHashCodes() {
    int[] docIds = new int[NUM_ROWS];
    int length = 0;
    for (int i = _random.nextInt(MAX_STEP_LENGTH); i < NUM_ROWS; i += _random.nextInt(MAX_STEP_LENGTH) + 1) {
      docIds[length++] = i;
    }

    int[] dictIds = new int[length];
    double[] hashCodes = new double[length];

    _dataFetcher.fetchSingleDictIds(DIMENSION_NAME, docIds, 0, length, dictIds, 0);
    _dataFetcher.fetchSingleHashCodes(DIMENSION_NAME, dictIds, 0, length, hashCodes, 0);

    for (int i = 0; i < length; i++) {
      if (hashCodes[i] != _dimensionValues[docIds[i]].hashCode()) {
        LOGGER.error("For index {}, hash code does not match: fetched value: {}, expected value: {}", i,
            hashCodes[i], _dimensionValues[docIds[i]].hashCode());
        LOGGER.error("Random Seed: {}", _randomSeed);
        Assert.fail();
      }
    }
  }
}
