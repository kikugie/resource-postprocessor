import json
import sys
import textwrap

"""
Converts flat JSON files used for Minecraft translations to nested YAML to be used with the YAMLang plugin.
Usage: python yamlang.py <input.json> <output.yaml>

Currently doesn't support list entires from owo-lib rich translations.
"""

def apply_wrapping(key: str, value: str, indent: str) -> list[str]:
    lines = []
    split = value.splitlines()
    if len(split) > 1:
        lines.append(f'{indent}{key}: |-')
        for line in split:
            lines.append(f'{indent}  {line}')
    elif len(value) > 80:
        split = textwrap.wrap(value, width=80)
        lines.append(f'{indent}{key}: >-')
        for line in split:
            lines.append(f'{indent}  {line}')
    else:
        lines.append(f'{indent}{key}: {value}')
    return lines


def to_yaml(data: dict[str, dict | str], indent: str = '') -> list[str]:
    lines = []
    for key, value in data.items():
        if isinstance(value, str):
            lines.extend(apply_wrapping(key, value, indent))
        elif isinstance(value, dict):
            lines.append(f'{indent}{key}:')
            lines.extend(to_yaml(value, indent + '  '))
        else: lines.append(f'{indent}{key}: ${value}')
    return lines

def clean_up_map(entry: dict[str, dict | str]):
    for key, value in entry.items():
        if isinstance(value, dict):
            if len(value) == 1 and '.' in value:
                entry[key] = value['.']
            clean_up_map(value)

def nest_lang(data: dict[str, str]):
    map = {}
    for key, value in data.items():
        steps: list[str] = key.split('.')
        entry: dict = map
        for step in steps:
            if step not in entry:
                entry[step] = {}
            entry = entry[step]
        entry['.'] = value
    return map

def process_lang(data: dict[str, str]):
    nested = nest_lang(data)
    clean_up_map(nested)
    return '\n'.join(to_yaml(nested))

def main():
    args = sys.argv[1:]
    if len(args) != 2:
        print(f'Usage: python {sys.argv[0]} <input.json> <output.yaml>')
        return
    input_file = args[0]
    output_file = args[1]
    with open(input_file, 'r') as file:
        data = json.load(file)
    processed = process_lang(data)
    with open(output_file, 'w') as file:
        file.write(processed)
    print("All done, bye!")

if __name__ == '__main__':
    main()