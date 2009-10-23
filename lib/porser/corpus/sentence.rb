module Porser
  module Corpus
    class Sentence
      def initialize(root_node)
        @root_node = root_node
      end
      
      def each_node(level = 0, &block)
        yield(@root_node, level)
        @root_node.each_node(level + 1, &block)
      end
    end
  end
end