package org.example.extraction;

/**
 * API call catcher and cache-er.
 * This class takes care for calling and caching the GitHub API.
 */
public class APICatcher {

    /*
     * Caching Algorithm:
     * 1. Catch the call to the API before it is made
     * 2. Compute a key that's the concatenation of the parameters and endpoint of the call
     * 3. Check the database with index 'key'
     * 4. If result is empty, make the call and cache the result
     * 5. Otherwise, return the cached result
     */

}
