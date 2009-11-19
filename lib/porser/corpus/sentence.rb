module Porser
  module Corpus
    def self.Sentence(arg)
      case arg
      when Sentence
        arg
      when String
        Sentence.parse(arg)
      when Category, PartOfSpeech
        Sentence.new(arg)
      else
        raise ArgumentError, "can't convert #{arg.class} to #{Sentence}"
      end
    end
    
    class Sentence
      attr_reader :root_node, :part_of_speeches
      
      def self.parse(string)
        self.new(SentenceParser.new.parse(string))
      end
      
      def initialize(root_node)
        @root_node        = root_node
        @indexes          = {}
        @part_of_speeches = []
        index!
      end
      
      def [](*index)
        @indexes[index]
      end

      def each_pos(&block)
        @part_of_speeches.each_with_index(&block)
      end
      
      def each_node(&block)
        yield(@root_node, [0])
        @root_node.each_node([0], &block)
      end
      
      def pretty_string
        @root_node.pretty_string
      end
      
      def to_s
        @root_node.to_s
      end
      
    private
      def index!        
        each_node do |child, index|
          @indexes[index] = child
          
          if child.is_a?(PartOfSpeech)
            @part_of_speeches << child
          end
        end
      end
    end
  end
end