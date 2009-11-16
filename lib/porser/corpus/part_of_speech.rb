module Porser
  module Corpus
    class PartOfSpeech
      attr_accessor :tag, :word, :extra
  
      def initialize(tag, word, extra = {})
        @tag   = tag
        @word  = word
        @extra = extra
      end
      
      def each(index = 0, &block)
        yield(self, index..(index + 1))
        index + 1
      end
      
      def pretty_string(level = 0)
        "\n" + ("  " * level) + to_s
      end
      
      def clean_string
        word
      end
      
      def to_s
        "(#{tag} #{word})"
      end
    end
  end
end