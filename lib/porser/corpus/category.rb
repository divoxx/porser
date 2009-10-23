module Porser
  module Corpus
    class Category
      attr_reader :tag, :children
      
      def initialize(tag, children = [])
        @tag      = tag
        @children = children
      end
      
      def <<(node)
        @children << node
      end
      
      def each_node(level = 0, &block)
        children.each do |child|
          yield(child, level)
          child.each_node(level + 1, &block) if child.respond_to?(:each_node)
        end
      end
    end
  end
end