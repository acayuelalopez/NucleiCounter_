# Nuclei Counter Plugin for ImageJ

**Nuclei Counter** is a plugin for ImageJ designed to facilitate the counting of cell nuclei in microscopy images through an indefinite number of blocks. It provides a graphical interface for selecting, processing, and analyzing regions of interest (ROIs), focusing on the quantification of nuclei within specified areas.

## Main Features

- Load and preview multiple images from a directory.
- Select and process images to identify and count cell nuclei.
- Manual ROI selection and editing.
- Automatic thresholding and morphological operations.
- Export results in CSV format.
- Save processed images as TIFF files.

## Requirements

- Java 8 or higher.
- ImageJ (preferably the Fiji distribution).
- Apache POI library for Excel export.

## Installation

1. Compile or download the JAR file of the plugin.
2. Place it in the `plugins` folder of ImageJ/Fiji.
3. Restart ImageJ.
4. Access the plugin via `Plugins > Nuclei Counter`.

## Workflow

1. Select a directory of images.
2. Define a region of interest (ROI) with the rectangle tool.
3. Process the image to identify and count cell nuclei.
4. View and adjust the ROIs if necessary.
5. Export the results and save the processed images.

## Outputs

- CSV file with quantitative data per image.
- TIFF images of the masks and generated montages.
- Individual and total results tables.

## Application

This plugin is useful for researchers in cell biology and histology, enabling semi-automated quantification of cell nuclei in microscopy images.

## License

Distributed under the MIT License.

