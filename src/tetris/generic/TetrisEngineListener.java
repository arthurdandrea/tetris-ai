/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetris.generic;

/**
 *
 * @author arthur
 */
public interface TetrisEngineListener {

    public void onGameOver(TetrisEngine engine, int lastScore, int lastLines);

    public void onNewBlock(TetrisEngine engine);
}
