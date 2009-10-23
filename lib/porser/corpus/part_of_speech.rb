module Porser
  module Corpus
    class PartOfSpeech
      attr_reader :tag, :word
  
      def initialize(tag, lema, word)
        @tag  = tag
        @lema = lema
        @word = word
      end
    end
  end
end