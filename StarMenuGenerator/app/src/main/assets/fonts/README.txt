OPTIONAL CARD FONTS
===================
The app works offline and, as delivered, draws the menu card with the phone's built-in serif /
sans-serif fonts (these include Malayalam). The original web project used three Google fonts.
To get exactly those looks, download them (they are free, SIL Open Font License) and copy the
files into THIS folder using EXACTLY these names, then rebuild:

  Elegant      PlayfairDisplay-Regular.ttf  PlayfairDisplay-Bold.ttf  PlayfairDisplay-Italic.ttf  PlayfairDisplay-BoldItalic.ttf
  Clean        DMSans-Regular.ttf           DMSans-Bold.ttf           DMSans-Italic.ttf           DMSans-BoldItalic.ttf
  Traditional  NotoSerifMalayalam-Regular.ttf  NotoSerifMalayalam-Bold.ttf

Any file that is missing is simply replaced by the system font (bold / italic are synthesised),
so you can add only the ones you want. No code change is needed.
