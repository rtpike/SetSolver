

**SetSolver** is a simple Android app uses OpenCV to find "sets" in the card game [Set] (http://www.setgame.com/set).

[Set Wikipedia page] (https://en.wikipedia.org/wiki/Set_(game))

## Details

SetSolver uses OpenCV and the phones camera to to identify sets and outlines the cards cards green. 
Tap on the screen to freze the image.  Tap again to resume real-time camera capture.

### Debug features ###

- **Inspect Cards**: Inspect detected cards individually.  Swipe right or left to see next card. 
- **Show Debug info**: overlay debug info over the image
  - Cards outlined **red** can not be identified
  - Cards outlined in **blue** are identified
  - Debug text: (card #), # of shapes, color, fill, shape type.  
    - Example: "(0)1:P:S:O" - Card contains One Purple Solid Oval

## Dependencies
OpenCV 3.2.0 - OpenCV Manager can be installed via the google app store.

## Know Issues ##

* Cards can't be overlapping and need to be on a high contrast background
* Sharp shadows or poor lighting can cause the cards to be misidentified







