## Figma

The project contains two scripts for importing styles and icons from Figma into React.

| Script                  |                                                                                                                                                                                                                                                                                                                        |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm run import-colors` | Imports global colours. A colour must be used in the design document in order to be detected..<br /><br />Outputs to `src/styles/theme/colors.ts`                                                                                                                                                                      |
| `npm run import-typography` | Imports global typography. A text field must be used in the design document in order to be detected..<br /><br />Outputs to `src/styles/theme/typography.ts`
<!-- | `npm run import-icons`  | Imports icons to React Components. Any object in the Figma prefixed `Icon/` will be imported. Icons are imported according to their names in Figma so long as they are prefixed with `Icon/`. <br /><br />An object labelled `Icon/Add` will result in a component being created at `src/components/Icons/AddIcon.tsx` | -->

### Running

To run these scripts yourself you will need to add 3 environment variables to your local `.env` file.

`FIGMA_API_TOKEN`

This can be generated from the Figma web interface by going to **Account Settings** and clicking the button under **Personal Access Tokens**

`FIGMA_FILE_KEY` - The file ID of the document containing the styles and icons to export. This can be grabbed from the document's URL.

`FIGMA_FILE_NODE_ID` - The file node ID of the page/nodes within the document containing the styles and icons to export. This can be grabbed from the document's URL.
