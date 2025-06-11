import csv
import os

CSV_PATH = 'intake/2-projects-final.csv'

REPOS_DIR = 'repos'
MISSING_REPOS_CSV = 'missing_repos.csv'
INCLUDED_REPOS_CSV = 'included_repos.csv'

CLONES_DIR = 'clones'
MISSING_CLONES_CSV = 'missing_clones.csv'

def sanitize_repo_name(repo_name):
    return repo_name.replace('/', '_')

def main():
    missing_json = []
    included_json = []
    missing_clones = []

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

if __name__ == '__main__':
    main()