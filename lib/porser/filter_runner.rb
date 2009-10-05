module Porser
  class FilterRunner
    class Sentence < StringIO
      def each_token
        rewind
        token = ""
        while char = getc
          if char == ?(
            type  = :tag
            start_pos = pos
            from_whitespace = false
          elsif char == ?\s || char == ?)
            end_pos = pos - 2
            yield(token, type, start_pos..end_pos) unless token.blank?
            token     = ""
            from_whitespace = true
          else
            if from_whitespace
              from_whitespace = false
              type = :word
              start_pos = pos - 1
            end
            token << char.chr
          end
        end
      end
    end
    
    def initialize(*filters)
      @filters = filters.map(&:new)
    end
    
    def run(sentence_str)
      return sentence_str if @filters.empty?
      sentence     = Sentence.new(sentence_str)
      new_sentence = sentence_str.dup
      offset = 0
      
      sentence.each_token do |token, type, range|
        @filters.each { |filter| filter.respond_to?(type) ? token = filter.send(type, token) : token }
        orig_size               = range.max - range.min
        new_range               = (range.min+offset)..(range.max+offset)
        new_sentence[new_range] = token
        offset                 += token.size - orig_size - 1
      end
      
      new_sentence
    end
  end
end