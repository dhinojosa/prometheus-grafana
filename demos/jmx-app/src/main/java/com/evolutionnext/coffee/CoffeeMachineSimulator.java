package com.evolutionnext.coffee;

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

        System.out.println("Coffee Machine Simulator started");
        System.out.println("MBean: " + name);
        System.out.println("Connect via JConsole or JDK Mission Control");
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
}
