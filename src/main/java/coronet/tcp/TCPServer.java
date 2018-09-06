package coronet.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toMap;


public class TCPServer
{
	private String ip;
	private int port;
	boolean isStop;
	public boolean isStop() {
		return isStop;
	}
	private static final Logger LOG = LoggerFactory.getLogger(TCPServer.class);
	private ServerSocket serverSocket;
	private  TCPServerDataFile serverDataFile;

	public TCPServer(int port)
	{
		this.port = port;
	}

	public TCPServer(int port, TCPServerDataFile serverDataFile)
	{
		this.port = port;
		this.serverDataFile = serverDataFile;

	}
	
	public void start() throws Exception
	{
        try
		{
			serverSocket = new ServerSocket(port);
			while (true)
			{
				//Create specific handler for each client. support TCP multi-client server architecture
				new EchoClientHandler(serverSocket.accept(),serverDataFile).start();
			}
		}
		catch (Exception ex)
		{
			LOG.error("Server is down" , ex);
			ex.printStackTrace();
		}


	}

	public void stop() throws IOException
	{
		isStop = true;
		serverSocket.close();
		serverDataFile.setRun(false);
	}

	/**
	 * TCP client Handler suppose to support TCP multi-client architecture
	 */
	private  class EchoClientHandler extends Thread implements ITCP
	{
		private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> tcpSeverCache;
		private Socket clientSocket;
		private PrintWriter out;
		private BufferedReader in;
		private TCPServerDataFile serverDataFile;

		public EchoClientHandler(Socket socket,TCPServerDataFile serverDataFile)
		{
			this.clientSocket = socket;
			this.serverDataFile = serverDataFile;
			tcpSeverCache = new ConcurrentHashMap<>();

		}

		/**
		 * Open an thread for each client create different PrintWriter/BufferReader for each client
		 */
		public void run() {
			try
			{
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				Thread fileThread = new Thread(serverDataFile);
				fileThread.start();
				String inputLine;
				while ((inputLine = in.readLine()) != null)
				{
					handleRequest(inputLine);
				}
				in.close();
				out.close();
				//clientSocket.close();


			} catch (IOException e)
			{
				LOG.error(e.getMessage());
				e.printStackTrace();
			}
		}


		/**
		 * Digest TCP client request, parse the content , extract the correct method to invoke.
		 * Gateway to all TCP Server functionality
		 * @param request
		 */
		private void handleRequest(String request)
		{
			try
			{
				if (request!=null)
				{
					//some kind of handshake to make sure client and server can communicate
					if ("hello server".equals(request))
					{
						out.println("hello client");
					}
					else
					{
						TCPResponse response = factoryMethod(request);
						ObjectMapper mapper = new ObjectMapper();
						String jsonResponse = mapper.writeValueAsString(response);
						out.println(jsonResponse);
					}
				}
			}
			catch (Exception ex)
			{
				LOG.error("Exception during marsheling" +ex.getMessage(),ex);
				ex.printStackTrace();
			}

		}

		/**
		 * Generic Factory method which extract de-coupled information
		 * Each method extract method name and related input parameters dynamically
		 * @param request TCP request contain TCP Server method name
		 * @return
		 */
		private TCPResponse factoryMethod(String request)
		{
			TCPRequest tcpRequest = new Gson().fromJson(request,TCPRequest.class);
			TCPResponse response = new TCPResponse();
			//I didn't wanted to use reflection cause of run time exception
			switch (tcpRequest.getMethod())
			{
				case "get":
				{
					List<Object> value = TCPUtils.extractInputParameters(tcpRequest);
					response = (value.get(0) instanceof TCPResponse)? (TCPResponse) value.get(0): this.get((String)value.get(0));
					break;
				}
				case "getAllKeys":
				{
					//extract specific 'getAllKeys' method input parameters and input validation
					List<Object> value = TCPUtils.extractInputParameters(tcpRequest);
					//In case of invalid request parameters and TCPResponse status error with relevant message is returned, avoiding run time exception
					response = (value.get(0) instanceof TCPResponse)? (TCPResponse) value.get(0): this.getAllKeys((String)value.get(0));
					break;
				}
				case "rightAdd":
				{
					//extract specific 'rightAdd' method input parameters
					List<Object> value = TCPUtils.extractInputParameters(tcpRequest);
					//In case of invalid request parameters and TCPResponse status error with relevant message is returned, avoiding run time exception
					response = (value.get(0) instanceof TCPResponse)? (TCPResponse) value.get(0): this.rightAdd((String)value.get(0),(String)value.get(1));
					break;
				}
				case "leftAdd":
				{
					//extract specific 'leftAdd' method input parameters
					List<Object> value = TCPUtils.extractInputParameters(tcpRequest);
					response = (value.get(0) instanceof TCPResponse)? (TCPResponse) value.get(0): this.leftAdd((String)value.get(0),(String)value.get(1));
					break;
				}
				case "set":
				{
					//extract specific 'set' method input parameters
					List<Object> value = TCPUtils.extractInputParameters(tcpRequest);
					response = (value.get(0) instanceof TCPResponse)? (TCPResponse) value.get(0): this.set((String)value.get(0),(List<String>)value.get(1));
					break;
				}
			}
			//Saving the Cache memory only if the request result is success
			if (response.getResponseCode().equals(TCPResponse.status.SUCCESS))
			{
				serverDataFile.saveFile(tcpSeverCache);
			}
			return response;
		}


		/**
		 *
		 * @param key
		 * @return
		 */
		@Override
		public TCPResponse get(String key)
		{
			ConcurrentHashMap<Integer,String> result = tcpSeverCache.get(key);
			return TCPUtils.buildResponse(result,"Successfully Completed");
		}

		/**
		 * Get All TCP Server key which contain this substring
		 * @param searchKey substring key
		 * @return TCPResponse class formatted to JSON string, result property is array of keys
		 * */
		@Override
		public TCPResponse getAllKeys(String searchKey)
		{
			List<String> keys = new ArrayList<>();
			try
			{
				// in case doens't exist
				tcpSeverCache.keySet().forEach(key->{
						if (key.contains(searchKey))
						{
							keys.add(key);
						}
			     });
			}
			catch (Exception ex)
			{
				LOG.error(ex.getMessage(),ex);
				ex.printStackTrace();
			}
			return   TCPUtils.<List<String>>buildResponse(keys,"Successfully Completed");

		}
		/**
		 * Add value  TCP Server key , the value is append the end of the list attached to specific element
		 * Add the value even though it's already exist
		 * @param key TCP Server key element
		 * @param value TCP Server key value to add
		 * @return TCPResponse class formatted to JSON string, result property list of array attached to given key
		 */
		@Override
		public TCPResponse rightAdd(String key, String value)
		{
			if (tcpSeverCache.containsKey(key))
			{
				//calculate the maximum key value in HashMap related to specific key elements
				int max = tcpSeverCache.get(key).keySet().stream().mapToInt(k->k).max().getAsInt()+1;
				tcpSeverCache.get(key).put(max,value);
			}
			else
			{
				//the entire method is invoked atomically
				//Create the sub map in case key doesn't exist
				tcpSeverCache.computeIfAbsent(key ,s-> {
					return new ConcurrentHashMap<Integer,String>(){{put(0,value);}};
				});
			}
			return TCPUtils.<ConcurrentHashMap<Integer,String>>buildResponse(tcpSeverCache.get(key),"Successfully Completed");
		}

		/**
		 * Add value  TCP Server key , the value is append the end of the list attached to specific element
		 * Add the value even though it's already exist
		 * @param key TCP Server key element
		 * @param value TCP Server key value to add
		 * @return TCPResponse class formatted to JSON string, result property list of array attached to given key
		 */
		@Override
		public <T>TCPResponse leftAdd(String key, String value)
		{
			if (tcpSeverCache.containsKey(key))
			{
				ThreadLocal<Integer> size =  new ThreadLocal<Integer>();
				size.set(tcpSeverCache.get(key).keySet().stream().mapToInt(k->k).min().getAsInt()-1);
				tcpSeverCache.get(key).put(size.get(),value);
			}
			else
			{
				//the entire method is invoked atomically
				//Create the sub ConcurrentHashMap in case key doesn't exist
				tcpSeverCache.computeIfAbsent(key ,s-> {
					return new ConcurrentHashMap<Integer,String>(){{put(0,value);}};
				});
			}
			return TCPUtils.<ConcurrentHashMap<Integer,String>>buildResponse(tcpSeverCache.get(key),"Successfully Completed");
		}

		/**
		 * Replace all content of a specific key
		 * @param key specific key
		 * @param values list of value to replace key content
		 * @return
		 */
		@Override
		public <T> TCPResponse set(String key, List<String> values) {
			ConcurrentHashMap<Integer,String> setValue = new ConcurrentHashMap<Integer,String>(IntStream.range(0,values.size())
					.boxed()
					.collect(Collectors.toConcurrentMap(i -> i, i -> values.get(i))));
			ConcurrentHashMap<Integer,String> result = tcpSeverCache.replace(key, setValue);
			return TCPUtils.<ConcurrentHashMap<Integer,String>>buildResponse(result,"Successfully Completed");
		}
	}








}
