package br.pucrio.inf.les.ese.lua_orchestrator;

import br.pucrio.inf.les.ese.lua_orchestrator.exception.WrongNumberOfParams;
import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;
import br.pucrio.inf.les.ese.lua_orchestrator.task.LuaTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        List<Callable<List>> luaTasks = new ArrayList<Callable<List>>();

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

        List<Future<List>> futures = executor.invokeAll( luaTasks );

        Map<String, Map<String,List<String>>> clients_client_elapsed_time_map = new HashMap<>();

        int numberOfMsgs = Integer.valueOf( num_messages );

        for( Future future : futures ){

            List<String> result = (List<String>) future.get();

            String client = result.get(0);

            Map<String,List<String>> client_elapsed_time_map = new HashMap<>();

            for( int i = 1; i < result.size(); i = i + (numberOfMsgs * 3) ){

                String client_client = result.get(i);
                List<String> times = new ArrayList<>();

                for( int j = i; j < i + (numberOfMsgs * 3); j = j + 3){

                    String time = result.get(j+2);
                    times.add(time);

                }

                client_elapsed_time_map.put( client_client, times );

            }

            clients_client_elapsed_time_map.put( client,  client_elapsed_time_map );

        }

        executor.shutdown();

        Map<String,List<Double>> client_average_time_map = new HashMap<>();

        //  realiza o calculo da media de tempo gasto pelo servidor e realizar o output do dado agregado

        for( Map.Entry<String,Map<String,List<String>>> client : clients_client_elapsed_time_map.entrySet() ){

            String client_name = client.getKey();

            // Map<String,List<String>> client_of_client_map = client.getValue();

            List<Double> sum_of_client = new ArrayList<Double>() {{
                for(int i = 0; i < numberOfMsgs; i++){
                    add(0.0);
                }
            }};

            // for each clients of client, get its respective time per message
            for( Map.Entry<String,Map<String,List<String>>> client_of_client : clients_client_elapsed_time_map.entrySet() ){

                if( client_of_client.getKey().contentEquals( client_name ) ){
                    continue;
                }

                List<String> times_for_client_of_client = client_of_client.getValue().get( client_name );

                int idx = 0;

                for( String time : times_for_client_of_client ){

                    Double time_d = Double.valueOf( time );

                    Double currValue = sum_of_client.get( idx );

                    sum_of_client.set( idx, currValue + time_d );

                    idx++;

                }

            }

            // aqui faco a media para cada mensagem enviada pelo cliente atual da iteracao
            List<Double> average_time_list = new ArrayList<>(numberOfMsgs);

            for( int i = 0; i < numberOfMsgs; i++ ){

                Double value = sum_of_client.get(i) / (numberOfClients - 1);

                average_time_list.add( i, value );

            }

            // adiciono em umap que possui o average time para cada mensagem enviada a partir deste cliente
            client_average_time_map.put( client_name, average_time_list );

        }

        System.out.println("Finished");

    }

}
