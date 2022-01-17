require('dotenv').config();

const { assert } = require('console');
const { camelCase } = require('lodash');
const fetch = require('node-fetch');

const API_ROOT = 'https://api.figma.com/v1';
const API_TOKEN = process.env.FIGMA_API_TOKEN;

assert(API_TOKEN, 'Figma API token not found');

async function request(url) {
  const res = await fetch(`${API_ROOT}/${url}`, {
    headers: {
      'X-Figma-Token': API_TOKEN,
    },
  });

  if (res && res.status !== 200)
    throw new Error('Could not fetch specified Figma File ID ');

  return res.json();
}

/**
 * Parse the name of a figma mode to an array path. Removes special
 * characters and and any numbering systems.
 * ie 'Typography / Heading 1' => ['typography', 'heading1']
 */
function getStylePath(style) {
  return style.name
    .replace(/^\d+./, '')
    .split(/ ?\/ ?/g)
    .map((part) => camelCase(part).replace(/\W/g, ''));
}

function extractStyles(child, fileStyles, types, fn) {
  const styles = child.styles;

  if (styles) {
    if (
      Array.isArray(types)
        ? Object.keys(styles).some((prop) => types.includes(prop))
        : styles[types]
    ) {
      fn(child, fileStyles);
    }
  }

  if (child.children) {
    child.children.forEach((child) =>
      extractStyles(child, fileStyles, types, fn)
    );
  }
}

module.exports = {
  request,
  getStylePath,
  extractStyles,
};
