package tetris.ai;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import tetris.generic.BlockMover;
import tetris.generic.BlockPosition;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.GameState;

public abstract class AbstractAI {
    protected final ListeningExecutorService executor;

    public AbstractAI(ListeningExecutorService executor) {
        this.executor = executor;
    }

    protected abstract ListenableFuture<BlockPosition> computeBestFit(TetrisEngine engine);

    public ListenableFuture<BlockMover> process(final TetrisEngine engine) {
        if (engine.getState() != GameState.PLAYING) {
            return Futures.immediateFuture(null);
        }
        return Futures.transform(this.computeBestFit(engine), new Function<BlockPosition, BlockMover>() {
            @Override
            public BlockMover apply(BlockPosition temp) {
                return new BlockMover(engine, temp);
            }
        });
    }
}
