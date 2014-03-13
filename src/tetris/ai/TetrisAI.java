package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Iterator;
import tetris.generic.TetrisEngine;
import tetris.util.FutureExtremes;
import tetris.util.functional.CartesianProduct;
import tetris.util.functional.CartesianProduct.Pair;

/*
 * This is the default tetris playing AbstractAI. It holds a reference to the tetris
 * engines so it can send key events when necessary and it knows the current block
 */
public class TetrisAI extends AbstractAI {

    // Constants (sort of) for score evaluation.
    public double _TOUCHING_EDGES = 3.97;
    public double _TOUCHING_WALLS = 6.52;
    public double _TOUCHING_FLOOR = 0.65;
    public double _HEIGHT = -3.78;
    public double _HOLES = -2.31;
    public double _BLOCKADE = -0.59;
    public double _CLEAR = 1.6;

    public TetrisAI(ListeningExecutorService executor) {
        super(executor);
    }
    
    public void MakeItDumb() {
        _TOUCHING_EDGES = -3.97;
        _TOUCHING_WALLS = -6.52;
        _TOUCHING_FLOOR = -0.65;
        _HEIGHT = +3.78;
        _HOLES = +2.31;
        _BLOCKADE = +0.59;
        _CLEAR = -1.6;
    }
    
    @Override
    protected ListenableFuture<BlockPosition> computeBestFit(final TetrisEngine engine) {
        BlockPosition[] currentPositions = engine.defs.getPossibleFits(engine.getActiveblock().type);
        BlockPosition[] nextPositions = engine.defs.getPossibleFits(engine.getNextblock().type);
        Iterator<Pair<BlockPosition>>
                cartesian = new CartesianProduct<>(currentPositions, nextPositions);
        
        ListenableFuture<BestFit> futureBestFit = FutureExtremes.calculate(cartesian, new EvalPosition(engine), executor, FutureExtremes.Extreme.MAX);
        return Futures.transform(futureBestFit, new Function<BestFit, BlockPosition>() {
            @Override
            public BlockPosition apply(BestFit input) {
                return input.first;
            }
        });
    }

    private int evalPosition(byte[][] mockgrid, TetrisEngine engine, BlockPosition r) throws GameOverException {        
        byte[][] bl = TetrisEngine.blockdef[r.type.ordinal()][r.rot];
        int cleared = 0;

        // Now we find the fitting HEIGHT by starting from the bottom and
        // working upwards. If we're fitting a line-block on an empty
        // grid then the HEIGHT would be HEIGHT-1, and it can't be any
        // lower than that, so that's where we'll start.
        int h;
        for (h = engine.defs.height - 1; ; h--) {
            // indicator. 1: fits. 0: doesn't fit. -1: game over.
            int fit_state = 1;

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    //we have to simulate lazy evaluation in order to avoid
                    //out of bounds errors.
                    if (bl[j][i] >= 1) {
                        //still have to check for overflow. X-overflow can't
                        //happen at this stage but Y-overflow can.

                        if (h + j >= engine.defs.height) {
                            fit_state = 0;
                        } else if (h + j < 0) {
                            fit_state = -1;
                        } else {
                            // Already filled, doesn't fit.
                            if (mockgrid[i + r.bx][h + j] >= 1) {
                                fit_state = 0;
                            }

                            // Still the possibility that another block
                            // might still be over it.
                            if (fit_state == 1) {
                                for (int h1 = h + j - 1; h1 >= 0; h1--) {
                                    if (mockgrid[i + r.bx][h1] >= 1) {
                                        fit_state = 0;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //We don't want game over so here:
            if (fit_state == -1) {
                throw new GameOverException();
            }

            //1 = found!
            if (fit_state == 1) {
                break;
            }
        }

        // copy over block position
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (bl[j][i] == 1) {
                    mockgrid[r.bx + i][h + j] = 2;
                }
            }
        }

        // check for clears
        boolean foundline;
        do {
            foundline = false;
            ML:
            for (int i = mockgrid[0].length - 1; i >= 0; i--) {
                for (int y = 0; y < mockgrid.length; y++) {
                    if (!(mockgrid[y][i] > 0)) {
                        continue ML;
                    }
                }
                // line i is full, clear it and copy
                cleared++;
                foundline = true;
                for (int a = i; a > 0; a--) {
                    for (int y = 0; y < mockgrid.length; y++) {
                        mockgrid[y][a] = mockgrid[y][a - 1];
                    }
                }
                break ML;
            }
        } while (foundline && cleared < 10);
        return cleared;
    }
    
    // Evaluate position not with one, but with two blocks.
    private BestFit evalPosition(TetrisEngine engine, BlockPosition position1, BlockPosition position2) {
        // First thing: Simulate the drop. Do this on a mock grid.
        // copying it here may seem like a waste but clearing it
        // after each iteration is too much of a hassle.
        // This copies the grid.
        byte[][] mockgrid = engine.createMockGrid();

        int cleared = 0;
        try {
            cleared += this.evalPosition(mockgrid, engine, position1);
            cleared += this.evalPosition(mockgrid, engine, position2);
        } catch (GameOverException e) {
            return new BestFit(position1, position2, Double.NEGATIVE_INFINITY);
        }

        // Now we evaluate the resulting position.
        // Part of the evaluation algorithm is to count the number of touching sides.
        // We do this by generating all pairs and seeing how many them are touching.
        // If they add up to 3, it means one of them is from the active block and the
        // other is a normal block (ie. they're touching).
        double score = 0.0;

        //horizontal pairs
        for (int i = 0; i < engine.defs.height; i++) {
            for (int j = 0; j < engine.defs.width - 1; j++) {
                if (j == 0 && mockgrid[j][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (j + 1 == engine.defs.width - 1 && mockgrid[j + 1][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (mockgrid[j][i] + mockgrid[j + 1][i] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        //vertical pairs
        for (int i = 0; i < engine.defs.width; i++) {
            for (int j = 0; j < engine.defs.height - 1; j++) {
                if (j + 1 == engine.defs.height - 1 && mockgrid[i][j + 1] == 2) {
                    score += _TOUCHING_FLOOR;
                }
                if (mockgrid[i][j] + mockgrid[i][j + 1] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        // Penalize HEIGHT.
        for (int i = 0; i < engine.defs.width; i++) {
            for (int j = 0; j < engine.defs.height; j++) {
                int curheight = engine.defs.height - j;
                if (mockgrid[i][j] > 0) {
                    score += curheight * _HEIGHT;
                }
            }
        }

        //Penalize holes. Also penalize blocks above holes.
        for (int i = 0; i < engine.defs.width; i++) {
            // Part 1: Count how many holes (space beneath blocks)
            boolean f = false;
            int holes = 0;
            for (int j = 0; j < engine.defs.height; j++) {
                if (mockgrid[i][j] > 0) {
                    f = true;
                }
                if (f && mockgrid[i][j] == 0) {
                    holes++;
                }
            }

            // Part 2: Count how many blockades (block above space)
            f = false;
            int blockades = 0;
            for (int j = engine.defs.height - 1; j >= 0; j--) {
                if (mockgrid[i][j] == 0) {
                    f = true;
                }
                if (f && mockgrid[i][j] > 0) {
                    blockades++;
                }
            }

            score += _HOLES * holes;
            score += _BLOCKADE * blockades;
        }

        score += cleared * _CLEAR;
        return new BestFit(position1, position2, score);
    }

    private static class BestFit implements Comparable<BestFit> {
        public final BlockPosition first;
        public final BlockPosition second;
        public final double score;

        BestFit(BlockPosition first, BlockPosition second, double score) {
            this.first = first;
            this.second = second;
            this.score = score;
        }

        @Override
        public int compareTo(BestFit o) {
            return Double.compare(score, o.score);
        }
    }

    private static class GameOverException extends Exception {
    }

    private class EvalPosition implements Function<Pair<BlockPosition>, BestFit> {
        private final TetrisEngine engine;

        EvalPosition(TetrisEngine engine) {
            this.engine = engine;
        }

        @Override
        public BestFit apply(Pair<BlockPosition> pair) {
            return evalPosition(engine, pair.first, pair.second);
        }
    }
}
