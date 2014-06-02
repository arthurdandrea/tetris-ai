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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.MoveResult;
import tetris.util.MyThread;

/**
 *
 * @author Arthur D'Andréa Alemar
 * @author Natali Silva Honda
 */
public class TCPNetwork extends Network {
    private static final Logger logger = Logger.getLogger(TCPNetwork.class.getName());

    private int port;
    private Socket socket;
    private boolean connected;
    private PrintStream out;
    private BufferedReader in;
    private ServerSocket serverSocket;
    private SocketAddress remoteAddress;

    public TCPNetwork(TetrisEngine local, TetrisEngine remote) {
        super(local, remote);
    }

    @Override
    public void start() {
        if (!tryOpenPort()) {
            return;
        }
        this.serverThread.start();
    }

    @Override
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

    @Override
    public boolean connect(InetAddress addr, int port) {
        try {
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
                localEngine.reset();
                remoteEngine.loadCompleteState(protocol.decodeCompleteState(newInput.readLine()));
                newOutput.println(protocol.encodeCompleteState(localEngine.dumpCompleteState()));
                newOutput.flush();

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
        this.remoteAddress = this.socket.getRemoteSocketAddress();
        this.out = clientOut;
        this.in = clientIn;
        this.connected = true;
        this.readThread.startOrResume();
        this.onConnected();
    }

    @Override
    protected void serverLoop(MyThread.ThreadControl control) {
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
                try {
                    localEngine.reset();
                    newOutput.println(protocol.encodeCompleteState(localEngine.dumpCompleteState()));
                    newOutput.flush();
                    remoteEngine.loadCompleteState(protocol.decodeCompleteState(newInput.readLine()));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    closeSocket(serverSideSocket);
                    return;
                }

                this.connected = true;
                this.startReadThread(serverSideSocket, newOutput, newInput);
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

    @Override
    public synchronized boolean isConnected() {
        return this.connected;
    }

    @Override
    public synchronized SocketAddress getRemoteAddress() {
        if (this.connected) {
            return this.remoteAddress;
        } else {
            return null;
        }
    }

    @Override
    public void sendChat(String string) {
        if (!this.connected) return;

        String encoded = this.protocol.encodeChat(string);
        this.out.print(delimitadorChat);
        this.out.println(encoded);
        this.out.flush();
    }

    @Override
    public void sendMove(MoveResult moveResult) {
        Objects.requireNonNull(moveResult);
        if (!this.connected) return;

        String encoded = this.protocol.encodeMoveResult(moveResult);
        this.out.print(delimitadorJogo);
        this.out.println(encoded);
        this.out.flush();
    }

    @Override
    protected void readLoop(MyThread.ThreadControl control) {
        while (control.check()) {
            try {
                if (in == null) break;
                String linha = in.readLine();
                if (linha == null || linha.isEmpty()) break;
                processLinha(linha);
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
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "erro ao fechar socket", ex);
        } finally {
            this.connected = false;
            this.in = null;
            this.out = null;
            this.socket = null;
            this.remoteAddress = null;
            this.onDisconnected();
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "erro ao fechar socket", ex);
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

    @Override
    public int getPort() {
        return this.port;
    }

    public static URI parseHostPort(String input) {
        URI uri;
        try {
            // WORKAROUND: add any scheme to make the resulting URI valid.
            uri = new URI("my://" + input); // may throw URISyntaxException
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || port == -1) {
              throw new URISyntaxException(uri.toString(),
                "URI must have host and port parts");
            }

            // here, additional checks can be performed, such as
            // presence of path, query, fragment, ...

        } catch (URISyntaxException ex) {
            return null;
        }
        return uri;
    }
}
