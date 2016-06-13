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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uk.ac.leeds.ccg.andyt.generic.utilities.Generic_Execution;
import uk.ac.leeds.ccg.andyt.generic.utilities.Generic_Time;

public class Scraper {

    File directory;
    static File sharedLogFile;
    String s_HipertyTipperty;
    String s_Resolver;
    String s_UniversityOfLeedsDataCiteDOIPrefix;
    String s_DOISuffix;
    String s_DOI;
    String s_DOIWithResolver;
    String s_URL;
    static String s_backslash = "/";
    File outDir;
    ExecutorService executorService;
    double connectionCount;
    double permittedConnectionRate;
    long startTime;
    String google = "http://www.google.com/search?q=";
    String charset = "UTF-8";
    String search;
    String userAgent = "RDL DOI Checker 0.0.1 (+http://www.geog.leeds.ac.uk/people/a.turner/src/...)";

    private static final int BUFFER_SIZE = 4096;

    /**
     * Creates a new instance of ZooplaHousepriceScraper
     */
    public Scraper() {
        //startTime = System.currentTimeMillis();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            // Defaults output directory.
            args[0] = "C:/Users/geoagdt/projects/RDL";
        }
        new Scraper().run(args);
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
                    Logger.getLogger(Scraper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     *
     * @param args
     */
    public void run(String[] args) {
        try {
            startTime = System.currentTimeMillis();
            double permittedConnectionsPerHour = 20000;
            permittedConnectionRate = permittedConnectionsPerHour / Generic_Time.MilliSecondsInHour;
            connectionCount = 0;
            sharedLogFile = new File(
                    args[0],
                    "log");
            getSharedLogFile().createNewFile();
            getSharedLogFile().deleteOnExit();
            directory = new File(args[0]);
            outDir = new File(
                    directory,
                    "out");
            outDir.mkdirs();
            s_HipertyTipperty = "http://";
            s_Resolver = "doi.org";
            s_UniversityOfLeedsDataCiteDOIPrefix = "10.5518";
            int i;
            //for (i = 1; i < 47; i++) {
            i = 7;
            s_DOISuffix = "" + i;
            s_DOI = s_UniversityOfLeedsDataCiteDOIPrefix + s_backslash + s_DOISuffix;
            s_DOIWithResolver = s_Resolver + s_backslash + s_DOI;

            //s_URL = "https://www.google.co.uk/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=%22doi.org%2F" + s_UniversityOfLeedsDataCiteDOIPrefix + "%2F" + s_DOISuffix + "%22";
            //s_URL = "https://www.google.co.uk/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=%22doi.org%2F" + s_UniversityOfLeedsDataCiteDOIPrefix + "%2F" + s_DOISuffix + "%22";
            search = "\"" + s_DOIWithResolver + "\"";

            HashMap<String, String> searchResult;
            //searchResult = getSearchResult(); // This is for a real search, currently just testing.
            searchResult = new HashMap<String, String>();
            String testurl;
            testurl = s_HipertyTipperty + "eprints.whiterose.ac.uk/87869/1/Brockway%20et%20al%202015%20China%20energy%20efficiency%20and%20decomposition.pdf";
            String testtitle;
            testtitle = "Download - White Rose Research Online";
            searchResult.put(testurl, testtitle);

            File pdfFile;
            pdfFile = new File(outDir,
                    "test.pdf");
            //String s = formatPDF(pdfFile);

            FileInputStream fis;
            fis = new FileInputStream(pdfFile);
            ParsePDF.parse(
                    pdfFile,
                    s_UniversityOfLeedsDataCiteDOIPrefix,
                    fis);
            fis.close();

            if (!searchResult.isEmpty()) {
                filterSearchResult(searchResult);
                HashMap<String, String> documents;
                documents = getDocuments(searchResult);
                boolean restart = false;
                getData(restart);
            } else {
                System.out.println("No search results for " + search);
            }
            //}
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } catch (Error e) {
            e.printStackTrace(System.err);
        }
    }

    public void filterSearchResult(HashMap<String, String> searchResult) {
        HashSet<String> keep;
        keep = new HashSet<String>();
        Iterator<String> ite;
        ite = searchResult.keySet().iterator();
        while (ite.hasNext()) {
            s_URL = ite.next();
            if (s_URL.contains("archive.researchdata.leeds.ac.uk")) {
                System.out.println("Not parsing search result from " + s_URL);
            } else {
                keep.add(s_URL);
            }
        }
        searchResult.keySet().retainAll(keep);
    }

    public HashMap<String, String> getDocuments(
            HashMap<String, String> searchResult) {
        HashMap<String, String> result;
        result = new HashMap<String, String>();
        Iterator<String> ite2;
        Iterator<String> ite;
        ite = searchResult.keySet().iterator();
        while (ite.hasNext()) {
            s_URL = ite.next();
            TreeSet<String> HTML;
            HTML = getAndFormatDocument();
            ite2 = HTML.iterator();
            while (ite2.hasNext()) {
                System.out.println(ite2.next());
            }
            //result.put(s_URL, HTML);
            result.put(s_URL, "");
        }
        return result;
    }

    public HashMap<String, String> getSearchResult() {
        HashMap<String, String> result;
        result = new HashMap<String, String>();
        try {
            Elements links;
            links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g>.r>a");
            for (Element link : links) {
                String title = link.text();
                String url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
                url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
                if (!url.startsWith("http")) {
                    continue; // Ads/news/etc.
                }
                System.out.println("Title: " + title);
                System.out.println("URL: " + url);
                result.put(url, title);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Scraper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Scraper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public void getData(
            boolean restart)
            throws Exception {
        // Initialisation
        //executorService = Executors.newCachedThreadPool();
        //int nThreads = 1 + (23 * 3) + (23 * 23 * 2); 
        int nThreads = 1;//256; 128;
        executorService = Executors.newFixedThreadPool(nThreads);
        HashSet<Future> futures = new HashSet<Future>();
        futures.addAll(getResult(restart));
        // Wait for results then shutdown executorService
        Generic_Execution.shutdownExecutorService(
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
        Generic_Execution.shutdownExecutorService(
                getExecutorService(),
                futures,
                this,
                100000L,
                10L);
    }

    private HashSet<Future> getResult(boolean restart)
            throws IOException {
        HashSet<Future> result = new HashSet<Future>();
        Run run = new Run(
                this, restart);
        result.add(getExecutorService().submit(run));
        return result;
    }

//    @Deprecated
//    public String getLowerCase(String upperCase) {
//        return Generic_StaticString.getLowerCase(upperCase);
//    }
    public void writeHouseprices(
            PrintWriter pw) {
        TreeSet<String> prices = getAndFormatDocument();
        Iterator aIterator = prices.iterator();
        while (aIterator.hasNext()) {
            pw.write((String) aIterator.next());
            pw.println();
        }
        pw.flush();
        //System.out.println( "Done " + _URLString );
    }

    /**
     * @param addressAdditionalPropertyDetails
     * @param tSecondPartOfPostcode
     * @param tFirstPartOfPostcode
     * @return number of records
     */
    public int writeHouseprices(
            PrintWriter outPW,
            PrintWriter logPW,
            PrintWriter sharedLogPW) {
        TreeSet<String> prices = getAndFormatDocument();
//        sharedLogPW.print('.');
//        sharedLogPW.flush();
        if (prices.isEmpty()) {
            logPW.println(s_URL + " 0 Records");
            logPW.flush();
            return 0;
        } else {
            Iterator aIterator = prices.iterator();
            while (aIterator.hasNext()) {
                outPW.write((String) aIterator.next());
                outPW.println();
            }
            outPW.flush();
            logPW.println(s_URL + " " + prices.size() + " Records");
            return prices.size();
        }
    }

    /**
     * @param logPW
     * @param sharedLogPW
     */
    public static void updateLog(
            PrintWriter logPW,
            PrintWriter sharedLogPW) {
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
        } catch (Exception e) {
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

    public TreeSet<String> getAndFormatDocument() {
        TreeSet<String> result = new TreeSet<String>();
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
                String message = s_URL + " connection.getResponseCode() "
                        + responseCode
                        + "see http://en.wikipedia.org/wiki/List_of_HTTP_status_codes";
                if (responseCode == 301 || responseCode == 302 || responseCode == 303
                        || responseCode == 403) {
                    message += "and http://en.wikipedia.org/wiki/HTTP_";
                    message += Integer.toString(responseCode);
                }
                throw new Error(message);
            }
            Map<String, List<String>> headerFields;
            headerFields = connection.getHeaderFields();
            if (!headerFields.isEmpty()) {
                System.out.println("<HeaderFields>");
                Iterator<String> ite;
                ite = headerFields.keySet().iterator();
                String key;
                String value2;
                List<String> value;
                while (ite.hasNext()) {
                    key = ite.next();
                    System.out.println("<HeaderKey>");
                    System.out.println("key " + key);
                    value = headerFields.get(key);
                    if (!value.isEmpty()) {
                        System.out.println("<HeaderValues>");
                        Iterator<String> ite2;
                        ite2 = value.iterator();
                        while (ite2.hasNext()) {
                            value2 = ite2.next();
                            System.out.println(value2);
                        }
                        System.out.println("</HeaderValues>");
                    }
                    System.out.println("</HeaderKey>");
                }
                System.out.println("</HeaderFields>");
            }
            String contentEncoding;
            contentEncoding = connection.getContentEncoding();
            System.out.println("contentEncoding " + contentEncoding);
            long contentLength;
            contentLength = connection.getContentLengthLong();
            System.out.println("contentLength " + contentLength);
            String contentType;
            contentType = connection.getContentType();
            System.out.println("contentType " + contentType);
            Object content;
            content = connection.getContent();
            System.out.println("contentClass " + content.getClass());

            InputStream inputStream;
            inputStream = connection.getInputStream();

            if (contentType.equalsIgnoreCase("application/pdf")) {
                File pdfFile;
                pdfFile = new File(outDir,
                        "test.pdf");

                FileOutputStream outputStream = new FileOutputStream(pdfFile);
                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                FileInputStream fis;
                fis = new FileInputStream(pdfFile);

                String s = formatPDF(pdfFile);

                if (s.contains(s_UniversityOfLeedsDataCiteDOIPrefix)) {
                    System.out.println("Paper contains " + s_UniversityOfLeedsDataCiteDOIPrefix);
                } else {
                    System.out.println("Paper does not contain " + s_UniversityOfLeedsDataCiteDOIPrefix);
                }
//               ParsePDF.parse(
//                       pdfFile,
//                       s_UniversityOfLeedsDataCiteDOIPrefix,
//                       fis);

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
        } catch (Exception e) {
            //e.printStackTrace(System.err);
        }
        return result;
    }

    public String formatPDF(File pdfFile) throws FileNotFoundException, IOException {
        String result;
        result = ParsePDF.parseToString(pdfFile);

        if (result.contains(s_UniversityOfLeedsDataCiteDOIPrefix)) {
            System.out.println("Paper contains " + s_UniversityOfLeedsDataCiteDOIPrefix);
            if (result.contains(s_DOI)) {
                System.out.println("Paper contains " + s_DOI);
                if (result.contains(s_DOIWithResolver)) {
                    System.out.println("Paper contains " + s_DOIWithResolver);
                }
            }
        } else {
            System.out.println("Paper does not contain " + s_UniversityOfLeedsDataCiteDOIPrefix);
        }
        return result;
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
        } catch (Exception e) {
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

}
