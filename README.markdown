= Porser

== Project Description

Porser is a Portuguese language probabilistic parser based on top of Dan Bikel's parser.

== Project Structure

The overall project structure is the following:

  + corpus/               # Folder that contains all corpus and subsets of it
   |- originals/          # The complete corpus in it's original format
   |- pre-processed       # The complete corpus pre-processed and ready to be used for selections
   |- selections/         # The corpus splitted in selections for train, dev and test
  + ext/                  # All custom packages and custom extensions for Bikel's parser
   |- src/                # Java package extensions
   |- settings/           # Custom settings for the parser
  + lib/                  # Libraries used in the project such as filters for processing corpus and building selections
   |- cli/                # Command line libraries, used by scripts/*
   |- filters/            # Filters to be run over a corpora to process it
   |- porser.rb           # Base ruby file that setups all the paths and common settings
  + scripts/              # Executable scripts
   |- create_selection    # Creates a corpus selection, splitting corpus in train, dev and test blocks.
  + vendor/               # Third-part stuff
   |- dbparser/           # Bikel's parser

Inside the corpus folder, the selections are going to be organized in this way:

  + 20090910130000/
   |- dev.txt
   |- test.gold.txt
   |- test.txt
   |- train.gold.txt 
   |- train.txt
   + 01-verb_without_hiphens-....
    |- head-rules.lisp
    |- settings.properties
    |- score.txt
    |- dev.txt
    |- test.gold.txt
    |- test.txt
    |- train.gold.txt 
    |- train.txt
    
== Dependencies

Most ruby dependencies are bundled with the project in the vendor/ folder. But some non-ruby dependencies must be installed:

* wc command (available in all unix variants)

== Authors

Rodrigo Kochenburger <divoxx at gmail dot com>
Marglon Lopes 
