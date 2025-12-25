package cachedb;

public class FlushTask {

    public final RowMutation mutation;

    public FlushTask(RowMutation mutation) {
        this.mutation = mutation;
    }
}
