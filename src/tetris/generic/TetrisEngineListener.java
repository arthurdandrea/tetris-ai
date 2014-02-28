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

    public void onGameStateChange(TetrisEngine engine);
    
    public void onGameOver(TetrisEngine engine, Score lastScore);

    public void onNewBlock(TetrisEngine engine);
}
