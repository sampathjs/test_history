export const getPreviousVersionNumbers = (currentVersion?: number) => {
  if (!currentVersion) {
    return;
  }

  return Array(currentVersion)
    .fill(currentVersion)
    .map((_, index) => currentVersion - index);
};
