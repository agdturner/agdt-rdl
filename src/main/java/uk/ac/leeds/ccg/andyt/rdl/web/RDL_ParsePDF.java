/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.leeds.ccg.andyt.rdl.web;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchema;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSObject;
//import org.apache.pdfbox.exceptions.CryptographyException;
//import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class RDL_ParsePDF {

    /**
     * https://svn.apache.org/viewvc/pdfbox/trunk/examples/ Based on
     * https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/PrintURLs.java?view=markup&pathrev=1703066
     *
     * @param f
     * @param filter
     * @param fis
     * @return
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static ArrayList<String[]> parseForLinks(
            File f,
            String filter,
            FileInputStream fis) throws IOException, TikaException, SAXException {
        ArrayList<String[]> result;
        result = new ArrayList<String[]>();
        PDDocument doc = PDDocument.load(f);
        int pageNum = 0;
        for (PDPage page : doc.getPages()) {
            pageNum++;

//            if (pageNum == 11) { //Degug test hack
            System.out.println("Parsing page " + pageNum);
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            List<PDAnnotation> annotations = page.getAnnotations();
            //first setup text extraction regions
            for (int j = 0; j < annotations.size(); j++) {
                PDAnnotation annot = annotations.get(j);
                if (annot instanceof PDAnnotationLink) {
                    PDAnnotationLink link = (PDAnnotationLink) annot;
                    PDRectangle rect = link.getRectangle();
                    //need to reposition link rectangle to match text space
                    float x = rect.getLowerLeftX();
                    float y = rect.getUpperRightY();
                    float width = rect.getWidth();
                    float height = rect.getHeight();
                    int rotation = page.getRotation();
                    if (rotation == 0) {
                        PDRectangle pageSize = page.getMediaBox();
                        y = pageSize.getHeight() - y;
                    } else if (rotation == 90) {
                        //do nothing
                    }

                    //Rectangle2D.Float awtRect = new Rectangle2D.Float(x, y, width, height);
                    // Rounding here could be a problem!
                    Rectangle2D.Double awtRect = new Rectangle2D.Double(x, y, width, height);
                    stripper.addRegion("" + j, awtRect);
                }
            }

            stripper.extractRegions(page);

             for (int j = 0; j < annotations.size(); j++) {
                PDAnnotation annot = annotations.get(j);
                if (annot instanceof PDAnnotationLink) {
                    PDAnnotationLink link = (PDAnnotationLink) annot;
                    PDAction action = link.getAction();
                    if (action == null) {
                        System.out.println(link.getContents());
                        System.out.println(annot.getClass().getName());
                        System.out.println(annot.getAnnotationName());
                        //System.out.println(annot.getNormalAppearanceStream().toString());
                        System.out.println(annot.getContents());
                        System.out.println(annot.getSubtype());
                    } else {
                        String urlText = stripper.getTextForRegion("" + j);
                        if (action instanceof PDActionURI) {
                            PDActionURI uri = (PDActionURI) action;
                            String url;
                            url = uri.getURI();
                            if (url.contains(filter)) {
                                String[] partResult;
                                partResult = new String[3];
                                partResult[0] = "Page " + pageNum;
                                partResult[1] = "urlText " + urlText;
                                partResult[2] = "URL " + uri.getURI();
                                System.out.println(partResult[0]);
                                System.out.println(partResult[1]);
                                System.out.println(partResult[2]);
                                System.out.println("URL " + uri.getURI());
                                result.add(partResult);
                            } else {
                                System.out.println("URL " + uri.getURI());
                            }
                        } else {
                            System.out.println(action.getType());
                        }
                    }
                } else {
                    System.out.println(annot.getClass().getName());
                    System.out.println(annot.getAnnotationName());
                    System.out.println(annot.getContents());
                    System.out.println(annot.getSubtype());
                }
            }

            //}
        }
//       PDDocument doc = PDDocument.load(f);
//        int pageNum = 0;
//        for (PDPage page : doc.getPages()) {
//            pageNum++;
//            List<PDAnnotation> annotations = page.getAnnotations();
//
//            for (PDAnnotation annotation : annotations) {
//                PDAnnotation annot = annotation;
//                if (annot instanceof PDAnnotationLink) {
//                    PDAnnotationLink link = (PDAnnotationLink) annot;
//                    PDAction action = link.getAction();
//                    if (action instanceof PDActionURI) {
//                        PDActionURI uri = (PDActionURI) action;
//                        String oldURI = uri.getURI();
//                        String name = annot.getAnnotationName();
//                        String contents = annot.getContents();
//                        PDAppearanceStream a = annot.getNormalAppearanceStream();
//                        //String newURI = "http://pdfbox.apache.org";
//                        System.out.println(oldURI + " " + name + " " + contents);
//                        //uri.setURI(newURI);
//                    }
//                }
//            }
//        }

//        result = parseWithTika(fis);
        //XMPSchema schema;
        //schema = new XMPSchema();
        //List<String> XMPBagOrSeqList;
        //XMPBagOrSeqList = getXMPBagOrSeqList(XMPSchema schema, String name) {
        
//        PDDocument tPDDocument;
//        tPDDocument = PDDocument.load(f);
//        COSDocument tCOSDocument;
//        tCOSDocument = tPDDocument.getDocument();
        
//        String header;
//        header = tCOSDocument.getHeaderString();
//        System.out.println(header);

//        PDDocumentCatalog tPDDocumentCatalog;
//        tPDDocumentCatalog = tPDDocument.getDocumentCatalog();
//        PDDocumentNameDictionary tPDDocumentNameDictionary;
//        tPDDocumentNameDictionary = tPDDocumentCatalog.getNames();
        
//        COSDictionary tCOSDictionary;
//        tCOSDictionary = tPDDocumentNameDictionary.getCOSDictionary();
        //tCOSDictionary.
//        PDPageNode tPDPageNode;
//        tPDPageNode = tPDDocumentCatalog.getPages();

//        List<COSObject> tCOSObjects;
//        tCOSObjects = tCOSDocument.getObjects();
//        int n;
//        n = tCOSObjects.size();
//        System.out.println(n);
//        COSObject aCOSObject;
//        String s;
//        for (int i = 0; i < n; i++) {
//            aCOSObject = tCOSObjects.get(i);
//            s = aCOSObject.toString();
//            System.out.println(s);
//        }

//        XMPMetadata tXMPMetadata;
//        tXMPMetadata = getXMPMetadata(tPDDocument);

//        Document XMPDocument;
//        XMPDocument = tXMPMetadata.getXMPDocument();
//        Node n;
//        n = XMPDocument.getFirstChild();
//        parseNode(n);
        return result;
    }

    private static void parseNode(Node n) {
        String nodeValue;
        nodeValue = n.getNodeValue();
        System.out.println(nodeValue);
        NamedNodeMap nnm;
        nnm = n.getAttributes();
        int nnml;
        nnml = nnm.getLength();
        System.out.println(nnml);
        //nnm.
        NodeList nl;
        nl = n.getChildNodes();
        int nll;
        nll = nl.getLength();
        Node cn;
        for (int i = 0; i < nll; i++) {
            cn = nl.item(i);
            String nn = cn.getNodeName();
            System.out.print("NodeName " + nn);
            String nv = cn.getNodeValue();
            System.out.println("NodeValue " + nv);
            //    cn.
        }
    }

    /**
     * Converts PDF to a String a page at a time.
     *
     * @param f
     * @return
     * @throws IOException
     */
    public static String parseToString(File f) throws IOException {
        String result;
        result = "";
        PDDocument doc = PDDocument.load(f);
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);
        //Rectangle rect = new Rectangle(10, 280, 275, 60);
        //PDPage firstPage = doc.getPage(0);
        for (PDPage page : doc.getPages()) {
            PDRectangle aPDRectangle;
            aPDRectangle = page.getBBox();
            Rectangle2D.Double rect = new Rectangle2D.Double(
                    aPDRectangle.getLowerLeftX(),
                    aPDRectangle.getLowerLeftY(),
                    //aPDRectangle.getUpperRightY(),
                    aPDRectangle.getWidth(),
                    aPDRectangle.getHeight());
            stripper.addRegion("class1", rect);
            stripper.extractRegions(page);
            System.out.println("<Text in the area:" + rect + ">");
            String text;
            text = stripper.getTextForRegion("class1");
            System.out.println(text);
            System.out.println("</Text in the area:" + rect + ">");
            result += text;
        }
        return result;
    }

    @Deprecated
    public static Object[] parseWithTika(FileInputStream fis)
            throws IOException, SAXException, TikaException {
        Object[] result;
        result = new Object[2];
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();

        //parsing the document using PDF parser
        PDFParser pdfparser = null;
        //try {
        pdfparser = new PDFParser();
//        } catch (CryptographyException ce) {
//            int debug = 1;
//        }
        pdfparser.parse(
                fis,
                handler,
                metadata,
                pcontext);
        Tika tika = new Tika();
        String filecontent = tika.parseToString(fis);

        //getting the content of the document
        String bodyContentString;
        bodyContentString = handler.toString();
        System.out.println("Contents of the PDF :" + bodyContentString);
        result[0] = bodyContentString;

        //getting metadata of the document
        System.out.println("Metadata of the PDF:");
        String[] metadataNames = metadata.names();
        result[1] = metadataNames;
        for (String name : metadataNames) {
            System.out.println(name + " : " + metadata.get(name));
        }
        return result;
    }

    private static XMPMetadata getXMPMetadata(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        //tPDPageNode.
        PDMetadata metaRaw = catalog.getMetadata();

        if (metaRaw == null) {
            return null;
        }

        XMPMetadata meta = new XMPMetadata(XMLUtil.parse(metaRaw
                .createInputStream()));
        //meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.class);
        return meta;
    }
}
