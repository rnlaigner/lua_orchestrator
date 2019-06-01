package br.pucrio.inf.les.ese.lua_orchestrator.executor;

import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Report;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Workbook;
import br.pucrio.inf.les.ese.lua_orchestrator.report.WorkbookCreator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class AnalysisPerMessage implements IAnalysis {

    final static Logger logger = Logger.getLogger(AnalysisPerMessage.class);

    @Override
    public void perform( String num_messages,
                                                  List<Future<List>> futures,
                                                  ExecutorService executor,
                                                  Integer numberOfClients,
                                                  TopicStrategy topicStrategy ) throws IOException, ExecutionException, InterruptedException {

        Map<String, Map<String, List<String>>> clients_client_elapsed_time_map = new HashMap<>();

        int numberOfMsgs = Integer.valueOf( num_messages );

        for( Future future : futures ){

            List<String> result = (List<String>) future.get();

            String client = null;
            try {
                client = result.get(0);
            }
            catch(IndexOutOfBoundsException e){
                executor.shutdown();
            }

            Map<String,List<String>> client_elapsed_time_map = new HashMap<>();

            for( int i = 1; i < result.size(); i = i + (numberOfMsgs * 3) ){

                String client_client = result.get(i);
                List<String> times = new ArrayList<>();

                for( int j = i; j < i + (numberOfMsgs * 3); j = j + 3){

                    try {
                        String time = result.get(j + 2);
                        times.add(time);
                    }
                    catch(Exception e){
                        logger.info("Exception");
                    }

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

                        Double time_d = 0.0;

                        try {
                            time_d = Double.valueOf(time) - Double.valueOf(time_sent_by_client.get(idx));
                        }
                        catch(Exception e){
                            logger.info("Exception");
                        }

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

        Report report = buildReport( client_average_time_map );

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

        // Pego um cliente randomly
        Integer num_clients = results.size();

        int randomNum = ThreadLocalRandom.current().nextInt(0, num_clients-1);

        String client_name = "client_" + randomNum;

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
