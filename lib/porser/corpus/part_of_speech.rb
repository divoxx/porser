module Porser
  module Corpus
    class PartOfSpeech
      attr_reader :tag, :word
  
      def initialize(tag, lema, word)
        @tag  = tag
        @lema = lema
        @word = word
      end
      
      def pretty_string(level = 0)
        "\n" + ("  " * level) + to_s
      end
      
      def to_s
        "(#{tag} #{word})"
      end
    end
  end
end