import csv
import os

CSV_PATH = 'intake/included_repos.csv'

REPOS_DIR = 'repos'
MISSING_REPOS_CSV = 'missing_repos.csv'
INCLUDED_REPOS_CSV = 'included_repos.csv'

CLONES_DIR = 'clones'
MISSING_CLONES_CSV = 'missing_clones.csv'

DOC_STATS_DIR = 'sampled_commits_doc_stats'
MISSING_DOC_STATS_CSV = 'missing_doc_stats.csv'

COMMENTS_DIR = 'sampled_commits_code_comments'
MISSING_COMMENTS_CSV = 'missing_code_comments.csv'

def sanitize_repo_name(repo_name):
    return repo_name.replace('/', '_')

def main():
    missing_json = []
    included_json = []
    missing_clones = []
    missing_doc_stats = []
    missing_comments = []

    with open(CSV_PATH, newline='', encoding='utf-8') as csvfile:
        for line in csvfile:
            repo_name = line.strip()
            if not repo_name:
                continue
            sanitized = sanitize_repo_name(repo_name)
            
            json_path = os.path.join(REPOS_DIR, f'{sanitized}.json')
            if not os.path.isfile(json_path):
                missing_json.append([repo_name])
            else:
                included_json.append([repo_name])
            
            clone_path = os.path.join(CLONES_DIR, sanitized)
            if not os.path.isdir(clone_path):
                missing_clones.append([repo_name])

            doc_stats_path = os.path.join(DOC_STATS_DIR, f'{sanitized}_doc_stats.csv')
            if not os.path.isfile(doc_stats_path):
                missing_doc_stats.append([repo_name])

            comments_path = os.path.join(COMMENTS_DIR, f'{sanitized}_comments.csv')
            if not os.path.isfile(comments_path):
                missing_comments.append([repo_name])

    if len(missing_json) > 0:
        with open(MISSING_REPOS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_json)

    if len(included_json) > 0:
        with open(INCLUDED_REPOS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(included_json)

    if len(missing_clones) > 0:
        with open(MISSING_CLONES_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_clones)

    if len(missing_doc_stats) > 0:
        with open(MISSING_DOC_STATS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_doc_stats)
    
    if len(missing_comments) > 0:
        with open(MISSING_COMMENTS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_comments)

if __name__ == '__main__':
    main()