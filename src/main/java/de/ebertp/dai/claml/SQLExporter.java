package de.ebertp.dai.claml;

import de.ebertp.dai.claml.model.ClaMLClass;
import de.ebertp.dai.claml.model.ClaMLRoot;
import de.ebertp.dai.claml.model.ClaMLRubric;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.exit;

/**
 * Convert an ICD-O-3 ClaML file to SQL and print the result.
 */
public class SQLExporter {

    public static void main(String[] args) {
        File input;

        //if no parameters are give, use default location
        if (args.length == 1) {
            input = new File(args[0]);
        } else {
            input = new File("clamlfile.xml");
        }

        ClaMLRoot claMLRoot = null;
        try {
            claMLRoot = new ClaMLParser().parseClaMLFromXML(input);
        } catch (DocumentException e1) {
            System.err.println("ClaMLParser: Error parsing ClaML File, exiting!");
            e1.printStackTrace();
        }

        if (claMLRoot == null) {
            System.err.println("claMLRoot is null!");
            exit(1);
        }

        ClaMLClass topology = claMLRoot.getChildren().get("T");
        ClaMLClass morphology = claMLRoot.getChildren().get("M");

        List<Icdo> topologies = new LinkedList<>();
        exportTopology(topology, topologies);

        System.out.println("INSERT INTO icd.icd (code, type, translation_en, translation_de) VALUES");
        Iterator<Icdo> icdIt = topologies.iterator();
        while (icdIt.hasNext()) {
            Icdo entry = icdIt.next();
            System.out.println(entry.toString() + (icdIt.hasNext() ? "," : ";"));
        }


        List<Morph> morphologies = new LinkedList<>();
        exportMorphology(morphology, morphologies);

        System.out.println("\n\n\n\n" +
                "INSERT INTO icd.morphology (code, translation_en, translation_de) VALUES");
        Iterator<Morph> morphIt = morphologies.iterator();
        while (morphIt.hasNext()) {
            Morph entry = morphIt.next();
            System.out.println(entry.toString() + (morphIt.hasNext() ? "," : ";"));
        }
    }

    private static void exportMorphology(ClaMLClass morphology, List<Morph> morphologies) {
        if (morphology.getKind().equals("category")) {
            String code = morphology.getCode().replace(":", "/");
            String label = null;
            for (ClaMLRubric rubric : morphology.getRubrics()) {
                if (rubric.getKind().equals("preferred") || rubric.getKind().equals("preferredLong")) {
                    label = rubric.getLabels().iterator().next().getValue();
                    break;
                }
            }
            morphologies.add(new Morph(code, "MISSING " + label, label));
        }

        for (ClaMLClass block : morphology.getChildren().values()) {
            exportMorphology(block, morphologies);
        }
    }

    private static void exportTopology(ClaMLClass top, List<Icdo> topologies) {
        if (top.getKind().equals("category")) {
            String code = top.getCode();
            String label = null;
            for (ClaMLRubric rubric : top.getRubrics()) {
                if (rubric.getKind().equals("preferred") || rubric.getKind().equals("preferredLong")) {
                    label = rubric.getLabels().iterator().next().getValue();
                }
            }
            topologies.add(new Icdo(code, "MISSING " + label, label));
        }

        for (ClaMLClass block : top.getChildren().values()) {
            exportTopology(block, topologies);
        }
    }

    private static class Icdo {
        private final String code;
        private final String labelEn;
        private final String labelDe;

        public Icdo(String code, String labelEn, String labelDe) {
            this.code = code;
            this.labelDe = labelDe;
            this.labelEn = labelEn;
        }

        @Override
        public String toString() {
            return "('" + code + "', 'ICD-O', '" + labelEn + "', '" + labelDe + "')";
        }
    }

    private static class Morph {
        private final String code;
        private final String labelEn;
        private final String labelDe;

        public Morph(String code, String labelEn, String labelDe) {
            this.code = code;
            this.labelEn = labelEn;
            this.labelDe = labelDe;
        }

        @Override
        public String toString() {
            return "('" + code + "', '" + labelEn + "', '" + labelDe + "')";
        }
    }
}
