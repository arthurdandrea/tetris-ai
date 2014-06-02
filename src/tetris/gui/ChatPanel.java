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

package tetris.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import tetris.net.MessageReciever;
import tetris.net.Network;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class ChatPanel extends JPanel {
    private final JTextArea chatTextArea;
    private final JButton sendButton;
    private final JScrollPane chatScrollPane;
    private final JScrollPane messageScrollPane;
    private final JTextArea messageTextArea;
    private final Network network;
    private JPanel bottomPanel;

    public ChatPanel(Network network) {
        this.network = network;
        this.chatScrollPane = new JScrollPane();
        this.chatTextArea = new JTextArea();
        this.messageScrollPane = new JScrollPane();
        this.messageTextArea = new JTextArea();
        this.sendButton = new JButton();
        this.network.addMessageReciever(new MessageReciever() {
            @Override
            public void messageRecieved(String message) {
                writeMessage("Inimigo disse: " + message);
            }
        });
        
        this.initComponents();
    }
    
    public void setFocusToTextArea() {
        this.messageTextArea.requestFocusInWindow();
    }
    
    private void initComponents() {
        this.chatTextArea.setEditable(false);
        this.chatTextArea.setLineWrap(true);
        this.chatScrollPane.setViewportView(chatTextArea);

        this.messageScrollPane.setMinimumSize(new Dimension(400, 400));
        this.messageScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        this.messageTextArea.setLineWrap(true);
        this.messageTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                mensageTextAreaKeyReleased(evt);
            }
        });
        this.messageScrollPane.setViewportView(messageTextArea);

        this.sendButton.setText("Enviar");
        this.sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        this.bottomPanel = new JPanel();
        this.bottomPanel.setLayout(new BoxLayout(this.bottomPanel, BoxLayout.LINE_AXIS));
        this.bottomPanel.add(this.messageScrollPane);
        this.bottomPanel.add(this.sendButton);
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(this.chatScrollPane);
        this.add(this.bottomPanel);
    }

    private void sendButtonActionPerformed(ActionEvent evt) {
        this.sendMessage();
    }

    private void mensageTextAreaKeyReleased(KeyEvent evt) {                                              
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.consume();
            this.sendMessage();
        }
        
    }
    
    private void sendMessage() {
        String text = this.messageTextArea.getText().trim();
        if (!text.isEmpty()) {
            this.network.sendChat(text);
            this.writeMessage("Você disse: " + text);
            this.messageTextArea.setText("");
        }
    }

    private void writeMessage(String texto){
        this.chatTextArea.append(texto + "\n");
        if (!this.chatTextArea.getText().isEmpty() && !this.chatTextArea.isFocusOwner()) {
            this.chatTextArea.setCaretPosition(this.chatTextArea.getText().length() - 1);
        }
    }

    public void clear() {
        this.chatTextArea.setText("");
        this.messageTextArea.setText("");
    }
}
