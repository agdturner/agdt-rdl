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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.leeds.ccg.andyt.rdl.core.RDL_Environment;
import uk.ac.leeds.ccg.andyt.rdl.io.RDL_Object;

/**
 * To be extended by Run methods.
 */
public abstract class RDL_RunAbstract extends RDL_Object implements Runnable {

    protected boolean restart;
    protected RDL_Scraper scraper;
    // For convenience
    protected String s_URL;

    // Other fields
    protected File outFile;
    protected PrintWriter outPR;
    protected File logFile;
    protected PrintWriter logPR;
    protected PrintWriter sharedLogPR;

    public RDL_RunAbstract(RDL_Environment env) {
        super(env);
    }

    protected final void init(RDL_Scraper scraper, boolean restart) {
        this.scraper = scraper;
        this.s_URL = scraper.s_URL;
        this.restart = restart;
    }

    public void checkRequestRate() {
//        double timeDiffSecs = (double) (System.currentTimeMillis() - startTime) / 1000D;
//        long requests = sharedLogFile.length();
//        double requestsRate = (double) requests / (double) timeDiffSecs;
//        synchronized (this) {
//            while (requestsRate > allowedRequestsPerSecond) {
//                try {
//                    this.wait(1000);
//                    timeDiffSecs = (double) (System.currentTimeMillis() - startTime) / 1000D;
//                    requests = sharedLogFile.length();
//                    requestsRate = (double) requests / (double) timeDiffSecs;
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(AbstractRun.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
    }

    protected void initialiseOutputs(
            String filenamepart) {
        try {
            File outDirectory = scraper.getDirectory();
            outDirectory.mkdirs();
            outFile = new File(outDirectory, filenamepart + ".csv");
            logFile = new File(outDirectory, filenamepart + ".log");
            //sharedLogPR = Generic_IO.getPrintWriter(sharedLogFile, true);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            outPR = env.ge.io.getPrintWriter(outFile, restart);
            logPR = env.ge.io.getPrintWriter(logFile, restart);
        } catch (IOException ex) {
            System.err.println(ex.toString());
            Logger.getLogger(RDL_RunAbstract.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checks the log file for completed run. If run completed then null is
     * returned otherwise a String[] of length 2 is returned the first element
     * being the first part of last postcode returned, the second part being the
     * second part of the last postcode returned.
     *
     * @param filenamepart
     * @return
     */
    protected String getRestartDOI(String filenamepart) {
        String r = null;
        try {
            File outDirectory = scraper.getDirectory();
            if (!outDirectory.exists()) {
                return null;
            }
            outDirectory.mkdirs();
            logFile = new File(outDirectory, filenamepart + ".log");
            if (logFile.length() == 0L) {
                return null;
            }
            String line;
            try (BufferedReader br = env.ge.io.getBufferedReader(logFile)) {
                StreamTokenizer st = new StreamTokenizer(br);
                env.ge.io.setStreamTokenizerSyntax1(st);
                int token = st.nextToken();
                line = null;
                while (token != StreamTokenizer.TT_EOF) {
                    switch (token) {
                        case StreamTokenizer.TT_WORD:
                            line = st.sval;
                        case StreamTokenizer.TT_EOL:
                            break;
                    }
                    token = st.nextToken();
                }
            }
            return line;
        } catch (IOException ex) {
            Logger.getLogger(RDL_RunAbstract.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    protected void finalise(int counter, int nDOI, int nDOIWithLinkBackPublications) {
        // Final reporting
        System.out.println("Attempted " + counter);
        logPR.println("numberOfDOIRecords " + nDOI);
        System.out.println("numberOfDOIRecords " + nDOI);
        logPR.println("numberOfDOIRecordsWithLinkBackPublications " + nDOIWithLinkBackPublications);
        System.out.println("numberOfDOIRecordsWithLinkBackPublications " + nDOIWithLinkBackPublications);
        outPR.close();
        logPR.close();
    }
}
