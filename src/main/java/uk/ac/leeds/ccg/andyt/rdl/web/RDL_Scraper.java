/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or distribute
 * this software, either in source code form or as a compiled binary, for any
 * purpose, commercial or non-commercial, and by any means.
 *
 * In jurisdictions that recognise copyright laws, the author or authors of this
 * software dedicate any and all copyright interest in the software to the
 * public domain. We make this dedication for the benefit of the public at large
 * and to the detriment of our heirs and successors. We intend this dedication
 * to be an overt act of relinquishment in perpetuity of all present and future
 * rights to this software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package uk.ac.leeds.ccg.andyt.rdl.web;

/**
 * This is a class for scraping data from the web via a google search.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.exception.TikaException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;
import uk.ac.leeds.ccg.andyt.generic.core.Generic_Environment;
import uk.ac.leeds.ccg.andyt.generic.execution.Generic_Execution;
import uk.ac.leeds.ccg.andyt.generic.util.Generic_Time;
import uk.ac.leeds.ccg.andyt.rdl.core.RDL_Environment;
import uk.ac.leeds.ccg.andyt.rdl.io.RDL_Files;
import uk.ac.leeds.ccg.andyt.rdl.io.RDL_Object;

public class RDL_Scraper extends RDL_Object {

    File directory;
    static File sharedLogFile;
    
    protected final Generic_Execution exec;

    boolean useOnlyCachedFiles;
    String s_filter;
    String s_HipertyTipperty;
    String s_Resolver;
    String s_UniversityOfLeedsDataCiteDOIPrefix;
    String s_DOISuffix;
    String s_DOI;
    String s_DOIWithResolver;
    String s_URL;
    static String s_backslash = "/";
    //File outDir;
    ExecutorService executorService;
    double connectionCount;
    double permittedConnectionRate;
    long startTime;
    String google = "http://www.google.com/search?q=";
    String charset = "UTF-8";
    String search;
    String userAgent = "RDL DOI Checker 0.0.1 (+http://www.geog.leeds.ac.uk/people/a.turner/src/...)";
    PrintWriter pwOut;

    private static final int BUFFER_SIZE = 4096;

    public RDL_Scraper(RDL_Environment env) {
        super(env);
        exec = new Generic_Execution(env.ge);
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                args = new String[1];
                // Defaults output directory.
                args[0] = "C:/Users/geoagdt/src/agdt/java/projects/agdt-java-project-ResearchDataLeeds/data";
            }
            new RDL_Scraper(new RDL_Environment(new Generic_Environment())).run(args);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public long getTimeRunningMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public double getConnectionRate() {
        return getConnections() / (double) getTimeRunningMillis();
    }

    public double getConnections() {
        return connectionCount;
    }

    private void checkConnectionRate() {
        long timeToWaitInMilliseconds = 1000;
        double connectionRate = getConnectionRate();
        synchronized (this) {
            while (connectionRate > permittedConnectionRate) {
                try {
                    System.out.println("connectionRate > permittedConnectionRate");
                    System.out.println("" + connectionRate + " > " + permittedConnectionRate);
                    wait(timeToWaitInMilliseconds);
                    connectionRate = getConnectionRate();
                } catch (InterruptedException ex) {
                    Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     *
     * @param args
     */
    public void run(String[] args) throws IOException {
        startTime = System.currentTimeMillis();
        double permittedConnectionsPerHour = 20000;
        permittedConnectionRate = permittedConnectionsPerHour / Generic_Time.MilliSecondsInHour;
        connectionCount = 0;
        initOutputAndLogFiles(args);
        sharedLogFile = new File(args[0], "log.txt");
        try {
            getSharedLogFile().createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        getSharedLogFile().deleteOnExit();
        directory = new File(args[0]);
        File mainOutputDir = new File(directory, "out");
        s_HipertyTipperty = "http://";
        s_Resolver = "doi.org";
        s_UniversityOfLeedsDataCiteDOIPrefix = "10.5518";

        s_filter = "archive.researchdata.leeds.ac.uk";

        // Options
        // Either run a test or the full monty.
        // <runTest>
//        useOnlyCachedFiles = true; // This run will only use files already pulled from the web.
////        useOnlyCachedFiles = false; // This will pull files from the web and check if they are different from before and use the most recent file.
//        int index;
//        index = 7;
//        runTest(index, useOnlyCachedFiles);
        // </runTest>
        // <fullMonty>
        //useOnlyCachedFiles = true; // This run will only use files already pulled from the web.
        useOnlyCachedFiles = false; // This will pull files from the web and check if they are different from before and use the most recent file.
        int imin; // the min index of the data in our repository.
        int imax; // the max index of the data in our repository.
//        imin = 1;
//        imax = 3;
//        runSearch(imin, imax, useOnlyCachedFiles);
//        imin = 5;
//        imax = 25;
//        runSearch(imin, imax, useOnlyCachedFiles);
//        imin = 26;
//        imax = 38;
//        runSearch(imin, imax, useOnlyCachedFiles);
//        imin = 40;
//        imax = 45;
//        runSearch(imin, imax, useOnlyCachedFiles);
//        imin = 46; // 46 is embargoed!
//        imax = 46;
//        runSearch(imin, imax, useOnlyCachedFiles);
        imin = 56;
        imax = 65;
        runSearch(imin, imax, useOnlyCachedFiles);
        // </fullMonty>
        pwOut.close();
    }

    public void initOutputAndLogFiles(String[] args) throws IOException {
        sharedLogFile = new File(args[0], "log");
        try {
            getSharedLogFile().createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        getSharedLogFile().deleteOnExit();
        directory = new File(args[0]);
        File outD = new File(directory, "out");
        RDL_Files.setDir(outD);
        outD = new File(directory, "logs");
        if (!outD.exists()) {
            outD.mkdirs();
            outD = env.ge.io.initialiseArchive(outD, 10, false);
        } else {
            outD = env.ge.io.initialiseArchive(outD, 10, true);
            outD = env.ge.io.addToArchive(outD, 10);
        }
        if (!outD.exists()) {
            outD.mkdirs();
        }
        File outF = new File(                outD,                "log.txt");
        try {
            outF.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        pwOut = env.ge.io.getPrintWriter(outF, false);
    }

    public void runTest(int index, boolean useOnlyCachedFiles) {
        try {
            s_DOISuffix = "" + index;
            s_DOI = s_UniversityOfLeedsDataCiteDOIPrefix + s_backslash + s_DOISuffix;
            s_DOIWithResolver = s_Resolver + s_backslash + s_DOI;
            search = "\"" + s_DOIWithResolver + "\"";
            HashMap<String, String> searchResult;
            //searchResult = getSearchResult(); // This is for a real search.
            searchResult = new HashMap<>();
            String testurl;
            testurl = s_HipertyTipperty + "eprints.whiterose.ac.uk/87869/1/Brockway%20et%20al%202015%20China%20energy%20efficiency%20and%20decomposition.pdf";
            String testtitle;
            testtitle = "Download - White Rose Research Online";
            searchResult.put(testurl, testtitle);
            filterSearchResult(searchResult);
            HashMap<String, Object[]> results;
            results = getResults(index, searchResult, useOnlyCachedFiles);
            writeResults(getResultsPrintWriter(index), results);
//            boolean restart = false;
//            getData(restart);
        } catch (Exception | Error e) {
            e.printStackTrace(System.err);
        }
    }

    public void runSearch(int imin, int imax, boolean useOnlyCachedFiles) {
        pwOut.println("Run a search from " + imin + " to " + imax + " useOnlyCachedFiles = " + useOnlyCachedFiles);
        try {
            // index of the dataset DOI to test
            for (int index = imin; index <= imax; index++) {
                s_DOISuffix = "" + index;
                s_DOI = s_UniversityOfLeedsDataCiteDOIPrefix + s_backslash + s_DOISuffix;
                s_DOIWithResolver = s_Resolver + s_backslash + s_DOI;

                //s_URL = "https://www.google.co.uk/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=%22doi.org%2F" + s_UniversityOfLeedsDataCiteDOIPrefix + "%2F" + s_DOISuffix + "%22";
                //s_URL = "https://www.google.co.uk/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=%22doi.org%2F" + s_UniversityOfLeedsDataCiteDOIPrefix + "%2F" + s_DOISuffix + "%22";
                search = "\"" + s_DOIWithResolver + "\"";

                try ( PrintWriter pw = getResultsPrintWriter(index)) {
                    HashMap<String, String> searchResult;
                    searchResult = getSearchResult();
                    if (!searchResult.isEmpty()) {
                        filterSearchResult(searchResult);
                        HashMap<String, Object[]> results;
                        results = getResults(index, searchResult, useOnlyCachedFiles);
                        writeResults(pw, results);
//                    boolean restart = false;
//                    getData(restart);
                    } else {
                        pw.println("No search result for search " + search);
                    }
                }
            }
        } catch (Exception | Error e) {
            e.printStackTrace(System.err);
        }
    }

    protected PrintWriter getResultsPrintWriter(int index) throws IOException {
        PrintWriter r;
        File dir = new File(RDL_Files.getDir(), "DOI_" + index);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File f;
        f = new File(dir, "out.txt");
        r = env.ge.io.getPrintWriter(f, false);
        return r;
    }

    /**
     * filters a search result to exclude those with a URL containing s_filter
     *
     * @param searchResult
     */
    public void filterSearchResult(HashMap<String, String> searchResult) {
        HashSet<String> keep = new HashSet<>();
        Iterator<String> ite;
        ite = searchResult.keySet().iterator();
        while (ite.hasNext()) {
            s_URL = ite.next();
            if (s_URL.contains(s_filter)) {
                System.out.println("Not parsing search result from " + s_URL);
            } else {
                keep.add(s_URL);
            }
        }
        searchResult.keySet().retainAll(keep);
    }

    /**
     * GEt and save all documents in the search results
     *
     * @param index
     * @param searchResult
     * @param useOnlyCachedFiles
     * @return
     */
    public HashMap<String, Object[]> getResults(int index,
            HashMap<String, String> searchResult, boolean useOnlyCachedFiles) {
        HashMap<String, Object[]> result;
        result = new HashMap<>();
        Iterator<String> ite;
        ite = searchResult.keySet().iterator();
        while (ite.hasNext()) {
            s_URL = ite.next();
            Object[] partResult;
            partResult = getAndParseDocument(index, useOnlyCachedFiles);
            result.put(s_URL, partResult);
        }
        return result;
    }

    /**
     * Use Jsoup to get search results.
     *
     * @return HashMap<String, String> result where the keys are URLs and the
     * values are Titles.
     */
    public HashMap<String, String> getSearchResult() {
        HashMap<String, String> r = new HashMap<>();
        //try {
        Elements links = getLinks();
        if (links != null) {
            links.forEach((link) -> {
                String title = link.text();
                String url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
                // Ads/news/etc.
                try {
                    url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (!(!url.startsWith("http"))) {
                    System.out.println("Title: " + title);
                    System.out.println("URL: " + url);
                    r.put(url, title);
                }
            });
        }
        return r;
    }

    public Elements getLinks() {
        Elements r = null;
        try {
            String url = google + URLEncoder.encode(search, charset);
            System.out.println(url);
            Document doc = Jsoup.connect(url).userAgent(userAgent).get();

            System.out.println(doc.toString());
            //r = doc.select(".g>.r>a");
            r = doc.select("a[href]");
            //r = doc.getElementsByClass("rc");
            if (r.size() > 0) {
                // Result!
                int debug = 1;
            }

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketTimeoutException e) {
            try {
                this.wait(100L);
            } catch (InterruptedException ex) {
                Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public void getData(
            boolean restart)
            throws Exception {
        // Initialisation
        //executorService = Executors.newCachedThreadPool();
        //int nThreads = 1 + (23 * 3) + (23 * 23 * 2); 
        int nThreads = 1;//256; 128;
        executorService = Executors.newFixedThreadPool(nThreads);
        HashSet<Future> futures = new HashSet<>();
        futures.addAll(getResult(restart));
        // Wait for results then shutdown executorService
        exec.shutdownExecutorService(
                getExecutorService(),
                futures,
                this,
                100000L,
                10L);
    }

    public void getData(
            boolean restart,
            int nThreads)
            throws Exception {
        // Initialisation
        //executorService = Executors.newCachedThreadPool();
        //int nThreads = 1 + (23 * 3) + (23 * 23 * 2); 
        //int nThreads = 256;
        //int nThreads = 128;
        //int nThreads = 64;
        //int nThreads = 23;
        //int nThreads = 10;
        executorService = Executors.newFixedThreadPool(nThreads);
        HashSet<Future> futures = getResult(restart);
        // Wait for results then shutdown executorService
        exec.shutdownExecutorService(
                getExecutorService(),
                futures,
                this,
                100000L,
                10L);
    }

    private HashSet<Future> getResult(boolean restart)
            throws IOException {
        HashSet<Future> result = new HashSet<>();
        RDL_Run run = new RDL_Run(
                this, restart);
        result.add(getExecutorService().submit(run));
        return result;
    }

    /**
     *
     * @param outPW
     * @param logPW
     * @param sharedLogPW
     * @param useOnlyCachedFiles
     * @return
     */
    public int writeResults(
            //int index,
            PrintWriter outPW,
            PrintWriter logPW,
            PrintWriter sharedLogPW,
            boolean useOnlyCachedFiles) {
        int index = 1; // This is a severely bad hack!
        Object[] results = getAndParseDocument(index, useOnlyCachedFiles);
        outPW.write((String) results[0]);
        outPW.write((String) results[1]);
        ArrayList<String[]> links = (ArrayList<String[]>) results[2];
        Iterator<String[]> ite = links.iterator();
        String[] linkss;
        while (ite.hasNext()) {
            linkss = ite.next();
            outPW.write(linkss[0] + ", " + linkss[1] + ", " + linkss[2]);
        }
        outPW.println();
        outPW.flush();
        return 1;
    }

    public void writeResults(PrintWriter pw, HashMap<String, Object[]> results) {
        Iterator<String> ite;
        ite = results.keySet().iterator();
        String url;
        Object[] urlResult;
        while (ite.hasNext()) {
            url = ite.next();
            urlResult = results.get(url);
            pw.println(url);
            if (urlResult != null) {
                //System.out.println((String) urlResult[0]);
                if (urlResult[1] != null) {
                    pw.println((String) urlResult[1]);
                }
                ArrayList<String[]> links;
                links = (ArrayList<String[]>) urlResult[2];
                pw.println("<links>");
                if (links.isEmpty()) {
                    pw.println("No " + s_DOIWithResolver + " links.");
                } else {
                    Iterator<String[]> ite2;
                    ite2 = links.iterator();
                    String[] linkss;
                    while (ite2.hasNext()) {
                        linkss = ite2.next();
                        pw.println("<link>");
                        pw.println(linkss[0]);
                        pw.print(linkss[1]);
                        pw.println(linkss[2]);
                        pw.println("</link>");
                    }
                }
                pw.println("</links>");
            } else {
                pw.println("No Result");
            }
            pw.println();
        }
    }

    /**
     * @param logPW
     * @param sharedLogPW
     */
    public void updateLog(PrintWriter logPW, PrintWriter sharedLogPW) {
        logPW.println("0 Records");
        sharedLogPW.println("0 Records");
        logPW.flush();
//        sharedLogPW.append('.');
        sharedLogPW.flush();
    }

    public boolean isReturningOutcode() {
        try {
            HttpURLConnection connection = getOpenHttpURLConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
//                if (responseCode == 301) {
//                    return false;
//                }
//                if (responseCode == 404) {
//                    return false;
//                }
                String message = s_URL + " connection.getResponseCode() "
                        + responseCode
                        + " see http://en.wikipedia.org/wiki/List_of_HTTP_status_codes";
                if (responseCode == 301 || responseCode == 302 || responseCode == 303
                        || responseCode == 403 || responseCode == 404) {
                    message += " and http://en.wikipedia.org/wiki/HTTP_";
                    message += Integer.toString(responseCode);
                }
                throw new Error(message);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    protected HttpURLConnection getOpenHttpURLConnection() {
        checkConnectionRate();
        connectionCount += 1D;
        HttpURLConnection result = null;
        URL aURL = null;
        try {
            aURL = new URL(s_URL);
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
//        // Set up proxy to use address and port from http://www.leeds.ac.uk/proxy.pac
//        int port = 3128;
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("www-cache.leeds.ac.uk", port));
        try {
//            result = (HttpURLConnection) aURL.openConnection(proxy);
            if (aURL != null) {
                result = (HttpURLConnection) aURL.openConnection();
            }
//            result.setRequestMethod("GET"); // GET is the default anyway.
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return result;
    }

    /**
     *
     * @param index
     * @param useOnlyCachedFiles
     * @return Object[] result where: result[0] is a String representation of
     * the pdfFile; result[1] is a String declaring whether the pdfFile contains
     * s_DOI; result[2] is an ArrayList<String[]> containing details of the
     * links in the pdfFile that are explicitly marked up as links.
     */
    public Object[] getAndParseDocument(int index, boolean useOnlyCachedFiles) {
        Object[] r = null;
        File pdfFile = null;
        if (s_URL.endsWith(".pdf")) {
            pdfFile = getFile(index, "");
        }
        if (useOnlyCachedFiles) {
            if (s_URL.endsWith(".pdf")) {
                if (pdfFile.exists()) {
                    try {
                        r = parsePDF(pdfFile);
                    } catch (IOException ex) {
                        Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                System.out.println(s_URL + " not a PDF File");
            }
        } else {
            try {
                HttpURLConnection c = getOpenHttpURLConnection();
                int responseCode = c.getResponseCode();
                if (responseCode != 200) {
                    handleNon200ResponseCodes(responseCode);
                    return r;
                }
                Map<String, List<String>> headerFields = c.getHeaderFields();
                if (!headerFields.isEmpty()) {
                    System.out.println("<HeaderFields>");
                    Iterator<String> ite = headerFields.keySet().iterator();
                    while (ite.hasNext()) {
                        String key = ite.next();
                        System.out.println("<HeaderKey>");
                        System.out.println("key " + key);
                        List<String> value = headerFields.get(key);
                        if (!value.isEmpty()) {
                            System.out.println("<HeaderValues>");
                            Iterator<String> ite2 = value.iterator();
                            while (ite2.hasNext()) {
                                System.out.println(ite2.next());
                            }
                            System.out.println("</HeaderValues>");
                        }
                        System.out.println("</HeaderKey>");
                    }
                    System.out.println("</HeaderFields>");
                }
                String contentEncoding = c.getContentEncoding();
                System.out.println("contentEncoding " + contentEncoding);
                long contentLength = c.getContentLengthLong();
                System.out.println("contentLength " + contentLength);
                String contentType = c.getContentType();
                System.out.println("contentType " + contentType);
                Object content = c.getContent();
                System.out.println("contentClass " + content.getClass());

                InputStream inputStream;
                inputStream = c.getInputStream();

                if (contentType.equalsIgnoreCase("application/pdf")) {
                    // Store pdf
                    if (pdfFile == null) {
                        pdfFile = getFile(index, ".pdf");
                    }
                    FileOutputStream fos;
                    fos = new FileOutputStream(pdfFile);
                    int bytesRead = -1;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                    inputStream.close();

                    r = parsePDF(pdfFile);

//                Object[] parsedPDF;
//                parsedPDF = ParsePDF.parseWithTika(fis);
//               String bodyContentString;
//               bodyContentString = (String) parsedPDF[0];
//               String linkText;
//               linkText = s_Resolver + "/" + s_UniversityOfLeedsDataCiteDOIPrefix + "/" + s_DOISuffix;
//               if (bodyContentString.contains(linkText)) {
//                   System.out.println("Found Link " + linkText);
//               } else {
//                   System.out.println("Not Found Link " + linkText);
//               }
                } else {
                    System.out.println("Not a PDF maybe this is HTML...");
                    System.out.println(contentType);
                }

//            br = new BufferedReader(
//                    new InputStreamReader(connection.getInputStream()));
//            try {
//                line = br.readLine();
//                if (line != null) {
//                    if (line.startsWith("%PDF")) {
//                        File pdfFile;
//                        pdfFile = new File(outDir,
//                                "test.pdf");
//                        PrintWriter pw;
//                        pw = new PrintWriter(pdfFile);
//                        pw.println(line);
//                        while ((line = br.readLine()) != null) {
//                            pw.println(line);
//                        }
//                        pw.flush();
//                        pw.close();
//                        FileInputStream fis;
//                        fis = new FileInputStream(pdfFile);
//                        ParsePDF.parse(fis);
//                    }
//                }
//                while ((line = br.readLine()) != null) {
//
//                    System.out.println(line);
//                    if (line.contains("house prices paid")) {
//                        while (true) {
//                            addResult(result,
//                                    br);
//                            // Scan to next record
//                            line = br.readLine();
//                            while (!line.contains("<tr >")) {
//                                line = br.readLine();
//                            }
//                            line = br.readLine();
//                            while (!line.contains("<tr >")) {
//                                line = br.readLine();
//                            }
//                        }
//                    }
//                }
//                System.out.println("HTML Parsed");
//            } catch (IOException e) {
//                //e.printStackTrace(System.err);
//            }
//            br.close();
                //}
            } catch (ConnectException e) {
                e.printStackTrace(System.err);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return r;
    }

    public void handleNon200ResponseCodes(int responseCode) {
        String message = s_URL + " connection.getResponseCode() "
                + responseCode + "\n"
                + "See: \n"
                + "http://en.wikipedia.org/wiki/List_of_HTTP_status_codes" + "\n";
        if (responseCode == 301 || responseCode == 302 || responseCode == 303
                || responseCode == 403) {
            message += "http://en.wikipedia.org/wiki/HTTP_";
            message += Integer.toString(responseCode);
            if (responseCode == 403) {
                message += " A web server may or may not return a 403 "
                        + "Forbidden HTTP status code in response to a "
                        + "request from a client for a web page or resource "
                        + "to indicate that the server can be reached and "
                        + "understood the request, but refuses to take any "
                        + "further action. Status code 403 responses are the "
                        + "result of the web server being configured to deny "
                        + "access, for some reason, to the requested resource "
                        + "by the client.";
            }
        }
        System.err.println(message);
    }

    /**
     *
     * @param pdfFile The PDF file to be parsed.
     * @return Object[] r where:
     * <ul>
     * <li>r[0] is a String representation of the pdfFile</li>
     * <li>r[1] is a String declaring whether the pdfFile contains s_DOI;
     * result[2] is an ArrayList<String[]> containing details of the links in
     * the pdfFile that are explicitly marked up as links.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Object[] parsePDF(File pdfFile) throws FileNotFoundException, IOException {
        Object[] r = new Object[3];
        String pdfAsAString;
        pdfAsAString = RDL_ParsePDF.parseToString(pdfFile);
        r[0] = pdfAsAString;
        if (pdfAsAString.contains(s_UniversityOfLeedsDataCiteDOIPrefix)) {
            r[1] = "Paper contains " + s_UniversityOfLeedsDataCiteDOIPrefix;
            System.out.println(r[1]);
            if (pdfAsAString.contains(s_DOI)) {
                r[1] = "Paper contains " + s_DOI;
                System.out.println(r[1]);
                if (pdfAsAString.contains(s_DOIWithResolver)) {
                    r[1] = "Paper contains " + s_DOIWithResolver;
                    System.out.println(r[1]);
                }
            }
        } else {
            r[1] = "Paper does not contain " + s_UniversityOfLeedsDataCiteDOIPrefix;
            System.out.println(r[1]);
        }
        FileInputStream fis;
        fis = env.ge.io.getFileInputStream(pdfFile);
        try {
            r[2] = RDL_ParsePDF.parseForLinks(
                    pdfFile,
                    s_UniversityOfLeedsDataCiteDOIPrefix,
                    fis);
        } catch (TikaException | SAXException ex) {
            Logger.getLogger(RDL_Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public String getHTMLandFormatPropertyDetails(
            String aURLString) {
        String result = "";
        HttpURLConnection connection;
        BufferedReader br;
        String line;
        try {
            connection = getOpenHttpURLConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 404) {
                    return result;
                }
                String message = aURLString + " connection.getResponseCode() "
                        + responseCode
                        + "see http://en.wikipedia.org/wiki/List_of_HTTP_status_codes";
                if (responseCode == 301 || responseCode == 302 || responseCode == 303
                        || responseCode == 403) {
                    message += "and http://en.wikipedia.org/wiki/HTTP_";
                    message += Integer.toString(responseCode);
                }
                throw new Error(message);
            }
            br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            try {
                while ((line = br.readLine()) != null) {
                    if (line.contains("attributes-update")) {
                        line = br.readLine();
                        while (!line.contains("<")) {
                            result += line.trim();
                            line = br.readLine();
                        }
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace(System.err);
            }
            br.close();
            //}
        } catch (IOException e) {
            //e.printStackTrace(System.err);
        }
        return result;
    }

    private void addResult(
            TreeSet<String> result,
            BufferedReader br) throws IOException {
        String s_Address;
        String tFormattedOutput;
        String s_PropertyType0;
        String s_PropertyType1;
        String s_DatePurchased;
        String s_Price;
        String line;
        // get s_DatePurchased;
        line = br.readLine();
        while (!line.contains("<strong>")) {
            line = br.readLine();
        }
        line = br.readLine();
        s_DatePurchased = line.trim();
        // get Address
        line = br.readLine();
        while (!line.startsWith(">")) {
            line = br.readLine();
        }
        s_Address = line.split("<")[0].substring(1);

//        boolean debug = false;
//        if (s_Address.startsWith("37")) {
//            debug = true;
//            System.out.println(s_Address);
//        }
        // get s_PropertyType0 s_PropertyType1
        line = br.readLine();
        while (!line.contains("attributes-update")) {

//            if (debug) {
//                System.out.println(line);
//            }
            line = br.readLine();
        }
        line = br.readLine();
        s_PropertyType0 = line.trim();
        br.readLine();
        line = br.readLine();
        s_PropertyType1 = line.trim();
        if (s_PropertyType1.equalsIgnoreCase("--,")) {
            s_PropertyType1 = "";
        }
        // get price
        line = br.readLine();
        while (!line.contains("&pound;")) {

//            if (debug) {
//                System.out.println(line);
//            }
            line = br.readLine();
        }
        String[] price = line.split("&pound;")[1].split("<")[0].split(",");
        s_Price = "";
        for (int i = 0; i < price.length; i++) {
            s_Price += price[i];
        }

        tFormattedOutput = "";

        result.add(tFormattedOutput);
        // Scan to next record
        line = br.readLine();
        while (!line.contains("<tr >")) {
            line = br.readLine();
        }
        line = br.readLine();
        while (!line.contains("<tr >")) {
            line = br.readLine();
        }
    }

    /**
     * @return the directory
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * @return the executorService
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * @return the sharedLogFile
     */
    public static File getSharedLogFile() {
        return sharedLogFile;
    }

    protected HashMap<Integer, HashMap<String, File>> files;

    protected HashMap<Integer, HashMap<String, File>> getFiles() {
        if (files == null) {
            files = new HashMap<>();
        }
        return files;
    }

    public File getFile(int index, String suffix) {
        File result;
        String[] split;
        HashMap<Integer, HashMap<String, File>> filess;
        filess = getFiles();
        HashMap<String, File> files;
        if (filess.containsKey(index)) {
            files = filess.get(index);
        } else {
            files = new HashMap<>();
            filess.put(index, files);
        }
        if (files.containsKey(s_URL)) {
            result = files.get(s_URL);
        } else {
            split = s_URL.split("/");
            String filename;
            filename = split[split.length - 1] + suffix;
            String dirname;
            dirname = s_URL.replace(":", "_");
            dirname = dirname.replace("/", "_");
            dirname += suffix;
            File dir = new File(RDL_Files.getDir(), "DOI_" + index);
            dir = new File(dir, dirname);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            result = new File(dir, filename);
            files.put(s_URL, result);
        }
        return result;
    }
}
