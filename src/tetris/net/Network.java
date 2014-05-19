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

import tetris.util.MyThread;
import com.google.common.base.Function;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.generic.TetrisEngine.Move;

/**
 *
 * @author Arthur D'Andréa Alemar
 * @author Natali Silva Honda
 */
public class Network {
    private static final String UNAVAILABLE = "to jogando po";
    private static final String HELLO = "mamao";
    private static final String delimitadorChat = "CHAT :";
    private static final String delimitadorJogo = "JOGO :";
    private static final Logger logger = Logger.getLogger(Network.class.getName());

    private int port;
    private Socket socket;
    private boolean connected;
    private PrintStream out;
    private BufferedReader in;
    private ServerSocket serverSocket;
    private final MyThread serverThread;
    private final MyThread readThread;

    public Network() {
        this.serverThread = new MyThread(new Function<MyThread.ThreadControl, Void>(){
            @Override
            public Void apply(MyThread.ThreadControl control) {
                serverLoop(control);
                return null;
            }
        }, "Network Server Thread");
        this.readThread = new MyThread(new Function<MyThread.ThreadControl, Void>(){

            @Override
            public Void apply(MyThread.ThreadControl control) {
                readLoop(control);
                return null;
            }
        }, "Network Read Thread");
    }

    public void start() {
        if (!tryOpenPort()) {
            return;
        }
        this.serverThread.start();
    }

    public void stop() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        this.closeSocket();
        this.serverThread.stop();
        this.readThread.stop();
    }

    public boolean connect(InetAddress addr, int port) {
        try {
            System.out.println(addr);
            System.out.println(port);
            Socket clientSocket = new Socket(addr, port);
            return this.processClientSideSocket(clientSocket);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "erro ao conectar", ex);
            return false;
        }
    }

    private synchronized boolean processClientSideSocket(Socket clientSocket) {
        BufferedReader newInput = getInputStream(clientSocket);
        PrintStream newOutput = getOutputStrem(clientSocket);
        if (newInput == null || newOutput == null) return false;
        newOutput.println(HELLO);
        try {
            if (readHelloMessage(newInput)) {
                // RECOMECA JOGO
                //estado_completo_inimigo = newInput.readLine();
                //newOutput.println(meu_estado_completo);

                startReadThread(clientSocket, newOutput, newInput);
            } else {
                clientSocket.close();
                return false;
            }
        } catch (IOException e) {
            closeSocket(clientSocket);
            this.connected = false;
            return false;
        }
        this.connected = true;
        return true;

    }

    private static boolean readHelloMessage(BufferedReader newInput) {
        String line;
        try {
            line = newInput.readLine();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        return line.equals(HELLO);
    }

    private void startReadThread(Socket clientSocket, PrintStream clientOut, BufferedReader clientIn) {
        this.socket = clientSocket;
        this.out = clientOut;
        this.in = clientIn;
        this.connected = true;
        this.readThread.startOrResume();
    }

    private void serverLoop(MyThread.ThreadControl control) {
        while (control.check()) {
            Socket serverSideSocket;
            try {
                serverSideSocket = serverSocket.accept();
            } catch (SocketException ex) {
                break;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                continue;
            } 
            this.processServerSideSocket(serverSideSocket);
        }
    }

    private synchronized void processServerSideSocket(Socket serverSideSocket) {
        BufferedReader newInput = getInputStream(serverSideSocket);
        PrintStream newOutput = getOutputStrem(serverSideSocket);
        if (newInput == null || newOutput == null) return;
        if (readHelloMessage(newInput)) {
            newOutput.println(HELLO);

            if (this.connected) {
                newOutput.println(UNAVAILABLE);
                newOutput.flush();
                closeSocket(serverSideSocket);
            } else {
                // RECOMECA JOGO
                //newOutput.println(meu_estado_completo);
                //estado_completo_inimigo = newInput.readLine();

                this.connected = true;
                this.startReadThread(socket, newOutput, newInput);
            }
        }
    }

    private boolean tryOpenPort() {
        int tries = 0;
        this.serverSocket = null;
        while (tries < 10 && serverSocket == null) {
            try {
                this.serverSocket = new ServerSocket(0);
            } catch (BindException ex) {
                logger.log(Level.SEVERE, "erro ao abrir porta");
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return false;
            }
            tries++;
        }
        if (serverSocket != null) {
            this.port = serverSocket.getLocalPort();
            return true;
        } else {
            return false;
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void sendChat(String string) {
        if (!this.connected) return;

        String[] splitStrings = string.split("\n");
        for (String parte : splitStrings) {
            this.out.println(delimitadorChat + parte);
            this.out.flush();
        }
    }

    public void sendMove(Move move) {
        Objects.requireNonNull(move);

        if (!this.connected) return;

        this.out.println(delimitadorJogo + move.ordinal());
        this.out.flush();
    }

    private void readLoop(MyThread.ThreadControl control) {
        while (control.check()) {
            try {
                String linha = in.readLine();
                if (linha.startsWith(delimitadorChat)) {
                    processLinhaChat(linha.substring(delimitadorChat.length()));
                } else if (linha.startsWith(delimitadorJogo)) {
                    processLinhaJogo(linha.substring(delimitadorJogo.length()));
                } else {
                    // TODO: ERRO
                }
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

    private synchronized void closeSocket() {
        try {
            if (this.out != null) {
                this.out.close();
            }
            if (this.in != null) {
                this.in.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ex1) {
            logger.log(Level.SEVERE, "erro ao fechar socket", ex1);
        } finally {
            this.connected = false;
            this.in = null;
            this.out = null;

            this.socket = null;
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ex1) {
            logger.log(Level.SEVERE, "erro ao fechar socket", ex1);
        }
    }
    
    private static BufferedReader getInputStream(Socket socket) {
        InputStream inputStream;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "o socket deveria ser 'readable'", ex);
            return null;
        }
        return new BufferedReader(new InputStreamReader(inputStream));
    }
    
    private static PrintStream getOutputStrem(Socket socket) {
        OutputStream outputStream;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "o socket deveria ser 'writable'", ex);
            return null;
        }
        return new PrintStream(outputStream);
    }

    private void processLinhaJogo(String linha) {
        Move move = Move.values()[Integer.parseInt(linha, 10)];
    }

    private void processLinhaChat(String linha) {
        System.out.println("recebeu chat: " + linha);
    }

    public int getPort() {
        return this.port;
    }

}
