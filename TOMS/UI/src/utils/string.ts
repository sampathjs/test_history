export const spitAndIncludeBySeparator = (string: string, separator: string) =>
  string
    .split(separator)
    .reduce<string[]>(
      (acc, current, index) =>
        index !== 0 ? [...acc, separator, current] : [...acc, current],
      []
    );
