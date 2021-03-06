// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache License 2.0 license. See LICENSE file in Ultrabrew Metrics
// for terms.

package io.ultrabrew.metrics.dropwizard;

import com.codahale.metrics.Slf4jReporter;
import io.ultrabrew.metrics.Counter;
import io.ultrabrew.metrics.Gauge;
import io.ultrabrew.metrics.MetricRegistry;
import io.ultrabrew.metrics.Timer;
import io.ultrabrew.metrics.reporters.SLF4JReporter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(50)
@State(Scope.Benchmark)
public class TagsBenchmark {

  private static final int CONSUME_CPU = 512;

  private static final String TAGNAME = "tagName";
  private static final String DROPWIZARD_COUNTER_TAGNAME_PREFIX = "counter.tagName.";
  private static final String DROPWIZARD_TIMER_TAGNAME_PREFIX = "timer.tagName.";
  private static final String DROPWIZARD_GAUGE_TAGNAME_PREFIX = "gauge.tagName.";

  private MetricRegistry foundationRegistry;
  private Counter foundationCounter;
  private Timer foundationTimer;
  private Gauge foundationGauge;
  private SLF4JReporter foundationReporter;

  private com.codahale.metrics.MetricRegistry dropwizardRegistry;
  private Slf4jReporter dropwizardReporter;

  private volatile long value = 0L;
  private volatile long tagValue = 0L;

  @Param({"1", "10", "100"})
  private int cardinality;

  @Setup
  public void setup() {
    foundationRegistry = new MetricRegistry();
    foundationCounter = foundationRegistry.counter("counter");
    foundationTimer = foundationRegistry.timer("timer");
    foundationGauge = foundationRegistry.gauge("gauge");
    foundationReporter = new SLF4JReporter("foundation");
    foundationRegistry.addReporter(foundationReporter);

    dropwizardRegistry = new com.codahale.metrics.MetricRegistry();
    dropwizardReporter = Slf4jReporter.forRegistry(dropwizardRegistry)
        .outputTo(LoggerFactory.getLogger("dropwizard"))
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.NANOSECONDS)
        .build();
  }

  @Benchmark
  public void counterFoundation() {
    foundationCounter.inc(TAGNAME, String.valueOf(tagValue++ % cardinality));
    Blackhole.consumeCPU(CONSUME_CPU);
  }

  @Benchmark
  public void counterDropwizard() {
    final com.codahale.metrics.Counter counter =
        dropwizardRegistry
            .counter(DROPWIZARD_COUNTER_TAGNAME_PREFIX + String.valueOf(tagValue++ % cardinality));
    counter.inc();
    Blackhole.consumeCPU(CONSUME_CPU);
  }

  @Benchmark
  public void timerFoundation() {
    final long startTime = foundationTimer.start();
    Blackhole.consumeCPU(CONSUME_CPU);
    foundationTimer.stop(startTime, TAGNAME, String.valueOf(tagValue++ % cardinality));
  }

  @Benchmark
  public void timerDropwizard() {
    final com.codahale.metrics.Timer timer =
        dropwizardRegistry
            .timer(DROPWIZARD_TIMER_TAGNAME_PREFIX + String.valueOf(tagValue++ % cardinality));
    final com.codahale.metrics.Timer.Context context = timer.time();
    Blackhole.consumeCPU(CONSUME_CPU);
    context.stop();
  }

  @Benchmark
  public void gaugeFoundation() {
    foundationGauge.set(value++ % 100L, TAGNAME, String.valueOf(tagValue++ % cardinality));
    Blackhole.consumeCPU(CONSUME_CPU);
  }

  @Benchmark
  public void gaugeDropwizard() {
    final com.codahale.metrics.Histogram histogram =
        dropwizardRegistry
            .histogram(DROPWIZARD_GAUGE_TAGNAME_PREFIX + String.valueOf(tagValue++ % cardinality));

    histogram.update(value++ % 100L);
    Blackhole.consumeCPU(CONSUME_CPU);
  }
}
