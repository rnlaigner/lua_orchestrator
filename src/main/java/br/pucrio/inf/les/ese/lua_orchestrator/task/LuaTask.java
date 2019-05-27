package br.pucrio.inf.les.ese.lua_orchestrator.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class LuaTask implements Callable<List> {

    public String params;

    public LuaTask(String params) {
        this.params = params;
    }

    @Override
    public List call() {

        List<String> stdOutList = new ArrayList<>();
        InputStream stdout = null;

        Process process = null;

        String projectPath = System.getProperty("user.dir");

        String character = null;
        String luaCmd = "lua";

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")){
            character = "\\";
            luaCmd = luaCmd + "5.1.exe";
        }
        else {
            character = "/";
        }

        try {

            StringBuilder sb = new StringBuilder(projectPath);

            String absolutePathToProject = sb.append(character).append("src").append(character).append("main").append(character).append("resources").append(character).toString();

            String luaAbsoluteCmd = absolutePathToProject + luaCmd;

            String subscriberLua = absolutePathToProject + "subscriber.lua" + " " + params;

            String toExec = luaAbsoluteCmd + " " + subscriberLua;

            process = Runtime.getRuntime().exec( toExec );

            process.waitFor();

            stdout = process.getInputStream();

            try{
                String line;
                BufferedReader input = new BufferedReader(new InputStreamReader(stdout));
                while ((line = input.readLine()) != null) {
                    stdOutList.add(line);
                }
                input.close();
            } catch(Exception e){
                e.printStackTrace();
                return null;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        return stdOutList;

    }

}
