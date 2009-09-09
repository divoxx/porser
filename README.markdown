= Porser

== Project Description

Porser is a Portuguese language probabilistic parser based on top of Dan Bikel's parser.

== Project Structure

  + corpus/           # Folder that contains all corpus and subsets of it
   |- originals/      # The complete corpus in it's original format
   |- pre-processed   # The complete corpus pre-processed and ready to be used for selections
   |- selections/     # The corpus splitted in selections for train, dev and test
  + ext/              # All custom packages and custom extensions for Bikel's parser
   |- src/            # Java package extensions
   |- settings/       # Custom settings for the parser
  + lib/              # Libraries used in the project such as filters for processing corpus and building selections
   |- filters/        # Filters to be run over a corpora to process it
  - scripts/          # Executable scripts
  + vendor/           # Third-part stuff
   |- dbparser/       # Bikel's parser

== Authors

Rodrigo Kochenburger <divoxx at gmail dot com>
Marglon Lopes 
