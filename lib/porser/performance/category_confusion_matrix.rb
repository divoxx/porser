module Porser
  module Performance
    class CategoryConfusionMatrix < ConfusionMatrix
      def account(gold_sentence, parsed_sentence)
        gold_ranges   = Corpus::Sentence(gold_sentence).tag_ranges
        parsed_ranges = Corpus::Sentence(parsed_sentence).tag_ranges
        gold_index    = 0
        parsed_index  = 0
        
        
        STDERR.puts Corpus::Sentence(gold_sentence).clean_string
        STDERR.puts ""
        STDERR.puts "Gold Sentence:"
        STDERR.puts gold_ranges.inspect
        STDERR.puts ""
        STDERR.puts "Parsed Sentence:"
        STDERR.puts parsed_ranges.inspect
        STDERR.puts "---"
        
        while gold = gold_ranges[gold_index] and parsed = parsed_ranges[parsed_index]
          gold_range, gold_tag     = gold
          parsed_range, parsed_tag = parsed
          
          if gold_range == parsed_range 
            sub_index = parsed_index
            
            while sub = parsed_ranges[sub_index]
              sub_range, sub_tag = sub
              break unless sub_range == gold_range
              
              if gold_tag == sub_tag
                store(gold_tag, sub_tag)
                parsed_ranges.delete([sub_range, sub_tag])
                found = true
              end
              
              sub_index += 1
            end
            
            unless found
              store(gold_tag, parsed_tag)
              parsed_index += 1
            end
              
            gold_index += 1
          elsif parsed_range.nil? || gold_range < parsed_range
            store(gold_tag, "#NF#")
            gold_index += 1
          elsif gold_range.nil? || parsed_range < gold_range
            store("#NF#", parsed_tag)
            parsed_index += 1
          end
        end
      end
    end
  end
end