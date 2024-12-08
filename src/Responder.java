import java.io.*;
import java.util.concurrent.locks.*;

public class Responder implements Runnable {
    private static final int MAX_INSTRUCTIONS = 10; // Limit of instructions

    private int acknowledgmentsProcessed = 0;

    private Lock lock;
    private Condition alignmentCondition;

    public Responder(Lock lock, Condition alignmentCondition) {
        this.lock = lock;
        this.alignmentCondition = alignmentCondition;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("recvrs.mxt"))) {
            while (acknowledgmentsProcessed < MAX_INSTRUCTIONS) {
                lock.lock();
                try {
                    while (!Main.planetsAligned && !Main.allInstructionsSent) {
                        alignmentCondition.await();
                    }

                    if (Main.allInstructionsSent && acknowledgmentsProcessed >= MAX_INSTRUCTIONS) {
                        break;
                    }

                    if (Main.planetsAligned) {
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
