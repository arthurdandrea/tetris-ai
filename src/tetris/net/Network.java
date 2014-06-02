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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisMoveListener;
import tetris.util.MyThread;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public abstract class Network {
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    protected static final String UNAVAILABLE = "to jogando po";
    protected static final String HELLO = "mamao";
    protected static final String delimitadorChat = "CHAT :";
    protected static final String delimitadorJogo = "JOGO :";
    
    protected final Protocol protocol;
    protected final TetrisEngine remoteEngine;
    protected final TetrisEngine localEngine;
    protected final MyThread serverThread;
    protected final MyThread readThread;
    private final List<MessageReciever> messageRecievers;
    private final PropertyChangeSupport propertyChangeSupport;
    private ConnectionState connectionState;
    private String connectionError;

    public Network(TetrisEngine local, TetrisEngine remote) {
        this.connectionState = ConnectionState.DISCONNECTED;
        this.protocol = Protocol.create();
        this.localEngine = local;
        this.remoteEngine = remote;
        this.messageRecievers = new ArrayList<>();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.localEngine.addMoveListener(new TetrisMoveListener() {
            @Override
            public void sucessfulMove(TetrisEngine.MoveResult move) {
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

    public final ConnectionState getConnectionState() {
        return this.connectionState;
    }
    
    public final String getConnectionError() {
        return this.connectionError;
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public synchronized void addMessageReciever(MessageReciever reciever) {
        this.messageRecievers.add(reciever);
    }
    
    protected void onConnecting() {
        assert this.connectionState == ConnectionState.DISCONNECTED;
        this.connectionState = ConnectionState.CONNECTING;
        this.connectionError = null;
        this.propertyChangeSupport.firePropertyChange("connectionState", ConnectionState.DISCONNECTED, ConnectionState.CONNECTING);
    }

    protected void onConnectionError(String error) {
        assert this.connectionState == ConnectionState.CONNECTING;
        this.connectionState = ConnectionState.DISCONNECTED;
        this.connectionError = error;
        this.propertyChangeSupport.firePropertyChange("connectionState", ConnectionState.CONNECTING, ConnectionState.DISCONNECTED);
    }

    protected void onConnected() {
        assert this.connectionState == ConnectionState.CONNECTING;
        this.connectionState = ConnectionState.CONNECTED;
        this.connectionError = null;
        this.propertyChangeSupport.firePropertyChange("connectionState", ConnectionState.CONNECTING, ConnectionState.CONNECTED);
    }
    
    protected void onDisconnected() {
        assert this.connectionState == ConnectionState.CONNECTED;
        this.connectionState = ConnectionState.DISCONNECTED;
        this.connectionError = null;
        this.propertyChangeSupport.firePropertyChange("connectionState", ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);
    }

    protected void processLinha(String linha) {
        if (linha == null || linha.isEmpty()) return;
        if (linha.startsWith(delimitadorChat)) {
            processLinhaChat(linha.substring(delimitadorChat.length()));
        } else if (linha.startsWith(delimitadorJogo)) {
            processLinhaJogo(linha.substring(delimitadorJogo.length()));
        } else {
            // TODO: ERRO
        }
    }

    protected void processLinhaJogo(String linha) {
        TetrisEngine.MoveResult moveResult = this.protocol.decodeMoveResult(linha);
        if (moveResult != null) {
            this.remoteEngine.tryMove(moveResult.move, moveResult.nextblock);
        }
    }

    protected synchronized void processLinhaChat(String linha) {
        String message = this.protocol.decodeChat(linha);
        for (MessageReciever messageReciever : messageRecievers) {
            messageReciever.messageRecieved(message);
        }
        System.out.println("recebeu chat: " + message);
    }

    public abstract boolean connect(InetAddress addr, int port);
    public abstract int getPort();
    public abstract void sendChat(String string);
    public abstract void sendMove(TetrisEngine.MoveResult moveResult);
    public abstract void start();
    public abstract void stop();
    public abstract SocketAddress getRemoteAddress();

    protected abstract void readLoop(MyThread.ThreadControl control);
    protected abstract void serverLoop(MyThread.ThreadControl control);
}
