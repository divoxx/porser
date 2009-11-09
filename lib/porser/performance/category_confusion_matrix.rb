module Porser
  module Performance
    class CategoryConfusionMatrix < ConfusionMatrix
      def initialize(gold_sentence, parsed_sentence)
        super()
        @gold_sentence   = Corpus::Sentence(gold_sentence)
        @parsed_sentence = Corpus::Sentence(parsed_sentence)
        compare!
      end

    private
      def compare!
        @gold_sentence.each_range do |range, expected_nodes|
          expected_nodes = expected_nodes.sort_by { |node| node.tag }
          got_nodes      = @parsed_sentence[range].sort_by { |node| node.tag }
          expected_idx   = 0
          got_idx        = 0
          stop_idx       = [expected_nodes.size, got_nodes.size].max
          
          while expected_idx < stop_idx || got_idx < stop_idx
            expected = expected_nodes[expected_idx]
            got      = got_nodes[got_idx]
            
            if expected.tag == got.tag
              expected_idx += 1
              got_idx      += 1
              store(expected.tag, got.tag)
            elsif !expected || (got && (expected.tag <=> got.tag) == -1)
              store(:not_found, got.tag)
              got += 1
            elsif !got || (expected && (expected.tag <=> got.tag) == +1)
              store(expected.tag, :not_found)
              expected += 1
            end
          end
        end
      end
    end
  end
end