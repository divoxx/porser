module Porser
  module Performance
    class PartOfSpeechConfusionMatrix < ConfusionMatrix
      def account(gold_sentence, parsed_sentence)
        Corpus::Sentence(gold_sentence).each do |node, range|
          if node.is_a?(Corpus::PartOfSpeech)
            expected_tag = node.tag
            result_tag   = Corpus::Sentence(parsed_sentence)[range][0].tag
            store(expected_tag, result_tag)
          end
        end
      end
    end
  end
end