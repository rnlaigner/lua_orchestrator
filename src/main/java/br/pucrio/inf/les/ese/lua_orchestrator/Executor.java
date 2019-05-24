package br.pucrio.inf.les.ese.lua_orchestrator;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Executor {

    private final int BUFFER_SIZE;

    private final int CONSUMER_SIZE;

    public Executor(List<String> repositoryURLS, int bufferSize, int consumerSize) {
        this.BUFFER_SIZE = bufferSize;
        this.CONSUMER_SIZE = consumerSize;
    }

    public void execute() throws ExecutionException
    {
        // create new thread pool
        ExecutorService application = Executors.newCachedThreadPool();

        // create BlockingBuffer to store ints
        Buffer sharedLocation = new Buffer(BUFFER_SIZE);

        for(int i = 0; i < 10; i++)
        {
            try
            {
                sharedLocation.set(null);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                throw new ExecutionException(e.getMessage(), e.getCause());
            }
        }

        for(int i = 0; i < CONSUMER_SIZE; i++)
        {
            application.execute( new Consumer( sharedLocation ) );
        }

        application.shutdown();
    }

}