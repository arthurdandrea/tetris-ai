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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tetris.generic.Block;
import tetris.generic.Definitions;
import tetris.generic.TetrisEngine.CompleteState;
import tetris.generic.TetrisEngine.Move;
import tetris.generic.TetrisEngine.MoveResult;
import tetris.generic.Tetromino;

/**
 *
 * @author Arthur D'Andréa Alemar
 */
public final class Protocol {
    private static final Logger logger = Logger.getLogger(Protocol.class.getName());
    private static final Pattern GAME_REGEX = Pattern.compile("^(\\d+)( (\\d+) (\\d+) (\\d+) (\\d+))?$");
    private static final Pattern blocksPattern = Pattern.compile("^blocks\\[(\\d+)\\]\\[(\\d+)\\]=(\\d+),(\\d+)$");
    private static final Pattern tetrominoPattern = Pattern.compile("^(active|next)block=(\\d+),(\\d+),(\\d+),(\\d+)$");


    public String encodeMoveResult(MoveResult moveResult) {
        StringBuilder builder = new StringBuilder();
        builder.append(moveResult.move.ordinal());
        if (moveResult.nextblock != null) {
            builder.append(' ').append(moveResult.nextblock.type.ordinal());
            builder.append(' ').append(moveResult.nextblock.x);
            builder.append(' ').append(moveResult.nextblock.y);
            builder.append(' ').append(moveResult.nextblock.rot);
        }
        return builder.toString();
    }
    
    public MoveResult decodeMoveResult(String linha) {
        Matcher matcher = GAME_REGEX.matcher(linha);
        if (!matcher.find()) {
            logger.log(Level.WARNING, "erro ao parsear string {0}", linha);
            return null;
        }
        Move move = Move.values()[Integer.parseInt(matcher.group(1), 10)];
        if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
            Tetromino.Type type = Tetromino.Type.values()[Integer.parseInt(matcher.group(3))];
            int x = Integer.parseInt(matcher.group(4));
            int y = Integer.parseInt(matcher.group(5));
            int rot = Integer.parseInt(matcher.group(6));
            Tetromino nextblock = new Tetromino(type, rot);
            nextblock.x = x;
            nextblock.y = y;
            return new MoveResult(move, true, nextblock);
        } else {
            return new MoveResult(move, true, null);
        }

    }
    public String encodeChat(String chat) {
        return chat.trim().split("\n")[0];
    }
    
    public String decodeChat(String chat) {
        return chat;
    }
    
    public String encodeCompleteState(CompleteState state) {
        StringBuilder builder = new StringBuilder();
        builder.append(state.definitions.width).append('x');
        builder.append(state.definitions.height).append(' ');
        for (int i = 0; i < state.blocks.length; i++) {
            for (int j = 0; j < state.blocks[i].length; j++) {
                Block block = state.blocks[i][j];
                if (block.getState() != 0) {
                    builder.append("blocks[").append(i)
                            .append("][").append(j).append("]=")
                            .append(block.getState()).append(',')
                            .append(block.getType().ordinal())
                            .append(' ');
                }
            }
        }
        if (state.activeblock != null) {
            builder.append("activeblock=")
                    .append(state.activeblock.type.ordinal()).append(',')
                    .append(state.activeblock.rot).append(',')
                    .append(state.activeblock.x).append(',')
                    .append(state.activeblock.y).append(' ');
        }
        if (state.nextblock != null) {
            builder.append("nextblock=")
                    .append(state.nextblock.type.ordinal()).append(',')
                    .append(state.nextblock.rot).append(',')
                    .append(state.nextblock.x).append(',')
                    .append(state.nextblock.y).append(' ');
        }
        return builder.toString();
    }
    
    public CompleteState decodeCompleteState(String input) {
        CompleteState state = new CompleteState();
        
        String size = input.substring(0, input.indexOf(' '));
        input = input.substring(input.indexOf(' ') + 1);
        
        String[] splitSize = size.split("x");
        int width = Integer.parseInt(splitSize[0]);
        int height = Integer.parseInt(splitSize[1]);
        state.definitions = new Definitions(width, height);
        state.blocks = new Block[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                state.blocks[i][j] = new Block(Block.EMPTY, null);
            }
        }
        for (String string : input.split(" ")) {
            Matcher blocksMatcher = blocksPattern.matcher(string);
            Matcher tetrominoMatcher = tetrominoPattern.matcher(string);
            
            if (blocksMatcher.find()) {
                int i = Integer.parseInt(blocksMatcher.group(1));
                int j = Integer.parseInt(blocksMatcher.group(2));
                int blockState = Integer.parseInt(blocksMatcher.group(3));
                int type = Integer.parseInt(blocksMatcher.group(4));

                state.blocks[i][j].setState(blockState);
                state.blocks[i][j].setType(Tetromino.Type.values()[type]);
            } else if (tetrominoMatcher.find()) {
                String activeOrNext = tetrominoMatcher.group(1);
                int type = Integer.parseInt(tetrominoMatcher.group(2));
                int rot = Integer.parseInt(tetrominoMatcher.group(3));
                int x = Integer.parseInt(tetrominoMatcher.group(4));
                int y = Integer.parseInt(tetrominoMatcher.group(5));

                if ("active".equals(activeOrNext)) {
                    state.activeblock = new Tetromino(Tetromino.Type.values()[type], rot);
                    state.activeblock.x = x;
                    state.activeblock.y = y;
                } else {
                    state.nextblock = new Tetromino(Tetromino.Type.values()[type], rot);
                    state.nextblock.x = x;
                    state.nextblock.y = y;
                }
            }
        }
        System.out.println(encodeCompleteState(state));
        return state;
    } 
}
