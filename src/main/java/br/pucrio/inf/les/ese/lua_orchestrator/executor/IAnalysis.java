package br.pucrio.inf.les.ese.lua_orchestrator.executor;

import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface IAnalysis {
    void perform(String num_messages,
                 List<Future<List>> futures,
                 ExecutorService executor,
                 Integer numberOfClients,
                 TopicStrategy topicStrategy) throws IOException, ExecutionException, InterruptedException;
}
