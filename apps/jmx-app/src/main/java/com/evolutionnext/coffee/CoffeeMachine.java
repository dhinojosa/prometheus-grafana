package com.evolutionnext.coffee;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CoffeeMachine implements CoffeeMachineMBean {

    private final AtomicLong ordersReceived   = new AtomicLong(0);
    private final AtomicLong ordersCompleted  = new AtomicLong(0);
    private final AtomicLong ordersFailed     = new AtomicLong(0);
    private final AtomicLong totalPrepTimeMs  = new AtomicLong(0);
    private final AtomicInteger beansRemaining  = new AtomicInteger(100);
    private final AtomicInteger waterRemaining  = new AtomicInteger(100);
    private final AtomicBoolean forceFailure    = new AtomicBoolean(false);

    private final BlockingQueue<String> orderQueue = new LinkedBlockingQueue<>();
    private volatile String lastDrinkPrepared = "None";
    private volatile String machineStatus     = "RUNNING";

    public CoffeeMachine() {
        startWorker();
    }

    public void acceptOrder(String drink) {
        ordersReceived.incrementAndGet();
        orderQueue.offer(drink);
    }

    private void startWorker() {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if ("PAUSED".equals(machineStatus)) {
                        Thread.sleep(500);
                        continue;
                    }

                    String drink = orderQueue.poll(1, TimeUnit.SECONDS);
                    if (drink == null) continue;

                    long start = System.currentTimeMillis();

                    if (forceFailure.getAndSet(false) || Math.random() < 0.10) {
                        ordersFailed.incrementAndGet();
                        machineStatus = "FAILED";
                        Thread.sleep(2000);
                        if (!"PAUSED".equals(machineStatus)) machineStatus = "RUNNING";
                        continue;
                    }

                    int prepTime = (int) (Math.random() * 3000) + 1000;
                    Thread.sleep(prepTime);

                    beansRemaining.updateAndGet(v -> Math.max(0, v - 1));
                    waterRemaining.updateAndGet(v -> Math.max(0, v - 1));
                    totalPrepTimeMs.addAndGet(System.currentTimeMillis() - start);
                    ordersCompleted.incrementAndGet();
                    lastDrinkPrepared = drink;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "coffee-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @Override public long getOrdersReceived()    { return ordersReceived.get(); }
    @Override public long getOrdersCompleted()   { return ordersCompleted.get(); }
    @Override public long getOrdersFailed()      { return ordersFailed.get(); }
    @Override public int  getCurrentQueueDepth() { return orderQueue.size(); }
    @Override public String getLastDrinkPrepared() { return lastDrinkPrepared; }
    @Override public String getMachineStatus()   { return machineStatus; }
    @Override public int getBeansRemaining()     { return beansRemaining.get(); }
    @Override public int getWaterRemaining()     { return waterRemaining.get(); }

    @Override
    public double getAveragePreparationTimeMs() {
        long completed = ordersCompleted.get();
        return completed == 0 ? 0.0 : (double) totalPrepTimeMs.get() / completed;
    }

    @Override public void pauseMachine()    { machineStatus = "PAUSED"; }
    @Override public void resumeMachine()   { if ("PAUSED".equals(machineStatus)) machineStatus = "RUNNING"; }
    @Override public void refillBeans()     { beansRemaining.set(100); }
    @Override public void refillWater()     { waterRemaining.set(100); }
    @Override public void simulateFailure() { forceFailure.set(true); }

    @Override
    public void resetStats() {
        ordersReceived.set(0);
        ordersCompleted.set(0);
        ordersFailed.set(0);
        totalPrepTimeMs.set(0);
        lastDrinkPrepared = "None";
    }
}
