package br.pucrio.inf.les.ese.lua_orchestrator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class LuaTask implements Callable<String> {

    public String params;

    public LuaTask(String params) {
        this.params = params;
    }

    @Override
    public String call() {

        OutputStream stdin = null;
        InputStream stderr = null;
        InputStream stdout = null;

        // launch EXE and grab stdin/stdout and stderr
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

            stdin = process.getOutputStream();
            stderr = process.getErrorStream();
            stdout = process.getInputStream();

            // TODO return table with values

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;

    }

}
