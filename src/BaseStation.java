import java.io.*;
import java.util.concurrent.locks.*;

public class BaseStation implements Runnable {
    private static final double ALIGNMENT_THRESHOLD = 10.0; // Degrees
    private static final int ORBIT_ARRAKIS = 12; // LTU units
    private static final int ORBIT_GIEDI_PRIME = 60; // LTU units
    private static final int FULL_CIRCLE = 360; // Degrees
    private static final int MAX_INSTRUCTIONS = 10; // Limit of instructions

    int positionArrakis;
    int positionGiedi;
    int instructionsSent = 0;

    private Lock lock;
    private Condition alignmentCondition;

    public BaseStation(int initialPositionArrakis, int initialPositionGiedi, Lock lock, Condition alignmentCondition) {
        this.positionArrakis = initialPositionArrakis;
        this.positionGiedi = initialPositionGiedi;
        this.lock = lock;
        this.alignmentCondition = alignmentCondition;
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
                        Main.planetsAligned = true;
                        alignmentCondition.signal();
                        writer.write(">><<>>/"); // Example instruction
                        writer.newLine();
                        System.out.println("Instruction sent at time " + timeUnit);
                        instructionsSent++;
                    } else {
                        Main.planetsAligned = false;
                    }
                } finally {
                    lock.unlock();
                }

                Thread.sleep(100); // Simulate 1 LTU unit
                timeUnit++;
            }
            lock.lock();
            try {
                Main.allInstructionsSent = true;
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
