package de.bwaldvogel.liblinear;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;

/**
 * A MapDb backed list factory which writes to a temporary file.
 */

public class MapDbFileListFactory extends ListFactory {

    List<String> generatedFiles = Lists.newArrayList();

    @Override
    public ArrayList<FeatureVector> createList(int size) {
        String listId = UUID.randomUUID().toString();
        String tempFile = "/tmp/" + listId + ".db";
        generatedFiles.add(tempFile);
        DB db = DBMaker.fileDB(tempFile).make();
        return new IndexTreeListWrapper(
              (IndexTreeList<FeatureVector>) db.indexTreeList(listId, Serializer.JAVA).createOrOpen());

    }

    @Override
    public void close() throws IOException {
        for (String file : generatedFiles) {
            Files.deleteIfExists(Paths.get(file));
        }
    }

    public static class IndexTreeListWrapper extends ArrayList<FeatureVector> {

        private final IndexTreeList<FeatureVector> indexTreeList;

        public IndexTreeListWrapper(IndexTreeList<FeatureVector> indexTreeList) {
            this.indexTreeList = indexTreeList;
        }

        public FeatureVector set(int index, FeatureVector element) {
            return indexTreeList.set(index, element);
        }

        public FeatureVector get(int index) {
            return indexTreeList.get(index);
        }

        public boolean add(FeatureVector element) {
            return indexTreeList.add(element);
        }

        public int size() {
            return indexTreeList.size();
        }

        public Iterator<FeatureVector> iterator() {
            return indexTreeList.iterator();
        }
    }
}
