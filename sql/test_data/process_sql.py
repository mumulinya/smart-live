import os
import re

dir_path = r'd:\smart-live\smart-live-Cloud\sql\test_data'

for filename in os.listdir(dir_path):
    if not filename.endswith('.sql'):
        continue
    filepath = os.path.join(dir_path, filename)
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all table names
    tables = re.findall(r'INSERT INTO `?([a-zA-Z0-9_]+)`?', content)
    unique_tables = []
    for t in tables:
        if t not in unique_tables:
            unique_tables.append(t)

    # Check if we already added TRUNCATE
    if 'TRUNCATE TABLE' in content:
        continue
        
    truncate_statements = [f'TRUNCATE TABLE `{t}`;' for t in unique_tables]
    truncate_block = '\n'.join(truncate_statements) + '\n'

    # Insert truncate block after "USE `...`;"
    use_match = re.search(r'USE `[^`]+`;\n', content)
    if use_match:
        pos = use_match.end()
        content = content[:pos] + truncate_block + content[pos:]
    else:
        # try to find the first statement maybe "SET "
        first_insert_pos = content.find('INSERT INTO')
        if first_insert_pos != -1:
             content = content[:first_insert_pos] + truncate_block + content[first_insert_pos:]

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Processed {filename}: truncated {unique_tables}')
