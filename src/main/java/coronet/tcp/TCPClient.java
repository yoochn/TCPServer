package coronet.tcp;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;



public class TCPClient implements ITCP
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int port;
    private String ip;

    private static final Logger LOG = LoggerFactory.getLogger(TCPServer.class);



    public TCPClient(String ip,int port)
    {
        this.ip = ip;
        this.port = port;
    }

    public void startConnection() throws IOException {

        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    /**
     * Generic TCP client send message method
     * @param msg TCPRequest class formatted to JSON string
     * @return TCPResponse class formatted to JSON string
     * @throws IOException
     */
    public String sendMessage(String msg) throws IOException
    {
        out.println(msg);
        String resp = in.readLine();
        return resp;
    }

    public void stopConnection() throws IOException
    {
        in.close();
        out.close();
        clientSocket.close();
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

    /**
     * Generic build method TCPRequest request and sent it to TCPServer
     * @param request
     * @return
     */
    private <T> TCPResponse<T> sendMessageByMethod(TCPRequest request)
    {
        ObjectMapper mapper = new ObjectMapper();
        String jsonGet = null;
        TCPResponse <T> rightAddResponse = new TCPResponse();
        try
        {
            jsonGet = mapper.writeValueAsString(request);
            String response = sendMessage(jsonGet);
            if (response.contains("responseCode"))
            {
                rightAddResponse  = mapper.readValue(response, new TypeReference<TCPResponse<T>>() {});
            }
        }
        catch (IOException e)
        {
            LOG.error("Error occurred during client response ", e);
            rightAddResponse.setMessage(e.getMessage());
            rightAddResponse.setResponseCode(TCPResponse.status.ERROR);
            e.printStackTrace();

        }
        return rightAddResponse;
    }
}
