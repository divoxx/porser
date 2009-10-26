module Porser
  module Corpus
    class Sentence
      attr_reader :root_node
      
      def self.parse(string)
        self.new(SentenceParser.new.parse(string))
      end
      
      def initialize(root_node)
        @root_node = root_node
      end
      
      def each_node(level = 0, &block)
        yield(@root_node, level)
        @root_node.each_node(level + 1, &block)
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