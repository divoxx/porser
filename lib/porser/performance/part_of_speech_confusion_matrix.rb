module Porser
  module Performance
    class PartOfSpeechConfusionMatrix < ConfusionMatrix
      def initialize(gold_sentence, parsed_sentence)
        super()
        @gold_sentence   = Corpus::Sentence(gold_sentence)
        @parsed_sentence = Corpus::Sentence(parsed_sentence)
        compare!
      end
                  
    private
      def compare!
        @gold_sentence.each do |node, range|
          if node.is_a?(Corpus::PartOfSpeech)
            expected_tag = node.tag
            result_tag   = @parsed_sentence[range].tag
            store(expected_tag, result_tag)
          end
        end
      end
    end
  end
end