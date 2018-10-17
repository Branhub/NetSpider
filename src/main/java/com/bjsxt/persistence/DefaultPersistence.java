package com.bjsxt.persistence;

import com.bjsxt.engine.Engine;
import com.bjsxt.parser.IPaserBean;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DefaultPersistence implements IPersistence,Runnable
{

    private BlockingQueue<Future<Map<String,String>>> dataParsed;
    private boolean isFinished = false;

    private IPaserBean paserBean;
    private String[] dataNames;
    private CSVFormat csvFormat = CSVFormat.EXCEL;
    private FileOutputStream fileOutputStream;
    private PrintWriter printWriter;
    private CSVPrinter csvPrinter;

    private int persistedCount = 0;

    public DefaultPersistence(BlockingQueue<Future<Map<String,String>>> dataParsed,
                              IPaserBean paserBean) throws IOException
    {
        this.dataParsed = dataParsed;
        this.paserBean = paserBean;
        dataNames = paserBean.getDataNames();
        csvFormat = csvFormat.withHeader(dataNames);

        File file = new File(paserBean.getOutputFileName());
        if (!file.exists())
        {
            if (!file.getParentFile().exists())
            {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }

        fileOutputStream = new FileOutputStream(file);
        printWriter = new PrintWriter(fileOutputStream);

        csvPrinter = new CSVPrinter(printWriter,csvFormat);
    }

    @Override
    public void run()
    {
        persist();
    }

    public Thread start()
    {
        Thread newThread = new Thread(this);
        newThread.start();
        return newThread;
    }

    @Override
    public void persist()
    {
        try
        {
            while (!isFinished && !Thread.currentThread().isInterrupted() && persistedCount < paserBean.getStopDataCount())
            {
                try
                {
                    Map<String, String> parseResult = dataParsed.take().get();
                    System.out.println(parseResult);
                    for (int i = 0;i < dataNames.length;i++)
                    {
                        csvPrinter.print(parseResult.get(dataNames[i]));
                    }
                    csvPrinter.println();
                    persistedCount = persistedCount + 1;
                }
                catch (InterruptedException e)
                {
                    System.out.println("data persistence thread interrupted");
                    Thread.currentThread().interrupt();
                    continue;
                }
                catch (ExecutionException e)
                {
                    continue;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    continue;
                }
            }

            Engine.getInstance().stop();
        }
        finally
        {
            try
            {
                csvPrinter.flush();
                printWriter.flush();
                fileOutputStream.flush();
                csvPrinter.close();
                printWriter.close();
                fileOutputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
