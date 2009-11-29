module Porser
  module Performance
    class CategoryConfusionMatrix < ConfusionMatrix
      def account(gold_sentence, parsed_sentence)
        gold_ranges   = Corpus::Sentence(gold_sentence).tag_ranges
        parsed_ranges = Corpus::Sentence(parsed_sentence).tag_ranges
        gold_index    = 0
        parsed_index  = 0

        loop do    # mudei para or
          gold_range, gold_tag     = gold   = gold_ranges[gold_index]
          parsed_range, parsed_tag = parsed = parsed_ranges[parsed_index]

          break if gold.nil? && parsed.nil?
          
          # Mudei a ordem 
          if parsed_range.nil? || (!gold_range.nil? && gold_range < parsed_range )        # garante tb gold_range não nulo
            store(gold_tag, "#NF#")
            gold_index += 1
          elsif gold_range.nil? || parsed_range < gold_range   # certo que parsed_range não é nulo
            store("#NF#", parsed_tag)
            parsed_index += 1
        
          else
            raise "Assertion error" unless !parsed_range.nil? && !gold_range.nil? && (gold_range == parsed_range) # isto deve ser verdade aqui
        
            found = false
            if gold_tag == parsed_tag
               store(gold_tag, parsed_tag)         # NOTA: Aqui houve um acerto
               gold_index += 1
               parsed_index += 1
            elsif gold_tag < parsed_tag
               sub_index = gold_index
               while !found and sub = gold_ranges[sub_index]
                 sub_range, sub_tag = sub
                 break unless  (sub_range == parsed_range) && (sub_tag <= parsed_tag)
                 if (sub_tag == parsed_tag) 
                    store(sub_tag, parsed_tag)   # NOTA: Aqui houve um acerto
                    # Veja se pode isto abaixo
                    gold_ranges.delete_at(sub_index)
                    parsed_ranges.delete_at(parsed_index)
                    found = true
                 end
                 sub_index += 1   # if not found implicit
               end
               if !found
                   store(gold_tag, parsed_tag)    # NOTA:  Aqui houve uma troca de gold tag para parsed_tag
                   gold_index += 1
                   parsed_index += 1
               end
               # else, don´t move indices and let it go back to outhermost while
            else  # gold_tag > parsed_tag
               sub_index = parsed_index
               while !found and sub = parsed_ranges[sub_index]
                 sub_range, sub_tag = sub
                 break unless (sub_range == gold_range) and (sub_tag <= gold_tag)
                 if (sub_tag == gold_tag) 
                    store(gold_tag, sub_tag)   # NOTA: Aqui houve um acerto
                    # Veja se pode isto abaixo
                    gold_ranges.delete_at(gold_index)
                    parsed_ranges.delete_at(sub_index)
                    found = true
                 end
                 sub_index += 1   # if not found implicit
               end
               if !found
                   store(gold_tag, parsed_tag)    # NOTA:  Aqui houve uma troca de gold tag para parsed_tag
                   gold_index += 1
                   parsed_index += 1
               end
               # else, don´t move indices and let it go back to outhermost while
            end
          end
        end
        
        # gold_ranges   = Corpus::Sentence(gold_sentence).tag_ranges
        # parsed_ranges = Corpus::Sentence(parsed_sentence).tag_ranges
        # gold_index    = 0
        # parsed_index  = 0        
        # 
        # while gold = gold_ranges[gold_index] and parsed = parsed_ranges[parsed_index]
        #   gold_range, gold_tag     = gold
        #   parsed_range, parsed_tag = parsed
        #   
        #   if gold_range == parsed_range 
        #     sub_index = parsed_index
        #     
        #     while sub = parsed_ranges[sub_index]
        #       sub_range, sub_tag = sub
        #       break unless sub_range == gold_range
        #       
        #       if gold_tag == sub_tag
        #         store(gold_tag, sub_tag)
        #         parsed_ranges.delete([sub_range, sub_tag])
        #         found = true
        #       end
        #       
        #       sub_index += 1
        #     end
        #     
        #     unless found
        #       store(gold_tag, parsed_tag)
        #       parsed_index += 1
        #     end
        #       
        #     gold_index += 1
        #   elsif parsed_range.nil? || gold_range < parsed_range
        #     store(gold_tag, "#NF#")
        #     gold_index += 1
        #   elsif gold_range.nil? || parsed_range < gold_range
        #     store("#NF#", parsed_tag)
        #     parsed_index += 1
        #   end
        # end
      end
    end
  end
end