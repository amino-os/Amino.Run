package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author terryz
 */
public class SequenceGeneratorTest {

    @Test
    public void testGetSequenceInParallelWithStep1() throws Exception {
        long loopCnt = 100000L, step = 1L;
        final SequenceGenerator seqGen = SequenceGenerator.newBuilder().name("seq").startingAt(1).step(1).build();

        Assert.assertEquals(loopCnt*step, getMaxSequence(seqGen, loopCnt));
    }

    @Test
    public void testGetSequenceInParallelWithStep10() throws Exception {
        long loopCnt=100000L, step=10;
        final SequenceGenerator seqGen = SequenceGenerator.newBuilder().name("seq").startingAt(10).step(step).build();

        Assert.assertEquals(loopCnt*step, getMaxSequence(seqGen, loopCnt));
    }

    private long getMaxSequence(final SequenceGenerator seqGen, long loopCnt) throws Exception {
        int threadPoolSize = 100;
        long max = Long.MIN_VALUE;

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<Long>> futures = new ArrayList<Future<Long>>();

        for (int i=0; i<loopCnt; i++) {
            futures.add(executor.submit(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    return seqGen.getNextSequence();
                }
            }));
        }

        for (int i=0; i<loopCnt; i++) {
            max = Math.max(futures.get(i).get(), max);
        }

        return max;
    }
}