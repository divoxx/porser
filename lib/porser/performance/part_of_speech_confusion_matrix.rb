module Porser
  module Performance
    class PartOfSpeechConfusionMatrix
      attr_reader :matrix
      
      def initialize(gold_sentence, parsed_sentence)
        @gold_sentence   = Corpus::Sentence(gold_sentence)
        @parsed_sentence = Corpus::Sentence(parsed_sentence)
        @matrix          = Hash.new { |h,k| h[k] = Hash.new { |h,k| h[k] = 0 }}
        compare!
      end
      
    private
      def compare!
        @parsed_sentence.each_pos do |pos, idx|
          @matrix[pos.tag][@gold_sentence.part_of_speeches[idx].tag] += 1
        end
      end
    end
  end
end