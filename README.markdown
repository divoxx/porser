Porser
======

Project Description
-------------------

Porser is a Portuguese language probabilistic parser based on top of Dan Bikel's parser.

Project Structure
-----------------

The overall project structure is the following:

    corpus/                               # Folder that contains all corpus and subsets of it
    |  originals/                         # The complete corpus in it's original format
    |  pre-processed/                     # The complete corpus pre-processed and ready to be used for selections
    |  selections/                        # The corpus splitted in selections for train, dev and test
    |  |  20090910130000/                 # A selection of train, dev and test named with the timestamp of the creation time
    |  |  |  devel.gold.txt               # The gold file for the development corpus
    |  |  |  devel.parseable.txt          # The development corpus without annotation with each line wrapped in parens
    |  |  |  test.gold.txt                # The gold file for the test corpus
    |  |  |  test.txt                     # The test corpus without annotation with each line wrapped in parens
    |  |  |  train.txt                    # The training corpus
    |  |  |  01-verb_without_hiphens-.... # An experiment on top of the selection, with changes to corpus, settings and head-rules
    |  |  |  |  head-rules.lisp           # The head rules finding for the experiment
    |  |  |  |  settings.properties       # The settings for the experiment
    |  |  |  |  score.txt                 # The score result for the experiment
    |  |  |  |  dev.gold.txt              # The development gold corpus with filters and modifications applied
    |  |  |  |  dev.parseable.txt         # The development parseable corpus with filters and modifications applied
    |  |  |  |  test.gold.txt             # The test gold corpus with filters and modifications applied
    |  |  |  |  test.parseable.txt        # The test parseable corpus with filters and modifications applied
    |  |  |  |  train.log                 # The log of the training process
    |  |  |  |  train.txt                 # The training corpus with filters applied
    |  |  |  ...                          # More experiments
    |  |  ...                             # More selections  
    ext/                                  # All custom packages and custom extensions for Bikel's parser
    |  src/                               # Java package extensions source code
    |  build/                             # Java package extensions compiled
    lib/                                  # Libraries used in the project such as filters for processing corpus and building selections
    |  cli/                               # Command line libraries, used by scripts/*
    |  filters/                           # Filters to be run over a corpora to process it
    |  porser.rb                          # Base ruby file that setups all the paths and common settings
    vendor/                               # Third-part stuff
    |  dbparser.jar                       # Bikel's parser

== Dependencies

Most ruby dependencies are bundled with the project in the vendor/ folder. But some non-ruby dependencies must be installed:

* wc command (available in all unix variants)

== Authors

* Rodrigo Kochenburger <divoxx@gmail.com>
* Marglon Gomes Lopes <marlonglopes@gmail.com>
