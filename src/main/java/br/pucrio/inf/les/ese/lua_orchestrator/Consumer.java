package br.pucrio.inf.les.ese.lua_orchestrator;

import java.util.Random;

public class Consumer implements Runnable
{
    private final static Random generator = new Random();
    private final Buffer sharedLocation;

    public Consumer(Buffer shared)
    {
        this.sharedLocation = shared;
    }

    public void run()
    {
        String gitUrl = "";

        while (!sharedLocation.bufferIsEmpty())
        {
            try
            {
                Thread.sleep( generator.nextInt( 3000 ) );

                // Runtime.getRuntime().exec('start "Data save" cmd /k "cd ' .. path .. ' & ' .. r_path .. 'bin\luae.exe testscript.lua & exit "')
            }
            catch ( InterruptedException exception )
            {
                exception.printStackTrace();
            }
        }

        System.out.printf( "\n%s %s\n%s\n",
                "Consumer read values totaling", gitUrl, "Terminating Consumer" );
    }
}
