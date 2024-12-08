import java.util.concurrent.locks.*;

public class Main {
    private static final int MAX_INSTRUCTIONS = 10; // Limit of instructions

    private static Lock lock = new ReentrantLock();
    private static Condition alignmentCondition = lock.newCondition();

    static volatile boolean planetsAligned = false;
    static volatile boolean allInstructionsSent = false;

    public static void main(String[] args) throws InterruptedException {
        // Starting positions of the planets in degrees
        int initialPositionArrakis = 0; // Example: Start at 0 degrees
        int initialPositionGiedi = 0; // Example: Start at 0 degrees

        Thread baseStation = new Thread(new BaseStation(initialPositionArrakis, initialPositionGiedi, lock, alignmentCondition));
        Thread responder = new Thread(new Responder(lock, alignmentCondition));

        baseStation.start();
        responder.start();

        baseStation.join();
        responder.join();
    }
}
