package br.pucrio.inf.les.ese.lua_orchestrator.executor;

import br.pucrio.inf.les.ese.lua_orchestrator.exception.WrongNumberOfParams;
import br.pucrio.inf.les.ese.lua_orchestrator.queue.TopicStrategy;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Report;
import br.pucrio.inf.les.ese.lua_orchestrator.report.Workbook;
import br.pucrio.inf.les.ese.lua_orchestrator.report.WorkbookCreator;
import br.pucrio.inf.les.ese.lua_orchestrator.task.LuaTask;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
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
        if (args.length < 8) {
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

        int numberOfClients = Integer.valueOf(num_clients);

        List<Callable> luaTasks = new ArrayList<Callable>();

        TopicStrategy topicStrategy = TopicStrategy.ONE_TOPIC.getName().contentEquals(strategy) ? TopicStrategy.ONE_TOPIC : TopicStrategy.ONE_TOPIC_PER_TEN;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);

        if (topicStrategy.equals(TopicStrategy.ONE_TOPIC)) {

            StringBuilder sb = new StringBuilder(ip);

            sb.append(" ").append(port).append(" ").append(num_clients).append(" ").append(num_messages)
                    .append(" ").append(payload_size).append(" ").append(wait_per_message).append(" ").append(topic).append(" ");

            String paramsBase = sb.toString();

            for (int i = 0; i < numberOfClients; i++) {

                String params = paramsBase + "client" + i;

                LuaTask luaTask = new LuaTask(params);

                luaTasks.add(luaTask);

            }

        } else {

            StringBuilder sb = new StringBuilder(ip);

            sb.append(" ").append(port).append(" ").append(num_clients).append(" ").append(num_messages)
                    .append(" ").append(payload_size).append(" ").append(wait_per_message).append(" ");

            String paramsBase = sb.toString();

            Integer tail = 0;

            String currentTopic;

            for (int i = 0; i < numberOfClients; i++) {

                Double r = Double.valueOf(i) % 10;

                if (r == 0 && i != 0) {
                    tail++;
                }

                currentTopic = topic + "_" + tail;

                String params = paramsBase + currentTopic + " " + "client" + i;

                LuaTask luaTask = new LuaTask(params);

                luaTasks.add(luaTask);

            }

        }

        // https://github.com/eclipse/mosquitto/issues/1177
        //List<Future<List>> futures = executor.invokeAll( luaTasks );
        List<Future<List>> futures = new ArrayList<>();

        for (Callable task : luaTasks) {
            futures.add(executor.submit(task));
            // avoid issue on mosquitto assigning uniqueID based on system time
            Thread.sleep(500);
        }

        // Analysis per client time elapsed

        Map<String, List<String>> client_elapsed_time_map = new HashMap<>();

        for( Future future : futures ) {

            List<String> result = (List<String>) future.get();

            String client, start_time, finish_time, elapsed_time = null;
            try {
                client = result.get(0);
                start_time = result.get(1);
                finish_time = result.get(2);
                elapsed_time = result.get(3);

                client_elapsed_time_map.put( client, Arrays.asList(start_time, finish_time, elapsed_time) );

            } catch (IndexOutOfBoundsException e) {
                executor.shutdown();
            }

        }

        executor.shutdown();

        System.out.println("Finished");

        WorkbookCreator workbookCreator = new WorkbookCreator();

        Report report = buildReport( client_elapsed_time_map );

        workbookCreator.create( report, null );


    }

    private static Report buildReport( Map<String, List<String>> results ) {

        Report report = new Report();

        Workbook workbook_1 = new Workbook();
        workbook_1.setName("Time Elapsed Per Client");

        List<String> headers_1 = new ArrayList<String>() {
            {
                add("Client");
                add("Start Time");
                add("Finish Time");
                add("Elapsed Time");
            }
        };
        workbook_1.setHeaders(headers_1);

        Workbook workbook_2 = new Workbook();
        workbook_2.setName("Total Average Time");

        List<String> headers_2 = new ArrayList<String>() {
            {
                add("Total Average Time");
            }
        };
        workbook_2.setHeaders(headers_2);

        Double sum = 0.0;

        for( Map.Entry<String,List<String>> entry : results.entrySet() ) {

            // me baseio pelos dados de um cliente... o primeiro!
            List<String> time_list = entry.getValue();

            //Mount line
            List<String> line = new ArrayList<String>();

            line.add(entry.getKey());
            line.add(time_list.get(0));
            line.add(time_list.get(1));
            line.add(time_list.get(2));

            sum = sum + Double.valueOf(time_list.get(2));

            workbook_1.addLine(line);

        }

        List<String> line = new ArrayList<String>();

        Double average_total = sum / results.size();

        line.add( String.valueOf( average_total ) );

        workbook_2.addLine(line);

        report.addWorkbook( workbook_1 );
        report.addWorkbook( workbook_2 );
        report.setName("inf2545");

        return report;

    }

}
