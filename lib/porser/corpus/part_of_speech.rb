module Porser
  module Corpus
    class PartOfSpeech
      attr_reader :tag, :word, :extra
  
      def initialize(tag, word, extra = {})
        @tag   = tag
        @word  = word
        @extra = extra
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