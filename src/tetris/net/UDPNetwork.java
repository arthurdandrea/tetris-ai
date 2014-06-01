/*
 * Copyright (C) 2014 Arthur D'Andréa Alemar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tetris.net;

import com.google.common.base.Function;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisMoveListener;
import static tetris.net.Network.HELLO;
import static tetris.net.Network.delimitadorChat;
import tetris.util.MyThread;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class UDPNetwork extends Network {
    private static final Logger logger = Logger.getLogger(UDPNetwork.class.getName());
    private boolean connected;
    private DatagramSocket socket;
    private int port;
    private DatagramSocket serverSocket;

    public UDPNetwork(TetrisEngine engine, TetrisEngine engine0) {
        super(engine, engine0);
    }

    @Override
    public boolean connect(InetAddress addr, int port) {
        if (this.connected) {
            return false;
        }
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.connect(addr, port);
            return processClientSideSocket(socket);
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private synchronized boolean processClientSideSocket(DatagramSocket clientSocket) {
        try {
            DatagramPacket packet = packetForString(HELLO, serverSocket);
        
            clientSocket.send(packet);
        
            byte[] buffer = new byte[65507];
        
            packet = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(packet);
            String helloMessage = new String(packet.getData(), 0, packet.getLength());
            if (!helloMessage.equals(HELLO)) {
                clientSocket.close();
                return false;
            }
            localEngine.reset();
            
            clientSocket.receive(packet);
            String completeState = new String(packet.getData(), 0, packet.getLength());
            remoteEngine.loadCompleteState(protocol.decodeCompleteState(completeState));
            packet = packetForString(protocol.encodeCompleteState(localEngine.dumpCompleteState()), socket);
            clientSocket.send(packet);
            startReadThread(clientSocket);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
    }


    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void sendChat(String string) {
        if (!this.connected) return;
        String encoded = delimitadorChat + this.protocol.encodeChat(string);
        byte[] buffer = encoded.getBytes(Charset.forName("ascii"));
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.send(packet);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void sendMove(TetrisEngine.MoveResult moveResult) {
        if (!this.connected) return;
        String encoded = delimitadorJogo + this.protocol.encodeMoveResult(moveResult);
        byte[] buffer = encoded.getBytes(Charset.forName("ascii"));
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.send(packet);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void start() {
        if (!this.tryOpenPort()) {
            return;
        }
        this.serverThread.start();
    }


    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void readLoop(MyThread.ThreadControl control) {
        byte[] buffer = new byte[65507];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (control.check()) {
            try {
                if (socket == null) break;
                socket.receive(packet);
                String linha = new String(packet.getData(), 0, packet.getLength());
                this.processLinha(linha);
            } catch (SocketException | InterruptedIOException ex) {
                closeSocket();
                break;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                closeSocket();
                break;
            }
        }
    }

    @Override
    protected void serverLoop(MyThread.ThreadControl control) {
        byte[] buffer = new byte[65507];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (control.check()) {
            
            try {
                this.serverSocket.receive(packet);
            } catch (SocketException ex) {
                break;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                continue;
            } 
            this.processServerSideSocket();
        }
    }

    private synchronized void processServerSideSocket() {
        try {
            byte[] buffer = new byte[65507];
            
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            String helloMessage = new String(packet.getData(), 0, packet.getLength());
            if (!helloMessage.equals(HELLO)) {
                return;
            }
            packet = packetForString(HELLO, serverSocket);
            serverSocket.send(packet);

            if (this.connected) {
                //newOutput.println(UNAVAILABLE);
                //newOutput.flush();
                
            } else {
                packet = packetForString(protocol.encodeCompleteState(localEngine.dumpCompleteState()), socket);
                serverSocket.send(packet);
                serverSocket.receive(packet);
                String completeState = new String(packet.getData(), 0, packet.getLength());
                remoteEngine.loadCompleteState(protocol.decodeCompleteState(completeState));
                
                this.connected = true;
                startReadThread(serverSocket);
            }
        } catch (IOException ex) {
            Logger.getLogger(UDPNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void startReadThread(DatagramSocket clientSocket) {
        this.socket = clientSocket;
        this.connected = true;
        this.readThread.startOrResume();
    }

    private synchronized void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    private boolean tryOpenPort() {
        try {
            DatagramSocket socket = new DatagramSocket();
            this.serverSocket = socket;
            this.port = socket.getLocalPort();
            return true;
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private static DatagramPacket packetForString(String string, DatagramSocket socket) {
        byte[] buffer = string.getBytes(Charset.forName("ascii"));
        return new DatagramPacket(buffer, buffer.length);
    }
}
