module Porser
  module Corpus
    class SentenceTokenizer
      def initialize(string)
        @string = string
      end

      def each
        while !@string.empty?
          case @string
          when /\A[^\s()-]+/m then yield([:WORD, $&])
          when /\A[^\s]/m    then yield([$&, $&])
          when /\A\s+/m      then nil
          end
        
          @string = $'
        end
      
        yield([false, '$'])
      end
    end
  end
end