package coronet.tcp;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class StandardTCPClient implements ITCP, Runnable
{
    private static final Logger Log = LoggerFactory.getLogger(TCPServer.class);

    private static final String LOG_TAG = "StandardTCPClient";
    private static final int NUM_RETRIES = 3;

    String ip;
    int port;
    private LinkedBlockingQueue<String> receivedQueue;
    private LinkedBlockingQueue<String> sendQueue;
    private volatile boolean m_isRunning;

    public StandardTCPClient(String ip, int port)
    {
        receivedQueue = new LinkedBlockingQueue<String>();
        sendQueue = new LinkedBlockingQueue<String>();
        this.port = port;
        this.ip = ip;
    }


    @Override
    public void run()
    {
        int connectionFailures = 0;
        while(connectionFailures < NUM_RETRIES)
        {
            try (
                    Socket socket = new Socket(this.ip,
                            this.port);
                    // from server
                    BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    // to server
                    PrintWriter outputStream = new PrintWriter(socket.getOutputStream(), true);
            )
            {
                connectionFailures = 0;
                StandardTCPClient client = this;
                Thread inputReader = new Thread(new ClientReaderHandler(client, receivedQueue, inputStream));
                Thread outputWriter = new Thread(new ClientWriterHandler(client, sendQueue,outputStream));
                setRunning(true);
                inputReader.start();
                outputWriter.start();
                //wait for the threads to finish, they never should
                synchronized (client)
                {
                    while (isRunning())
                        wait();
                }

                // its not clear to us why the threads are stopping.
                // force them to stop so we can recreate
                inputReader.interrupt();
                outputWriter.interrupt();

                // Annoyingly, thread.interrupt() does not stop a blocking buffered stream
                // readline() io call. Close the socket to force the blocking call to end.
                socket.close();
                // wait for them to stop then attempt to recreate.
                inputReader.join();
                outputWriter.join();
            } catch (java.io.IOException ex)
            {
                setRunning(false);
                Log.warn(LOG_TAG, "IO Exception:", ex);
                connectionFailures++;
            }
            catch (InterruptedException ex)
            {
                setRunning(false);
                Log.error(LOG_TAG, "Thread interrupted");
                Thread.currentThread().interrupt();
                return;
            }
            //wait 1 second then attempt to reconnect
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex)
            {
                Log.error(LOG_TAG, "retry loop interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Get  TCP Server key list of elements
     * @param key TCP Sever key name
     * @return TCPResponse class formatted to JSON string, result property is array of elements
     * */
    @Override
    public <T> TCPResponse<T> get(String key)
    {
        ArrayList<String> params = new ArrayList<String>(){{add(0,"value");}};
        TCPRequest request =  new TCPRequest("get",new HashMap<String,List<String>>(){{put("value", Arrays.asList(key));}});
        request.setParamNames(params);
        return sendMessageByMethod(request);
    }

    /**
     * Get All TCP Server key which contain this substring
     * @param key substring key
     * @return TCPResponse class formatted to JSON string, result property is array of keys
     * */
    @Override
    public <T> TCPResponse<T> getAllKeys(String key)
    {
        ArrayList<String> params = new ArrayList<String>(){{add(0,"value");}};
        TCPRequest request =  new TCPRequest("getAllKeys",new HashMap<String,List<String>>(){{put(params.get(0), Arrays.asList(key));}});
        request.setParamNames(params);
        return sendMessageByMethod(request);
    }

    /**
     * Add value  TCP Server key , the value is append the end of the list attached to specific element
     * Add the value even though it's already exist
     * @param key TCP Server key element
     * @param value TCP Server key value to add
     * @return TCPResponse class formatted to JSON string, result property list of array attached to given key
     */
    @Override
    public <T>TCPResponse <T> rightAdd(String key, String value)
    {
        ArrayList<String> params = new ArrayList<String>(){{add(0,"key");add(1,"value");}};
        TCPRequest request =  new TCPRequest("rightAdd",new HashMap<String,List<String>>()
        {{put(params.get(0), Arrays.asList(key));
            put(params.get(1), Arrays.asList(value));}});
        request.setParamNames(params);
        return sendMessageByMethod(request);
    }


    /**
     * Add value  TCP Server key , the value is append the start of the list attached to specific element
     * Add the value even though it's already exist
     * @param key TCP Server key element
     * @param value TCP Server key value to add
     * @return TCPResponse class formatted to JSON string, result property list of array attached to given key
     */
    @Override
    public <T>TCPResponse <T> leftAdd(String key, String value)
    {
        ArrayList<String> params = new ArrayList<String>(){{add(0,"key");add(1,"value");}};
        TCPRequest request =  new TCPRequest("leftAdd",new HashMap<String,List<String>>(){{put("key", Arrays.asList(key));put("value", Arrays.asList(value));}});
        request.setParamNames(params);
        return sendMessageByMethod(request);
    }

    /**
     * Replace all content of a specific key
     * @param key specific key
     * @param values list of value to replace key content
     * @return
     */
    @Override
    public <T>TCPResponse <T> set(String key, List<String> values)
    {
        ArrayList<String> params = new ArrayList<String>(){{add(0,"key");add(1,"value");}};
        TCPRequest request =  new TCPRequest("set",
                new HashMap<String,List<String>>(){{
                    put(params.get(0), Arrays.asList(key))
                    ;put(params.get(1), values);}});
        request.setParamNames(params);
        return sendMessageByMethod(request);
    }

    private <T> TCPResponse<T> sendMessageByMethod(TCPRequest request)
    {
        ObjectMapper mapper = new ObjectMapper();
        String jsonGet = null;
        TCPResponse <T> rightAddResponse = new TCPResponse();
        try
        {
            jsonGet = mapper.writeValueAsString(request);
            sendQueue.put(jsonGet);
            boolean isRun = true;
            while (isRun)
            {
                String response = receivedQueue.take();
                if ((response!= null))
                {
                    isRun = false;
                    rightAddResponse  = mapper.readValue(response, new TypeReference<TCPResponse<T>>() {});
                }

            }
        }
        catch (IOException e)
        {
            Log.error("Error occurred during client response ", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return rightAddResponse;
    }

    public boolean isRunning()
    {
        return m_isRunning;
    }

    synchronized public void setRunning(boolean running)
    {
        m_isRunning = running;
    }
}
