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
import java.net.InetAddress;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisMoveListener;
import tetris.util.MyThread;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public abstract class Network {
    protected static final String UNAVAILABLE = "to jogando po";
    protected static final String HELLO = "mamao";
    protected static final String delimitadorChat = "CHAT :";
    protected static final String delimitadorJogo = "JOGO :";
    
    protected final Protocol protocol;
    protected final TetrisEngine remoteEngine;
    protected final TetrisEngine localEngine;
    protected final MyThread serverThread;
    protected final MyThread readThread;
    
    public Network(TetrisEngine local, TetrisEngine remote) {
        this.protocol = Protocol.create();
        this.localEngine = local;
        this.remoteEngine = remote;

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

    protected void processLinhaChat(String linha) {
        String chat = this.protocol.decodeChat(linha);
        System.out.println("recebeu chat: " + chat);
    }

    public abstract boolean connect(InetAddress addr, int port);
    public abstract int getPort();
    public abstract boolean isConnected();
    public abstract void sendChat(String string);
    public abstract void sendMove(TetrisEngine.MoveResult moveResult);
    public abstract void start();
    public abstract void stop();

    protected abstract void readLoop(MyThread.ThreadControl control);
    protected abstract void serverLoop(MyThread.ThreadControl control);
}
