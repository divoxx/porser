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
      
      def pretty_string(level = 0)
        children_str = @children.map { |child| child.pretty_string(level + 1) }.join(" ")
        indent_str   = "  " * level
        line_break   = "\n" unless level == 0
        "#{line_break}#{indent_str}(#{@tag}#{children_str})"
      end
      
      def to_s
        children_str = @children.map { |child| child.to_s }.join(" ")
        "(#{@tag} #{children_str})"
      end
    end
  end
end