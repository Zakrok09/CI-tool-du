package org.example.fetching;

public class FetchSettings {
    public static boolean set = false;
    public static boolean branches;
    public static boolean commits;
    public static boolean documentationStats;
    public static boolean pullRequests;
    public static boolean issues;
    public static boolean issueComments;
    public static boolean releases;
    public static boolean deployments;
    public static boolean users;

    public static void All() {
        FetchSettings.set = true;
        FetchSettings.branches = true;
        FetchSettings.commits = true;
        FetchSettings.documentationStats = false;
        FetchSettings.pullRequests = true;
        FetchSettings.issues = true;
        FetchSettings.issueComments = true;
        FetchSettings.releases = true;
        FetchSettings.deployments = true;
        FetchSettings.users = true;
    }

    public static void None() {
        FetchSettings.set = true;
        FetchSettings.branches = false;
        FetchSettings.commits = false;
        FetchSettings.documentationStats = false;
        FetchSettings.pullRequests = false;
        FetchSettings.issues = false;
        FetchSettings.issueComments = false;
        FetchSettings.releases = false;
        FetchSettings.deployments = false;
        FetchSettings.users = false;
    }

    public static void Daniel() {
        FetchSettings.set = true;
        FetchSettings.branches = false;
        FetchSettings.commits = true;
        FetchSettings.documentationStats = true;
        FetchSettings.pullRequests = false;
        FetchSettings.issues = true;
        FetchSettings.issueComments = false;
        FetchSettings.releases = true;
        FetchSettings.deployments = false;
        FetchSettings.users = false;
    }
}
