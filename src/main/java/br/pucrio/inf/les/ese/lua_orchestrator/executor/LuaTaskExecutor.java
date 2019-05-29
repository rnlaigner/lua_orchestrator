package br.pucrio.inf.les.ese.lua_orchestrator.executor;

import br.pucrio.inf.les.ese.lua_orchestrator.exception.WrongNumberOfParams;
import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Report;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Workbook;
import br.pucrio.inf.les.ese.lua_orchestrator.report.WorkbookCreator;
import br.pucrio.inf.les.ese.lua_orchestrator.task.LuaTask;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class LuaTaskExecutor {

    final static Logger logger = Logger.getLogger(LuaTaskExecutor.class);

    public static void main( String[] args ) throws WrongNumberOfParams, InterruptedException, ExecutionException, IOException {

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

            sb.append(" ").append( port ).append(" ").append( num_messages ).append(" ").append( payload_size ).append(" ").append( wait_per_message ).append(" ");

            String paramsBase = sb.toString();

            Integer tail = 0;

            String currentTopic;

            for(int i = 0; i < numberOfClients; i++){

                Double r = Double.valueOf(i) % 10;

                if(r == 0 && i != 0){
                    tail++;
                }

                currentTopic = topic + "_" + tail;

                String params = paramsBase + currentTopic + " " + "client_" + i;

                LuaTask luaTask = new LuaTask(params);

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

        logger.info( "executor is down" );

        Map<String,List<Double>> client_average_time_map = new HashMap<>();

        //  realiza o calculo da media de tempo gasto pelo servidor e realizar o output do dado agregado

        for( Map.Entry<String,Map<String,List<String>>> client : clients_client_elapsed_time_map.entrySet() ){

            String client_name = client.getKey();

            List<Double> sum_of_client = new ArrayList<Double>() {{
                for(int i = 0; i < numberOfMsgs; i++){
                    add(0.0);
                }
            }};

            // lista que armazena o time do envio do cliente
            List<String> time_sent_by_client = client.getValue().get( client_name );

            // for each clients of client, get its respective time per message
            for( Map.Entry<String,Map<String,List<String>>> client_of_client : clients_client_elapsed_time_map.entrySet() ){

                if( client_of_client.getKey().contentEquals( client_name ) ){
                    continue;
                }

                List<String> times_for_client_of_client = client_of_client.getValue().get( client_name );

                // verifica se estao no mesmo topico
                if(times_for_client_of_client != null) {

                    int idx = 0;

                    for (String time : times_for_client_of_client) {

                        Double time_d = Double.valueOf(time) - Double.valueOf( time_sent_by_client.get(idx) );

                        Double currValue = sum_of_client.get(idx);

                        sum_of_client.set(idx, currValue + time_d);

                        idx++;

                    }

                }

            }

            // aqui faco a media para cada mensagem enviada pelo cliente atual da iteracao
            List<Double> average_time_list = new ArrayList<>(numberOfMsgs);

            Integer numberOfClientsForAvg = numberOfClients;

            // should consider strategy
            if (topicStrategy.equals(TopicStrategy.ONE_TOPIC_PER_TEN)){
                Integer r = numberOfClients / 10;
                numberOfClientsForAvg = r == 0 ? numberOfClients : numberOfClients / r;
            }

            for( int i = 0; i < numberOfMsgs; i++ ){
                Double value = sum_of_client.get(i) / (numberOfClientsForAvg - 1);
                average_time_list.add( i, value );
            }

            // adiciono em umap que possui o average time para cada mensagem enviada a partir deste cliente
            client_average_time_map.put( client_name, average_time_list );

        }

        System.out.println("Finished");

        WorkbookCreator workbookCreator = new WorkbookCreator();

        Report report = buildReport(  client_average_time_map  );

        workbookCreator.create( report, null );

    }

    private static Report buildReport( Map<String,List<Double>> results ) {

        Report report = new Report();

        Workbook workbook_1 = new Workbook();
        workbook_1.setName("Average per message");

        List<String> headers_1 = new ArrayList<String>() {
            {
                add("Client");
                add("Message Index");
                add("Average Time");
            }
        };
        workbook_1.setHeaders(headers_1);

        Workbook workbook_2 = new Workbook();
        workbook_2.setName("Total Average");

        List<String> headers_2 = new ArrayList<String>() {
            {
                add("Client");
                add("Total Average Time");
            }
        };
        workbook_2.setHeaders(headers_2);

        String client_name = "client_0";

        // me baseio pelos dados de um cliente... o primeiro!
        List<Double> client_average_time_list = results.get(client_name);

        for(int i = 0; i < client_average_time_list.size(); i++ ){

            //Mount line
            List<String> line = new ArrayList<String>();

            line.add( client_name );
            line.add(String.valueOf(i+1));
            line.add( String.valueOf(client_average_time_list.get(i)));

            workbook_1.addLine( line );

        }

        List<String> line = new ArrayList<String>();
        line.add(client_name);
        Double average_total = client_average_time_list.stream().mapToDouble(Double::doubleValue).sum() / client_average_time_list.size();
        line.add( String.valueOf( average_total ) );

        workbook_2.addLine(line);

        report.addWorkbook( workbook_1 );
        report.addWorkbook( workbook_2 );
        report.setName("inf2545");

        return report;

    }

}
