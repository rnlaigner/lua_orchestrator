package br.pucrio.inf.les.ese.lua_orchestrator;

import br.pucrio.inf.les.ese.lua_orchestrator.exception.WrongNumberOfParams;
import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LuaTaskExecutor {

    public static void main( String[] args ) throws WrongNumberOfParams, InterruptedException, ExecutionException {

        //  ip do servidor mosquitto

        //  port do servidor mosquitto

        //  number of clients

        //  topicStrategy: ONE_TOPIC, ONE_TOPIC_PER_TEN

        //  payload size

        //  wait per message

        if(args.length < 8){
            throw new WrongNumberOfParams("<server_address> <server_port> <number_clients> <number_messages> <topic_strategy> <payload_size> <wait_per_message> <topic>");
        }

        String ip = args[0];

        String port = args[1];

        String num_clients = args[2];

        String num_messages = args[3];

        String strategy = args[4];

        String payload_size = args[5];

        String wait_per_message = args[6];

        String topic = args[7];

        int numberOfClients = Integer.valueOf( num_clients );

        List<Callable<String>> luaTasks = new ArrayList<Callable<String>>();

        TopicStrategy topicStrategy = TopicStrategy.ONE_TOPIC.getName().contentEquals( strategy ) ? TopicStrategy.ONE_TOPIC : TopicStrategy.ONE_TOPIC_PER_TEN ;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);

        if ( topicStrategy.equals(TopicStrategy.ONE_TOPIC) ){

            StringBuilder sb = new StringBuilder( ip );

            sb.append(" ").append( port ).append(" ").append( num_messages ).append(" ").append( payload_size )
            .append(" ").append( wait_per_message ).append(" ").append( topic ).append(" ");

            String paramsBase = sb.toString();

            for(int i = 0; i < numberOfClients; i++){

                String params = paramsBase + "client_" + i;

                LuaTask luaTask = new LuaTask(params);

                //executor.execute( luaTask );
                luaTasks.add( luaTask );

            }

        } else {

            StringBuilder sb = new StringBuilder( ip );

            sb.append(" ").append( port ).append(" ").append(" ").append( num_messages ).append( payload_size ).append( wait_per_message ).append(" ");

            String paramsBase = sb.toString();

            String currentTopic = topic + "_0";

            for(int i = 1; i <= numberOfClients; i++){

                Double r = Double.valueOf(i) % 10;

                if ( r == 0 ){
                    currentTopic = topic + "_" + r.intValue();
                }

                String params = paramsBase + currentTopic + " " + "client_" + i;

                LuaTask luaTask = new LuaTask(params);

                //executor.execute( luaTask );
                luaTasks.add( luaTask );

            }

        }

        List<Future<String>> futures = executor.invokeAll( luaTasks );

        for( Future future : futures ){

            future.get();

        }

        executor.shutdown();

        //boolean tasksEnded = executor.awaitTermination(1, TimeUnit.HOURS );
        // boolean tasksEnded = executor.awaitTermination( 3, TimeUnit.MINUTES );

//        while( ! executor.isTerminated() ){
//
//        }
//        boolean tasksEnded = true;
//
//        if ( tasksEnded ) {
//
//            // TODO todos os tasks devem colocar seus respectivos outputs em uma tabela
//
//            // assim, eu posso colocar a tabela em um excel e realizar os calculos
//
//            System.out.println(""); // print contents
//        }
//        else {
//            System.out.println("Timed out while waiting for tasks to finish.");
//        }

    }

}
