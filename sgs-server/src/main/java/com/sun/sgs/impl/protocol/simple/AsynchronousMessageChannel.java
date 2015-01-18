/*
 * Copyright 2007-2010 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * --
 */

package com.sun.sgs.impl.protocol.simple;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketAdapter;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.Handshakedata;

import com.sun.sgs.impl.nio.DelegatingCompletionHandler;
import com.sun.sgs.impl.sharedutil.LoggerWrapper;
import com.sun.sgs.nio.channels.AsynchronousByteChannel;
import com.sun.sgs.nio.channels.CompletionHandler;
import com.sun.sgs.nio.channels.IoFuture;
import com.sun.sgs.nio.channels.ReadPendingException;
import com.sun.sgs.nio.channels.WritePendingException;

/**
 * A wrapper channel that reads and writes complete messages by framing
 * messages with a 2-byte message length, and masking (and re-issuing) partial
 * I/O operations.  Also enforces a fixed buffer size when reading.
 */
public class AsynchronousMessageChannel implements Channel {

    /** The number of bytes used to represent the message length. */
    public static final int PREFIX_LENGTH = 2;

    /** The logger for this class. */
    static final LoggerWrapper logger = new LoggerWrapper(
	Logger.getLogger(AsynchronousMessageChannel.class.getName()));

    
    final WebsocketContext websocketCtx;

    /** Whether there is a read underway. */
    final AtomicBoolean readPending = new AtomicBoolean();

    /** Whether there is a write underway. */
    final AtomicBoolean writePending = new AtomicBoolean();

    /** The read buffer. */
    final ByteBuffer readBuffer;

    /**
     * Creates a new instance of this class with the given channel and read
     * buffer size.
     * 
     * @param	channel a channel
     * @param	readBufferSize the number of bytes in the read buffer
     * @throws	IllegalArgumentException if {@code readBufferSize} is smaller
     *		than {@value #PREFIX_LENGTH}
     */
    public AsynchronousMessageChannel(AsynchronousByteChannel channel,
                                      int readBufferSize)
    {
	if (readBufferSize < PREFIX_LENGTH) {
	    throw new IllegalArgumentException(
		"The readBufferSize must not be smaller than " +
		PREFIX_LENGTH);
	}
	readBuffer = ByteBuffer.allocateDirect(readBufferSize);
	this.websocketCtx = new WebsocketContext(channel, readBuffer);
    }

    /* -- Methods for reading and writing -- */

    /**
     * Initiates reading a complete message from this channel.  Returns a
     * future which will contain a read-only view of a buffer containing the
     * complete message.  Calls {@code handler} when the read operation has
     * completed, if {@code handler} is not {@code null}.  The buffer's
     * position will be set to {@code 0} and it's limit will be set to the
     * length of the complete message.  The contents of the buffer will remain
     * valid until the next call to {@code read}.
     * 
     * @param	handler the completion handler object; can be {@code null}
     * @return	a future representing the result of the operation
     * @throws	BufferOverflowException if the buffer does not contain enough
     *		space to read the next message
     * @throws	ReadPendingException if a read is in progress
     */
    public IoFuture<ByteBuffer, Void> read(CompletionHandler<ByteBuffer,
                                                             Void> handler)
    {
        if (!readPending.compareAndSet(false, true)) {
            throw new ReadPendingException();
	}
        return new Reader(handler).start();
    }

    /**
     * Initiates writing a complete message from the given buffer to the
     * underlying channel, and returns a future for controlling the operation.
     * Writes bytes starting at the buffer's current position and up to its
     * limit.
     * 
     * @param	src the buffer from which bytes are to be retrieved
     * @param	handler the completion handler object; can be {@code null}
     * @return	a future representing the result of the operation
     * @throws	WritePendingException if a write is in progress
     */
    public IoFuture<Void, Void> write(ByteBuffer src,
                                      CompletionHandler<Void, Void> handler)
    {
        if (!writePending.compareAndSet(false, true)) {
            throw new WritePendingException();
	}
        return new Writer(handler, src).start();
    }

    /* -- Implement Channel -- */

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
    	websocketCtx.close();

    }

    /** {@inheritDoc} */
    @Override
    public boolean isOpen() {
        return websocketCtx.isOpen();
    }

    /* -- Other methods and classes -- */

    /**
     * Returns the length of the complete message, including the length prefix,
     * based on the data read into the buffer between position 0 and the
     * current position, or {@code -1} if the length cannot be determined.
     * 
     * @return	the length, or {@code -1}
     */
    int getMessageLength() {
	return (readBuffer.position() >= PREFIX_LENGTH)
	    ? (readBuffer.getShort(0) & 0xffff) + PREFIX_LENGTH : -1;
    }

    final class WebsocketContext extends WebSocketAdapter
    {
    	private boolean firstRead = true;
    	private boolean isWebsocket = false;
    	private boolean connected = false;

    	// magic number to find websockets
    	public static final int WEBSOCKET_LENGTH = 1195725856;

    	
    	private WebSocketImpl websocket;
    	final AsynchronousByteChannel channel;

    	private ByteBuffer wsInputBuffer;
    	private ByteBuffer wsOutputBuffer;
    	
    	private LinkedList<ByteBuffer> incoming = new LinkedList<ByteBuffer>();
    	
    	public WebsocketContext(AsynchronousByteChannel channel,ByteBuffer readbuffer)
    	{
    		this.channel = channel;
    		this.wsOutputBuffer = readbuffer;
    		this.wsInputBuffer = ByteBuffer.allocate(4096);
    	}

		public boolean isOpen() {
			return  channel.isOpen();
		}

		public void close() throws IOException {
	    	channel.close();
		}

		IoFuture<Integer,Void> read(CompletionHandler<Integer, Void> handler)
		{
			return new WebsocketReader(handler).start();
		}
		
		IoFuture<Integer,Void> write(ByteBuffer bb,CompletionHandler<Integer, Void> handler)
		{
			return new WebsocketWriter(bb,handler).start();
		}
		
		
		
        final public class WebsocketWriter
        extends DelegatingCompletionHandler<Integer, Void, Integer, Void>
        {
        	private final ByteBuffer writeBuffer;
        	private int wroteBytes = 0;
        	
        	private WebsocketWriter(ByteBuffer writeBuffer,CompletionHandler<Integer, Void> handler) {
        		super(null, handler);
        		this.writeBuffer = writeBuffer;
            }
        	
        	
        	
    		@Override
    		protected IoFuture<Integer, Void> implStart() {
    			System.out.println("WS: Write Start");
    			if (isWebsocket)
    			{
    				websocket.send(writeBuffer);
    				
    				if (!websocket.outQueue.isEmpty())
    				{
    					ByteBuffer outData = websocket.outQueue.poll();
    					return channel.write(outData,this);
    				} else {
    					throw new RuntimeException("no websocket-frames to send!?");
    				}
    				
    			}
    			else
    			{
    				return channel.write( writeBuffer, this);
    			}
    		}
    		
    		@Override
    		protected IoFuture<Integer, Void> implCompleted(
    				IoFuture<Integer, Void> innerResult) throws Exception {
    			// TODO Auto-generated method stub

    			wroteBytes  += innerResult.getNow();

    			if (isWebsocket)
    			{
    				if (!websocket.outQueue.isEmpty())
    				{
    					ByteBuffer outData = websocket.outQueue.poll();
    					return channel.write(outData,this);
    				} 
    			}
    			
    			System.out.println("WS: write complete:"+wroteBytes);
    			set(wroteBytes);
    			return null;

    		}    		
        }

        final public class WebsocketReader
        extends DelegatingCompletionHandler<Integer, Void, Integer, Void>
        {
        	private WebsocketReader(CompletionHandler<Integer, Void> handler) {
        		super(null, handler);
            }
        	
        	private final void wsDecode()
        	{

        		
        		ByteBuffer decodeBuffer = wsInputBuffer.duplicate();
        		decodeBuffer.flip();

        		websocket.decode(decodeBuffer);
				
        		if (decodeBuffer.position() == wsInputBuffer.position()) {
        			wsInputBuffer.clear();
        		} 
        		else if (decodeBuffer.position() < wsInputBuffer.position()){
        			wsInputBuffer = decodeBuffer.compact();
        		}
        		else {
        			throw new RuntimeException("Websocket-Decoder read more bytes than available!?!??");
        		}
        	}
        	
    		@Override
    		protected IoFuture<Integer, Void> implStart() {
        		System.out.println("WS: READ START");

    			if (isWebsocket || firstRead) // in websocket-mode use a inputbuffer
    			{
        			wsInputBuffer.clear();
        			return channel.read(wsInputBuffer, this);
    			}
    			else // if not using websocket just write in the outbutbuffer, since there is no further processing needed
    			{
    				return channel.read(wsOutputBuffer, this);
    			}
    		}
    		
    		@Override
    		protected IoFuture<Integer, Void> implCompleted(
    				IoFuture<Integer, Void> innerResult) throws Exception {
    			int readBytes = innerResult.getNow();
        		
    			System.out.println("WS: READ COMPLETED "+readBytes);

        		if (readBytes < 0)
    			{
    				set(readBytes);
    				return null;
    			}
    			
    			// check for websocket by checking if the first two chars are "GE"(T HTTP...)
    			if (firstRead)
    			{
    				if(readBytes < 2) {
    					// load more
    					return channel.read(wsInputBuffer, this);
    				}
    				
					firstRead = false;

					if (wsInputBuffer.getInt(0) == WEBSOCKET_LENGTH) {
    					isWebsocket = true;
    					websocket = new WebSocketImpl(WebsocketContext.this, WebSocketImpl.defaultdraftlist);
    				}
    			}
    			
    			if (isWebsocket)
    			{
					// read handshake and create response
					wsDecode();
					
    				if (!connected && !websocket.outQueue.isEmpty())
    				{
    						ByteBuffer handshakeResponse = websocket.outQueue.poll();
        					connected = true;
        					return channel.write(handshakeResponse, new CompletionHandler<Integer, Void>() {
								@Override
								public void completed(
										IoFuture<Integer, Void> result) {
									channel.read(wsInputBuffer,WebsocketReader.this);	
								}
							});

    				}
    				
    					
					int outBytes = 0;
					while (!incoming.isEmpty())
					{
    					ByteBuffer outputData = incoming.pollFirst();
    					outBytes += outputData.remaining();
    					// write the data to the outputBuffer
    					wsOutputBuffer.put(outputData);
					}
					if (outBytes > 0) {
						set(outBytes);
						// passed data to outer completionhandler 
						return null;
					} else {
						return channel.read(wsOutputBuffer, this);
					}
    				
    
    				
					
    			}
    			else // no websocket, just bypass the data
    			{
    				if (readBytes > 0)
    				{
        				if (!connected) // first time here
        				{
        					connected = true;
        					wsInputBuffer.flip();
        					wsOutputBuffer.put(wsInputBuffer);
        				}
        				// otherwise we read directly in the outputBuffer
        				set(readBytes);
        				return null;
    				}
    				else
    				{
    					return channel.read(wsOutputBuffer, this);
    				}
    			}
    			
    		}        	
        }

		@Override
		public void onWebsocketMessage(WebSocket conn, String message) {
			System.out.println("WS: OnStrMessage: "+message);
		}

		@Override
		public void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
			incoming.add(blob);
			System.out.println("WS: OnWebMessage "+blob.limit());
		}

		@Override
		public void onWebsocketOpen(WebSocket conn, Handshakedata d) {
			System.out.println("WS: Open!");
		}

		@Override
		public void onWebsocketClose(WebSocket ws, int code, String reason,
				boolean remote) {
			System.out.println("WS: Close!");
		}

		@Override
		public void onWebsocketClosing(WebSocket ws, int code, String reason,
				boolean remote) {
			System.out.println("WS: Closing!");
			
		}

		@Override
		public void onWebsocketCloseInitiated(WebSocket ws, int code,
				String reason) {
			System.out.println("WS: Close init!");
		}

		@Override
		public void onWebsocketError(WebSocket conn, Exception ex) {
			System.out.println("Error:"+ex);
			
		}

		@Override
		public void onWriteDemand(WebSocket conn) {
			// TODO Auto-generated method stub
			System.out.println("WriteDemand");
			
		}

		@Override
		public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
			// TODO Auto-generated method stub
			return null;
		}


    }

    
    /**
     * Implement a completion handler for reading a complete message from the
     * underlying byte stream.
     */
    private final class Reader
	extends DelegatingCompletionHandler<ByteBuffer, Void, Integer, Void>
    {
	/** The length of the message, or -1 if not yet known. */
        private int messageLen = -1;

	/** Creates an instance with the specified attachment and handler. */
        Reader(CompletionHandler<ByteBuffer, Void> handler) {
            super(null, handler);
        }

	/** Clear the readPending flag. */
        @Override
        protected void done() {
            readPending.set(false);
            super.done();
        }

        /** Start reading into the buffer. */
        @Override
        protected IoFuture<Integer, Void> implStart() {
	    int position = readBuffer.position();
	    if (position > 0) {
		/* Skip previous message, moving remaining bytes to front */
		int len = getMessageLength();
		assert len > 0;
		if (position > len) {
		    readBuffer.position(len);
		    readBuffer.limit(position);
		    readBuffer.compact();
		} else {
		    readBuffer.clear();
		}
	    }
            return processBuffer();
        }

	/** Process the results of reading so far and read more if needed. */
        @Override
        protected IoFuture<Integer, Void> implCompleted(
	    IoFuture<Integer, Void> result)
            throws ExecutionException, EOFException
        {
            int bytesRead = result.getNow();
            if (bytesRead < 0) {
		throw new EOFException("The message was incomplete");
	    }
	    return processBuffer();
        }

	/**
	 * Process the results of reading into the buffer, and return a future
	 * to read more if needed.
	 */
        private IoFuture<Integer, Void> processBuffer() {
            if (messageLen < 0) {
                messageLen = getMessageLength();
                if (messageLen >= 0) {
                    if (readBuffer.limit() < messageLen) {
			/* Buffer is too small to hold complete message */
                        throw new BufferOverflowException();
                    }
                }
            }
			if (messageLen >= 0 && readBuffer.position() >= messageLen) {
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "{0} read complete {1}:{2}", this,
							messageLen, readBuffer.position());
				}
				/*
				 * Return a read-only buffer containing just the message bytes
				 * without the length prefix.
				 */
				ByteBuffer result = readBuffer.duplicate();
				result.limit(messageLen);
				result.position(PREFIX_LENGTH);
				set(result.slice().asReadOnlyBuffer());
				return null;
			} else {
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "{0} read incomplete {1}:{2}",
							this, messageLen, readBuffer.position());
				}
//				return channel.read(readBuffer, this);
				return websocketCtx.read(this);
			}
		}
    }

    /**
     * Implement a completion handler for writing a complete message to the
     * underlying byte stream.
     */
    private final class Writer
	extends DelegatingCompletionHandler<Void, Void, Integer, Void>
    {
	/**
	 * The byte buffer containing the bytes to send, with the size
	 * prepended.
	 */
	private final ByteBuffer srcWithSize;

	/**
	 * Creates an instance with the specified attachment and handler, and
	 * sending the bytes in the specified buffer.
	 */
        Writer(CompletionHandler<Void, Void> handler, ByteBuffer src) {
            super(null, handler);
	    int size = src.remaining();
	    assert size < Short.MAX_VALUE;
	    /* Prepend the size as a short. */
	    /*
	     * XXX: Maybe avoid copying by doing two writes?  -tjb@sun.com
	     * (02/29/2008)
	     */
	    srcWithSize = ByteBuffer.allocate(2 + size);
	    srcWithSize.putShort((short) size)
		.put(src)
		.flip();
        }

	/** Clear the writePending flag. */
        @Override
        protected void done() {
            writePending.set(false);
            super.done();
        }

	/** Start writing from the buffer. */
        @Override
        protected IoFuture<Integer, Void> implStart() {
            return websocketCtx.write(srcWithSize,this);
        }

	/** Process the results of writing so far and write more if needed. */
        @Override
        protected IoFuture<Integer, Void> implCompleted(
	    IoFuture<Integer, Void> result)
            throws ExecutionException
        {
	    /* See if computation already failed. */
	    result.getNow();
            if (srcWithSize.hasRemaining()) {
                /* Write some more */
                return websocketCtx.write(srcWithSize, this);
            } else {
                /* Finished */
                return null;
            }
        }
    }
}
