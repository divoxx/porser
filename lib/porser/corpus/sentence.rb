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
      end
      
      def [](lookup_range)
        @root_node[lookup_range]
      end
      
      def each(&block)
        @root_node.each(&block)
      end
      
      def each_range(&block)
        @root_node.each_range(&block)
      end
      
      def pretty_string
        @root_node.pretty_string
      end
      
      def to_s
        @root_node.to_s
      end
    end
  end
end