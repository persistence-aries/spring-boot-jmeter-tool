package com.aries.jmeter.socket.min.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.kut3.util.BytesUtil;

public class ServerSideSocket {
    private Logger logger = LoggerFactory.getLogger(ServerSideSocket.class);
    private LinkedBlockingQueue<byte[]> tempQueue = new LinkedBlockingQueue<byte[]>();
    private SocketChannel socketChannel;
    private boolean isRun = true;
    private boolean isNeedConnected = false;
    private String host;
    private int port;
    private int MAX_BYTE = 102400;
    private ByteBuffer tempBuffer;
    private int mode;
    public static final int ReadMode = 1;
    private String systemName;
    private BytesUtil bytesUtil;
    byte[] bNotice ;

    public void run() {
        try {
            int serverPort = 4020;
            ServerSocket serverSocket = new ServerSocket(serverPort);
            serverSocket.setSoTimeout(20000);
            while(true) {
                System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");

                Socket server = serverSocket.accept();
                System.out.println("Just connected to " + server.getRemoteSocketAddress());

                PrintWriter toClient =
                        new PrintWriter(server.getOutputStream(),true);
                BufferedReader fromClient =
                        new BufferedReader(
                                new InputStreamReader(server.getInputStream()));
                String line = fromClient.readLine();
                while(line != null) {

                }



                System.out.println("Server received: " + line);
                toClient.println("Thank you for connecting to " + server.getLocalSocketAddress() + "\nGoodbye!");
            }
        }
        catch(UnknownHostException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        catch(IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }


    public void send(byte[] msg) {
        if(isRun) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(MAX_BYTE);
                buffer.clear();
                buffer.put(msg);
                buffer.flip();
                socketChannel.write(buffer);
            } catch (IOException e) {
                try {
                    socketChannel.close();
                    socketChannel = null;
                } catch (IOException e1) {
                    logger.warn("because (" + e.getMessage() + ") try to close socketChannel then got exception as above:" +  e1.getMessage(), e1);
                }
                if (!isConnected()) { connect(); }
            }
        }
    }


    private void connect() {
        logger.info("connect to server " + host + ":" + port + " is start!!");

        int retryTimes = 0;
        do {
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    logger.warn(e1.getMessage(), e1);
                }
//							logger.info("connect do loop in " + port );
//							logger.info("connect isRun: " + isRun );
                if(retryTimes >0){
                    logger.info("Connect retry to server " + host + ":" + port + " (Count): " + retryTimes);
                }

                InetAddress address = InetAddress.getByName(host);
                SocketAddress saddr = new InetSocketAddress(address, port);
                socketChannel = SocketChannel.open();
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE  , true);
                socketChannel.setOption(StandardSocketOptions.TCP_NODELAY,true);
                socketChannel.setOption(StandardSocketOptions.SO_LINGER, 0);
                socketChannel.setOption(StandardSocketOptions.IP_TOS, 4 );
                socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR ,true);
                socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 2048);
                socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 2048);
                socketChannel.connect(saddr);

//							retryTimes = 0;
                logger.info("connect to server " + host + ":" + port + " is success!!");
                isNeedConnected = false;

            } catch (ConnectException ce) {
                logger.warn("[ConnectException]" + ce.getMessage() + "(server " + host + ":" + port + ")" );
                isNeedConnected = true;
                retryTimes++;
                try {
                    if( (retryTimes >10) && (retryTimes % 10 == 1)){
                        logger.info(" after retry connect (" + (retryTimes -1) +") Thread will suspend 2 minutes");
                        Thread.sleep( 2 * 60 * 1000);
                        retryTimes = 0;
                    }else{
                        logger.warn(" [ConnectException] will  sleep..."  + "(server " + host + ":" + port + ")" );
                        Thread.sleep(retryTimes* 1000);
                        logger.warn(" [ConnectException] after sleep..."  + "(server " + host + ":" + port + ")" );
                    }
                } catch (InterruptedException e1) {
                    logger.warn(" [ConnectException] [InterruptedException] "  + e1.getMessage() +  "(server " + host + ":" + port + ")" );
                    logger.warn(e1.getMessage(), e1);
                }
            } catch (IOException e) {
                logger.warn("[IOException]" +e.getMessage() + "(server " + host + ":" + port + ")" );
                isNeedConnected = true;
                retryTimes++;
                try {
                    if( (retryTimes >10) && (retryTimes % 10 == 1)){
                        logger.info(" after retry connect (" + (retryTimes -1) +") Thread will suspend 2 minutes");
                        Thread.sleep( 2 * 60 * 1000);
                        retryTimes = 0;
                    }else{
                        logger.warn(" [IOException] will  sleep..."  + "(server " + host + ":" + port + ")" );
                        Thread.sleep(retryTimes* 1000);
                        logger.warn(" [IOException] after sleep..."  + "(server " + host + ":" + port + ")" );
                    }
                } catch (InterruptedException e1) {
                    logger.warn(" [IOException] [InterruptedException] "  + e1.getMessage() +  "(server " + host + ":" + port + ")" );
                    logger.warn(e1.getMessage(), e1);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                logger.warn(" [Exception] "  + e.getMessage() +  "(server " + host + ":" + port + ")" );
            }finally{
                logger.warn(" [finally]  isRun:("  + isRun +  ")isNeedConnected:("  + isNeedConnected +  ").....(server " + host + ":" + port + ")" );
            }
        } while ( isRun && isNeedConnected);

        if (mode == ReadMode) {
//						logger.info("connect do loop in (mode == ReadMode) " + port );
            receive();
        }
    }



    public void receive() {
        ByteBuffer socketBuffer = ByteBuffer.allocate(MAX_BYTE);
        while(isRun) {
            try {
                if (socketBuffer.hasRemaining()) {
                    int bytesRead = socketChannel.read(socketBuffer);
//								logger.info(" bytesRead:" + bytesRead);
                    if (bytesRead != -1) {
                        // First: Read date from socket, and save data in temp buffer.
                        socketBuffer.flip();
                        byte[] temp = new byte[bytesRead];
                        socketBuffer.get(temp, 0, bytesRead);
                        tempBuffer.put(temp);
                        socketBuffer.compact(); // Reset buffer and keep unread data

                        // Second: Cut data by length
                        tempBuffer.flip();
                        do {
                            if ((tempBuffer.limit() - tempBuffer.position()) >= 4) {
                                // get length word
                                tempBuffer.mark();
                                byte[] dataLengthByte = new byte[4];
                                tempBuffer.get(dataLengthByte, 0, 4);
                                tempBuffer.reset();
                                int dataLength = -1;
//											if ("EDDA".equals(systemName)) {
//												dataLength = bytesUtil.convEddaMsgLengthBytesToInt(dataLengthByte);
//											} else if ("EACH".equals(systemName)) {
//												dataLength = bytesUtil.convEachMsgLengthBytesToInt(dataLengthByte);
//											}
                                logger.debug(systemName + " msg, length=" + dataLength);

                                // get one set data
                                if ((dataLength > -1) && ((tempBuffer.limit() - tempBuffer.position()) >= dataLength)) {
                                    byte[] msgData = new byte[dataLength];
                                    tempBuffer.get(msgData, 0, dataLength);
                                    logger.debug("(" + systemName + "-" + port + ") receive message=" + Arrays.toString(msgData));

                                    tempQueue.put(msgData);
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } while (tempBuffer.limit() > tempBuffer.position());
                        tempBuffer.compact(); // Reset buffer and keep unread data
                    }else{//bytesRead == -1
                        try {
                            socketChannel.close();
                            socketChannel = null;
                        } catch (IOException e1) {
                            logger.warn("because ( bytesRead == -1 ) try to close socketChannel then got exception as above:" +  e1.getMessage(), e1);
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn(e.getMessage());
                try {
                    socketChannel.close();
                    socketChannel = null;
                } catch (IOException e1) {
                    logger.warn("because (" + e.getMessage() + ") try to close socketChannel then got exception as above:" +  e1.getMessage(), e1);
                }
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
            if(isRun) {
//							logger.info(" isConnected() Loop here !!!");

                if (!isConnected()) {
                    try {
                        logger.info(" will tempQueue.put(bNotice).....");
                        tempQueue.put(bNotice);
                        logger.info("tempQueue.size():" + tempQueue.size());
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(),e);
                    }
                    connect();
                }
            }
        }
    }




    public boolean isConnected() {
        return (socketChannel != null) && isRun && socketChannel.isConnected();
    }


    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ServerSideSocket srv = new ServerSideSocket();
        srv.run();
    }
}
