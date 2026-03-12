package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Getter
public class McdParserReader {

    public static final int ENTITY_RELATION_INDEX_OBJECT = 1;

    private final List<String> entities;

    private final List<String> relations;

    private final FileEntity file;

    public McdParserReader(FileEntity file) {
        this.entities = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.file = file;
    }

    public void parseFile() throws Exception {
        byte[] fileData = this.file.getFile();
        if (fileData == null || fileData.length == 0) return;

        try (ByteArrayInputStream input = new ByteArrayInputStream(fileData)) {
            ObjectInputStream oIn = new ObjectInputStream(input);
            int idxObj = 0;

            while (true) {
                try {
                    Object obj = oIn.readObject();
                    if (obj instanceof ArrayList && idxObj == ENTITY_RELATION_INDEX_OBJECT) {
                        this.parseItems((List<?>) obj);
                        break;
                    }
                    idxObj++;
                } catch (EOFException e) {
                    break;
                }
            }
            oIn.close();
        }
    }

        public void parseItems (List < ? > list) throws Exception {
            for (Object item : list) {
                if (item != null) {
                    String className = item.getClass().getName();

                    if (className.equals("IhmMCD2.IhmEntite2") || className.equals("IhmMCD.IhmEntite")) {

                        Method methodGetEntite = item.getClass().getMethod("getEntite");
                        Object entite = methodGetEntite.invoke(item);

                        Method methodGetNom = entite.getClass().getMethod("getNom");
                        String nomEntite = (String) methodGetNom.invoke(entite);
                        this.entities.add(nomEntite);
                    }
                    if (className.equals("IhmMCD2.IhmRelation2") || className.equals("IhmMCD.IhmRelation")) {

                        Method methodGetRelation = item.getClass().getMethod("getRelation");
                        Object relation = methodGetRelation.invoke(item);

                        Method methodGetNom = relation.getClass().getMethod("getNom");
                        String nomRelation = (String) methodGetNom.invoke(relation);
                        this.relations.add(nomRelation);
                    }
                }
            }
        }
}
