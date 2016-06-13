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

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Class for formatting postcodes of the an_naa format.
 */
public class Run extends AbstractRun {

    public Run(
            Scraper s,
            boolean restart) {
        init(s, restart);
    }

    @Override
    public void run() {
        if (restart == false) {
            formatNew();
        } else {
            // Initialise output files
            String filenamepart = "f";
            String[] DOIForRestart = getDOIForRestart(filenamepart);
            if (DOIForRestart == null) {
                formatNew();
            } else {
                if (DOIForRestart[0] != null) {
                    String a0Restart = DOIForRestart[0].substring(0, 1);
                    initialiseOutputs(filenamepart);
                    // Process
                    int n0;
                    int _int0;
                    int counter = 0;
                    int numberOfHousepriceRecords = 0;
                    int numberOfPostcodesWithHousepriceRecords = 0;
                    boolean a0Restarter = false;
                    boolean n0Restarter = false;
                    boolean secondPartPostcodeRestarter = false;
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
                                    if (s.isReturningOutcode()) {
                                                _int0 = s.writeHouseprices(
                                                        outPR,
                                                        logPR,
                                                        sharedLogPR);
                                                counter++;
                                                numberOfHousepriceRecords += _int0;
                                                if (_int0 > 0) {
                                                    numberOfPostcodesWithHousepriceRecords++;
                                                }
                                                } else {
                                        Scraper.updateLog(
                                                logPR,
                                                sharedLogPR);
                                    }
                                }
                            }
                        }
//                        System.out.println(getReportString(
//                                counter,
//                                numberOfHousepriceRecords,
//                                numberOfPostcodesWithHousepriceRecords));
                    // Final reporting
                    finalise(counter, numberOfHousepriceRecords, numberOfPostcodesWithHousepriceRecords);
                }
            }
        }
    }

    private void formatNew() {
        // Initialise output files
        String filenamepart = "f";
        initialiseOutputs(filenamepart);
        // Process
        int n0;
        int _int0;
        int counter = 0;
       int numberOfDOIRecords = 0;
            int numberOfDOIRecordsWithLinkBackPublications = 0;
        for (n0 = 0; n0 < 10; n0++) {
                 checkRequestRate();
                if (s.isReturningOutcode()) {
                         _int0 = s.writeHouseprices(
                                outPR,
                                logPR,
                                sharedLogPR);
                        counter++;
                        numberOfDOIRecords += _int0;
                        if (_int0 > 0) {
                            numberOfDOIRecordsWithLinkBackPublications++;
                        }
                } else {
                    s.updateLog(
                            logPR,
                            sharedLogPR);
            }
//            System.out.println(getReportString(
//                    counter,
//                    numberOfHousepriceRecords,
//                    numberOfPostcodesWithHousepriceRecords));
        }
        // Final reporting
        finalise(counter, numberOfDOIRecords, numberOfDOIRecordsWithLinkBackPublications);
    }
}
