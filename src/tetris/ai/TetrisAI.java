package tetris.ai;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.List;
import java.util.concurrent.Callable;
import tetris.generic.TetrisEngine;
import tetris.generic.Tetromino.Type;

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

    private static class GetPossibleFits implements Callable<List<BlockPosition>> {
        private final TetrisEngine engine;
        private final Type type;

        public GetPossibleFits(TetrisEngine engine, Type type) {
            this.engine = engine;
            this.type = type;
        }

        @Override
        public List<BlockPosition> call() throws Exception {
            return getPossibleFits(engine, type);
        }
    }

    private class EvalPosition implements Callable<Double> {

        private final BlockPosition nextPosition;
        private final BlockPosition curPosition;
        private final TetrisEngine engine;

        public EvalPosition(TetrisEngine engine, BlockPosition curPosition, BlockPosition nextPosition) {
            this.engine = engine;
            this.curPosition = curPosition;
            this.nextPosition = nextPosition;
        }

        @Override
        public Double call() throws Exception {
            return evalPosition(engine, curPosition, nextPosition);
        }

    }

    private class ConsumeChoices extends AbstractFuture<BlockPosition> {
        private double max;
        private BlockPosition max_b;
        private final List<BlockPosition> posfits;
        private final List<BlockPosition> posfits2;
        private final int length;

        ConsumeChoices(List<BlockPosition> posfits, List<BlockPosition> posfits2, ListenableFuture<Double>[] futures) {
            this.max = Double.NEGATIVE_INFINITY;
            this.max_b = null;
            this.posfits = posfits;
            this.posfits2 = posfits2;
            this.length = futures.length - 1;
            for (int i = 0; i < futures.length; i++) {
                this.consume(i, futures[i]);
            }
        }

        private void consume(final int index, final ListenableFuture<Double> future) {
            Futures.addCallback(future, new FutureCallback<Double>() {
                @Override
                public void onSuccess(Double score) {
                    if (score >= max) {
                        max_b = posfits.get(index / posfits2.size());
                        max = score;
                    }
                    if (index == length) {
                        set(max_b);
                    }
                }

                @Override
                public void onFailure(Throwable thrwbl) {
                    setException(thrwbl);
                }
            }, executor);
        }
    }

    @Override
    protected ListenableFuture<BlockPosition> computeBestFit(final TetrisEngine engine) {
        ListenableFuture<List<BlockPosition>>
                posfitsF = executor.submit(new GetPossibleFits(engine, engine.getActiveblock().type)),
                posfits2F = executor.submit(new GetPossibleFits(engine, engine.getNextblock().type));
        return Futures.transform(Futures.allAsList(posfitsF, posfits2F), new AsyncFunction<List<List<BlockPosition>>, BlockPosition>() {
            @Override
            public ListenableFuture<BlockPosition> apply(List<List<BlockPosition>> results) {
                List<BlockPosition> posfits = results.get(0);
                List<BlockPosition> posfits2 = results.get(1);
                ListenableFuture<Double>[] futures = new ListenableFuture[posfits.size() * posfits2.size()];
                for (int i = 0; i < posfits.size(); i++) {
                    for (int j = 0; j < posfits2.size(); j++) {
                        int index = i * posfits2.size() + j;
                        futures[index] = executor.submit(new EvalPosition(engine, posfits.get(i), posfits2.get(j)));
                    }
                }
                return new ConsumeChoices(posfits, posfits2, futures);
            }
        }, executor);
    }

    // Evaluate position not with one, but with two blocks.
    double evalPosition(TetrisEngine ge, BlockPosition p, BlockPosition q) {
        // First thing: Simulate the drop. Do this on a mock grid.
        // copying it here may seem like a waste but clearing it
        // after each iteration is too much of a hassle.
        // This copies the grid.
        byte[][] mockgrid = ge.getMockGrid();

        int cleared = 0;
        for (int block = 1; block <= 2; block++) {

            byte[][] bl;
            BlockPosition r;

            if (block == 1) {
                r = p;
            } else {
                r = q;
            }

            if (block == 1) {
                bl = TetrisEngine.blockdef[ge.getActiveblock().type.ordinal()][r.rot];
            } else {
                bl = TetrisEngine.blockdef[ge.getNextblock().type.ordinal()][r.rot];
            }

            // Now we find the fitting HEIGHT by starting from the bottom and
            // working upwards. If we're fitting a line-block on an empty
            // grid then the HEIGHT would be HEIGHT-1, and it can't be any
            // lower than that, so that's where we'll start.
            int h;
            for (h = ge.HEIGHT - 1;; h--) {

                // indicator. 1: fits. 0: doesn't fit. -1: game over.
                int fit_state = 1;

                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        //check for bounds.
                        boolean block_p = bl[j][i] >= 1;

                        //we have to simulate lazy evaluation in order to avoid
                        //out of bounds errors.
                        if (block_p) {
                            //still have to check for overflow. X-overflow can't
                            //happen at this stage but Y-overflow can.

                            if (h + j >= ge.HEIGHT) {
                                fit_state = 0;
                            } else if (h + j < 0) {
                                fit_state = -1;
                            } else {
                                boolean board_p = mockgrid[i + r.bx][h + j] >= 1;

                                // Already filled, doesn't fit.
                                if (board_p) {
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
                    return -99999999;
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
            } while (foundline);
        }

        // Now we evaluate the resulting position.
        // Part of the evaluation algorithm is to count the number of touching sides.
        // We do this by generating all pairs and seeing how many them are touching.
        // If they add up to 3, it means one of them is from the active block and the
        // other is a normal block (ie. they're touching).
        double score = 0.0;

        //horizontal pairs
        for (int i = 0; i < ge.HEIGHT; i++) {
            for (int j = 0; j < ge.WIDTH - 1; j++) {
                if (j == 0 && mockgrid[j][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (j + 1 == ge.WIDTH - 1 && mockgrid[j + 1][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (mockgrid[j][i] + mockgrid[j + 1][i] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        //vertical pairs
        for (int i = 0; i < ge.WIDTH; i++) {
            for (int j = 0; j < ge.HEIGHT - 1; j++) {
                if (j + 1 == ge.HEIGHT - 1 && mockgrid[i][j + 1] == 2) {
                    score += _TOUCHING_FLOOR;
                }
                if (mockgrid[i][j] + mockgrid[i][j + 1] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        // Penalize HEIGHT.
        for (int i = 0; i < ge.WIDTH; i++) {
            for (int j = 0; j < ge.HEIGHT; j++) {
                int curheight = ge.HEIGHT - j;
                if (mockgrid[i][j] > 0) {
                    score += curheight * _HEIGHT;
                }
            }
        }

        //Penalize holes. Also penalize blocks above holes.
        for (int i = 0; i < ge.WIDTH; i++) {
            // Part 1: Count how many holes (space beneath blocks)
            boolean f = false;
            int holes = 0;
            for (int j = 0; j < ge.HEIGHT; j++) {
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
            for (int j = ge.HEIGHT - 1; j >= 0; j--) {
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

        /*
         * for (int i1 = 0; i1 < mockgrid.length; i1++) { for (int i2 = 0; i2 <
         * mockgrid[0].length; i2++) { System.out.print(mockgrid[i1][i2] + " ");
         * } System.out.println(); }
         System.out.println(score);
         */
        //System.exit(0);
        return score;
    }
}
