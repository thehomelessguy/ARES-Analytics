import os
import re
import glob

def find_kt_files(base_dir):
    files = []
    for root, _, filenames in os.walk(base_dir):
        if 'build' in root or '.gradle' in root or '.idea' in root:
            continue
        for f in filenames:
            if f.endswith('.kt'):
                files.append(os.path.join(root, f))
    return files

def add_kdoc_if_missing(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    changed = False

    # Regex to match a declaration. 
    # In Kotlin, default is public. We should match:
    # (public )?(class|interface|object|enum class|fun|val|var) [Name]
    # We should ignore override, private, protected, internal.
    
    decl_pattern = re.compile(r'^(\s*)(?:public\s+)?(class|interface|object|enum\s+class|fun|val|var)\s+([a-zA-Z0-9_]+)')
    ignore_modifiers = ['private', 'protected', 'internal', 'override']

    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # skip annotations
        if stripped.startswith('@'):
            new_lines.append(line)
            i += 1
            continue

        # check if it is a declaration
        match = decl_pattern.match(line)
        has_ignore = any(mod in line for mod in ignore_modifiers)
        
        # To be safe, let's just do a simple heuristic
        if match and not has_ignore:
            indent = match.group(1)
            decl_type = match.group(2).replace(' ', '')
            name = match.group(3)

            # check if previous lines have kdoc
            has_kdoc = False
            for j in range(len(new_lines)-1, -1, -1):
                prev_line = new_lines[j].strip()
                if prev_line == '':
                    continue
                if prev_line.startswith('@'):
                    continue
                if prev_line.endswith('*/'):
                    has_kdoc = True
                break

            if not has_kdoc:
                # Add kdoc
                kdoc = [
                    f"{indent}/**",
                    f"{indent} * {name} {decl_type}.",
                    f"{indent} */"
                ]
                new_lines.extend(kdoc)
                changed = True
        
        new_lines.append(line)
        i += 1

    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
        return True
    return False

if __name__ == '__main__':
    base_dirs = ['app/src', 'gateway/src', 'shared/src']
    updated = []
    checked = 0
    for d in base_dirs:
        kt_files = find_kt_files(d)
        for kt in kt_files:
            checked += 1
            if add_kdoc_if_missing(kt):
                updated.append(kt)
    
    print(f"Checked {checked} files.")
    print(f"Updated {len(updated)} files.")
    for u in updated:
        print("Updated:", u)
