package sapphire.policy.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Easier to manage timer, as utils.Timer is dumbish
 */
public class ResettableTimer {
    private TimerTask templateTask; // We need to create a new instance each time.
    private TimerTask task;
    private long delay;
    private Timer timer; // Delegate to the dumb timer.

    /**
     * Constructor - does not actually start the timer.  Call start() for that.
     * @param templateTask What to run
     * @param delay How long to wait after start before running it.
     */
    public ResettableTimer(TimerTask templateTask, long delay) {
        this.delay = delay;
        this.templateTask = templateTask;
        this.task = null;
    }

    /**
     * Start the timer.
     */
    public void start() {
        // this.cancel();
        this.timer = new Timer();
        this.task = new TimerTask() { public void run() { templateTask.run();}};
        timer.schedule(task, delay);
    }

    /**
     *  Reset the timer.
     */
    public void reset() {
        this.cancel();
        this.start();
    }

    /**
     * Cancel the timer until run again.
     */
    public void cancel() {
        if (this.timer!=null) {
            this.timer.cancel();
            this.timer.purge();
        }
        if (this.task != null){
            this.task.cancel();
            this.task = null;
        }
    }
}
