import csv
import os

CSV_PATH = 'intake/2-projects-final.csv'

REPOS_DIR = 'repos'
MISSING_REPOS_CSV = 'missing_repos.csv'

DOC_STATS_DIR = 'sampled_commits_doc_stats'
MISSING_DOC_STATS_CSV = 'missing_doc_stats.csv'

COMMENTS_DIR = 'sampled_commits_code_comments'
MISSING_COMMENTS_CSV = 'missing_code_comments.csv'

def sanitize_repo_name(repo_name):
    return repo_name.replace('/', '_')

def get_repo_names():
    repo_names = set()
    with open(CSV_PATH, newline='', encoding='utf-8') as csvfile:
        for line in csvfile:
            repo_names.add(sanitize_repo_name(line.strip()))
    return repo_names

def check_extra_files(directories):
    valid_repos = get_repo_names()
    suffixes = [".json", "_doc_stats.csv", "_comments.csv"]

    for directory in directories:
        print(f"\nChecking directory: {directory}")
        if not os.path.isdir(directory):
            print(f"Directory {directory} does not exist.")
            continue

        extra_files = []
        for filename in os.listdir(directory):
            repo_name = filename
            for suffix in suffixes:
                if repo_name.endswith(suffix):
                    repo_name = repo_name[: -len(suffix)]
                    break
            if repo_name not in valid_repos:
                extra_files.append(filename)

        if extra_files:
            print("Extra files found:")
            for f in extra_files:
                print(f"  {f}")
        else:
            print("No extra files found.")

def check_missing():
    missing_json = []
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

    if len(missing_doc_stats) > 0:
        with open(MISSING_DOC_STATS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_doc_stats)
    
    if len(missing_comments) > 0:
        with open(MISSING_COMMENTS_CSV, 'w', newline='', encoding='utf-8') as outfile:
            writer = csv.writer(outfile)
            writer.writerows(missing_comments)

if __name__ == '__main__':
    check_extra_files([REPOS_DIR, DOC_STATS_DIR, COMMENTS_DIR])
    check_missing()