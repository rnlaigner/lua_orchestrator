package br.pucrio.inf.les.ese.lua_orchestrator;

import java.util.concurrent.ArrayBlockingQueue;

public class Buffer
{
    private final ArrayBlockingQueue<String> buffer;

    public Buffer( int size )
    {
        buffer = new ArrayBlockingQueue<String>( size );
    }

    public boolean bufferIsEmpty() {
        return buffer.size() > 0;
    }

    public void set( String value ) throws InterruptedException
    {
        buffer.put( value ); // place value in buffer
        System.out.printf( "%s%s\t%s%d\n", "Producer writes ", value,
                "Buffer cells occupied: ", buffer.size() );
    }


    public String get() throws InterruptedException
    {
        String readValue = buffer.take(); // remove value from buffer
        System.out.printf( "%s %s\t%s%d\n", "Consumer reads ",
                readValue, "Buffer cells occupied: ", buffer.size() );

        return readValue;
    }
}