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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.Move;
import tetris.generic.TetrisEngine.MoveResult;
import tetris.generic.TetrisMoveListener;
import tetris.generic.Tetromino;
import tetris.util.MyThread;

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

    private final TetrisEngine remoteEngine;
    private final TetrisEngine localEngine;

    public Network(TetrisEngine local, TetrisEngine remote) {
        this.localEngine = local;
        this.remoteEngine = remote;

        this.localEngine.addMoveListener(new TetrisMoveListener() {
            @Override
            public void sucessfulMove(MoveResult move) {
                sendMove(move);
            }
        });
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
                
                remoteEngine.unserializeCompleteState(newInput.readLine());
                newOutput.println(localEngine.serializeCompleteState());
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
                try {
                    newOutput.println(localEngine.serializeCompleteState());
                    newOutput.flush();
                    remoteEngine.unserializeCompleteState(newInput.readLine());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    closeSocket(serverSideSocket);
                    return;
                }

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

    public void sendMove(MoveResult move) {
        Objects.requireNonNull(move);

        if (!this.connected) return;
        StringBuilder builder = new StringBuilder();
        builder.append(delimitadorJogo);
        builder.append(move.move.ordinal());
        if (move.nextblock != null) {
            builder.append(' ').append(move.nextblock.type.ordinal());
            builder.append(' ').append(move.nextblock.x);
            builder.append(' ').append(move.nextblock.y);
            builder.append(' ').append(move.nextblock.rot);
        }
        this.out.println(builder.toString());
        this.out.flush();
    }

    private void readLoop(MyThread.ThreadControl control) {
        while (control.check()) {
            try {
                if (in == null) break;
                String linha = in.readLine();
                if (linha == null || linha.isEmpty()) break;
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
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "erro ao fechar socket", ex);
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

    private void processLinhaJogo(String linha) {
        Pattern REGEX = Pattern.compile("^(\\d+)( (\\d+) (\\d+) (\\d+) (\\d+))?$");
        Matcher matcher = REGEX.matcher(linha);
        if (!matcher.find()) {
            logger.log(Level.WARNING, "erro ao parsear string {0}", linha);
            return;
        }
        Move move = Move.values()[Integer.parseInt(matcher.group(1), 10)];
        if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
//            builder.append(' ').append(move.nextblock.type.ordinal());
//            builder.append(' ').append(move.nextblock.x);
//            builder.append(' ').append(move.nextblock.y);
//            builder.append(' ').append(move.nextblock.rot);

            Tetromino.Type type = Tetromino.Type.values()[Integer.parseInt(matcher.group(3))];
            int x = Integer.parseInt(matcher.group(4));
            int y = Integer.parseInt(matcher.group(5));
            int rot = Integer.parseInt(matcher.group(6));
            Tetromino nextblock = new Tetromino(type, rot);
            nextblock.x = x;
            nextblock.y = y;
            this.remoteEngine.tryMove(move, nextblock);
        } else {
            this.remoteEngine.tryMove(move);
        }
        
    }

    private void processLinhaChat(String linha) {
        System.out.println("recebeu chat: " + linha);
    }

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
