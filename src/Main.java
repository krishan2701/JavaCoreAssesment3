import java.io.*;
import java.util.concurrent.locks.*;

public class Main {
    private static final double ALIGNMENT_THRESHOLD = 10.0; // Degrees
    private static final int ORBIT_ARRAKIS = 12; // LTU units
    private static final int ORBIT_GIEDI_PRIME = 60; // LTU units
    private static final int FULL_CIRCLE = 360; // Degrees
    private static final int MAX_INSTRUCTIONS = 10; // Limit of instructions

    private static Lock lock = new ReentrantLock();
    private static Condition alignmentCondition = lock.newCondition();

    private static volatile boolean planetsAligned = false;
    private static volatile boolean allInstructionsSent = false;

    public static void main(String[] args) throws InterruptedException {
        // Starting positions of the planets in degrees
        int initialPositionArrakis = 0; // Example: Start at 0 degrees
        int initialPositionGiedi = 0; // Example: Start at 0 degrees

        Thread baseStation = new Thread(new BaseStation(initialPositionArrakis, initialPositionGiedi));
        Thread responder = new Thread(new Responder());

        baseStation.start();
        responder.start();

        baseStation.join();
        responder.join();
    }

    static class BaseStation implements Runnable {
        private int positionArrakis;
        private int positionGiedi;
        private int instructionsSent = 0;

        public BaseStation(int initialPositionArrakis, int initialPositionGiedi) {
            this.positionArrakis = initialPositionArrakis;
            this.positionGiedi = initialPositionGiedi;
        }

        @Override
        public void run() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("trans.mxt"))) {
                int timeUnit = 0;

                while (instructionsSent < MAX_INSTRUCTIONS) {
                    lock.lock();
                    try {
                        updatePositions(timeUnit);

                        if (isAligned() && instructionsSent < MAX_INSTRUCTIONS) {
                            planetsAligned = true;
                            alignmentCondition.signal();
                            writer.write(">><<>>/"); // Example instruction
                            writer.newLine();
                            System.out.println("Instruction sent at time " + timeUnit);
                            instructionsSent++;
                        } else {
                            planetsAligned = false;
                        }
                    } finally {
                        lock.unlock();
                    }

                    Thread.sleep(100); // Simulate 1 LTU unit
                    timeUnit++;
                }
                lock.lock();
                try {
                    allInstructionsSent = true;
                    alignmentCondition.signalAll();
                } finally {
                    lock.unlock();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void updatePositions(int timeUnit) {
            positionArrakis = (positionArrakis + (FULL_CIRCLE / ORBIT_ARRAKIS)) % FULL_CIRCLE;
            positionGiedi = (positionGiedi + (FULL_CIRCLE / ORBIT_GIEDI_PRIME)) % FULL_CIRCLE;
        }

        private boolean isAligned() {
            int angleDifference = Math.abs(positionArrakis - positionGiedi);
            return angleDifference <= ALIGNMENT_THRESHOLD || angleDifference >= FULL_CIRCLE - ALIGNMENT_THRESHOLD;
        }
    }

    static class Responder implements Runnable {
        private int acknowledgmentsProcessed = 0;

        @Override
        public void run() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("recvrs.mxt"))) {
                while (acknowledgmentsProcessed < MAX_INSTRUCTIONS) {
                    lock.lock();
                    try {
                        while (!planetsAligned && !allInstructionsSent) {
                            alignmentCondition.await();
                        }

                        if (allInstructionsSent && acknowledgmentsProcessed >= MAX_INSTRUCTIONS) {
                            break;
                        }

                        if (planetsAligned) {
                            System.out.println("Processing instruction...");
                            Thread.sleep(100); // Simulate processing time
                            writer.write("/.<.<."); // Example acknowledgment
                            writer.newLine();
                            System.out.println("Acknowledgment sent.");
                            acknowledgmentsProcessed++;
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
