package de.bwaldvogel.liblinear;

import de.bwaldvogel.liblinear.MapDbFileListFactory.IndexTreeListWrapper;
import java.util.ArrayList;
import java.util.UUID;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;

/**
 * A MapDb backed list factory which writes to a off-heap memory.
 */

public class MapDbOffHeapListFactory extends ListFactory {

    @Override
    public ArrayList<FeatureVector> createList(int size) {
        String listId = UUID.randomUUID().toString();
        DB db = DBMaker.memoryDirectDB().make();
        return new IndexTreeListWrapper(
              (IndexTreeList<FeatureVector>) db.indexTreeList(listId, Serializer.JAVA).createOrOpen());

    }

    @Override
    public void close() {
        //NOOP
    }


}
