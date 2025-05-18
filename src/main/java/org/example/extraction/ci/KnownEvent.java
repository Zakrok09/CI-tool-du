package org.example.extraction.ci;

/**
 * @link <a href="https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows">Workflow triggers from GitHub</a>
 */
public enum KnownEvent {
    BRANCH_PROTECTION_RULE,
    CHECK_RUN,
    CHECK_SUITE,
    CREATE,
    DELETE,
    DEPLOYMENT,
    DEPLOYMENT_STATUS,
    DISCUSSION,
    DISCUSSION_COMMENT,
    FORK,
    GOLLUM,
    ISSUE_COMMENT,
    ISSUES,
    LABEL,
    MERGE_GROUP,
    MILESTONE,
    PAGE_BUILD,
    PUBLIC,
    PULL_REQUEST,
    PULL_REQUEST_REVIEW,
    PULL_REQUEST_REVIEW_COMMENT,
    PULL_REQUEST_TARGET,
    PUSH,
    REGISTRY_PACKAGE,
    RELEASE,
    REPOSITORY_DISPATCH,
    SCHEDULE,
    STATUS,
    WATCH,
    WORKFLOW_CALL,
    WORKFLOW_DISPATCH,
    WORKFLOW_RUN;
}
