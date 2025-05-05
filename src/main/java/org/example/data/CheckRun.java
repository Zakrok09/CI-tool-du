package org.example.data;

import java.io.Serializable;
import java.time.Instant;
import java.io.IOException;

import org.kohsuke.github.GHCheckRun;

public class CheckRun extends GitHubObject implements Serializable {
    /* From kohsuke.github.GHCheckRun.Conclusion
     * TODO: Maybe create an enum ourselves?
     * public static enum Conclusion {
    //     /** The action required. */
    //     ACTION_REQUIRED,
    //     /** The cancelled. */
    //     CANCELLED,
    //     /** The failure. */
    //     FAILURE,
    //     /** The neutral. */
    //     NEUTRAL,
    //     /** The skipped. */
    //     SKIPPED,
    //     /** The stale. */
    //     STALE,
    //     /** The success. */
    //     SUCCESS,
    //     /** The timed out. */
    //     TIMED_OUT,
    //     /** The unknown. */
    //     UNKNOWN;
    //  */

    public String conclusion;
    public Instant completed_at;

    public CheckRun() {}

    public CheckRun(GHCheckRun checkRun) throws IOException {
        super(checkRun);

        conclusion = checkRun.getConclusion().toString();
        completed_at = checkRun.getCompletedAt();
    }
}
