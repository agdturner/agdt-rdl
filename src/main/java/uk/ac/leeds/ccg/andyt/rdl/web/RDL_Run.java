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

public class RDL_Run extends RDL_RunAbstract {

    public RDL_Run(RDL_Scraper s, boolean restart) {
        super(s.env);
        init(s, restart);
    }

    @Override
    public void run() {
        if (restart == false) {
            formatNew();
        } else {
            // Initialise output files
            String filenamepart = "f";
            String restartDOI = getRestartDOI(filenamepart);
            if (restartDOI == null) {
                formatNew();
            } else {
                initialiseOutputs(filenamepart);
                // Process
                int n0;
                int i;
                int counter = 0;
                int nDOI = 0;
                int nDOIWithLinkBackPublications = 0;
                boolean a0Restarter = false;
                boolean n0Restarter = false;
                if (!a0Restarter) {
//                            if (a0.equalsIgnoreCase(a0Restart)) {
//                                a0Restarter = true;
//                            }
                    int debug = 1;
                } else {
                    for (n0 = 0; n0 < 10; n0++) {
                        if (!n0Restarter) {
//                                    if (n0 == n0Restart) {
//                                        n0Restarter = true;
//                                    }
                            int debug = 1;
                        } else {
                            checkRequestRate();
                            if (scraper.isReturningOutcode()) {
                                i = scraper.writeResults(outPR, logPR,
                                        sharedLogPR, scraper.useOnlyCachedFiles);
                                counter++;
                                nDOI += i;
                                if (i > 0) {
                                    nDOIWithLinkBackPublications++;
                                }
                            } else {
                                scraper.updateLog(logPR, sharedLogPR);
                            }
                        }
                    }
                }
                // Final reporting
                finalise(counter, nDOI, nDOIWithLinkBackPublications);
            }
        }
    }

    private void formatNew() {
        // Initialise output files
        String filenamepart = "f";
        initialiseOutputs(filenamepart);
        // Process
        int n0;
        int i;
        int counter = 0;
        int numberOfDOIRecords = 0;
        int nDOIWithLinkBackPublications = 0;
        for (n0 = 0; n0 < 10; n0++) {
            checkRequestRate();
            if (scraper.isReturningOutcode()) {
                i = scraper.writeResults(outPR, logPR, sharedLogPR, scraper.useOnlyCachedFiles);
                counter++;
                numberOfDOIRecords += i;
                if (i > 0) {
                    nDOIWithLinkBackPublications++;
                }
            } else {
                scraper.updateLog(logPR, sharedLogPR);
            }
        }
        // Final reporting
        finalise(counter, numberOfDOIRecords, nDOIWithLinkBackPublications);
    }
}
