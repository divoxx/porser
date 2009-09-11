Porser
======

Project Description
-------------------

Porser is a Portuguese language probabilistic parser based on top of Dan Bikel's parser.

Project Structure
-----------------

The overall project structure is the following:

    corpus/                                                 # Folder that contains all corpus and subsets of it
    |  originals/                                           # The complete corpus in it's original format
    |  pre-processed/                                       # The complete corpus pre-processed and ready to be used for selections
    |  selections/                                          # The corpus splitted in selections for train, dev and test
    |  |  2009.09.10_13:00:00/                              # A selection of train, dev and test named with the timestamp of the creation time
    |  |  |  devel.gold.txt                                 # The development corpus with annotation
    |  |  |  devel.parseable.txt                            # The development corpus without annotations with each line wrapped in parens
    |  |  |  test.gold.txt                                  # The test corpus with annotation
    |  |  |  test.parseable.txt                             # The test corpus without annotations with each line wrapped in parens
    |  |  |  train.gold.txt                                 # The training corpus with annotations
    |  |  |  train.parseable.txt                            # The training corpus without annotations with each line wrapped in parens
    |  |  |  2009.09.10_13:00:00-verb_without_hiphens-....  # An experiment on top of the selection, with changes to corpus, settings and head-rules
    |  |  |  |  corpus.dev.gold.txt                         # The development gold corpus with filters and modifications applied
    |  |  |  |  corpus.dev.parseable.txt                    # The development parseable corpus with filters and modifications applied
    |  |  |  |  corpus.test.gold.txt                        # The test gold corpus with filters and modifications applied
    |  |  |  |  corpus.test.parseable.txt                   # The test parseable corpus with filters and modifications applied
    |  |  |  |  corpus.train.log                            # The log of the training process
    |  |  |  |  corpus.train.txt                            # The training corpus with filters applied
    |  |  |  |  head-rules.lisp                             # The head rules finding for the experiment
    |  |  |  |  score.txt                                   # The score result for the experiment
    |  |  |  |  settings.properties                         # The settings for the experiment
    |  |  |  ...                                            # More experiments
    |  |  ...                                               # More selections  
    ext/                                                    # All custom packages and custom extensions for Bikel's parser
    |  src/                                                 # Java package extensions source code
    |  build/                                               # Java package extensions compiled
    lib/                                                    # Libraries used in the project such as filters for processing corpus and building selections
    |  porser/                                              # Porser's libraries
    |  |  cli/                                              # Command line libraries, used by scripts/*
    |  |  filters/                                          # Filters to be run over a corpora to process it
    |  |  ...                                               # Other library files, mostly *.rb
    |  porser.rb                                            # Base ruby file that setups all the paths and common settings
    |  tasks/                                               # Rake tasks for the appication
    vendor/                                                 # Third-part stuff
    |  dbparser.jar                                         # Bikel's parser

Dependencies
------------

Most ruby dependencies are bundled with the project in the vendor/ folder. But some non-ruby dependencies must be installed:

* wc command (available in all unix variants)

Authors
-------

* Rodrigo Kochenburger <divoxx@gmail.com>
* Marglon Gomes Lopes <marlonglopes@gmail.com>
