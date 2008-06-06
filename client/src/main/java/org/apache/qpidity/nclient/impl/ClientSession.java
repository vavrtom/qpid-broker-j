package org.apache.qpidity.nclient.impl;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.qpidity.QpidException;
import org.apache.qpidity.api.Message;
import org.apache.qpidity.nclient.ClosedListener;
import org.apache.qpidity.nclient.MessagePartListener;
import org.apache.qpidity.transport.MessageAcceptMode;
import org.apache.qpidity.transport.MessageAcquireMode;
import org.apache.qpidity.transport.Option;
import org.apache.qpidity.transport.Range;
import org.apache.qpidity.transport.RangeSet;

/**
 * Implements a Qpid Sesion.
 */
public class ClientSession extends org.apache.qpidity.transport.Session implements  org.apache.qpidity.nclient.DtxSession
{
    static
    {
            String max = "message_size_before_sync"; // KB's
            try
            {
                MAX_NOT_SYNC_DATA_LENGH = new Long(System.getProperties().getProperty(max, "200000000"));
            }
            catch (NumberFormatException e)
            {
                // use default size
                MAX_NOT_SYNC_DATA_LENGH = 200000000;
            }
            String flush = "message_size_before_flush";
            try
            {
                MAX_NOT_FLUSH_DATA_LENGH = new Long(System.getProperties().getProperty(flush, "2000000"));
            }
            catch (NumberFormatException e)
            {
                // use default size
                MAX_NOT_FLUSH_DATA_LENGH = 20000000;
            }
    }

    private static  long MAX_NOT_SYNC_DATA_LENGH;
    private static  long MAX_NOT_FLUSH_DATA_LENGH;

    private Map<String,MessagePartListener> _messageListeners = new ConcurrentHashMap<String,MessagePartListener>();
    private ClosedListener _exceptionListner;
    private RangeSet _rejectedMessages;
    private long _currentDataSizeNotSynced;
    private long _currentDataSizeNotFlushed;

    public ClientSession(byte[] name)
    {
        super(name);
    }

    public void messageAcknowledge(RangeSet ranges, boolean accept)
    {
        for (Range range : ranges)
        {
            super.processed(range);
        }
        super.flushProcessed();
        if( accept )
        {
            messageAccept(ranges);
        }
    }

    public void messageSubscribe(String queue, String destination, short acceptMode, short acquireMode, MessagePartListener listener, Map<String, Object> filter, Option... options)
    {
        setMessageListener(destination,listener);
        super.messageSubscribe(queue, destination, MessageAcceptMode.get(acceptMode),
                               MessageAcquireMode.get(acquireMode), null, 0, filter,
                               options);
    }

    public void messageTransfer(String destination, Message msg, short acceptMode, short acquireMode) throws IOException
    {
        // The javadoc clearly says that this method is suitable for small messages
        // therefore reading the content in one shot.
        ByteBuffer  data = msg.readData();
        super.messageTransfer(destination, MessageAcceptMode.get(acceptMode),
                              MessageAcquireMode.get(acquireMode));
       // super.header(msg.getDeliveryProperties(),msg.getMessageProperties()  );
        if( msg.getHeader() == null || msg.getDeliveryProperties().isDirty() || msg.getMessageProperties().isDirty() )
        {
            msg.setHeader( super.header(msg.getDeliveryProperties(),msg.getMessageProperties()) );
            msg.getDeliveryProperties().setDirty(false);
            msg.getMessageProperties().setDirty(false);
        }
        else
        {
            super.header(msg.getHeader());
        }
        data( data );
        endData();
    }

    public void sync()
    {
        super.sync();
        _currentDataSizeNotSynced = 0;
    }

    /* -------------------------
     * Data methods
     * ------------------------*/

    public void data(ByteBuffer buf)
    {
        _currentDataSizeNotSynced = _currentDataSizeNotSynced + buf.remaining();
        _currentDataSizeNotFlushed = _currentDataSizeNotFlushed + buf.remaining();
        super.data(buf);
    }

    public void data(String str)
    {
        _currentDataSizeNotSynced = _currentDataSizeNotSynced + str.getBytes().length;
        super.data(str);
    }

    public void data(byte[] bytes)
    {
        _currentDataSizeNotSynced = _currentDataSizeNotSynced + bytes.length;
        super.data(bytes);
    }

    public void messageStream(String destination, Message msg, short acceptMode, short acquireMode) throws IOException
    {
        super.messageTransfer(destination, MessageAcceptMode.get(acceptMode),
                              MessageAcquireMode.get(acquireMode));
        super.header(msg.getDeliveryProperties(),msg.getMessageProperties());
        boolean b = true;
        int count = 0;
        while(b)
        {
            try
            {
                System.out.println("count : " + count++);
                data(msg.readData());
            }
            catch(EOFException e)
            {
                b = false;
            }
        }
        endData();
    }

    public void endData()
    {
        super.endData();
    /*    if( MAX_NOT_SYNC_DATA_LENGH != -1 && _currentDataSizeNotSynced >= MAX_NOT_SYNC_DATA_LENGH)
        {
            sync();
        }
         if( MAX_NOT_FLUSH_DATA_LENGH != -1 && _currentDataSizeNotFlushed >= MAX_NOT_FLUSH_DATA_LENGH)
        {
           executionFlush();
            _currentDataSizeNotFlushed = 0;
        }*/
    }

    public RangeSet getRejectedMessages()
    {
        return _rejectedMessages;
    }

    public void setMessageListener(String destination, MessagePartListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Cannot set message listener to null");
        }
        _messageListeners.put(destination, listener);
    }

    public void setClosedListener(ClosedListener exceptionListner)
    {
        _exceptionListner = exceptionListner;
    }

    void setRejectedMessages(RangeSet rejectedMessages)
    {
        _rejectedMessages = rejectedMessages;
    }

    void notifyException(QpidException ex)
    {
        _exceptionListner.onClosed(null, null, null);
    }

    Map<String,MessagePartListener> getMessageListeners()
    {
        return _messageListeners;
    }
}
