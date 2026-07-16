package com.evolutionnext.coffee;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import jdk.jfr.consumer.RecordingStream;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;

public class CoffeeMachineSimulator {

    private static final List<String> DRINKS = List.of("Espresso", "Latte", "Cappuccino", "Americano");

    public static void main(String[] args) throws Exception {
        CoffeeMachine machine = new CoffeeMachine();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.evolutionnext.coffee:type=CoffeeMachine");
        mbs.registerMBean(machine, name);

        startJfrToPrometheusBridge();

        System.out.println("Coffee Machine Simulator started");
        System.out.println("MBean: " + name);
        System.out.println("Connect via JConsole or JDK Mission Control");
        System.out.println("JFR-derived JVM metrics: http://localhost:9406/metrics");
        System.out.println("─".repeat(95));

        while (true) {
            String drink = DRINKS.get((int) (Math.random() * DRINKS.size()));
            machine.acceptOrder(drink);

            System.out.printf("[%-7s] %-12s | recv: %3d  done: %3d  fail: %3d  queue: %2d  beans: %3d  water: %3d  avg: %5.0fms%n",
                    machine.getMachineStatus(),
                    drink,
                    machine.getOrdersReceived(),
                    machine.getOrdersCompleted(),
                    machine.getOrdersFailed(),
                    machine.getCurrentQueueDepth(),
                    machine.getBeansRemaining(),
                    machine.getWaterRemaining(),
                    machine.getAveragePreparationTimeMs());

            Thread.sleep((long) (Math.random() * 2000) + 1000);
        }
    }

    /**
     * Unlike JMX, JFR has no off-the-shelf Prometheus javaagent - the JVM streams
     * events for free, but turning them into metrics is on us. This subscribes to
     * two event types live, in-process, and pushes them into Prometheus metrics.
     */
    private static void startJfrToPrometheusBridge() throws Exception {
        Histogram gcPauseSeconds = Histogram.builder()
                .name("jvm_jfr_gc_pause_seconds")
                .help("GC pause duration, from JFR jdk.GarbageCollection events")
                .classicUpperBounds(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1)
                .register();

        Counter allocatedBytesTotal = Counter.builder()
                .name("jvm_jfr_allocated_bytes_total")
                .help("Bytes allocated, from JFR jdk.ObjectAllocationInNewTLAB events")
                .register();

        HTTPServer.builder().port(9406).buildAndStart();

        RecordingStream rs = new RecordingStream();
        rs.enable("jdk.GarbageCollection");
        rs.enable("jdk.ObjectAllocationInNewTLAB");

        rs.onEvent("jdk.GarbageCollection", event ->
                gcPauseSeconds.observe(event.getDuration().toNanos() / 1_000_000_000.0));

        rs.onEvent("jdk.ObjectAllocationInNewTLAB", event ->
                allocatedBytesTotal.inc(event.getLong("allocationSize")));

        rs.startAsync();
    }
}
