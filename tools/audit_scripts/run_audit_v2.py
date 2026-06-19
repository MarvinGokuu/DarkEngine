import os
import re

SRC_DIR = r"C:\Users\theca\Documents\GitHub\DarkEngine\src"
REPORT_PATH = r"C:\Users\theca\.gemini\antigravity-ide\brain\0c224870-e794-4005-b4e2-9a4f64d59770\artifacts\full_audit_report.md"

def audit_files():
    report = ["# DarkEngine Full Static Audit Report\n\n"]
    report.append("This is an automated static analysis checking for Mechanical Sympathy transgressions across all 102 `.java` files.\n\n")
    
    rules = {
        "Zero-GC (new allocation)": re.compile(r'\bnew\s+[A-Z][a-zA-Z0-9_]*\s*\('),
        "Zero-GC (String concat)": re.compile(r'".*"\s*\+.*|.*\+\s*".*"'),
        "I/O Blocking (System.out/err)": re.compile(r'System\.(out|err)\.print'),
        "FPU Overhead (float/double usage)": re.compile(r'\b(float|double)\b'),
        "Modulo instead of Bitwise": re.compile(r'\S+\s*%\s*\S+')
    }
    
    total_files = 0
    total_issues = 0
    
    for root, _, files in os.walk(SRC_DIR):
        for f in files:
            if f.endswith('.java'):
                total_files += 1
                filepath = os.path.join(root, f)
                rel_path = os.path.relpath(filepath, SRC_DIR)
                
                with open(filepath, 'r', encoding='utf-8', errors='ignore') as java_file:
                    lines = java_file.readlines()
                
                file_issues = []
                for idx, line in enumerate(lines):
                    line_stripped = line.strip()
                    if line_stripped.startswith("//") or line_stripped.startswith("*"):
                        continue # Skip comments
                    
                    for rule_name, pattern in rules.items():
                        if pattern.search(line_stripped):
                            file_issues.append((idx + 1, rule_name, line_stripped))
                            total_issues += 1
                
                if file_issues:
                    report.append(f"## 📁 {rel_path}\n")
                    report.append("| Line | Transgression | Code Snippet |\n")
                    report.append("|------|---------------|--------------|\n")
                    for line_num, rule_name, snippet in file_issues:
                        # truncate snippet if too long
                        snip = snippet[:80] + "..." if len(snippet) > 80 else snippet
                        report.append(f"| {line_num} | **{rule_name}** | `{snip}` |\n")
                    report.append("\n")

    report.insert(1, f"**Summary**: Audited {total_files} files. Found {total_issues} potential issues.\n\n")
    
    # Ensure artifacts directory exists
    os.makedirs(os.path.dirname(REPORT_PATH), exist_ok=True)
    with open(REPORT_PATH, 'w', encoding='utf-8') as out_file:
        out_file.write("".join(report))

if __name__ == "__main__":
    audit_files()
