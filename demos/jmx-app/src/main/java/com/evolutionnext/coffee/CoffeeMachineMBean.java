package com.evolutionnext.coffee;

public interface CoffeeMachineMBean {

    // Attributes
    long getOrdersReceived();
    long getOrdersCompleted();
    long getOrdersFailed();
    int getCurrentQueueDepth();
    double getAveragePreparationTimeMs();
    String getLastDrinkPrepared();
    String getMachineStatus();
    int getBeansRemaining();
    int getWaterRemaining();

    // Operations
    void pauseMachine();
    void resumeMachine();
    void refillBeans();
    void refillWater();
    void resetStats();
    void simulateFailure();
}
