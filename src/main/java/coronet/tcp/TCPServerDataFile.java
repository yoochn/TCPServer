package coronet.tcp;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class TCPServerDataFile implements Runnable,AutoCloseable
{
    private String fileName;
    private String filePath;
    private boolean isRun;
    private ConcurrentLinkedQueue<ConcurrentHashMap<String, ConcurrentHashMap<Integer,String>>> queue;
    private Path path;


    private  FileWriter fw;
    private  BufferedWriter bw;
    private  PrintWriter out;

    public TCPServerDataFile(String filePath, String fileName)
    {
        this.fileName = fileName;
        this.filePath = filePath;
        this.path = Paths.get(filePath,fileName);
    }

    /**
     * Init the File buffering classes
     * */
    public void init()
    {
        this.queue = new ConcurrentLinkedQueue<>();
        try
        {
            this.fw = new FileWriter(path.toFile(), true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.bw = new BufferedWriter(fw);
        this.out = new PrintWriter(bw);
    }





    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }


    /**
     *  Save the current TCP Server cache map, this method is queuing TCP Server map status
     *  And write it Async
     * @param tcpSeverCache
     * */
    public void saveFile(ConcurrentHashMap<String, ConcurrentHashMap<Integer,String>> tcpSeverCache )
    {
        this.queue.add(tcpSeverCache);
    }

    @Override
    public void run()
    {
        while(!queue.isEmpty())
        {
            try
            {
                ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> item = queue.poll();
                String jsonMap = new Gson().toJson(item);
                if (!jsonMap.equals("{}"))
                {
                    out.print(jsonMap);
                    this.out.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void flush()
    {
        try
        {
            this.fw.flush();
            this.bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Get file directory
     */
    public String getFilePath()
    {
        return filePath;
    }

    /**
     * Set file directory
     * @param filePath
     */
    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public boolean isRun() {
        return isRun;
    }

    public void setRun(boolean run) {
        isRun = run;
    }

    /***
     * Convert the file into TCP Server hashMap, in case that the server was down or restarted with latest cache map
     * @return TCP Sever latest cache map
     */
    public ConcurrentHashMap<String, ConcurrentHashMap<Integer,String>>  fromFileToMap()
    {
        ConcurrentHashMap<String, ConcurrentHashMap<Integer,String>> jsonFile = new ConcurrentHashMap<>();
        Type classType = new TypeToken<ConcurrentHashMap<String, ConcurrentHashMap<Integer,String>>>() {
        }.getType();
        try (JsonReader reader = new JsonReader(new StringReader(path.getFileName().toString())))
        {
            Gson gson = new GsonBuilder().create();
            gson.fromJson(reader,classType);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return jsonFile;
    }

    @Override
    public void close() throws Exception
    {
        this.fw.close();
        this.bw.close();
        this.out.close();
    }


}
