package tn.fss.soa;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * UniversityManager : validation XML/XSD et interrogation XPath.
 */
public class Main {

    private static final String XSD = "universite.xsd";
    private static final String XML_VALIDE = "universite.xml";
    private static final String XML_INVALIDE = "universite-invalide.xml";

    public static void main(String[] args) throws Exception {
        // ---------- Fonctionnalité 1 : validation XML/XSD ----------
        System.out.println("===== 1. Validation XML/XSD =====");
        boolean valide = validerXml(XML_VALIDE);
        validerXml(XML_INVALIDE);

        if (!valide) {
            System.out.println("Document de référence invalide : requêtes XPath annulées.");
            return;
        }

        // ---------- Fonctionnalité 2 : interrogation XPath ----------
        System.out.println("\n===== 2. Interrogation XPath =====");
        Document doc = chargerDocument(XML_VALIDE);
        XPath xpath = XPathFactory.newInstance().newXPath();

        afficher(xpath, doc, "Tous les étudiants",
                "//etudiant",
                "concat(@id, ' - ', prenom, ' ', nom, ' (', niveau, ')')");

        afficher(xpath, doc, "Recherche de la matière « Bases de données »",
                "//matiere[nom='Bases de données']",
                "concat(@id, ' - ', nom, ' (coefficient ', coefficient, ')')");

        notesSuperieuresOuEgales(xpath, doc, 15);

        afficher(xpath, doc, "Étudiants de niveau LSI3",
                "//etudiant[niveau='LSI3']",
                "concat(prenom, ' ', nom)");

        afficher(xpath, doc, "Recherche d'une matière inexistante",
                "//matiere[nom='Intelligence artificielle']",
                "nom");
    }

    /** Valide un fichier XML (placé dans resources) avec le schéma XSD. */
    static boolean validerXml(String nomXml) {
        System.out.print("Validation de " + nomXml + " : ");
        try {
            URL xsd = ressource(XSD);
            URL xml = ressource(nomXml);

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(xsd);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml.toExternalForm()));

            System.out.println("document VALIDE");
            return true;
        } catch (SAXException e) {
            System.out.println("document INVALIDE");
            System.out.println("   -> " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("erreur de lecture : " + e.getMessage());
            return false;
        }
    }

    /** Charge le document XML en arbre DOM. */
    static Document chargerDocument(String nomXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = ressource(nomXml).openStream()) {
            return builder.parse(in);
        }
    }

    /**
     * Évalue une requête XPath retournant des nœuds, puis affiche pour chaque nœud
     * le résultat d'une expression d'affichage relative à ce nœud.
     */
    static void afficher(XPath xpath, Document doc, String titre,
                         String requete, String affichage) throws Exception {
        System.out.println("\n> " + titre);
        System.out.println("  XPath : " + requete);

        XPathExpression expr = xpath.compile(requete);
        NodeList noeuds = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (noeuds.getLength() == 0) {
            System.out.println("  Aucun résultat.");
            return;
        }
        for (int i = 0; i < noeuds.getLength(); i++) {
            Node n = noeuds.item(i);
            System.out.println("  - " + xpath.evaluate(affichage, n));
        }
    }

    /** Notes supérieures ou égales à un seuil, avec le nom de l'étudiant et de la matière. */
    static void notesSuperieuresOuEgales(XPath xpath, Document doc, int seuil) throws Exception {
        String requete = "//note[@valeur>=" + seuil + "]";
        System.out.println("\n> Notes supérieures ou égales à " + seuil);
        System.out.println("  XPath : " + requete);

        NodeList notes = (NodeList) xpath.compile(requete).evaluate(doc, XPathConstants.NODESET);
        if (notes.getLength() == 0) {
            System.out.println("  Aucun résultat.");
            return;
        }
        for (int i = 0; i < notes.getLength(); i++) {
            Node note = notes.item(i);
            String etudiantId = xpath.evaluate("@etudiant", note);
            String matiereId = xpath.evaluate("@matiere", note);
            String valeur = xpath.evaluate("@valeur", note);
            String etudiant = xpath.evaluate(
                    "concat(//etudiant[@id='" + etudiantId + "']/prenom, ' ', //etudiant[@id='" + etudiantId + "']/nom)", doc);
            String matiere = xpath.evaluate("//matiere[@id='" + matiereId + "']/nom", doc);
            System.out.println("  - " + etudiant + " : " + valeur + " en " + matiere);
        }
    }

    private static URL ressource(String nom) throws IOException {
        URL url = Main.class.getClassLoader().getResource(nom);
        if (url == null) {
            throw new IOException("Ressource introuvable : " + nom);
        }
        return url;
    }
}